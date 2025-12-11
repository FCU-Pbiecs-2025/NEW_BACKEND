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

    /**
     * 發送審核狀態變更通知郵件（HTML 格式）
     * @param toEmail 收件人 email
     * @param applicantName 申請人名稱
     * @param childName 幼兒名稱
     * @param institutionName 申請機構名稱
     * @param caseNumber 案件編號
     * @param applicationDate 申請日期
     * @param newStatus 新的審核狀態
     * @param currentOrder 目前序號（可為 null）
     * @param reason 備註說明（可為 null）
     */
    public void sendApplicationStatusChangeEmail(
            String toEmail,
            String applicantName,
            String childName,
            String institutionName,
            Long caseNumber,
            String applicationDate,
            String newStatus,
            Integer currentOrder,
            String reason) throws MessagingException {

        String subject = "【托育申請審核通知】" + getStatusDisplay(newStatus);
        String htmlContent = buildApplicationStatusChangeEmail(
                applicantName,
                childName,
                institutionName,
                caseNumber,
                applicationDate,
                newStatus,
                currentOrder,
                reason
        );

        sendHtmlEmail(toEmail, subject, htmlContent);
        System.out.println("✅ 審核狀態變更通知郵件已發送給: " + toEmail + " (狀態: " + newStatus + ")");
    }

    /**
     * 構建審核狀態變更的 HTML 郵件內容
     */
    private String buildApplicationStatusChangeEmail(
            String applicantName,
            String childName,
            String institutionName,
            Long caseNumber,
            String applicationDate,
            String status,
            Integer currentOrder,
            String reason) {

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <style>\n");
        html.append("    * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("    body { font-family: 'Segoe UI', 'Microsoft YaHei', Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f5f5f5; }\n");
        html.append("    .container { max-width: 600px; margin: 20px auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }\n");
        html.append("    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px 20px; text-align: center; }\n");
        html.append("    .header h1 { font-size: 24px; margin-bottom: 5px; }\n");
        html.append("    .header p { font-size: 14px; opacity: 0.9; }\n");
        html.append("    .content { padding: 30px 20px; }\n");
        html.append("    .greeting { font-size: 16px; margin-bottom: 20px; }\n");
        html.append("    .status-box { background: linear-gradient(135deg, ").append(getStatusGradient(status)).append("); color: white; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0; }\n");
        html.append("    .status-box .status-text { font-size: 28px; font-weight: bold; margin-bottom: 5px; }\n");
        html.append("    .status-box .status-label { font-size: 14px; opacity: 0.9; }\n");
        html.append("    .case-summary { background: #f9f9f9; border-left: 4px solid #667eea; padding: 15px; margin: 20px 0; border-radius: 4px; }\n");
        html.append("    .summary-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #eee; }\n");
        html.append("    .summary-row:last-child { border-bottom: none; }\n");
        html.append("    .summary-label { font-weight: 600; color: #667eea; width: 120px; }\n");
        html.append("    .summary-value { color: #333; flex: 1; text-align: right; }\n");
        html.append("    .message-box { background: ").append(getStatusMessageBg(status)).append("; border: 1px solid ").append(getStatusBorder(status)).append("; padding: 15px; border-radius: 4px; margin: 20px 0; }\n");
        html.append("    .message-box p { font-size: 14px; line-height: 1.6; }\n");
        html.append("    .reason-section { margin: 20px 0; padding: 15px; background: #fff3cd; border-left: 4px solid #ffc107; border-radius: 4px; }\n");
        html.append("    .reason-section .label { font-weight: 600; color: #856404; margin-bottom: 8px; }\n");
        html.append("    .reason-section .content { color: #856404; font-size: 14px; line-height: 1.6; }\n");
        html.append("    .footer { background: #f5f5f5; padding: 20px; text-align: center; font-size: 12px; color: #999; border-top: 1px solid #eee; }\n");
        html.append("    .footer p { margin: 5px 0; }\n");
        html.append("    .next-steps { background: #e8f4f8; border: 1px solid #b3d9e3; padding: 15px; border-radius: 4px; margin: 20px 0; }\n");
        html.append("    .next-steps h3 { color: #0066cc; font-size: 14px; margin-bottom: 10px; }\n");
        html.append("    .next-steps ul { margin-left: 20px; font-size: 14px; }\n");
        html.append("    .next-steps li { margin: 5px 0; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <div class=\"container\">\n");
        html.append("    <div class=\"header\">\n");
        html.append("      <h1>📋 托育申請審核通知</h1>\n");
        html.append("      <p>Application Status Update</p>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"content\">\n");
        html.append("      <div class=\"greeting\">\n");
        html.append("        <p>親愛的 <strong>").append(applicantName).append("</strong> 您好，</p>\n");
        html.append("      </div>\n");

        // 狀態盒子
        html.append("      <div class=\"status-box\">\n");
        html.append("        <div class=\"status-text\">").append(getStatusEmoji(status)).append(" ").append(getStatusDisplay(status)).append("</div>\n");
        html.append("        <div class=\"status-label\">您的申請狀態已更新</div>\n");
        html.append("      </div>\n");

        // 案件摘要
        html.append("      <div class=\"case-summary\">\n");
        html.append("        <div class=\"summary-row\">\n");
        html.append("          <div class=\"summary-label\">幼兒名稱：</div>\n");
        html.append("          <div class=\"summary-value\"><strong>").append(childName).append("</strong></div>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"summary-row\">\n");
        html.append("          <div class=\"summary-label\">申請機構：</div>\n");
        html.append("          <div class=\"summary-value\"><strong>").append(institutionName).append("</strong></div>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"summary-row\">\n");
        html.append("          <div class=\"summary-label\">案件編號：</div>\n");
        html.append("          <div class=\"summary-value\">").append(caseNumber).append("</div>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"summary-row\">\n");
        html.append("          <div class=\"summary-label\">申請日期：</div>\n");
        html.append("          <div class=\"summary-value\">").append(applicationDate).append("</div>\n");
        html.append("        </div>\n");

        // 只在有序號時顯示
        if (currentOrder != null && currentOrder > 0) {
            html.append("        <div class=\"summary-row\">\n");
            html.append("          <div class=\"summary-label\">目前序號：</div>\n");
            html.append("          <div class=\"summary-value\"><strong style=\"color: #dc3545; font-size: 18px;\">").append(currentOrder).append("</strong></div>\n");
            html.append("        </div>\n");
        }

        html.append("      </div>\n");

        // 狀態訊息
        html.append("      <div class=\"message-box\">\n");
        html.append("        <p>").append(getDetailedStatusMessage(status)).append("</p>\n");
        html.append("      </div>\n");

        // 備註說明（如有）
        if (reason != null && !reason.isEmpty()) {
            html.append("      <div class=\"reason-section\">\n");
            html.append("        <div class=\"label\">備註說明：</div>\n");
            html.append("        <div class=\"content\">").append(reason).append("</div>\n");
            html.append("      </div>\n");
        }

        // 根據狀態顯示後續步驟
        html.append(getNextStepsHtml(status, currentOrder));

        html.append("      <p style=\"margin-top: 20px; font-size: 14px; color: #666;\">\n");
        html.append("        如有任何疑問，請登入系統查詢或與我們聯繫。\n");
        html.append("      </p>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"footer\">\n");
        html.append("      <p>此為系統自動發送的通知郵件，請勿直接回覆。</p>\n");
        html.append("      <p>&copy; 2024 托育申請系統. All rights reserved.</p>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    // ===== 輔助方法 =====

    private String getStatusDisplay(String status) {
        switch (status) {
            case "已錄取":
                return "已錄取";
            case "候補中":
                return "候補中";
            case "已退件":
                return "已退件";
            case "需要補件":
                return "需要補件";
            case "審核中":
                return "審核中";
            default:
                return status; // 返回原始狀態
        }
    }

    private String getStatusEmoji(String status) {
        switch (status) {
            case "已錄取":
                return "✅";
            case "候補中":
                return "⏳";
            case "已退件":
                return "❌";
            case "需要補件":
                return "⚠️";
            case "審核中":
                return "📋";
            default:
                return "📌";
        }
    }

    private String getStatusGradient(String status) {
        switch (status) {
            case "已錄取":
                return "#28a745 0%, #20c997 100%";
            case "候補中":
                return "#ffc107 0%, #fd7e14 100%";
            case "已退件":
                return "#dc3545 0%, #c82333 100%";
            case "需要補件":
                return "#ff9800 0%, #f57c00 100%";
            default:
                return "#667eea 0%, #764ba2 100%";
        }
    }

    private String getStatusMessageBg(String status) {
        switch (status) {
            case "已錄取":
                return "#d4edda";
            case "候補中":
                return "#fff3cd";
            case "已退件":
                return "#f8d7da";
            case "需要補件":
                return "#fff3cd";
            default:
                return "#e7f3ff";
        }
    }

    private String getStatusBorder(String status) {
        switch (status) {
            case "已錄取":
                return "#c3e6cb";
            case "候補中":
                return "#ffeaa7";
            case "已退件":
                return "#f5c6cb";
            case "需要補件":
                return "#ffeaa7";
            default:
                return "#b3d9e3";
        }
    }

    private String getDetailedStatusMessage(String status) {
        switch (status) {
            case "已錄取":
                return "🎉 <strong>恭喜！</strong>您的申請已通過審核，幼兒已被錄取。" +
                        "請於規定時間內完成報到手續。我們期待與您合作！";
            case "候補中":
                return "⏳ 您的申請已進入候補名單。當有名額空出時，我們將依序通知您。" +
                        "感謝您的耐心等待！";
            case "已退件":
                return "很遺憾，您的申請未通過審核。" +
                        "如對審核結果有疑問，歡迎透過系統聯繫我們進行諮詢。";
            case "需要補件":
                return "我們已收到您的申請，但需要您補齊相關文件以繼續審核流程。" +
                        "請儘快上傳所需文件，以加快審核速度。";
            default:
                return "感謝您提交的申請，目前正在審核中，請耐心等待。";
        }
    }

    private String getNextStepsHtml(String status, Integer currentOrder) {
        StringBuilder html = new StringBuilder();

        switch (status) {
            case "已錄取":
                html.append("      <div class=\"next-steps\">\n");
                html.append("        <h3>📝 後續步驟：</h3>\n");
                html.append("        <ul>\n");
                html.append("          <li>1. 登入系統確認錄取通知</li>\n");
                html.append("          <li>2. 完成線上簽約</li>\n");
                html.append("          <li>3. 按時完成報到手續</li>\n");
                html.append("          <li>4. 聯繫機構確認開學日期</li>\n");
                html.append("        </ul>\n");
                html.append("      </div>\n");
                break;
            case "候補中":
                html.append("      <div class=\"next-steps\">\n");
                html.append("        <h3>📝 後續安排：</h3>\n");
                html.append("        <ul>\n");
                html.append("          <li>您的序號：<strong>").append(currentOrder).append("</strong></li>\n");
                html.append("          <li>我們將在有名額時依序通知候補名單內的家長</li>\n");
                html.append("          <li>請保持聯繫方式暢通，以免錯過通知</li>\n");
                html.append("          <li>可同時申請其他機構以增加錄取機會</li>\n");
                html.append("        </ul>\n");
                html.append("      </div>\n");
                break;
            case "需要補件":
                html.append("      <div class=\"next-steps\">\n");
                html.append("        <h3>⚠️ 補件期限：</h3>\n");
                html.append("        <ul>\n");
                html.append("          <li>請於 <strong>7 天內</strong>補齊所有文件</li>\n");
                html.append("          <li>透過系統上傳文件</li>\n");
                html.append("          <li>逾期未補件將視為自動放棄申請</li>\n");
                html.append("        </ul>\n");
                html.append("      </div>\n");
                break;
        }

        return html.toString();
    }
}

