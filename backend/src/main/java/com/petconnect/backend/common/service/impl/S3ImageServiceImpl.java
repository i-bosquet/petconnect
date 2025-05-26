package com.petconnect.backend.common.service.impl;

import com.petconnect.backend.common.service.ImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.petconnect.backend.common.helper.ValidateHelper;


/**
 * Service implementation for managing image file storage using AWS S3.
 * Active only when the 'prod' (or 'aws') profile is active.
 *
 * @author ibosquet
 */
@Service
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class S3ImageServiceImpl implements ImageService {

    private final S3Client s3Client;
    private final ValidateHelper validateHelper;

    @Value("${aws.s3.bucket.images}")
    private String imagesBucketName;

    @Override
    public String storeImage(MultipartFile file, String subDirectory) throws IOException {
        log.debug("Attempting to store image in S3. Bucket: {}, Subdirectory: {}, Filename: {}",
                imagesBucketName, subDirectory, file.getOriginalFilename());

        validateHelper.validateFileSize(file);
        validateHelper.validateFileType(file);

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        int extensionIndex = originalFilename.lastIndexOf('.');
        if (extensionIndex > 0) {
            fileExtension = originalFilename.substring(extensionIndex);
        }
        String uniqueFilename = UUID.randomUUID() + fileExtension;

        String s3Key = subDirectory + "/" + uniqueFilename;
        if (s3Key.startsWith("/")) {
            s3Key = s3Key.substring(1);
        }


        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(imagesBucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Successfully stored image '{}' to S3 bucket '{}' with key '{}'", originalFilename, imagesBucketName, s3Key);
            return s3Key;
        } catch (Exception e) {
            log.error("Failed to store image to S3. Bucket: {}, Key: {}. Error: {}", imagesBucketName, s3Key, e.getMessage(), e);
            throw new IOException("Failed to store image to S3: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteImage(String s3Key) {
        if (!org.springframework.util.StringUtils.hasText(s3Key)) {
            log.warn("Attempted to delete image from S3 with empty or null key. Skipping.");
            return;
        }

        log.debug("Attempting to delete image from S3. Bucket: {}, Key: {}", imagesBucketName, s3Key);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(imagesBucketName)
                .key(s3Key)
                .build();
        try {
            s3Client.deleteObject(deleteObjectRequest);
            log.info("Successfully deleted image from S3. Bucket: {}, Key: {}", imagesBucketName, s3Key);
        } catch (Exception e) {
            log.error("Failed to delete image from S3. Bucket: {}, Key: {}. Error: {}", imagesBucketName, s3Key, e.getMessage(), e);
        }
    }

    @Override
    public Path getAbsolutePath(String s3Key) {
        log.warn("getAbsolutePath is not directly applicable for S3ImageService. Key: {}", s3Key);
        throw new UnsupportedOperationException("getAbsolutePath is not supported for S3ImageService. Use a method to get a presigned URL or S3 object URL instead.");
    }
}
