package Group4.Childcare.Service;

import Group4.Childcare.Model.PasswordResetToken;
import Group4.Childcare.Model.Users;
import Group4.Childcare.Repository.PasswordResetTokenRepository;
import Group4.Childcare.Repository.UserJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

  @Autowired
  private PasswordResetTokenRepository tokenRepository;

  @Autowired
  private UserJdbcRepository userJdbcRepository;

  @Autowired(required = false)
  private JavaMailSender mailSender;

  @Value("${app.frontend.url:http://localhost:5173}")
  private String frontendUrl;

  @Value("${app.password-reset.token-expiry-minutes:30}")
  private int tokenExpiryMinutes;

  /**
   * 處理忘記密碼請求（供 AuthController 使用）
   * @param email 使用者 email
   */
  @Transactional
  public void requestReset(String email) {
    requestPasswordReset(email);
  }

  /**
   * 驗證 token 是否有效（供 AuthController 使用）
   * @param email 使用者 email
   * @param rawToken 原始 token
   * @return 是否有效
   */
  public boolean verifyToken(String email, String rawToken) {
    System.out.println("=== Verifying token ===");
    System.out.println("Email: " + email);
    System.out.println("Raw token (first 20 chars): " + (rawToken != null && rawToken.length() > 20 ? rawToken.substring(0, 20) + "..." : rawToken));

    // 先驗證 email 對應的使用者是否存在
    Optional<Users> userOpt = userJdbcRepository.findByEmail(email);
    if (userOpt.isEmpty()) {
      System.out.println("❌ User not found for email: " + email);
      return false;
    }

    Users user = userOpt.get();
    System.out.println("✓ User found: " + user.getUserID());

    // 生成 token hash 來查詢
    String tokenHash = hashToken(rawToken);
    System.out.println("Generated token hash (first 20 chars): " + tokenHash.substring(0, 20) + "...");

    // 驗證 token
    Optional<PasswordResetToken> tokenOpt = validateToken(rawToken);
    if (tokenOpt.isEmpty()) {
      System.out.println("❌ Token not found or invalid (expired/invalidated)");

      // 額外檢查：看看資料庫中是否有該 hash 的 token（不管有效與否）
      try {
        String checkSql = "SELECT COUNT(*) FROM password_reset_tokens WHERE TokenHash = ?";
        System.out.println("Checking if token exists in DB at all...");
      } catch (Exception e) {
        System.err.println("Error checking token: " + e.getMessage());
      }

      return false;
    }

    PasswordResetToken token = tokenOpt.get();
    System.out.println("✓ Token found in database");
    System.out.println("Token UserID: " + token.getUserID());
    System.out.println("Token ExpiresAt: " + token.getExpiresAt());
    System.out.println("Token Invalidated: " + token.getInvalidated());

    // 確認 token 屬於該使用者
    boolean matches = token.getUserID().equals(user.getUserID());
    if (!matches) {
      System.out.println("❌ Token UserID does not match user: " + token.getUserID() + " vs " + user.getUserID());
    } else {
      System.out.println("✓ Token belongs to the user");
    }

    return matches;
  }

  /**
   * 使用 token 重設密碼（供 UsersController 使用）
   * @param rawToken 原始 token
   * @param newPassword 新密碼
   * @return 是否成功
   */
  @Transactional
  public boolean resetPassword(String rawToken, String newPassword) {
    return resetPasswordInternal(rawToken, newPassword);
  }

  /**
   * 使用 email 和 token 重設密碼（供 AuthController 使用）
   * @param email 使用者 email
   * @param rawToken 原始 token
   * @param newPassword 新密碼
   * @return 是否成功
   */
  @Transactional
  public boolean resetPassword(String email, String rawToken, String newPassword) {
    // 先驗證 email 和 token
    if (!verifyToken(email, rawToken)) {
      return false;
    }

    // 使用原有的 resetPassword 方法
    return resetPasswordInternal(rawToken, newPassword);
  }

  /**
   * 處理忘記密碼請求
   * @param email 使用者 email
   * @return 處理結果
   */
  @Transactional
  public PasswordResetResult requestPasswordReset(String email) {
    try {
      System.out.println("=== Starting password reset for email: " + email + " ===");

      // 1. 用 email 找 user
      Optional<Users> userOpt = userJdbcRepository.findByEmail(email);

      if (userOpt.isEmpty()) {
        System.out.println("User not found for email: " + email);
        // 為了安全性，不透露 email 是否存在
        return new PasswordResetResult(true, "如果該 email 存在於系統中，您將會收到重設密碼的郵件", null);
      }

      Users user = userOpt.get();
      System.out.println("User found: " + user.getUserID());

      // 2. 將該使用者所有舊的 token 設為失效
      try {
        int invalidated = tokenRepository.invalidateAllTokensByUserID(user.getUserID());
        System.out.println("Invalidated " + invalidated + " old tokens");
      } catch (Exception e) {
        System.err.println("Error invalidating old tokens: " + e.getMessage());
        // Continue even if invalidation fails
      }

      // 3. 產生 raw token（給使用者看的部分）
      String rawToken = generateSecureToken();
      System.out.println("Generated raw token");

      // 4. hash(rawToken) → 存到 TokenHash
      String tokenHash = hashToken(rawToken);
      System.out.println("Generated token hash");

      // 5. 新增一筆資料到 password_reset_tokens
      PasswordResetToken resetToken = new PasswordResetToken();
      resetToken.setTokenID(UUID.randomUUID());
      resetToken.setUserID(user.getUserID());
      resetToken.setTokenHash(tokenHash);
      resetToken.setCreatedAt(LocalDateTime.now());
      resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(tokenExpiryMinutes));
      resetToken.setInvalidated(false);

      System.out.println("Saving password reset token to database...");
      tokenRepository.save(resetToken);
      System.out.println("Token saved successfully");

      // 6. 寄 email 給使用者
      String resetLink = frontendUrl + "/reset-password?token=" + rawToken + "&email=" + email;
      sendPasswordResetEmail(user.getEmail(), user.getName(), resetLink);

      return new PasswordResetResult(true, "重設密碼郵件已發送，請檢查您的信箱", rawToken);
    } catch (Exception e) {
      System.err.println("=== Error in requestPasswordReset ===");
      System.err.println("Error type: " + e.getClass().getName());
      System.err.println("Error message: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("Failed to process password reset request: " + e.getMessage(), e);
    }
  }

  /**
   * 驗證 token 是否有效
   * @param rawToken 原始 token
   * @return 對應的 PasswordResetToken，若無效則為 empty
   */
  public Optional<PasswordResetToken> validateToken(String rawToken) {
    System.out.println("=== Validating token ===");
    String tokenHash = hashToken(rawToken);
    System.out.println("Looking for token with hash: " + tokenHash.substring(0, 20) + "...");
    System.out.println("Full token hash: " + tokenHash);

    // 先檢查 token 是否存在（不管有效與否）
    Optional<PasswordResetToken> anyToken = tokenRepository.findByTokenHash(tokenHash);
    if (anyToken.isPresent()) {
      PasswordResetToken token = anyToken.get();
      System.out.println("✓ Token found in database (regardless of validity)");
      System.out.println("  TokenID: " + token.getTokenID());
      System.out.println("  UserID: " + token.getUserID());
      System.out.println("  ExpiresAt: " + token.getExpiresAt());
      System.out.println("  Invalidated: " + token.getInvalidated());
      System.out.println("  Current time: " + LocalDateTime.now());
      System.out.println("  Time difference (minutes): " +
        java.time.Duration.between(LocalDateTime.now(), token.getExpiresAt()).toMinutes());

      // 手動檢查有效性
      boolean notInvalidated = !token.getInvalidated();
      boolean notExpired = token.getExpiresAt().isAfter(LocalDateTime.now());

      System.out.println("  Manual validity check:");
      System.out.println("    - Not invalidated: " + notInvalidated);
      System.out.println("    - Not expired: " + notExpired);

      if (!notInvalidated) {
        System.out.println("❌ Token is marked as invalidated");
      }
      if (!notExpired) {
        System.out.println("❌ Token has expired");
      }
    } else {
      System.out.println("❌ Token not found in database at all");
      System.out.println("   This means the token hash doesn't match any record");
    }

    // 查詢有效的 token
    Optional<PasswordResetToken> result = tokenRepository.findValidTokenByHash(tokenHash);

    if (result.isPresent()) {
      System.out.println("✓✓ Valid token found by repository query");
    } else {
      System.out.println("❌ No valid token found by repository query");
    }

    return result;
  }

  /**
   * 使用 token 重設密碼（內部方法）
   * @param rawToken 原始 token
   * @param newPassword 新密碼
   * @return 是否成功
   */
  private boolean resetPasswordInternal(String rawToken, String newPassword) {
    Optional<PasswordResetToken> tokenOpt = validateToken(rawToken);

    if (tokenOpt.isEmpty()) {
      return false;
    }

    PasswordResetToken token = tokenOpt.get();

    // 更新密碼
    Optional<Users> userOpt = userJdbcRepository.findById(token.getUserID());
    if (userOpt.isEmpty()) {
      return false;
    }

    Users user = userOpt.get();
    user.setPassword(newPassword); // 實際環境應該加密
    userJdbcRepository.save(user);

    // 將 token 設為失效並記錄使用時間
    token.setInvalidated(true);
    token.setUsedAt(LocalDateTime.now());
    tokenRepository.save(token);

    return true;
  }

  /**
   * 生成安全的隨機 token
   */
  private String generateSecureToken() {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /**
   * 使用 SHA-256 對 token 進行雜湊
   */
  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not found", e);
    }
  }

  /**
   * 發送重設密碼郵件
   */
  private void sendPasswordResetEmail(String toEmail, String userName, String resetLink) {
    if (mailSender == null) {
      System.out.println("=== 郵件發送模擬（未配置 JavaMailSender）===");
      System.out.println("收件人: " + toEmail);
      System.out.println("使用者: " + userName);
      System.out.println("重設連結: " + resetLink);
      System.out.println("==========================================");
      return;
    }

    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(toEmail);
      message.setSubject("【托育系統】重設密碼通知");
      message.setText(
        "親愛的 " + (userName != null ? userName : "使用者") + " 您好，\n\n" +
        "我們收到了您重設密碼的請求。\n\n" +
        "請點擊以下連結來重設您的密碼（連結有效時間為 " + tokenExpiryMinutes + " 分鐘）：\n\n" +
        resetLink + "\n\n" +
        "如果您沒有申請重設密碼，請忽略此郵件。\n\n" +
        "此郵件為系統自動發送，請勿直接回覆。\n\n" +
        "托育系統 敬上"
      );

      mailSender.send(message);
      System.out.println("Password reset email sent to: " + toEmail);
    } catch (Exception e) {
      System.err.println("Failed to send password reset email: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * 密碼重設結果
   */
  public static class PasswordResetResult {
    private final boolean success;
    private final String message;
    private final String token; // 僅用於測試/開發環境

    public PasswordResetResult(boolean success, String message, String token) {
      this.success = success;
      this.message = message;
      this.token = token;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getToken() { return token; }
  }
}

