package com.petconnect.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS SDK configuration, active when 'prod' (or 'aws') profile is active.
 * Configures the S3Client bean.
 * @author ibosquet
 */
@Configuration
@Profile("prod")
public class AwsConfig {

    @Value("${aws.region}") // From application-prod.properties
    private String awsRegion;

    /**
     * Provides a configured S3Client bean for interacting with Amazon S3.
     * The S3 client is configured with the AWS region specified
     * in the application properties.
     *
     * @return A preconfigured instance of S3Client for Amazon S3 interactions.
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(InstanceProfileCredentialsProvider.builder().build())
                .build();
    }
}
