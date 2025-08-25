package com.spec.plun.email.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spec.plun.email.util.RedisUtil;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EmailService {
	
	private final JavaMailSender javaMailSender;
	private final RedisUtil redisUtil;
	
	private static final String CODE_PRE = "email:";

    @Value("${spring.mail.username}")
    private String senderEmail;

    public String createCode() {
        Random random = new Random();
        StringBuilder key = new StringBuilder();

        for (int i = 0; i < 6; i++) { // 인증 코드 6자리
            int index = random.nextInt(2); // 0~1까지 랜덤, 랜덤값으로 switch문 실행

            switch (index) {
                case 0 -> key.append((char) (random.nextInt(26) + 65)); // 대문자
                case 1 -> key.append(random.nextInt(10)); // 숫자
            }
        }
        return key.toString();
    }

    public MimeMessage createMail(String mail, String authCode) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();

        message.setFrom(senderEmail);
        message.setRecipients(MimeMessage.RecipientType.TO, mail);
        message.setSubject("Plun 이메일 인증");
        String body = "";
        body += "<h3>인증 번호 확인 후 이메일 인증 완료해주세요.</h3>";
        body += "<h1> plun </h1>";
        body += "<h1>  인증번호 : " + authCode + "</h1>";
        body += "<h3>감사합니다.</h3>";
        message.setText(body, "UTF-8", "html");
        return message;
    }

    // 메일 발송
    public boolean sendSimpleMessage(String sendEmail) throws MessagingException {
    	String authCode = createCode(); // 랜덤 인증번호 생성
        MimeMessage message = createMail(sendEmail, authCode); // 메일 생성
        try {
            javaMailSender.send(message); // 메일 발송
            Date fiveMinutes = Date.from(Instant.now().plus(5,ChronoUnit.MINUTES));
            redisUtil.setDataExpire(CODE_PRE +sendEmail , authCode, fiveMinutes);
            log.info("[EMAIL] saved OTP: key={}, code={}, ttl=300s", CODE_PRE +sendEmail, authCode);
            return true;
        } catch (MailException e) {
        	log.error("메일 발송 실패 to {} : {}", sendEmail, e.getMessage(), e);
            return false;
        }
    }

	public boolean verifyCode(String email, String code) {
		String key = CODE_PRE + email;
		String saved = redisUtil.getData(key);
		log.info("[EMAIL] verify: key={}, saved={}, input={}", key, saved, code);
		if (saved == null) return false; 
		boolean ok = saved.equals(code);
		if(ok) redisUtil.deleteData(key);
		return ok;
	}

}
