package com.taskflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender sender;
    @Value("${app.mail-from}")
    private String from;
    @Value("${app.frontend-url}")
    private String frontendUrl;

    public String link(String path) {
        return frontendUrl.split(",")[0] + path;
    }

    /** Fire-and-forget: a mail outage must never fail the request that triggered it. */
    @Async
    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(body);
            sender.send(m);
        } catch (Exception e) {
            log.warn("Email to {} failed: {}", to, e.getMessage());
        }
    }
}
