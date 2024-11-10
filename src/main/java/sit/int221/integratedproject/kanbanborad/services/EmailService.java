package sit.int221.integratedproject.kanbanborad.services;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender emailSender;

    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendEmail(String to, String subject, String text, String displayName, String replyTo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        message.setFrom(displayName + "<monthon.mukk@kmutt.ac.th>");
        message.setReplyTo(replyTo);

        emailSender.send(message);
    }
}