package com.petconnect.backend.common.service.impl;

import com.petconnect.backend.common.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async; // Para envío asíncrono
import org.springframework.stereotype.Service;

/**
 * Implementation of the EmailService using Spring Boot Mail.
 * Handles the construction and sending of emails, like password-reset links.
 * Uses JavaMailSender configured via application properties.
 * Email sending is performed asynchronously.
 *
 * @author ibosquet
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService  {
    private final JavaMailSender mailSender;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${spring.mail.username}")
    private String mailFromAddress;

    /**
     * {@inheritDoc}
     * Sends the email asynchronously.
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

        log.info("Attempting to send password reset email to {} for token {}", recipientEmail, resetToken);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(mailFromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(mimeMessage);
            log.info("Password reset email sent successfully to {}", recipientEmail);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", recipientEmail, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending password reset email to {}: {}", recipientEmail, e.getMessage(), e);
        }
    }
}
