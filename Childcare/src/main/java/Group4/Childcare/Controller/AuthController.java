package Group4.Childcare.Controller;

import Group4.Childcare.Service.AuthService;
import Group4.Childcare.Service.PasswordResetService;
import Group4.Childcare.Service.RecaptchaService;
import Group4.Childcare.DTO.ForgotPasswordRequest;
import Group4.Childcare.DTO.VerifyResetTokenRequest;
import Group4.Childcare.DTO.ResetPasswordRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin // 若前後端不同網域就需要設定
public class AuthController {

    @Autowired
    private AuthService authService;

    // Added services for password reset
    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private RecaptchaService recaptchaService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginRequest) {
        String account = loginRequest.get("account");
        String password = loginRequest.get("password");
        return authService.login(account, password);
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> registerRequest) {
        return authService.register(registerRequest);
    }

    // New endpoint: forgot password
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            System.out.println("=== Received forgot-password request ===");
            System.out.println("Email: " + request.getEmail());
            System.out.println("RecaptchaToken: " + request.getRecaptchaToken());

            if (request.getEmail() == null || request.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "請提供信箱"));
            }

//             暫時註解 reCAPTCHA 驗證，以便測試寄信功能
             if (request.getRecaptchaToken() != null && !request.getRecaptchaToken().isBlank()) {
                 if (!recaptchaService.verify(request.getRecaptchaToken())) {
                     return ResponseEntity.badRequest().body(Map.of("success", false, "message", "reCAPTCHA 驗證失敗"));
                 }
             }

            passwordResetService.requestReset(request.getEmail());
            System.out.println("=== Password reset request processed ===");

            // Always return generic success
            return ResponseEntity.ok(Map.of("success", true, "message", "若信箱存在，重置連結已寄出"));
        } catch (Exception e) {
            System.err.println("=== Error in forgotPassword ===");
            System.err.println("Error type: " + e.getClass().getName());
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();

            // Return more detailed error for debugging
            String errorDetail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "處理請求時發生錯誤",
                "error", errorDetail
            ));
        }
    }

    // New endpoint: verify reset token
    @PostMapping("/verify-reset-token")
    public ResponseEntity<?> verifyResetToken(@RequestBody VerifyResetTokenRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank() || request.getToken() == null || request.getToken().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少必要參數"));
        }
        boolean valid = passwordResetService.verifyToken(request.getEmail(), request.getToken());
        if (!valid) {
            return ResponseEntity.ok(Map.of("success", false, "message", "驗證失敗或連結已失效"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Token 有效"));
    }

    // New endpoint: reset password
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank() || request.getToken() == null || request.getToken().isBlank() || request.getNewPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少必要參數"));
        }
        // 暫時註解 reCAPTCHA 驗證，以便測試重置密碼功能
        // if (!recaptchaService.verify(request.getRecaptchaToken())) {
        //     return ResponseEntity.badRequest().body(Map.of("success", false, "message", "reCAPTCHA 驗證失敗"));
        // }
        boolean success = passwordResetService.resetPassword(request.getEmail(), request.getToken(), request.getNewPassword());
        if (!success) {
            return ResponseEntity.ok(Map.of("success", false, "message", "密碼重置失敗"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "密碼重置成功"));
    }
}