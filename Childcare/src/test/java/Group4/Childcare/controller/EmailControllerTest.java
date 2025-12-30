package Group4.Childcare.controller;

import Group4.Childcare.Controller.EmailController;
import Group4.Childcare.Service.EmailService;
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

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * EmailController 單元測試
 * 
 * 測試覆蓋:
 * 1. sendSimpleEmail - 發送簡單郵件
 * 2. sendHtmlEmail - 發送HTML郵件
 * 3. sendRegistrationConfirmation - 發送註冊確認郵件
 * 4. sendPasswordResetEmail - 發送密碼重設郵件
 * 5. sendApplicationStatusEmail - 發送申請狀態通知郵件
 */
@ExtendWith(MockitoExtension.class)
class EmailControllerTest {

        @Mock
        private EmailService emailService;

        @InjectMocks
        private EmailController controller;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .defaultResponseCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8)
                                .build();
                objectMapper = new ObjectMapper();
        }

        // ===== sendSimpleEmail 測試 =====
        @Test
        void testSendSimpleEmail_Success() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("subject", "測試主旨");
                request.put("text", "測試內容");

                doNothing().when(emailService).sendSimpleEmail(anyString(), anyString(), anyString());

                mockMvc.perform(post("/api/email/send-simple")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(content().string("郵件發送成功"));

                verify(emailService, times(1)).sendSimpleEmail("test@example.com", "測試主旨", "測試內容");
        }

        @Test
        void testSendSimpleEmail_MissingTo() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("subject", "測試主旨");
                request.put("text", "測試內容");

                mockMvc.perform(post("/api/email/send-simple")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("收件人信箱不能為空"));
        }

        @Test
        void testSendSimpleEmail_EmptyTo() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "  ");
                request.put("subject", "測試主旨");
                request.put("text", "測試內容");

                mockMvc.perform(post("/api/email/send-simple")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("收件人信箱不能為空"));
        }

        @Test
        void testSendSimpleEmail_MissingSubject() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("text", "測試內容");

                mockMvc.perform(post("/api/email/send-simple")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("郵件主旨不能為空"));
        }

        @Test
        void testSendSimpleEmail_EmptySubject() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("subject", "");
                request.put("text", "測試內容");

                mockMvc.perform(post("/api/email/send-simple")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("郵件主旨不能為空"));
        }

        @Test
        void testSendSimpleEmail_MissingText() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("subject", "測試主旨");

                mockMvc.perform(post("/api/email/send-simple")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("郵件內容不能為空"));
        }

        @Test
        void testSendSimpleEmail_Exception() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("subject", "測試主旨");
                request.put("text", "測試內容");

                doThrow(new RuntimeException("SMTP error")).when(emailService)
                                .sendSimpleEmail(anyString(), anyString(), anyString());

                mockMvc.perform(post("/api/email/send-simple")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(content().string(org.hamcrest.Matchers.containsString("郵件發送失敗")));
        }

        // ===== sendHtmlEmail 測試 =====
        @Test
        void testSendHtmlEmail_Success() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("subject", "HTML主旨");
                request.put("htmlContent", "<h1>Hello</h1>");

                doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());

                mockMvc.perform(post("/api/email/send-html")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(content().string("HTML郵件發送成功"));
        }

        @Test
        void testSendHtmlEmail_MissingTo() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("subject", "HTML主旨");
                request.put("htmlContent", "<h1>Hello</h1>");

                mockMvc.perform(post("/api/email/send-html")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("收件人信箱不能為空"));
        }

        @Test
        void testSendHtmlEmail_MissingSubject() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("htmlContent", "<h1>Hello</h1>");

                mockMvc.perform(post("/api/email/send-html")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("郵件主旨不能為空"));
        }

        @Test
        void testSendHtmlEmail_MissingHtmlContent() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("subject", "HTML主旨");

                mockMvc.perform(post("/api/email/send-html")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("HTML內容不能為空"));
        }

        @Test
        void testSendHtmlEmail_Exception() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("subject", "HTML主旨");
                request.put("htmlContent", "<h1>Hello</h1>");

                doThrow(new RuntimeException("SMTP error")).when(emailService)
                                .sendHtmlEmail(anyString(), anyString(), anyString());

                mockMvc.perform(post("/api/email/send-html")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(content().string(org.hamcrest.Matchers.containsString("郵件發送失敗")));
        }

        // ===== sendRegistrationConfirmation 測試 =====
        @Test
        void testSendRegistrationConfirmation_Success() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("username", "testuser");

                doNothing().when(emailService).sendRegistrationConfirmation(anyString(), anyString());

                mockMvc.perform(post("/api/email/send-registration-confirmation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(content().string("註冊確認郵件發送成功"));
        }

        @Test
        void testSendRegistrationConfirmation_MissingParams() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");

                mockMvc.perform(post("/api/email/send-registration-confirmation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("缺少必要參數：to, username"));
        }

        @Test
        void testSendRegistrationConfirmation_Exception() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("username", "testuser");

                doThrow(new RuntimeException("SMTP error")).when(emailService)
                                .sendRegistrationConfirmation(anyString(), anyString());

                mockMvc.perform(post("/api/email/send-registration-confirmation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(content().string(org.hamcrest.Matchers.containsString("郵件發送失敗")));
        }

        // ===== sendPasswordResetEmail 測試 =====
        @Test
        void testSendPasswordResetEmail_Success() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("resetToken", "abc123token");

                doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());

                mockMvc.perform(post("/api/email/send-password-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(content().string("密碼重設郵件發送成功"));
        }

        @Test
        void testSendPasswordResetEmail_MissingParams() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");

                mockMvc.perform(post("/api/email/send-password-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("缺少必要參數：to, resetToken"));
        }

        @Test
        void testSendPasswordResetEmail_Exception() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("resetToken", "abc123token");

                doThrow(new RuntimeException("SMTP error")).when(emailService)
                                .sendPasswordResetEmail(anyString(), anyString());

                mockMvc.perform(post("/api/email/send-password-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(content().string(org.hamcrest.Matchers.containsString("郵件發送失敗")));
        }

        // ===== sendApplicationStatusEmail 測試 =====
        @Test
        void testSendApplicationStatusEmail_Success() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("applicantName", "測試者");
                request.put("status", "已錄取");
                request.put("details", "恭喜您已錄取");

                doNothing().when(emailService).sendApplicationStatusEmail(anyString(), anyString(), anyString(),
                                anyString());

                mockMvc.perform(post("/api/email/send-application-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(content().string("申請狀態通知郵件發送成功"));
        }

        @Test
        void testSendApplicationStatusEmail_MissingTo() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("applicantName", "測試者");
                request.put("status", "已錄取");
                request.put("details", "恭喜您已錄取");

                mockMvc.perform(post("/api/email/send-application-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("缺少必要參數：to, applicantName, status, details"));
        }

        @Test
        void testSendApplicationStatusEmail_MissingApplicantName() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("status", "已錄取");
                request.put("details", "恭喜您已錄取");

                mockMvc.perform(post("/api/email/send-application-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("缺少必要參數：to, applicantName, status, details"));
        }

        @Test
        void testSendApplicationStatusEmail_Exception() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("to", "test@example.com");
                request.put("applicantName", "測試者");
                request.put("status", "已錄取");
                request.put("details", "恭喜您已錄取");

                doThrow(new RuntimeException("SMTP error")).when(emailService)
                                .sendApplicationStatusEmail(anyString(), anyString(), anyString(), anyString());

                mockMvc.perform(post("/api/email/send-application-status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(content().string(org.hamcrest.Matchers.containsString("郵件發送失敗")));
        }
}
