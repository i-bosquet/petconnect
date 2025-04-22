package com.petconnect.backend.common.helper;

import java.util.Formatter;

/**
 * Utility class for common formatting operations.
 *
 * @author ibosquet
 */
public class FormatUtils {
    private FormatUtils() {
    }

    /**
     * Converts a byte array to its hexadecimal string representation.
     *
     * @param bytes The byte array. Cannot be null.
     * @return The hexadecimal string.
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Byte array cannot be null for hex conversion.");
        }
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }
}
