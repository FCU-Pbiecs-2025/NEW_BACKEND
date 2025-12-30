package Group4.Childcare.controller;

import Group4.Childcare.Service.AuthService;
import Group4.Childcare.Service.PasswordResetService;
import Group4.Childcare.Service.RecaptchaService;
import Group4.Childcare.DTO.ForgotPasswordRequest;
import Group4.Childcare.DTO.LoginRequest;
import Group4.Childcare.DTO.VerifyResetTokenRequest;
import Group4.Childcare.DTO.ResetPasswordRequest;
import Group4.Childcare.Controller.AuthController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 單元測試
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private RecaptchaService recaptchaService;

    @InjectMocks
    private AuthController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    // ===== login 測試 =====
    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setAccount("testuser");
        request.setPassword("password123");
        request.setRecaptchaToken("valid-token");

        when(recaptchaService.verify("valid-token")).thenReturn(true);

        Map<String, Object> response = new HashMap<>();
        response.put("token", "jwt-token");
        response.put("user", Map.of("id", "123"));
        when(authService.login("testuser", "password123")).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("jwt-token")));
    }

    @Test
    void testLogin_InvalidCaptcha() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setAccount("testuser");
        request.setPassword("password123");
        request.setRecaptchaToken("invalid-token");

        when(recaptchaService.verify("invalid-token")).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setAccount("testuser");
        request.setPassword("wrongpassword");
        request.setRecaptchaToken("valid-token");

        when(recaptchaService.verify("valid-token")).thenReturn(true);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Invalid credentials");
        when(authService.login("testuser", "wrongpassword")).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ===== register 測試 =====
    @Test
    void testRegister_Success() throws Exception {
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("account", "newuser");
        registerRequest.put("password", "password123");
        registerRequest.put("email", "test@example.com");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());
    }

    // ===== forgotPassword 測試 =====
    @Test
    void testForgotPassword_Success() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");
        request.setRecaptchaToken("valid-token");

        when(recaptchaService.verify("valid-token")).thenReturn(true);
        doNothing().when(passwordResetService).requestReset("test@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testForgotPassword_EmptyEmail() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("");
        request.setRecaptchaToken("valid-token");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testForgotPassword_NullEmail() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setRecaptchaToken("valid-token");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testForgotPassword_InvalidCaptcha() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");
        request.setRecaptchaToken("invalid-token");

        when(recaptchaService.verify("invalid-token")).thenReturn(false);

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===== verifyResetToken 測試 =====
    @Test
    void testVerifyResetToken_Valid() throws Exception {
        VerifyResetTokenRequest request = new VerifyResetTokenRequest();
        request.setEmail("test@example.com");
        request.setToken("valid-token");

        when(passwordResetService.verifyToken("test@example.com", "valid-token")).thenReturn(true);

        mockMvc.perform(post("/api/auth/verify-reset-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testVerifyResetToken_Invalid() throws Exception {
        VerifyResetTokenRequest request = new VerifyResetTokenRequest();
        request.setEmail("test@example.com");
        request.setToken("invalid-token");

        when(passwordResetService.verifyToken("test@example.com", "invalid-token")).thenReturn(false);

        mockMvc.perform(post("/api/auth/verify-reset-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void testVerifyResetToken_MissingParams() throws Exception {
        VerifyResetTokenRequest request = new VerifyResetTokenRequest();
        request.setEmail("test@example.com");

        mockMvc.perform(post("/api/auth/verify-reset-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===== resetPassword 測試 =====
    @Test
    void testResetPassword_Success() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setToken("valid-token");
        request.setNewPassword("newPassword123");

        when(passwordResetService.resetPassword("test@example.com", "valid-token", "newPassword123"))
                .thenReturn(true);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testResetPassword_Failed() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setToken("invalid-token");
        request.setNewPassword("newPassword123");

        when(passwordResetService.resetPassword("test@example.com", "invalid-token", "newPassword123"))
                .thenReturn(false);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void testResetPassword_MissingParams() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
