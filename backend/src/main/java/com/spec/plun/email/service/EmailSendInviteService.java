package com.spec.plun.email.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EmailSendInviteService {
	
	private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.front-url}") 
    private String frontUrl;
    
    public MimeMessage createMail(String mail, String email, String name) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();

        message.setFrom(senderEmail);
        message.setRecipients(MimeMessage.RecipientType.TO, mail);
        message.setSubject("Plun 이메일 인증");
        String body = "";
        body += "<h1>" + name + " (" + email + ")님이 회원님을 초대하셨습니다.</h1>";
        body += "<h2> plun </h2>";
        body += "<p><a href=\"" + frontUrl + "\">바로가기</a></p>";
        body += "<h3>감사합니다.</h3>";
        message.setText(body, "UTF-8", "html");
        return message;
    }

    // 메일 발송
    public boolean sendSimpleMessage(String sendEmail,  String email ,String name) {
    	try {
            MimeMessage message = createMail(sendEmail, email, name); // 여기서도 MessagingException 가능
            javaMailSender.send(message);
            return true;
        } catch (MessagingException | MailException e) {
            log.error("메일 발송 실패 to {} : {}", sendEmail, e.getMessage(), e);
            return false;
        }
    }

}
