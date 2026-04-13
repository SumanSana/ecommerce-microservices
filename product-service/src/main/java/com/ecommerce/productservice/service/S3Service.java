package com.ecommerce.productservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadByteArray(byte[] data, String fileName, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key("products/" + fileName)
                    .contentType(contentType)
                    .acl(ObjectCannedACL.PUBLIC_READ) // Makes the image URL accessible to Angular
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));

            // Generate URL dynamically: MinIO style or AWS style
            if (endpoint != null && !endpoint.isBlank()) {
                // Returns: http://localhost:9070/ekart-images/products/image.jpg
                return String.format("%s/%s/products/%s", endpoint, bucketName, fileName);
            } else {
                // Returns: https://ekart-images.s3.ap-south-1.amazonaws.com/products/image.jpg
                return String.format("https://%s.s3.%s.amazonaws.com/products/%s", 
                                      bucketName, region, fileName);
            }
        } catch (Exception e) {
            throw new RuntimeException("S3 Upload Failed: " + e.getMessage(), e);
        }
    }
}