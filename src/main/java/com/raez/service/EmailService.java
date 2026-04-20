package com.raez.service;

import com.raez.dao.SmtpSettingsDAO;
import com.raez.model.SmtpSettings;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Sends email using the SMTP config stored in smtp_settings.
 * Silently no-ops if SMTP is disabled (so the app keeps working without creds).
 */
public class EmailService {

    private static final SmtpSettingsDAO dao = new SmtpSettingsDAO();

    /** @return true if email was sent, false if SMTP disabled or failed. */
    public static boolean send(String to, String subject, String body) {
        SmtpSettings s = dao.load();
        if (!s.isEnabled || s.host == null || s.host.isBlank()) {
            System.out.println("[EmailService] SMTP disabled or unconfigured. Skipped email to " + to);
            System.out.println("[EmailService] -- Subject: " + subject);
            System.out.println("[EmailService] -- Body:\n" + body);
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", s.host);
        props.put("mail.smtp.port", String.valueOf(s.port));
        props.put("mail.smtp.auth", s.username != null && !s.username.isBlank() ? "true" : "false");
        if (s.useTls) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(s.username, s.password);
            }
        });

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(s.fromAddress, s.fromName));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject(subject);
            msg.setText(body);
            Transport.send(msg);
            System.out.println("[EmailService] Sent to " + to + " via " + s.host);
            return true;
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            System.err.println("[EmailService] Send failed: " + e.getMessage());
            return false;
        }
    }
}
