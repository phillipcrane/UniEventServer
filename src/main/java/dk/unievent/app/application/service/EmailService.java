package dk.unievent.app.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.TemplateEngine;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

// sends transactional emails via Spring Mail with Thymeleaf HTML templates.
// async variants fire and forget so callers don't wait on SMTP.
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${unievent.security.organizer-key.expiration-hours:24}")
    private long organizerKeyExpirationHours;

    // fire and forget: logs errors rather than propagating them so the caller doesn't have to handle async failures
    @Async
    public void sendOrganizerInvitationEmailAsync(String to, String key) {
        try {
            sendOrganizerInvitationEmail(to, key);
        } catch (Exception e) {
            log.error("Failed to send organizer invitation email to {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendOrganizerInvitationEmail(String to, String key) throws MessagingException {
        // 1. build the Thymeleaf context with the key, expiry hours, and pre-filled registration URL
        Context context = new Context();
        context.setVariable("key", key);
        context.setVariable("expirationHours", organizerKeyExpirationHours);
        context.setVariable("registerUrl", frontendUrl + "/signup-organizer?key=" + key);

        // 2. render the HTML template
        String htmlContent = templateEngine.process("emails/organizer-invitation", context);

        // 3. build and send the MIME message
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setFrom(fromEmail);
        helper.setSubject("You're Invited to Organize Events on UniEvent!");
        helper.setText(htmlContent, true); // true = HTML
        mailSender.send(message);
        log.info("Organizer invitation sent from {} to {}", fromEmail, to);
    }

    public void sendSimpleEmail(String to, String subject, String textContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setFrom(fromEmail);
        helper.setSubject(subject);
        helper.setText(textContent, false); // false = plain text
        mailSender.send(message);
        log.info("Simple email sent from {} to {} with subject: {}", fromEmail, to, subject);
    }
}
