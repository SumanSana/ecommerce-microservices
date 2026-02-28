package com.ecommerce.notificationservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MailService {

	private final JavaMailSender mailSender;
	private final String fromEmail;

	public MailService(JavaMailSender mailSender, @Value("${spring.mail.username}") String fromEmail) {
		this.mailSender = mailSender;
		this.fromEmail = fromEmail;
	}

	public void sendEmail(String to, String subject, String content) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);

			helper.setFrom(this.fromEmail);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(content, true);

			mailSender.send(message);
			log.info("Email successfully sent to {}", to);
		} catch (Exception e) {
			log.error("Failed to send email to {}: {}", to, e.getMessage());
		}
	}
}