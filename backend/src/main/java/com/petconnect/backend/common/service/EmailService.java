package com.petconnect.backend.common.service;

/**
 * Service interface for sending emails.
 *
 * @author ibosquet
 */
public interface EmailService {
    /**
     * Sends a password-reset email to the specified user.
     *
     * @param recipientEmail The email address of the user.
     * @param recipientName  The name of the user (for personalization).
     * @param resetToken     The password reset token.
     */
    void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetToken);
}
