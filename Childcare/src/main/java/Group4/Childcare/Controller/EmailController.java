package Group4.Childcare.Controller;

import Group4.Childcare.Service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
@CrossOrigin
public class EmailController {

    @Autowired
    private EmailService emailService;

    /**
     * 發送簡單郵件
     */
    @PostMapping("/send-simple")
    public ResponseEntity<String> sendSimpleEmail(@RequestBody Map<String, String> request) {
        try {
            String to = request.get("to");
            String subject = request.get("subject");
            String text = request.get("text");

            if (to == null || to.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("收件人信箱不能為空");
            }
            if (subject == null || subject.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("郵件主旨不能為空");
            }
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("郵件內容不能為空");
            }

            emailService.sendSimpleEmail(to, subject, text);
            return ResponseEntity.ok("郵件發送成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("郵件發送失敗：" + e.getMessage());
        }
    }

    /**
     * 發送HTML郵件
     */
    @PostMapping("/send-html")
    public ResponseEntity<String> sendHtmlEmail(@RequestBody Map<String, String> request) {
        try {
            String to = request.get("to");
            String subject = request.get("subject");
            String htmlContent = request.get("htmlContent");

            if (to == null || to.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("收件人信箱不能為空");
            }
            if (subject == null || subject.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("郵件主旨不能為空");
            }
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("HTML內容不能為空");
            }

            emailService.sendHtmlEmail(to, subject, htmlContent);
            return ResponseEntity.ok("HTML郵件發送成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("郵件發送失敗：" + e.getMessage());
        }
    }

    /**
     * 發送註冊確認郵件
     */
    @PostMapping("/send-registration-confirmation")
    public ResponseEntity<String> sendRegistrationConfirmation(@RequestBody Map<String, String> request) {
        try {
            String to = request.get("to");
            String username = request.get("username");

            if (to == null || username == null) {
                return ResponseEntity.badRequest().body("缺少必要參數：to, username");
            }

            emailService.sendRegistrationConfirmation(to, username);
            return ResponseEntity.ok("註冊確認郵件發送成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("郵件發送失敗：" + e.getMessage());
        }
    }

    /**
     * 發送密碼重設郵件
     */
    @PostMapping("/send-password-reset")
    public ResponseEntity<String> sendPasswordResetEmail(@RequestBody Map<String, String> request) {
        try {
            String to = request.get("to");
            String resetToken = request.get("resetToken");

            if (to == null || resetToken == null) {
                return ResponseEntity.badRequest().body("缺少必要參數：to, resetToken");
            }

            emailService.sendPasswordResetEmail(to, resetToken);
            return ResponseEntity.ok("密碼重設郵件發送成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("郵件發送失敗：" + e.getMessage());
        }
    }

    /**
     * 發送申請狀態通知郵件
     */
    @PostMapping("/send-application-status")
    public ResponseEntity<String> sendApplicationStatusEmail(@RequestBody Map<String, String> request) {
        try {
            String to = request.get("to");
            String applicantName = request.get("applicantName");
            String status = request.get("status");
            String details = request.get("details");

            if (to == null || applicantName == null || status == null || details == null) {
                return ResponseEntity.badRequest().body("缺少必要參數：to, applicantName, status, details");
            }

            emailService.sendApplicationStatusEmail(to, applicantName, status, details);
            return ResponseEntity.ok("申請狀態通知郵件發送成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("郵件發送失敗：" + e.getMessage());
        }
    }
}
