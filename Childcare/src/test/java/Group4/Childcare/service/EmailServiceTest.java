package Group4.Childcare.service;

import Group4.Childcare.Service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EmailService 單元測試
 * 
 * 測試覆蓋：
 * 1. sendSimpleEmail() - 簡單郵件
 * 2. sendHtmlEmail() - HTML 郵件
 * 3. sendRegistrationConfirmation() - 註冊確認郵件
 * 4. sendPasswordResetEmail() - 密碼重設郵件
 * 5. sendApplicationStatusEmail() - 申請狀態通知
 * 6. sendApplicationStatusChangeEmail() - 審核狀態變更郵件
 * 7. 各種狀態的郵件格式
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private EmailService emailService;
    private EmailService emailServiceNoMailer;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, "test@example.com", "http://localhost:5173");
        emailServiceNoMailer = new EmailService(null, "test@example.com", "http://localhost:5173");
    }

    // ========== sendSimpleEmail() 測試 ==========

    @Test
    void testSendSimpleEmail_Success() {
        emailService.sendSimpleEmail("recipient@example.com", "Test Subject", "Test Body");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendSimpleEmail_NoMailSender() {
        // 不應拋出異常，只是不會發送郵件
        emailServiceNoMailer.sendSimpleEmail("recipient@example.com", "Test Subject", "Test Body");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // ========== sendHtmlEmail() 測試 ==========

    @Test
    void testSendHtmlEmail_Success() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHtmlEmail("recipient@example.com", "Test Subject", "<html>Test</html>");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendHtmlEmail_NoMailSender() throws MessagingException {
        // 不應拋出異常，只是不會發送郵件
        emailServiceNoMailer.sendHtmlEmail("recipient@example.com", "Test Subject", "<html>Test</html>");

        verify(mailSender, never()).createMimeMessage();
    }

    // ========== sendRegistrationConfirmation() 測試 ==========

    @Test
    void testSendRegistrationConfirmation_Success() {
        emailService.sendRegistrationConfirmation("recipient@example.com", "TestUser");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendRegistrationConfirmation_NoMailSender() {
        emailServiceNoMailer.sendRegistrationConfirmation("recipient@example.com", "TestUser");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // ========== sendPasswordResetEmail() 測試 ==========

    @Test
    void testSendPasswordResetEmail_Success() {
        emailService.sendPasswordResetEmail("recipient@example.com", "reset-token-123");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendPasswordResetEmail_NoMailSender() {
        emailServiceNoMailer.sendPasswordResetEmail("recipient@example.com", "reset-token-123");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // ========== sendApplicationStatusEmail() 測試 ==========

    @Test
    void testSendApplicationStatusEmail_Success() {
        emailService.sendApplicationStatusEmail("recipient@example.com", "Test User", "已錄取", "恭喜");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendApplicationStatusEmail_NoMailSender() {
        emailServiceNoMailer.sendApplicationStatusEmail("recipient@example.com", "Test User", "已錄取", "恭喜");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // ========== sendApplicationStatusChangeEmail() 測試 ==========

    @Test
    void testSendApplicationStatusChangeEmail_Accepted() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendApplicationStatusChangeEmail(
                "recipient@example.com", "家長名", "幼兒名", "機構名",
                12345L, "2024-12-28", "已錄取", 1, null);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendApplicationStatusChangeEmail_Waitlisted() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendApplicationStatusChangeEmail(
                "recipient@example.com", "家長名", "幼兒名", "機構名",
                12345L, "2024-12-28", "候補中", 5, null);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendApplicationStatusChangeEmail_Rejected() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendApplicationStatusChangeEmail(
                "recipient@example.com", "家長名", "幼兒名", "機構名",
                12345L, "2024-12-28", "已退件", null, "不符合資格");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendApplicationStatusChangeEmail_NeedSupplement() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendApplicationStatusChangeEmail(
                "recipient@example.com", "家長名", "幼兒名", "機構名",
                12345L, "2024-12-28", "需要補件", null, "請補齊身分證影本");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendApplicationStatusChangeEmail_Reviewing() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendApplicationStatusChangeEmail(
                "recipient@example.com", "家長名", "幼兒名", "機構名",
                12345L, "2024-12-28", "審核中", null, null);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendApplicationStatusChangeEmail_UnknownStatus() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendApplicationStatusChangeEmail(
                "recipient@example.com", "家長名", "幼兒名", "機構名",
                12345L, "2024-12-28", "未知狀態", null, null);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendApplicationStatusChangeEmail_WithNullCaseNumber() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendApplicationStatusChangeEmail(
                "recipient@example.com", "家長名", "幼兒名", "機構名",
                null, "2024-12-28", "已錄取", 1, null);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendApplicationStatusChangeEmail_WithReason() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendApplicationStatusChangeEmail(
                "recipient@example.com", "家長名", "幼兒名", "機構名",
                12345L, "2024-12-28", "已錄取", 1, "額外備註說明");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendApplicationStatusChangeEmail_NoMailSender() throws MessagingException {
        // 不應拋出異常，只是不會發送郵件
        emailServiceNoMailer.sendApplicationStatusChangeEmail(
                "recipient@example.com", "家長名", "幼兒名", "機構名",
                12345L, "2024-12-28", "已錄取", 1, null);

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void testSendApplicationStatusChangeEmail_ZeroCurrentOrder() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendApplicationStatusChangeEmail(
                "recipient@example.com", "家長名", "幼兒名", "機構名",
                12345L, "2024-12-28", "候補中", 0, null);

        verify(mailSender).send(mimeMessage);
    }

    // ========== sendEmail (alias) 測試 ==========

    @Test
    void testSendEmail_Success() {
        emailService.sendEmail("recipient@example.com", "Subject", "Content");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
