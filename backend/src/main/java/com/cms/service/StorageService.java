package com.cms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public void createBucketIfNotExists(String bucket) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Created bucket: {}", bucket);
        }
    }

    public void putObject(String bucket, String key, InputStream data, long contentLength, String contentType) {
        createBucketIfNotExists(bucket);
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .contentLength(contentLength)
                        .build(),
                RequestBody.fromInputStream(data, contentLength));
    }

    public InputStream getObject(String bucket, String key) {
        return s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    public void deleteObject(String bucket, String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    public void copyObject(String sourceBucket, String sourceKey, String destBucket, String destKey) {
        createBucketIfNotExists(destBucket);
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(sourceBucket)
                .sourceKey(sourceKey)
                .destinationBucket(destBucket)
                .destinationKey(destKey)
                .build());
    }

    public String presignGetUrl(String bucket, String key, Duration expiry) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build())
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public String presignPutUrl(String bucket, String key, Duration expiry, String contentType) {
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build())
                .build();
        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    public String initiateMultipartUpload(String bucket, String key, String contentType) {
        createBucketIfNotExists(bucket);
        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build());
        return response.uploadId();
    }

    public String uploadPart(String bucket, String key, String uploadId, int partNumber,
                             InputStream data, long contentLength) {
        UploadPartResponse response = s3Client.uploadPart(
                UploadPartRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .contentLength(contentLength)
                        .build(),
                RequestBody.fromInputStream(data, contentLength));
        return response.eTag();
    }

    public void completeMultipartUpload(String bucket, String key, String uploadId,
                                        List<CompletedPart> completedParts) {
        s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder()
                        .parts(completedParts)
                        .build())
                .build());
    }

    public void abortMultipartUpload(String bucket, String key, String uploadId) {
        s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .build());
    }
}
