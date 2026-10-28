package com.cropdetector.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailService, "mailUsername", "testsender@gmail.com");
        ReflectionTestUtils.setField(emailService, "mailFrom", "testsender@gmail.com");
        ReflectionTestUtils.setField(emailService, "senderName", "FloraScan Support");
    }

    @Test
    void skipsDispatchWhenEmailDeliveryDisabled() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", false);

        boolean sent = emailService.sendOtpEmail("farmer@example.com", "123456", "LOGIN", 300);

        assertFalse(sent);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void skipsDispatchWhenUsernameIsBlank() {
        ReflectionTestUtils.setField(emailService, "mailUsername", "");

        boolean sent = emailService.sendOtpEmail("farmer@example.com", "123456", "LOGIN", 300);

        assertFalse(sent);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendsEmailSuccessfullyWhenConfigured() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        boolean sent = emailService.sendOtpEmail("farmer@example.com", "654321", "LOGIN", 300);

        assertTrue(sent);
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void handlesMailExceptionGracefullyWithoutThrowing() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP connection timed out")).when(mailSender).send(mimeMessage);

        boolean sent = emailService.sendOtpEmail("farmer@example.com", "654321", "REGISTER", 300);

        assertFalse(sent);
        verify(mailSender, times(1)).send(mimeMessage);
    }
}
