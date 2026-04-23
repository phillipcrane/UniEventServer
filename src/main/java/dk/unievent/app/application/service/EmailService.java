package dk.unievent.app.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Email service for sending transactional emails using Spring Mail.
 * Supports HTML templates with Thymeleaf and async sending.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${frontend.url}")
    private String frontendUrl;

    /**
     * Sends organizer invitation email asynchronously.
     * Does not throw exceptions - logs errors instead to prevent async task failures.
     *
     * @param to Email address of the organizer
     * @param key Single-use invitation key (32 characters)
     */
    @Async
    public void sendOrganizerInvitationEmailAsync(String to, String key) {
        try {
            sendOrganizerInvitationEmail(to, key);
        } catch (MessagingException e) {
            log.error("Failed to send organizer invitation email to {}: {}", to, e.getMessage(), e);
        }
    }

    /**
     * Sends organizer invitation email synchronously.
     *
     * @param to Email address of the organizer
     * @param key Single-use invitation key (32 characters)
     * @throws MessagingException if email sending fails
     */
    public void sendOrganizerInvitationEmail(String to, String key) throws MessagingException {
        // Prepare template context with variables
        Context context = new Context();
        context.setVariable("key", key);
        context.setVariable("expirationHours", 24);
        context.setVariable("registerUrl", frontendUrl + "/register-organizer?key=" + key);

        // Render HTML template with context
        String htmlContent = templateEngine.process("emails/organizer-invitation", context);

        // Create and configure MIME message
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setFrom(fromEmail);
        helper.setSubject("You're Invited to Organize Events on UniEvent!");
        helper.setText(htmlContent, true);  // true = isHtml

        // Send email
        mailSender.send(message);
        log.info("Organizer invitation sent from {} to {}", fromEmail, to);
    }

    /**
     * Generic method for sending simple text emails.
     * Useful for other transactional emails in the future.
     *
     * @param to Email address of the recipient
     * @param subject Email subject
     * @param textContent Plain text content
     * @throws MessagingException if email sending fails
     */
    public void sendSimpleEmail(String to, String subject, String textContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setFrom(fromEmail);
        helper.setSubject(subject);
        helper.setText(textContent, false);  // false = plain text

        mailSender.send(message);
        log.info("Simple email sent from {} to {} with subject: {}", fromEmail, to, subject);
    }
}
