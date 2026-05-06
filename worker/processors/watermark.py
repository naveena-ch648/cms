"""Watermark processor — applies text watermark to images and PDFs for shared content."""

import io
import tempfile

import boto3
import pymysql
from PIL import Image, ImageDraw, ImageFont

from config import Config

WATERMARK_OPACITY = 80  # 0-255
WATERMARK_ANGLE = -45
WATERMARK_FONT_SIZE = 36


def get_s3_client():
    return boto3.client(
        "s3",
        endpoint_url=Config.MINIO_ENDPOINT,
        aws_access_key_id=Config.MINIO_ACCESS_KEY,
        aws_secret_access_key=Config.MINIO_SECRET_KEY,
        region_name=Config.MINIO_REGION,
    )


def get_db_connection():
    return pymysql.connect(
        host=Config.MYSQL_HOST,
        port=Config.MYSQL_PORT,
        user=Config.MYSQL_USER,
        password=Config.MYSQL_PASSWORD,
        database=Config.MYSQL_DATABASE,
        charset="utf8mb4",
    )


def process_watermark(file_id: str, link_token: str, org_id: str):
    """Apply watermark to an image file and store the watermarked version in MinIO."""
    conn = get_db_connection()
    try:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT storage_bucket, storage_key, mime_type FROM files WHERE uuid = %s",
                (file_id,),
            )
            row = cur.fetchone()
            if not row:
                print(f"File {file_id} not found in database")
                return

            bucket, key, mime_type = row

        if not mime_type or not mime_type.startswith("image/"):
            print(f"Watermark only supports images, skipping mime_type={mime_type}")
            return

        s3 = get_s3_client()

        # Download original image
        response = s3.get_object(Bucket=bucket, Key=key)
        image_data = response["Body"].read()

        # Apply watermark
        watermarked = apply_watermark(image_data, link_token)

        # Upload watermarked version
        watermark_key = f"watermarked/{link_token}/{key.split('/')[-1]}"
        s3.put_object(
            Bucket=bucket,
            Key=watermark_key,
            Body=watermarked,
            ContentType=mime_type,
        )

        # Update shared_links with watermarked key (optional metadata)
        print(f"Watermarked file stored at {bucket}/{watermark_key}")

    finally:
        conn.close()


def apply_watermark(image_data: bytes, watermark_text: str) -> bytes:
    """Apply a diagonal text watermark to an image."""
    img = Image.open(io.BytesIO(image_data)).convert("RGBA")
    width, height = img.size

    # Create watermark overlay
    watermark = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(watermark)

    # Use default font with size
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", WATERMARK_FONT_SIZE)
    except (IOError, OSError):
        font = ImageFont.load_default()

    # Draw diagonal watermark text across the image
    text = f"SHARED: {watermark_text[:16]}"
    text_bbox = draw.textbbox((0, 0), text, font=font)
    text_width = text_bbox[2] - text_bbox[0]
    text_height = text_bbox[3] - text_bbox[1]

    # Tile the watermark across the image
    step_x = text_width + 100
    step_y = text_height + 150

    for y in range(-height, height * 2, step_y):
        for x in range(-width, width * 2, step_x):
            draw.text((x, y), text, fill=(128, 128, 128, WATERMARK_OPACITY), font=font)

    # Rotate and composite
    watermark = watermark.rotate(WATERMARK_ANGLE, expand=False, center=(width // 2, height // 2))
    result = Image.alpha_composite(img, watermark)

    # Convert back to bytes
    output = io.BytesIO()
    result = result.convert("RGB")
    output_format = "JPEG"
    result.save(output, format=output_format, quality=85)
    output.seek(0)
    return output.getvalue()
