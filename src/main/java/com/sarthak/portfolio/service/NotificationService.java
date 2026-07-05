package com.sarthak.portfolio.service;

import com.sarthak.portfolio.model.ContactMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class NotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${portfolio.alert.email}")
    private String alertEmail;

    @Value("${twilio.account.sid}")
    private String twilioSid;

    @Value("${twilio.auth.token}")
    private String twilioToken;

    @Value("${twilio.from.number}")
    private String twilioFrom;

    @Value("${twilio.to.number}")
    private String twilioTo;

    @Async
    public void sendAlerts(ContactMessage message) {
        sendEmail(message);
        sendSms(message);
    }

    private void sendEmail(ContactMessage message) {
        if (mailSender == null) {
            System.out.println("MailSender not configured. Skipping email alert.");
            return;
        }
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(alertEmail);
            mailMessage.setSubject("Portfolio Contact Alert: " + message.getSubject());
            mailMessage.setText(
                "You have received a new message from your portfolio contact form:\n\n" +
                "Sender Name: " + message.getName() + "\n" +
                "Sender Email: " + message.getEmail() + "\n" +
                "Sender Phone: " + message.getPhone() + "\n\n" +
                "Message:\n" + message.getMessage() + "\n\n" +
                "Timestamp: " + message.getTimestamp()
            );
            mailSender.send(mailMessage);
            System.out.println("Email alert sent successfully to " + alertEmail);
        } catch (Exception e) {
            System.err.println("Failed to send email alert: " + e.getMessage());
        }
    }

    private void sendSms(ContactMessage message) {
        if (twilioSid == null || twilioSid.isEmpty() || twilioToken == null || twilioToken.isEmpty()) {
            System.out.println("Twilio credentials not configured. Skipping SMS alert.");
            return;
        }
        try {
            String smsBody = "New portfolio inquiry from " + message.getName() + " (" + message.getEmail() + "): " + message.getMessage();
            if (smsBody.length() > 160) {
                smsBody = smsBody.substring(0, 157) + "...";
            }

            String urlStr = "https://api.twilio.com/2010-04-01/Accounts/" + twilioSid + "/Messages.json";
            URL url = URI.create(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // Basic Auth
            String auth = twilioSid + ":" + twilioToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Form parameters
            String postData = "From=" + URLEncoder.encode(twilioFrom, "UTF-8") +
                              "&To=" + URLEncoder.encode(twilioTo, "UTF-8") +
                              "&Body=" + URLEncoder.encode(smsBody, "UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("SMS alert sent successfully to " + twilioTo);
            } else {
                System.err.println("Twilio SMS send failed with HTTP response code: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Failed to send SMS alert: " + e.getMessage());
        }
    }
}
