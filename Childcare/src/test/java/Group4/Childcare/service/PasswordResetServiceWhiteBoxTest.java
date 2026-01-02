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
import org.springframework.mail.MailSendException;
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

    @Test
    @DisplayName("TC-PR-08: 請求重設 - 清除舊 Token 失敗仍應繼續")
    void testRequestReset_InvalidateTokensFails() {
        // Arrange
        when(userJdbcRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        // 模擬清除舊 Token 時拋出異常
        when(tokenRepository.invalidateAllTokensByUserID(TEST_USER_ID)).thenThrow(new RuntimeException("DB Error"));

        // Act
        PasswordResetService.PasswordResetResult result = passwordResetService.requestPasswordReset(TEST_EMAIL);

        // Assert
        assertTrue(result.isSuccess()); // 流程應繼續執行
        verify(tokenRepository).save(any(PasswordResetToken.class)); // 新 Token 仍應被儲存
    }

    @Test
    @DisplayName("TC-PR-09: 請求重設 - 發生未預期錯誤應拋出 RuntimeException")
    void testRequestReset_UnexpectedError() {
        // Arrange
        when(userJdbcRepository.findByEmail(TEST_EMAIL)).thenThrow(new RuntimeException("Database Down"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            passwordResetService.requestPasswordReset(TEST_EMAIL);
        });
        assertTrue(exception.getMessage().contains("Failed to process password reset request"));
    }

    @Test
    @DisplayName("TC-PR-10: 請求重設 - 寄信失敗應被 Catch")
    void testRequestReset_SendEmailFails() {
        // Arrange
        when(userJdbcRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        // 模擬寄信失敗
        doThrow(new MailSendException("SMTP Error")).when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        PasswordResetService.PasswordResetResult result = passwordResetService.requestPasswordReset(TEST_EMAIL);

        // Assert
        assertTrue(result.isSuccess()); // 流程應視為成功 (Token 已生成)
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("TC-PR-15: 請求重設 - User Name 為 null (覆蓋郵件內容分支)")
    void testRequestReset_UserNameNull() {
        // Arrange
        testUser.setName(null); // 設定 Name 為 null
        when(userJdbcRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.invalidateAllTokensByUserID(TEST_USER_ID)).thenReturn(1);

        // Act
        PasswordResetService.PasswordResetResult result = passwordResetService.requestPasswordReset(TEST_EMAIL);

        // Assert
        assertTrue(result.isSuccess());
        verify(mailSender).send(any(SimpleMailMessage.class));
        // 這裡雖然無法直接驗證郵件內容字串 (因為是在 private method 內組裝)，
        // 但只要執行過這段程式碼，JaCoCo 就會標記為已覆蓋。
    }

    // ============================================================
    // 4.2 驗證 Token (verifyToken)
    // ============================================================

    @Test
    @DisplayName("TC-PR-03: Token 有效 - 驗證通過")
    void testVerifyToken_Valid() {
        // Arrange
        String rawToken = "valid-raw-token";
        PasswordResetToken validToken = new PasswordResetToken();
        validToken.setUserID(TEST_USER_ID);
        validToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        validToken.setInvalidated(false);

        when(userJdbcRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
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
        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setUserID(TEST_USER_ID);
        expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        expiredToken.setInvalidated(false);

        when(userJdbcRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));
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
        PasswordResetToken usedToken = new PasswordResetToken();
        usedToken.setUserID(TEST_USER_ID);
        usedToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        usedToken.setInvalidated(true);

        when(userJdbcRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(usedToken));
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
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.empty());

        // Act
        boolean isValid = passwordResetService.verifyToken(TEST_EMAIL, rawToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("TC-PR-11: Token 存在但 UserID 不匹配 - 驗證失敗")
    void testVerifyToken_UserIDMismatch() {
        // Arrange
        String rawToken = "valid-token-other-user";
        PasswordResetToken otherUserToken = new PasswordResetToken();
        otherUserToken.setUserID(UUID.randomUUID()); // 不同的 UserID
        otherUserToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        otherUserToken.setInvalidated(false);

        when(userJdbcRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(otherUserToken));
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.of(otherUserToken));

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

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(validToken));
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.of(validToken));
        when(userJdbcRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

        // Act
        boolean result = passwordResetService.resetPassword(rawToken, newPassword);

        // Assert
        assertTrue(result);
        verify(userJdbcRepository).save(testUser);
        assertEquals(encodedPassword, testUser.getPassword());
        verify(tokenRepository).save(validToken);
        assertTrue(validToken.getInvalidated());
    }

    @Test
    @DisplayName("TC-PR-12: 重設密碼 (含 Email) - 驗證失敗則不重設")
    void testResetPasswordWithEmail_VerifyFails() {
        // Arrange
        String rawToken = "invalid-token";
        String newPassword = "newPass123";

        // 模擬 verifyToken 失敗 (例如找不到 User)
        when(userJdbcRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act
        boolean result = passwordResetService.resetPassword(TEST_EMAIL, rawToken, newPassword);

        // Assert
        assertFalse(result);
        verify(userJdbcRepository, never()).save(any(Users.class));
    }

    @Test
    @DisplayName("TC-PR-13: 重設密碼 - Token 有效但 User 不存在")
    void testResetPassword_UserNotFound() {
        // Arrange
        String rawToken = "valid-token-no-user";
        String newPassword = "newPass123";

        PasswordResetToken validToken = new PasswordResetToken();
        validToken.setUserID(TEST_USER_ID);
        validToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        validToken.setInvalidated(false);

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(validToken));
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.of(validToken));
        // 模擬 User 找不到
        when(userJdbcRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        // Act
        boolean result = passwordResetService.resetPassword(rawToken, newPassword);

        // Assert
        assertFalse(result);
        verify(userJdbcRepository, never()).save(any(Users.class));
    }

    @Test
    @DisplayName("TC-PR-14: 重設密碼 (含 Email) - 驗證成功並重設")
    void testResetPasswordWithEmail_Success() {
        // Arrange
        String rawToken = "valid-token";
        String newPassword = "newPass123";
        String encodedPassword = "encodedNewPassword";

        PasswordResetToken validToken = new PasswordResetToken();
        validToken.setUserID(TEST_USER_ID);
        validToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        validToken.setInvalidated(false);

        // verifyToken 需要的 Mock
        when(userJdbcRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(validToken));
        when(tokenRepository.findValidTokenByHash(anyString())).thenReturn(Optional.of(validToken));

        // resetPasswordInternal 需要的 Mock
        // 注意：verifyToken 已經呼叫過 findValidTokenByHash，這裡 resetPasswordInternal 會再呼叫一次
        // Mockito 預設會回傳相同的結果，所以不需要額外設定
        when(userJdbcRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

        // Act
        boolean result = passwordResetService.resetPassword(TEST_EMAIL, rawToken, newPassword);

        // Assert
        assertTrue(result);
        verify(userJdbcRepository).save(testUser);
        assertEquals(encodedPassword, testUser.getPassword());
        verify(tokenRepository).save(validToken);
        assertTrue(validToken.getInvalidated());
    }
}
