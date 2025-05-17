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

    /**
     * Sends a notification to the provided clinic administrator email address, indicating
     * that the keys related to the specified clinic have been changed.
     *
     * @param adminEmail The email address of the clinic administrator to whom the notification
     *                   will be sent. Must not be null or empty.
     * @param clinicName The name of the clinic whose keys were changed. Must not be null or empty.
     */
    void sendClinicKeysChangedNotification(String adminEmail, String clinicName);

    /**
     * Sends a notification to a veterinarian, indicating that their associated keys have been changed.
     *
     * @param vetEmail The email address of the veterinarian to whom the notification will be sent.
     *                 Must not be null or empty.
     * @param vetName  The name of the veterinarian to personalize the notification.
     *                 Must not be null or empty.
     */
    void sendVetKeysChangedNotification(String vetEmail, String vetName);
}
