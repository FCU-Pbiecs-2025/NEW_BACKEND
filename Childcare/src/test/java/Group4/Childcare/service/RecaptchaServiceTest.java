package Group4.Childcare.service;

import Group4.Childcare.DTO.RecaptchaResponse;
import Group4.Childcare.Service.RecaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RecaptchaService 單元測試
 * 測試設計：等價類劃分 + 邊界值分析
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class RecaptchaServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RecaptchaService recaptchaService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recaptchaService, "recaptchaSecret", "test-secret");
        ReflectionTestUtils.setField(recaptchaService, "recaptchaUrl", "https://test.google.com/recaptcha");
        ReflectionTestUtils.setField(recaptchaService, "recaptchaThreshold", 0.5f);
        ReflectionTestUtils.setField(recaptchaService, "restTemplate", restTemplate);
    }

    // ===== 等價類：null 或空 token =====
    @Test
    void testVerify_NullToken_ReturnsFalse() {
        boolean result = recaptchaService.verify(null);
        assertFalse(result);
    }

    @Test
    void testVerify_EmptyToken_ReturnsFalse() {
        boolean result = recaptchaService.verify("");
        assertFalse(result);
    }

    // ===== 等價類：有效 token + 成功響應 =====
    @Test
    void testVerify_ValidToken_HighScore_ReturnsTrue() {
        RecaptchaResponse response = new RecaptchaResponse();
        response.setSuccess(true);
        response.setScore(0.9f);

        when(restTemplate.postForObject(anyString(), any(), eq(RecaptchaResponse.class)))
                .thenReturn(response);

        boolean result = recaptchaService.verify("valid-token");

        assertTrue(result);
    }

    @Test
    void testVerify_ValidToken_LowScore_ReturnsFalse() {
        RecaptchaResponse response = new RecaptchaResponse();
        response.setSuccess(true);
        response.setScore(0.3f); // 低於閾值 0.5

        when(restTemplate.postForObject(anyString(), any(), eq(RecaptchaResponse.class)))
                .thenReturn(response);

        boolean result = recaptchaService.verify("valid-token");

        assertFalse(result);
    }

    // ===== 邊界值：score = threshold =====
    @Test
    void testVerify_ValidToken_ExactThreshold_ReturnsTrue() {
        RecaptchaResponse response = new RecaptchaResponse();
        response.setSuccess(true);
        response.setScore(0.5f); // 剛好等於閾值

        when(restTemplate.postForObject(anyString(), any(), eq(RecaptchaResponse.class)))
                .thenReturn(response);

        boolean result = recaptchaService.verify("valid-token");

        assertTrue(result);
    }

    // ===== 等價類：失敗響應 =====
    @Test
    void testVerify_NotSuccess_ReturnsFalse() {
        RecaptchaResponse response = new RecaptchaResponse();
        response.setSuccess(false);
        response.setScore(0.9f);

        when(restTemplate.postForObject(anyString(), any(), eq(RecaptchaResponse.class)))
                .thenReturn(response);

        boolean result = recaptchaService.verify("invalid-token");

        assertFalse(result);
    }

    @Test
    void testVerify_NullResponse_ReturnsFalse() {
        when(restTemplate.postForObject(anyString(), any(), eq(RecaptchaResponse.class)))
                .thenReturn(null);

        boolean result = recaptchaService.verify("some-token");

        assertFalse(result);
    }

    // ===== 異常處理 =====
    @Test
    void testVerify_Exception_ReturnsFalse() {
        when(restTemplate.postForObject(anyString(), any(), eq(RecaptchaResponse.class)))
                .thenThrow(new RuntimeException("Network error"));

        boolean result = recaptchaService.verify("some-token");

        assertFalse(result);
    }
}
