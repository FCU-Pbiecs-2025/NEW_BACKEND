package Group4.Childcare.service;

import Group4.Childcare.Model.PasswordResetToken;
import Group4.Childcare.Model.Users;
import Group4.Childcare.Repository.PasswordResetTokenRepository;
import Group4.Childcare.Repository.UserJdbcRepository;
import Group4.Childcare.Service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceWhiteBoxTest {

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserJdbcRepository userJdbcRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private PasswordEncoder passwordEncoder;

    private Users testUser;
    private final String TEST_EMAIL = "test@example.com";
    private final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // 初始化測試使用者
        testUser = new Users();
        testUser.setUserID(TEST_USER_ID);
        testUser.setEmail(TEST_EMAIL);
        testUser.setName("Test User");
        testUser.setPassword("oldPasswordHash");

        // 注入配置屬性
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(passwordResetService, "tokenExpiryMinutes", 30);
    }

    // ============================================================
    // 4.1 請求重設密碼 (requestReset)
    // ============================================================

    @Test
    @DisplayName("TC-PR-01: Email 存在 - 應生成 Token 並發送郵件")
    void testRequestReset_EmailExists() {
        // Arrange
        when(userJdbcRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.invalidateAllTokensByUserID(TEST_USER_ID)).thenReturn(1);

        // Act
        PasswordResetService.PasswordResetResult result = passwordResetService.requestPasswordReset(TEST_EMAIL);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getToken()); // 在測試環境中會回傳 raw token

        // 驗證 Token 是否儲存
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertEquals(TEST_USER_ID, savedToken.getUserID());
        assertNotNull(savedToken.getTokenHash());
        assertFalse(savedToken.getInvalidated());
        assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now()));

        // 驗證郵件是否發送
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("TC-PR-02: Email 不存在 - 不應生成 Token 且不發送郵件")
    void testRequestReset_EmailNotExists() {
        // Arrange
        when(userJdbcRepository.findByEmail("nonexist@example.com")).thenReturn(Optional.empty());

        // Act
        PasswordResetService.PasswordResetResult result = passwordResetService.requestPasswordReset("nonexist@example.com");

        // Assert
        assertTrue(result.isSuccess()); // 安全考量，仍回傳成功訊息
        assertNull(result.getToken()); // 但沒有 Token

        // 驗證沒有儲存 Token
        verify(tokenRepository, never()).save(any(PasswordResetToken.class));
        // 驗證沒有發送郵件
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // ============================================================
    // 4.2 驗證 Token (verifyToken)
    // ============================================================

    @Test
    @DisplayName("TC-PR-03: Token 有效 - 驗證通過")
    void testVerifyToken_Valid() {
        // Arrange
        String rawToken = "valid-raw-token";
        // 模擬 requestReset 流程來獲取正確的 hash 邏輯 (或是透過 Reflection 呼叫 private method，這裡簡化直接模擬 Repository 行為)
        // 由於 hashToken 是 private，我們無法直接在測試中生成一樣的 hash，
        // 但我們可以透過模擬 findValidTokenByHash 回傳一個有效的 Token 物件來測試。

        PasswordResetToken validToken = new PasswordResetToken();
        validToken.setUserID(TEST_USER_ID);
        validToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        validToken.setInvalidated(false);

        when(userJdbcRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        // 模擬 Repository 根據 hash 找到 Token (這裡 anyString() 代表 hash 後的字串)
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(validToken));
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.of(validToken));

        // Act
        boolean isValid = passwordResetService.verifyToken(TEST_EMAIL, rawToken);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("TC-PR-04: Token 過期 - 驗證失敗")
    void testVerifyToken_Expired() {
        // Arrange
        String rawToken = "expired-raw-token";
        
        // 模擬過期的 Token
        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setUserID(TEST_USER_ID);
        expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // 已過期
        expiredToken.setInvalidated(false);

        when(userJdbcRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        
        // findByTokenHash 可能找得到
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));
        // 但 findValidTokenByHash 應該找不到 (因為過期)
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.empty());

        // Act
        boolean isValid = passwordResetService.verifyToken(TEST_EMAIL, rawToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("TC-PR-05: Token 已失效 (Invalidated) - 驗證失敗")
    void testVerifyToken_Invalidated() {
        // Arrange
        String rawToken = "used-raw-token";

        // 模擬已失效的 Token
        PasswordResetToken usedToken = new PasswordResetToken();
        usedToken.setUserID(TEST_USER_ID);
        usedToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        usedToken.setInvalidated(true); // 已失效

        when(userJdbcRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        
        // findByTokenHash 可能找得到
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(usedToken));
        // 但 findValidTokenByHash 應該找不到 (因為 invalidated=true)
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.empty());

        // Act
        boolean isValid = passwordResetService.verifyToken(TEST_EMAIL, rawToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("TC-PR-06: Token 雜湊不符 - 驗證失敗")
    void testVerifyToken_WrongHash() {
        // Arrange
        String rawToken = "wrong-token";

        when(userJdbcRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        // Repository 找不到對應 Hash 的 Token
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.empty());

        // Act
        boolean isValid = passwordResetService.verifyToken(TEST_EMAIL, rawToken);

        // Assert
        assertFalse(isValid);
    }

    // ============================================================
    // 4.3 重設密碼 (resetPassword)
    // ============================================================

    @Test
    @DisplayName("TC-PR-07: 重設成功 - 更新密碼並失效 Token")
    void testResetPassword_Success() {
        // Arrange
        String rawToken = "valid-token";
        String newPassword = "newPass123";
        String encodedPassword = "encodedNewPassword";

        PasswordResetToken validToken = new PasswordResetToken();
        validToken.setUserID(TEST_USER_ID);
        validToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        validToken.setInvalidated(false);

        // 模擬 Token 驗證通過
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(validToken));
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.of(validToken));
        
        // 模擬 User 查詢
        when(userJdbcRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        
        // 模擬密碼加密
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

        // Act
        boolean result = passwordResetService.resetPassword(rawToken, newPassword);

        // Assert
        assertTrue(result);
        
        // 驗證 User 密碼已更新
        verify(userJdbcRepository).save(testUser);
        assertEquals(encodedPassword, testUser.getPassword());

        // 驗證 Token 已設為失效
        verify(tokenRepository).save(validToken);
        assertTrue(validToken.getInvalidated());
        assertNotNull(validToken.getUsedAt());
    }
}
