package Group4.Childcare.controller;

import Group4.Childcare.Controller.AuthController;
import Group4.Childcare.DTO.LoginRequest;
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
import static org.mockito.Mockito.when;
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

    @Test
    @DisplayName("TC-L-01: 登入成功 - reCAPTCHA 通過且帳密正確")
    public void testLoginSuccess() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("validUser");
        loginRequest.setPassword("validPass");
        loginRequest.setRecaptchaToken("valid-token");

        // Mock reCAPTCHA 驗證通過
        when(recaptchaService.verify("valid-token")).thenReturn(true);

        // Mock AuthService 登入成功
        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("success", true);
        successResponse.put("user", Map.of("account", "validUser", "name", "Test User"));
        when(authService.login("validUser", "validPass")).thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.account").value("validUser"));
    }

    @Test
    @DisplayName("TC-L-02: 機器人驗證失敗 - reCAPTCHA 驗證失敗")
    public void testLoginRecaptchaFail() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("validUser");
        loginRequest.setPassword("validPass");
        loginRequest.setRecaptchaToken("invalid-token");

        // Mock reCAPTCHA 驗證失敗
        when(recaptchaService.verify("invalid-token")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("機器人驗證失敗，請重試！"));
    }

    @Test
    @DisplayName("TC-L-03: 帳號或密碼錯誤 - reCAPTCHA 通過但 AuthService 回傳錯誤")
    public void testLoginAuthFail() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("wrongUser");
        loginRequest.setPassword("wrongPass");
        loginRequest.setRecaptchaToken("valid-token");

        // Mock reCAPTCHA 驗證通過
        when(recaptchaService.verify("valid-token")).thenReturn(true);

        // Mock AuthService 登入失敗 (包含 error key)
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", "帳號或密碼錯誤");
        when(authService.login("wrongUser", "wrongPass")).thenReturn(errorResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("帳號或密碼錯誤"));
    }

    @Test
    @DisplayName("TC-L-04: 帳號被停用 - reCAPTCHA 通過但帳號狀態異常")
    public void testLoginAccountDisabled() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("disabledUser");
        loginRequest.setPassword("validPass");
        loginRequest.setRecaptchaToken("valid-token");

        // Mock reCAPTCHA 驗證通過
        when(recaptchaService.verify("valid-token")).thenReturn(true);

        // Mock AuthService 回傳帳號停用錯誤 (包含 error key)
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", "Account Disabled");
        errorResponse.put("message", "帳號未啟用或已被停用");
        when(authService.login("disabledUser", "validPass")).thenReturn(errorResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("帳號未啟用或已被停用"));
    }
}
