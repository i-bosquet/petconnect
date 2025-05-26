package com.petconnect.backend.common.service.impl;

import com.petconnect.backend.common.helper.Utils;
import com.petconnect.backend.common.service.KeyStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service implementation for managing cryptographic key file storage using AWS S3.
 * Active only when the 'prod' profile is active.
 * For 'getAbsolutePath' methods, it assumes keys might be cached locally within the container
 * from S3 at startup for the 'prod' profile (simplified TFG approach).
 *
 * @author ibosquet
 */
@Service
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class S3KeyStorageServiceImpl implements KeyStorageService {
    private final S3Client s3Client;

    @Value("${aws.s3.bucket.keys}")
    private String keysBucketName;

    private final Path localProdPublicKeysCachePath = Paths.get("/app/prod_cached_keys/public_keys");
    private final Path localProdPrivateKeysCachePath = Paths.get("/app/prod_cached_keys/private_encrypted_keys");

    @PostConstruct
    private void initLocalCacheDirectories() {
        try {
            Files.createDirectories(localProdPublicKeysCachePath);
            Files.createDirectories(localProdPrivateKeysCachePath);
            log.info("S3KeyStorageService: Ensured local cache directories exist for 'prod' profile: {} and {}",
                    localProdPublicKeysCachePath, localProdPrivateKeysCachePath);
        } catch (IOException e) {
            log.error("S3KeyStorageService: Could not create local key cache directories. " +
                    "getAbsolutePathFor... methods might fail if keys are not pre-cached from S3.", e);
        }
    }

    @Override
    public String storePublicKey(MultipartFile file, String subDirectory, String desiredFilenameBase) throws IOException {
        return storeKeyFileToS3(file, subDirectory, desiredFilenameBase, "Public");
    }

    @Override
    public String storeEncryptedPrivateKey(MultipartFile file, String subDirectory, String desiredFilenameBase) throws IOException {
        return storeKeyFileToS3(file, subDirectory, desiredFilenameBase, "Private Encrypted");
    }

    @Override
    public void deleteKey(String s3Key) {
        if (!StringUtils.hasText(s3Key)) {
            log.warn("S3KeyStorageService: Attempted to delete key from S3 with empty or null S3 key. Skipping.");
            return;
        }
        log.debug("S3KeyStorageService: Attempting to delete key from S3. Bucket: {}, Key: {}", keysBucketName, s3Key);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(keysBucketName)
                .key(s3Key)
                .build();
        try {
            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3KeyStorageService: Successfully deleted key from S3. Bucket: {}, Key: {}", keysBucketName, s3Key);
        } catch (Exception e) {
            log.error("S3KeyStorageService: Failed to delete key from S3. Bucket: {}, Key: {}. Error: {}",
                    keysBucketName, s3Key, e.getMessage(), e);
        }
    }

    @Override
    public InputStream getPublicKeyContent(String s3Key) throws IOException {
        if (!StringUtils.hasText(s3Key)) {
            throw new IllegalArgumentException("S3 key for public key content cannot be empty or null.");
        }
        log.debug("S3KeyStorageService: Attempting to get content for public key from S3. Bucket: {}, Key: {}", keysBucketName, s3Key);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(keysBucketName)
                .key(s3Key)
                .build();
        try {
            ResponseInputStream<GetObjectResponse> s3Object;
            s3Object = s3Client.getObject(getObjectRequest);
            return s3Object;
        } catch (Exception e) {
            log.error("S3KeyStorageService: Failed to get public key content from S3. Bucket: {}, Key: {}. Error: {}",
                    keysBucketName, s3Key, e.getMessage(), e);
            throw new IOException("Failed to retrieve public key from S3: " + s3Key, e);
        }
    }

    @Override
    public InputStream getPrivateKeyContent(String s3Key) throws IOException {
        if (!StringUtils.hasText(s3Key)) {
            throw new IllegalArgumentException("S3 key for private key content cannot be empty or null.");
        }
        log.debug("S3KeyStorageService: Attempting to get content for private key from S3. Bucket: {}, Key: {}", keysBucketName, s3Key);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(keysBucketName)
                .key(s3Key)
                .build();
        try {
            ResponseInputStream<GetObjectResponse> s3Object;
            s3Object = s3Client.getObject(getObjectRequest);
            return s3Object;
        } catch (Exception e) {
            log.error("S3KeyStorageService: Failed to get private key content from S3. Bucket: {}, Key: {}. Error: {}",
                    keysBucketName, s3Key, e.getMessage(), e);
            throw new IOException("Failed to retrieve private key from S3: " + s3Key, e);
        }
    }

    @Override
    public Path getAbsolutePathForPublicKey(String s3Key) {
        log.error("S3KeyStorageService: getAbsolutePathForPublicKey was called with S3 key '{}' in 'prod' profile. " +
                "This method is intended for filesystem-based dev profile. " +
                "Use getPublicKeyContent() instead for S3.", s3Key);
        throw new UnsupportedOperationException("getAbsolutePathForPublicKey is not supported in S3KeyStorageService when " +
                "'prod' profile is active. Use getPublicKeyContent().");
    }

    @Override
    public Path getAbsolutePathForPrivateKey(String s3Key) {
        log.error("S3KeyStorageService: getAbsolutePathForPrivateKey was called with S3 key '{}' in 'prod' profile. " +
                "This method is intended for filesystem-based dev profile. " +
                "Use getPrivateKeyContent() instead for S3.", s3Key);
        throw new UnsupportedOperationException("getAbsolutePathForPrivateKey is not supported in S3KeyStorageService" +
                " when 'prod' profile is active. Use getPrivateKeyContent().");
    }

    /**
     * Stores the provided key file in the specified S3 bucket under the given subdirectory and filename.
     * Validates the file and its type, constructs the appropriate S3 key, and uploads the file to S3.
     *
     * @param file the key file to be stored; must not be null or empty
     * @param subDirectory the subdirectory within the S3 bucket where the file should be stored; must not be blank
     * @param desiredFilenameBase the desired base name for the file to be stored; must not be blank
     * @param keyTypeForLog a descriptive string identifying the type of key for logging purposes
     * @return the S3 key (path within the bucket) where the file has been stored
     * @throws IOException if an I/O error occurs during file upload
     * @throws IllegalArgumentException if any of the input parameters are invalid
     */
    private String storeKeyFileToS3(MultipartFile file, String subDirectory, String desiredFilenameBase, String keyTypeForLog) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(keyTypeForLog + " key file cannot be null or empty.");
        }
        if (!StringUtils.hasText(subDirectory)) {
            throw new IllegalArgumentException("Subdirectory for " + keyTypeForLog + " key storage cannot be blank.");
        }
        if (!StringUtils.hasText(desiredFilenameBase)) {
            throw new IllegalArgumentException("Desired filename base for " + keyTypeForLog + " key cannot be blank.");
        }

        Utils.validateKeyFileType(file);
        String finalFilename = Utils.generateFinalFilenameForKey(file, desiredFilenameBase);
        String s3Key = StringUtils.cleanPath(subDirectory) + "/" + finalFilename;
        if (s3Key.startsWith("/")) {
            s3Key = s3Key.substring(1);
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(keysBucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .build();
        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("S3KeyStorageService: Successfully stored {} key '{}' to S3 bucket '{}' with key '{}'",
                    keyTypeForLog, file.getOriginalFilename(), keysBucketName, s3Key);
            return s3Key; // Return the S3 key (path within the bucket)
        } catch (Exception e) {
            log.error("S3KeyStorageService: Failed to store {} key to S3. Bucket: {}, Key: {}. Error: {}",
                    keyTypeForLog, keysBucketName, s3Key, e.getMessage(), e);
            throw new IOException("Failed to store " + keyTypeForLog + " key to S3: " + e.getMessage(), e);
        }
    }
}
