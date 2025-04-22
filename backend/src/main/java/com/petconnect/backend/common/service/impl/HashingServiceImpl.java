package com.petconnect.backend.common.service.impl;

import com.petconnect.backend.exception.HashingException;
import com.petconnect.backend.common.service.HashingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.petconnect.backend.common.helper.FormatUtils;

/**
 * Implementation of the HashingService interface using SHA-256.
 *
 * @author ibosquet
 */
@Service
@Slf4j
public class HashingServiceImpl implements HashingService {
    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * {@inheritDoc}
     */
    @Override
    public String hashString(String input) throws HashingException {
        if (input == null) {
            throw new IllegalArgumentException("Input string for hashing cannot be null.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return FormatUtils.bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Hashing algorithm {} not found!", HASH_ALGORITHM, e);
            throw new HashingException("Failed to initialize hashing algorithm: " + HASH_ALGORITHM, e);
        }
    }
}
