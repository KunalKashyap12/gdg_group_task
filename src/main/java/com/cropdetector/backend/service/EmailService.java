package com.cropdetector.backend.service;

import java.io.UnsupportedEncodingException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:}")
    private String mailFrom;

    @Value("${app.mail.sender-name:FloraScan Support}")
    private String senderName;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Checks if real SMTP email dispatch is configured and enabled.
     */
    public boolean isEmailDeliveryConfigured() {
        return mailEnabled && mailSender != null && mailUsername != null && !mailUsername.trim().isBlank();
    }

    /**
     * Sends an OTP verification email to the recipient.
     *
     * @param recipientEmail destination address
     * @param otpCode 6-digit verification code
     * @param purpose context/action (LOGIN, REGISTER, etc.)
     * @param expirySeconds validity window in seconds
     * @return true if successfully dispatched via SMTP, false otherwise
     */
    public boolean sendOtpEmail(String recipientEmail, String otpCode, String purpose, int expirySeconds) {
        if (!mailEnabled) {
            log.info("[EMAIL DISPATCH] Email delivery disabled (app.mail.enabled=false). Destination: {}", recipientEmail);
            return false;
        }

        if (mailSender == null || mailUsername == null || mailUsername.trim().isBlank()) {
            log.warn("[EMAIL NOTICE] Gmail SMTP username is not configured in .env (MAIL_USERNAME). " +
                     "Email not sent to {}. Check console logs or API response for OTP: {}", recipientEmail, otpCode);
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromAddress = (mailFrom != null && !mailFrom.trim().isBlank()) ? mailFrom.trim() : mailUsername.trim();
            try {
                helper.setFrom(new InternetAddress(fromAddress, senderName));
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(fromAddress);
            }

            helper.setTo(recipientEmail.trim());

            String actionLabel = "LOGIN".equalsIgnoreCase(purpose) ? "Sign In" : "Verification";
            helper.setSubject(otpCode + " is your FloraScan " + actionLabel + " Code");

            int expiryMinutes = Math.max(1, expirySeconds / 60);

            String htmlBody = buildOtpHtmlBody(otpCode, purpose, expiryMinutes);
            String plainText = buildOtpPlainText(otpCode, purpose, expiryMinutes);

            helper.setText(plainText, htmlBody);

            mailSender.send(message);
            log.info("[EMAIL SUCCESS] OTP verification email successfully sent to {}", recipientEmail);
            return true;
        } catch (MessagingException | MailException ex) {
            log.error("[EMAIL ERROR] Could not dispatch email to {}. Cause: {}", recipientEmail, ex.getMessage(), ex);
            return false;
        }
    }

    private String buildOtpPlainText(String otpCode, String purpose, int expiryMinutes) {
        return "FloraScan Crop Disease Diagnostic\n\n"
             + "Your verification code is: " + otpCode + "\n\n"
             + "This code is requested for: " + purpose + "\n"
             + "It is valid for " + expiryMinutes + " minutes.\n\n"
             + "SECURITY NOTICE: Do not share this code with anyone.\n"
             + "If you did not request this verification code, please ignore this email.";
    }

    private String buildOtpHtmlBody(String otpCode, String purpose, int expiryMinutes) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>FloraScan Verification Code</title>
              <style>
                body { margin: 0; padding: 0; background-color: #f4f6f8; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #2d3748; }
                .wrapper { max-width: 560px; margin: 30px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06); }
                .header { background: linear-gradient(135deg, #15803d, #166534); padding: 32px 24px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.5px; }
                .header p { margin: 6px 0 0; font-size: 14px; opacity: 0.9; }
                .content { padding: 32px 28px; text-align: center; }
                .lead { font-size: 16px; margin-bottom: 24px; color: #4a5568; line-height: 1.5; }
                .otp-box { background: #f0fdf4; border: 2px dashed #22c55e; border-radius: 10px; padding: 18px 24px; display: inline-block; margin: 12px 0 24px; }
                .otp-code { font-size: 34px; font-weight: 800; letter-spacing: 8px; color: #166534; font-family: 'Courier New', Courier, monospace; margin: 0; }
                .expiry-note { font-size: 13px; color: #64748b; margin-top: 6px; }
                .security-warning { background-color: #fef2f2; border-left: 4px solid #ef4444; padding: 12px 16px; text-align: left; border-radius: 4px; margin: 24px 0 16px; font-size: 13px; color: #991b1b; }
                .footer { padding: 20px 24px; background: #f8fafc; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="wrapper">
                <div class="header">
                  <h1>&#127807; FloraScan AI</h1>
                  <p>Crop Health &amp; Disease Diagnostic Platform</p>
                </div>
                <div class="content">
                  <p class="lead">Use the following one-time password (OTP) to complete your <strong>%s</strong> request:</p>
                  <div class="otp-box">
                    <div class="otp-code">%s</div>
                    <div class="expiry-note">&#9201; Valid for %d minutes</div>
                  </div>
                  <div class="security-warning">
                    <strong>Security Notice:</strong> Never share this OTP with anyone. FloraScan representatives will never ask for your verification code.
                  </div>
                </div>
                <div class="footer">
                  &copy; FloraScan AI &bull; Intelligent Agricultural Support System
                </div>
              </div>
            </body>
            </html>
            """.formatted(purpose, otpCode, expiryMinutes);
    }
}
