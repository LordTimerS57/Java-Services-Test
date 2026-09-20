package com.exa.util;

import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public final class MailUtil {
    private MailUtil() { }

    public static void sendPasswordCode(String to, String prenom, String code) {
        String host = System.getenv("SMTP_HOST");
        if (host == null || host.isBlank()) {
            System.out.println("[DEV] Code de confirmation pour " + to + " : " + code);
            return;
        }
        String user = System.getenv("SMTP_USER");
        String pass = System.getenv("SMTP_PASSWORD");
        String from = System.getenv().getOrDefault("SMTP_FROM", user);

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", System.getenv().getOrDefault("SMTP_PORT", "587"));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(jakarta.mail.Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Ne-laiko - Code de confirmation", "UTF-8");
            message.setText("Bonjour " + prenom + ",\n\n"
                    + "Votre code de confirmation pour modifier votre mot de passe est : " + code + "\n"
                    + "Il est valable 10 minutes.\n\n"
                    + "Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.", "UTF-8");
            Transport.send(message);
        } catch (Exception exception) {
            throw new IllegalStateException("Envoi de l'email impossible", exception);
        }
    }
}