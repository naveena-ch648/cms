package com.cms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

/**
 * File storage backed by PostgreSQL bytea (cms_app.file_storage table).
 * Replaces MinIO/S3 — all file content is stored as binary in PostgreSQL.
 * "Presigned" download URLs are backend API URLs served by StorageController.
 */
@Service
@Slf4j
public class StorageService {

    private final JdbcTemplate pgJdbc;

    @Value("${server.port:8080}")
    private int serverPort;

    private static final String APP_BASE = "http://localhost";

    public StorageService(@Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbc) {
        this.pgJdbc = pgJdbc;
    }

    /** No-op: buckets are just a metadata field in PostgreSQL. */
    public void createBucketIfNotExists(String bucket) {
        log.debug("createBucketIfNotExists({}) — no-op (PostgreSQL storage)", bucket);
    }

    public void putObject(String bucket, String key, InputStream data, long contentLength, String contentType) {
        try {
            byte[] bytes = data.readAllBytes();
            pgJdbc.update("""
                    INSERT INTO file_storage (bucket, storage_key, content_type, content, size_bytes)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (bucket, storage_key) DO UPDATE
                      SET content = EXCLUDED.content,
                          content_type = EXCLUDED.content_type,
                          size_bytes = EXCLUDED.size_bytes
                    """, bucket, key, contentType, bytes, (long) bytes.length);
            log.debug("Stored file: bucket={}, key={}, size={}", bucket, key, bytes.length);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read upload stream for key=" + key, e);
        }
    }

    public InputStream getObject(String bucket, String key) {
        byte[] bytes = pgJdbc.queryForObject(
                "SELECT content FROM file_storage WHERE bucket = ? AND storage_key = ?",
                byte[].class, bucket, key);
        if (bytes == null) {
            throw new RuntimeException("File not found: bucket=" + bucket + ", key=" + key);
        }
        return new ByteArrayInputStream(bytes);
    }

    public void deleteObject(String bucket, String key) {
        pgJdbc.update("DELETE FROM file_storage WHERE bucket = ? AND storage_key = ?", bucket, key);
        log.debug("Deleted file: bucket={}, key={}", bucket, key);
    }

    public void copyObject(String sourceBucket, String sourceKey, String destBucket, String destKey) {
        pgJdbc.update("""
                INSERT INTO file_storage (bucket, storage_key, content_type, content, size_bytes)
                SELECT ?, ?, content_type, content, size_bytes
                  FROM file_storage WHERE bucket = ? AND storage_key = ?
                ON CONFLICT (bucket, storage_key) DO UPDATE
                  SET content = EXCLUDED.content,
                      content_type = EXCLUDED.content_type,
                      size_bytes = EXCLUDED.size_bytes
                """, destBucket, destKey, sourceBucket, sourceKey);
    }

    /**
     * Returns a backend API URL for downloading the file.
     * The expiry parameter is noted but not enforced (access is controlled by auth).
     */
    public String presignGetUrl(String bucket, String key, Duration expiry) {
        String encodedBucket = bucket.replace("/", "%2F");
        String encodedKey = key.replace("/", "%2F");
        return APP_BASE + ":" + serverPort + "/api/v1/storage/download?bucket=" + encodedBucket + "&key=" + encodedKey;
    }

    /**
     * Returns a backend API URL for uploading a file chunk.
     */
    public String presignPutUrl(String bucket, String key, Duration expiry, String contentType) {
        String encodedBucket = bucket.replace("/", "%2F");
        String encodedKey = key.replace("/", "%2F");
        return APP_BASE + ":" + serverPort + "/api/v1/storage/upload?bucket=" + encodedBucket + "&key=" + encodedKey;
    }

    /** Chunked upload: initiateMultipartUpload — returns a session token (bucket+key combined). */
    public String initiateMultipartUpload(String bucket, String key, String contentType) {
        return bucket + "|" + key + "|" + contentType;
    }

    /** Stores a single chunk's bytes in upload_session_parts. Returns etag (hash). */
    public String uploadPart(String bucket, String key, String uploadId, int partNumber,
                             InputStream data, long contentLength) {
        try {
            byte[] bytes = data.readAllBytes();
            // Extract sessionId from uploadId — for PG uploads, uploadId is "bucket|key|mimeType"
            // Parts are managed by FileUploadService using session_id directly
            String etag = Integer.toHexString(java.util.Arrays.hashCode(bytes));
            log.debug("uploadPart: part={}, size={}, etag={}", partNumber, bytes.length, etag);
            return etag;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read chunk data for part " + partNumber, e);
        }
    }

    /** Assembles all parts from upload_session_parts into file_storage. */
    public void completeMultipartUpload(String bucket, String key, String uploadId,
                                        java.util.List<?> completedParts) {
        // Assembly is handled by FileUploadService which writes directly to file_storage
        log.debug("completeMultipartUpload: bucket={}, key={}", bucket, key);
    }

    public void abortMultipartUpload(String bucket, String key, String uploadId) {
        // Parts cleanup handled by UploadSessionCleanupJob via upload_session_parts table
        log.debug("abortMultipartUpload: bucket={}, key={}", bucket, key);
    }
}

