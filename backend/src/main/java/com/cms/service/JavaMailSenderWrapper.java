package com.cms.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around {@link JavaMailSender} to keep other services
 * free from direct mail dependencies.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JavaMailSenderWrapper {

    private final JavaMailSender mailSender;

    @Value("${email.digest.from}")
    private String fromAddress;

    public void sendSimple(String to, String subject, String plainText) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(plainText, false);
        mailSender.send(message);
        log.debug("Plain email sent to {}", to);
    }
}
