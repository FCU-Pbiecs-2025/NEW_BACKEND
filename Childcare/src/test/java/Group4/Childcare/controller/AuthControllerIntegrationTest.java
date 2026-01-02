package Group4.Childcare.controller;

import Group4.Childcare.Controller.AuthController;
import Group4.Childcare.DTO.ForgotPasswordRequest;
import Group4.Childcare.DTO.LoginRequest;
import Group4.Childcare.DTO.ResetPasswordRequest;
import Group4.Childcare.DTO.VerifyResetTokenRequest;
import Group4.Childcare.Service.AuthService;
import Group4.Childcare.Service.PasswordResetService;
import Group4.Childcare.Service.RecaptchaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AuthControllerIntegrationTest {

    private MockMvc mockMvc;

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    @Mock
    private RecaptchaService recaptchaService;

    @Mock
    private PasswordResetService passwordResetService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        // 使用 standaloneSetup 建構 MockMvc，不需要 Spring Context
        this.mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    // ============================================================
    // Login 測試
    // ============================================================

    @Test
    @DisplayName("TC-L-01: 登入成功 - reCAPTCHA 通過且帳密正確")
    public void testLoginSuccess() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("validUser");
        loginRequest.setPassword("validPass");
        loginRequest.setRecaptchaToken("valid-token");

        when(recaptchaService.verify("valid-token")).thenReturn(true);
        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("success", true);
        successResponse.put("user", Map.of("account", "validUser"));
        when(authService.login("validUser", "validPass")).thenReturn(successResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("TC-L-02: 機器人驗證失敗")
    public void testLoginRecaptchaFail() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("validUser");
        loginRequest.setPassword("validPass");
        loginRequest.setRecaptchaToken("invalid-token");

        when(recaptchaService.verify("invalid-token")).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("機器人驗證失敗，請重試！"));
    }

    @Test
    @DisplayName("TC-L-03: 帳號或密碼錯誤")
    public void testLoginAuthFail() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("wrongUser");
        loginRequest.setPassword("wrongPass");
        loginRequest.setRecaptchaToken("valid-token");

        when(recaptchaService.verify("valid-token")).thenReturn(true);
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", "帳號或密碼錯誤");
        when(authService.login("wrongUser", "wrongPass")).thenReturn(errorResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("帳號或密碼錯誤"));
    }

    @Test
    @DisplayName("TC-L-04: 帳號被停用")
    public void testLoginAccountDisabled() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("disabledUser");
        loginRequest.setPassword("validPass");
        loginRequest.setRecaptchaToken("valid-token");

        when(recaptchaService.verify("valid-token")).thenReturn(true);
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", "Account Disabled");
        errorResponse.put("message", "帳號未啟用或已被停用");
        when(authService.login("disabledUser", "validPass")).thenReturn(errorResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("帳號未啟用或已被停用"));
    }

    // ============================================================
    // Forgot Password 測試
    // ============================================================

    @Test
    @DisplayName("TC-FP-01: 忘記密碼 - Recaptcha Token 為空 (跳過驗證)")
    public void testForgotPassword_NoRecaptcha() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");
        request.setRecaptchaToken(null); // Token 為 null

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(recaptchaService, never()).verify(anyString());
    }

    @Test
    @DisplayName("TC-FP-04: 忘記密碼 - Recaptcha Token 為空字串 (跳過驗證)")
    public void testForgotPassword_EmptyRecaptcha() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");
        request.setRecaptchaToken(""); // Token 為空字串

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(recaptchaService, never()).verify(anyString());
    }

    @Test
    @DisplayName("TC-FP-02: 忘記密碼 - Service 拋出異常 (有 Message)")
    public void testForgotPassword_ServiceError() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("error@example.com");

        doThrow(new RuntimeException("DB Error")).when(passwordResetService).requestReset("error@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("DB Error"));
    }

    @Test
    @DisplayName("TC-FP-05: 忘記密碼 - Service 拋出異常 (無 Message)")
    public void testForgotPassword_ServiceErrorNoMessage() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("error@example.com");

        // 拋出沒有 message 的異常
        doThrow(new RuntimeException()).when(passwordResetService).requestReset("error@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("RuntimeException")); // 預期回傳 Class Name
    }

    @Test
    @DisplayName("TC-FP-03: 忘記密碼 - Email 為空")
    public void testForgotPassword_NoEmail() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("請提供信箱"));
    }

    // ============================================================
    // Verify Reset Token 測試
    // ============================================================

    @Test
    @DisplayName("TC-VR-01: 驗證 Token - Email 為空字串")
    public void testVerifyToken_EmptyEmail() throws Exception {
        VerifyResetTokenRequest request = new VerifyResetTokenRequest();
        request.setEmail("");
        request.setToken("valid-token");

        mockMvc.perform(post("/api/auth/verify-reset-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-VR-05: 驗證 Token - Email 為 Null")
    public void testVerifyToken_NullEmail() throws Exception {
        VerifyResetTokenRequest request = new VerifyResetTokenRequest();
        request.setEmail(null);
        request.setToken("valid-token");

        mockMvc.perform(post("/api/auth/verify-reset-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-VR-02: 驗證 Token - Token 為空字串")
    public void testVerifyToken_EmptyToken() throws Exception {
        VerifyResetTokenRequest request = new VerifyResetTokenRequest();
        request.setEmail("test@example.com");
        request.setToken("");

        mockMvc.perform(post("/api/auth/verify-reset-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-VR-06: 驗證 Token - Token 為 Null")
    public void testVerifyToken_NullToken() throws Exception {
        VerifyResetTokenRequest request = new VerifyResetTokenRequest();
        request.setEmail("test@example.com");
        request.setToken(null);

        mockMvc.perform(post("/api/auth/verify-reset-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-VR-03: 驗證 Token - 成功")
    public void testVerifyToken_Success() throws Exception {
        VerifyResetTokenRequest request = new VerifyResetTokenRequest();
        request.setEmail("test@example.com");
        request.setToken("valid-token");

        when(passwordResetService.verifyToken("test@example.com", "valid-token")).thenReturn(true);

        mockMvc.perform(post("/api/auth/verify-reset-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("TC-VR-04: 驗證 Token - 失敗")
    public void testVerifyToken_Fail() throws Exception {
        VerifyResetTokenRequest request = new VerifyResetTokenRequest();
        request.setEmail("test@example.com");
        request.setToken("invalid-token");

        when(passwordResetService.verifyToken("test@example.com", "invalid-token")).thenReturn(false);

        mockMvc.perform(post("/api/auth/verify-reset-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ============================================================
    // Reset Password 測試
    // ============================================================

    @Test
    @DisplayName("TC-RP-01: 重設密碼 - NewPassword 為 Null")
    public void testResetPassword_NoPassword() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setToken("valid-token");
        request.setNewPassword(null);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-RP-04: 重設密碼 - Email 為 Null")
    public void testResetPassword_NullEmail() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(null);
        request.setToken("valid-token");
        request.setNewPassword("newPass123");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-RP-06: 重設密碼 - Email 為空字串")
    public void testResetPassword_EmptyEmail() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("");
        request.setToken("valid-token");
        request.setNewPassword("newPass123");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-RP-05: 重設密碼 - Token 為 Null")
    public void testResetPassword_NullToken() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setToken(null);
        request.setNewPassword("newPass123");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-RP-07: 重設密碼 - Token 為空字串")
    public void testResetPassword_EmptyToken() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setToken("");
        request.setNewPassword("newPass123");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-RP-02: 重設密碼 - 成功")
    public void testResetPassword_Success() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setToken("valid-token");
        request.setNewPassword("newPass123");

        when(passwordResetService.resetPassword("test@example.com", "valid-token", "newPass123")).thenReturn(true);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("TC-RP-03: 重設密碼 - 失敗")
    public void testResetPassword_Fail() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setToken("invalid-token");
        request.setNewPassword("newPass123");

        when(passwordResetService.resetPassword("test@example.com", "invalid-token", "newPass123")).thenReturn(false);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }
}
