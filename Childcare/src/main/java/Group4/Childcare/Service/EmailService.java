package Group4.Childcare.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String frontendUrl;

    @Autowired
    public EmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String fromEmail,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.frontendUrl = frontendUrl;
    }

    /**
     * 檢查郵件服務是否可用
     */
    private boolean isMailServiceAvailable() {
        if (mailSender == null) {
            System.out.println("Warning: JavaMailSender is not configured. Email will not be sent.");
            return false;
        }
        return true;
    }

    /**
     * 發送簡單文字郵件
     * @param toEmail 收件人 email
     * @param subject 郵件主旨
     * @param text 郵件內容
     */
    public void sendSimpleEmail(String toEmail, String subject, String text) {
        if (!isMailServiceAvailable()) {
            System.out.println("=== 郵件模擬（未配置 JavaMailSender）===");
            System.out.println("收件人: " + toEmail);
            System.out.println("主旨: " + subject);
            System.out.println("內容: " + text);
            System.out.println("==========================================");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
        System.out.println("Simple email sent to: " + toEmail);
    }

    /**
     * 發送 HTML 格式郵件
     * @param toEmail 收件人 email
     * @param subject 郵件主旨
     * @param htmlContent HTML 內容
     */
    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) throws MessagingException {
        if (!isMailServiceAvailable()) {
            System.out.println("=== HTML 郵件模擬（未配置 JavaMailSender）===");
            System.out.println("收件人: " + toEmail);
            System.out.println("主旨: " + subject);
            System.out.println("HTML 內容: " + htmlContent);
            System.out.println("==========================================");
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true 表示是 HTML
        mailSender.send(message);
        System.out.println("HTML email sent to: " + toEmail);
    }

    /**
     * 發送註冊確認郵件
     * @param toEmail 收件人 email
     * @param username 使用者名稱
     */
    public void sendRegistrationConfirmation(String toEmail, String username) {
        String subject = "【托育系統】註冊成功通知";
        String text =
            "親愛的 " + username + " 您好，\n\n" +
            "恭喜您已成功註冊托育系統帳號！\n\n" +
            "您現在可以使用此帳號登入系統，享受我們提供的各項服務。\n\n" +
            "如有任何問題，請隨時與我們聯繫。\n\n" +
            "此郵件為系統自動發送，請勿直接回覆。\n\n" +
            "托育系統 敬上";

        sendSimpleEmail(toEmail, subject, text);
    }

    /**
     * 發送重設密碼郵件
     * @param toEmail 收件人 email
     * @param resetToken 重設密碼 token
     */
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String subject = "【托育系統】重設密碼連結";
        String text =
            "親愛的使用者您好，\n\n" +
            "我們收到了您重設密碼的請求。\n\n" +
            "請點擊以下連結重設您的密碼（30 分鐘內有效）：\n\n" +
            resetLink + "\n\n" +
            "如果您沒有申請重設密碼，請忽略此郵件。\n\n" +
            "此郵件為系統自動發送，請勿直接回覆。\n\n" +
            "托育系統 敬上";

        sendSimpleEmail(toEmail, subject, text);
    }

    /**
     * 發送申請狀態通知郵件
     * @param toEmail 收件人 email
     * @param applicantName 申請人姓名
     * @param status 申請狀態
     * @param details 詳細說明
     */
    public void sendApplicationStatusEmail(String toEmail, String applicantName, String status, String details) {
        String subject = "【托育系統】申請狀態更新通知";
        String text =
            "親愛的 " + applicantName + " 您好，\n\n" +
            "您的托育申請狀態已更新。\n\n" +
            "目前狀態：" + status + "\n\n" +
            "詳細說明：\n" + details + "\n\n" +
            "如有任何疑問，請登入系統查詢或與我們聯繫。\n\n" +
            "此郵件為系統自動發送，請勿直接回覆。\n\n" +
            "托育系統 敬上";

        sendSimpleEmail(toEmail, subject, text);
    }

    /**
     * 發送一般通知郵件（別名方法）
     * @param toEmail 收件人 email
     * @param subject 郵件主旨
     * @param content 郵件內容
     */
    public void sendEmail(String toEmail, String subject, String content) {
        sendSimpleEmail(toEmail, subject, content);
    }
}

