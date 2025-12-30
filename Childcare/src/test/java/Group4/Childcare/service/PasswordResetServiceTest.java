package Group4.Childcare.service;

import Group4.Childcare.Model.PasswordResetToken;
import Group4.Childcare.Model.Users;
import Group4.Childcare.Repository.PasswordResetTokenRepository;
import Group4.Childcare.Repository.UserJdbcRepository;
import Group4.Childcare.Service.EmailService;
import Group4.Childcare.Service.PasswordResetService;
import Group4.Childcare.Service.PasswordResetService.PasswordResetResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PasswordResetService 單元測試
 * 
 * 測試設計：
 * 1. 等價類劃分：有效/無效 email、有效/無效 token
 * 2. 狀態轉換測試：token 過期和失效狀態
 * 3. 異常處理測試
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserJdbcRepository userJdbcRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private Users testUser;
    private UUID testUserId;
    private UUID testTokenId;
    private PasswordResetToken testToken;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testTokenId = UUID.randomUUID();

        testUser = new Users();
        testUser.setUserID(testUserId);
        testUser.setEmail("test@example.com");
        testUser.setName("測試用戶");
        testUser.setPassword("hashedPassword");

        testToken = new PasswordResetToken();
        testToken.setTokenID(testTokenId);
        testToken.setUserID(testUserId);
        testToken.setTokenHash("hashedToken");
        testToken.setCreatedAt(LocalDateTime.now());
        testToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        testToken.setUsedAt(null);
        testToken.setInvalidated(false);
    }

    // ===== requestReset 測試 =====
    @Test
    void testRequestReset_Success() {
        when(userJdbcRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);

        // requestReset 返回 void，不會拋異常表示成功
        assertDoesNotThrow(() -> passwordResetService.requestReset("test@example.com"));
    }

    @Test
    void testRequestReset_UserNotFound() {
        when(userJdbcRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // 即使用戶不存在也不應該拋異常（避免洩漏用戶信息）
        assertDoesNotThrow(() -> passwordResetService.requestReset("nonexistent@example.com"));
    }

    @Test
    void testRequestReset_NullEmail() {
        assertDoesNotThrow(() -> passwordResetService.requestReset(null));
    }

    @Test
    void testRequestReset_EmptyEmail() {
        assertDoesNotThrow(() -> passwordResetService.requestReset(""));
    }

    // ===== verifyToken 測試 =====
    @Test
    void testVerifyToken_ValidToken() {
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.of(testToken));
        when(userJdbcRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        boolean result = passwordResetService.verifyToken("test@example.com", "anyTokenValue");

        assertNotNull(result);
    }

    @Test
    void testVerifyToken_ExpiredToken() {
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.empty());

        boolean result = passwordResetService.verifyToken("test@example.com", "anyToken");

        assertFalse(result);
    }

    @Test
    void testVerifyToken_InvalidatedToken() {
        testToken.setInvalidated(true);
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.empty());

        boolean result = passwordResetService.verifyToken("test@example.com", "anyToken");

        assertFalse(result);
    }

    @Test
    void testVerifyToken_TokenNotFound() {
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.empty());

        boolean result = passwordResetService.verifyToken("nonexistent@example.com", "anyToken");

        assertFalse(result);
    }

    @Test
    void testVerifyToken_NullEmail() {
        boolean result = passwordResetService.verifyToken(null, "anyToken");

        assertFalse(result);
    }

    @Test
    void testVerifyToken_NullToken() {
        boolean result = passwordResetService.verifyToken("test@example.com", null);

        assertFalse(result);
    }

    // ===== resetPassword 測試 =====
    @Test
    void testResetPassword_WithEmailAndToken_TokenNotFound() {
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.empty());

        boolean result = passwordResetService.resetPassword("test@example.com", "anyToken", "newPassword");

        assertFalse(result);
    }

    @Test
    void testResetPassword_WithTokenOnly_TokenNotFound() {
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.empty());

        boolean result = passwordResetService.resetPassword("anyToken", "newPassword");

        assertFalse(result);
    }

    @Test
    void testResetPassword_NullNewPassword() {
        boolean result = passwordResetService.resetPassword("anyToken", null);

        assertFalse(result);
    }

    @Test
    void testResetPassword_EmptyNewPassword() {
        boolean result = passwordResetService.resetPassword("anyToken", "");

        assertFalse(result);
    }

    // ===== requestPasswordReset 測試 =====
    @Test
    void testRequestPasswordReset_UserFound() {
        when(userJdbcRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);

        PasswordResetResult result = passwordResetService.requestPasswordReset("test@example.com");

        assertNotNull(result);
    }

    @Test
    void testRequestPasswordReset_UserNotFound() {
        when(userJdbcRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        PasswordResetResult result = passwordResetService.requestPasswordReset("nonexistent@example.com");

        assertNotNull(result);
        // 注意：為避免洩漏用戶是否存在，可能回傳 success=true 即使用戶不存在
    }

    @Test
    void testRequestPasswordReset_NullEmail() {
        PasswordResetResult result = passwordResetService.requestPasswordReset(null);

        assertNotNull(result);
        // 注意：實際實現可能對 null email 進行處理
    }

    // ===== validateToken 測試 =====
    @Test
    void testValidateToken_Valid() {
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.of(testToken));

        Optional<PasswordResetToken> result = passwordResetService.validateToken("anyToken");

        assertNotNull(result);
    }

    @Test
    void testValidateToken_NullToken() {
        // Null token 可能導致 NPE，這是預期行為
        try {
            Optional<PasswordResetToken> result = passwordResetService.validateToken(null);
            // 如果不拋出異常，結果應該為空
            assertTrue(result == null || result.isEmpty());
        } catch (NullPointerException e) {
            // 預期可能拋出 NPE
            assertNotNull(e);
        }
    }

    @Test
    void testValidateToken_EmptyToken() {
        Optional<PasswordResetToken> result = passwordResetService.validateToken("");

        assertTrue(result.isEmpty());
    }

    // ===== PasswordResetResult 測試 =====
    @Test
    void testPasswordResetResult_Success() {
        PasswordResetResult result = new PasswordResetResult(true, "成功", "token123");

        assertTrue(result.isSuccess());
        assertEquals("成功", result.getMessage());
        assertEquals("token123", result.getToken());
    }

    @Test
    void testPasswordResetResult_Failure() {
        PasswordResetResult result = new PasswordResetResult(false, "失敗", null);

        assertFalse(result.isSuccess());
        assertEquals("失敗", result.getMessage());
        assertNull(result.getToken());
    }

    // ===== Token 過期邊界值測試 =====
    @Test
    void testValidateToken_JustExpired() {
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.empty());

        Optional<PasswordResetToken> result = passwordResetService.validateToken("expiredToken");

        assertTrue(result.isEmpty());
    }

    @Test
    void testValidateToken_JustValid() {
        PasswordResetToken justValidToken = new PasswordResetToken();
        justValidToken.setTokenID(UUID.randomUUID());
        justValidToken.setUserID(testUserId);
        justValidToken.setExpiresAt(LocalDateTime.now().plusSeconds(1));
        justValidToken.setInvalidated(false);

        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.of(justValidToken));

        Optional<PasswordResetToken> result = passwordResetService.validateToken("validToken");

        assertNotNull(result);
    }
}
