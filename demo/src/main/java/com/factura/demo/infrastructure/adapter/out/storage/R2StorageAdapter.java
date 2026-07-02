package com.factura.demo.infrastructure.adapter.out.storage;

import com.factura.demo.application.port.out.DocumentStoragePort;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;

public class R2StorageAdapter implements DocumentStoragePort {

    private final String bucketName;
    private final String cdnUrl;
    private final S3Client s3Client;

    public R2StorageAdapter(String bucketName, String accessKey, String secretKey, String endpoint, String cdnUrl) {
        this.bucketName = bucketName;
        this.cdnUrl = cdnUrl;

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.US_EAST_1) // Region can be any valid region for R2
                .build();
    }

    @Override
    public String saveXml(String documentId, String xmlContent, String documentType) {
        String key = documentType + "/" + documentId + ".xml";

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/xml")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromString(xmlContent, StandardCharsets.UTF_8));

        return cdnUrl.endsWith("/") ? cdnUrl + key : cdnUrl + "/" + key;
    }
}
