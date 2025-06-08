package com.petconnect.backend.common.service.impl;

import com.petconnect.backend.common.service.EmailService;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async; // Para envío asíncrono
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Implementation of the EmailService using Spring Boot Mail.
 * Handles the construction and sending of emails, like password-reset links.
 * Use JavaMailSender configured via application properties.
 * Email sending is performed asynchronously.
 *
 * @author ibosquet
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService  {
    public static final String ENCODING = "utf-8";
    private final JavaMailSender mailSender;
    private final Environment environment;

    @Value("${app.frontend.dev.url:#{null}}")
    private String frontendDevUrl;

    @Value("${app.frontend.prod.url:#{null}}")
    private String frontendProdUrl;

    @Value("${spring.mail.username}")
    private String mailFromAddress;

    private String frontendBaseUrl;

    /**
     * Initializes the service by determining the correct frontend URL based on the active Spring profile.
     */
    @PostConstruct
    public void init() {
        // Check which profile is active and set the frontend base URL accordingly
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            this.frontendBaseUrl = this.frontendProdUrl;
            log.info("EmailService initialized for 'prod' profile. Frontend URL: {}", this.frontendBaseUrl);
        } else {
            this.frontendBaseUrl = this.frontendDevUrl;
            log.info("EmailService initialized for 'dev' profile. Frontend URL: {}", this.frontendBaseUrl);
        }
        // Fallback just in case no property resolves, even though it shouldn't happen
        if (this.frontendBaseUrl == null) {
            this.frontendBaseUrl = "http://localhost:5173";
            log.warn("Could not determine frontend URL from profiles, using default: {}", this.frontendBaseUrl);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Async
    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetToken) {

        String resetUrl = frontendBaseUrl + "/reset-password?token=" + resetToken;

        String subject = "PetConnect - Password Reset Request";
        String content = String.format(
                "<p>Hello %s,</p>" +
                        "<p>You requested a password reset for your PetConnect account.</p>" +
                        "<p>Click the link below to set a new password:</p>" +
                        "<p><a href=\"%s\">Reset Password</a></p>" +
                        "<p>This link will expire in 1 hour.</p>" +
                        "<p>If you didn't request this, please ignore this email.</p>" +
                        "<br>" +
                        "<p>Thanks,<br>The PetConnect Team</p>",
                recipientName, resetUrl
        );
        sendEmail(recipientEmail, subject, content, "password reset email");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Async
    public void sendClinicKeysChangedNotification(String adminEmail, String clinicName) {
        String subject = "PetConnect - Clinic Security Alert: Cryptographic Keys Updated";
        String content = String.format(
                        "<p>Hello Admin,</p>" +
                        "<p>This is an important security notification regarding your PetConnect account for the clinic: <strong>%s</strong>.</p>" +
                        "<p>We are informing you that one or more cryptographic keys (public and/or private key path) associated with your clinic have been recently updated in our system.</p>" +
                        "<p><strong>If you authorized this change</strong>, you can disregard this message. Your clinic's signing and verification capabilities will now use the new keys.</p>" +
                        "<p><strong>If you did NOT authorize this change, or if you have any concerns, please contact PetConnect support immediately.</strong></p>" +
                        "<p>For security reasons, we recommend reviewing your clinic's settings and staff access if you suspect any unauthorized activity.</p>" +
                        "<br>" +
                        "<p>Sincerely,<br>The PetConnect Security Team</p>",
                clinicName
        );
        sendEmail(adminEmail, subject, content, "clinic keys changed notification");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Async
    public void sendVetKeysChangedNotification(String vetEmail, String vetName) {
        String subject = "PetConnect - Security Alert: Your Signing Keys Updated";
        String content = String.format(
                        "<p>Hello Dr. %s,</p>" +
                        "<p>This is an important security notification regarding your PetConnect veterinarian account.</p>" +
                        "<p>We are informing you that one or more of your individual cryptographic keys (public key path and/or encrypted private key path) have been recently updated in our system by a clinic administrator.</p>" +
                        "<p><strong>If you requested this change or were aware of it (e.g., you provided new key files to your clinic admin)</strong>, you can disregard this message. Your digital signing capabilities will now use the new keys, and you will need to use your new private key password for signing operations.</p>" +
                        "<p><strong>If you did NOT authorize this change, or if you have any concerns, please contact your clinic administrator and/or PetConnect support immediately.</strong></p>" +
                        "<p>For security, always ensure your private key password is kept confidential.</p>" +
                        "<br>" +
                        "<p>Sincerely,<br>The PetConnect Security Team</p>",
                vetName
        );
        sendEmail(vetEmail, subject, content, "vet keys changed notification");
    }

    /**
     * Sends an email to the specified recipient with the given subject and content.
     * Logs the process and handles any exceptions that occur during the email-sending process.
     *
     * @param recipientEmail the email address of the recipient
     * @param subject the subject of the email
     * @param content the content of the email, in HTML format
     * @param logContext a description of the email's context for logging purposes
     */
    private void sendEmail(String recipientEmail, String subject, String content, String logContext) {
        log.info("Attempting to send {} to {}", logContext, recipientEmail);
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, ENCODING);
            helper.setFrom(mailFromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(mimeMessage);
            log.info("{} sent successfully to {}", logContext.replace("email", "notification"), recipientEmail);
        } catch (MessagingException e) {
            log.error("Failed to send {} to {}: {}", logContext, recipientEmail, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending {} to {}: {}", logContext, recipientEmail, e.getMessage(), e);
        }
    }
}
