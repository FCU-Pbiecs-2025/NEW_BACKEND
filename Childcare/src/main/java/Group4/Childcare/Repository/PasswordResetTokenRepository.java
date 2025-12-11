package Group4.Childcare.Repository;

import Group4.Childcare.Model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

  /**
   * 根據 TokenHash 查詢有效的 token
   */
  @Query(value = "SELECT * FROM password_reset_tokens WHERE TokenHash = :tokenHash AND Invalidated = 0 AND ExpiresAt > GETDATE()", nativeQuery = true)
  Optional<PasswordResetToken> findValidTokenByHash(@Param("tokenHash") String tokenHash);

  /**
   * 根據 TokenHash 查詢 token（用於除錯，不檢查時效性）
   */
  @Query(value = "SELECT * FROM password_reset_tokens WHERE TokenHash = :tokenHash", nativeQuery = true)
  Optional<PasswordResetToken> findByTokenHash(@Param("tokenHash") String tokenHash);

  /**
   * 將使用者所有未過期的 token 設為失效
   */
  @Modifying
  @Query(value = "UPDATE password_reset_tokens SET Invalidated = 1 WHERE UserID = :userID AND Invalidated = 0", nativeQuery = true)
  int invalidateAllTokensByUserID(@Param("userID") UUID userID);

  /**
   * 根據 UserID 查詢是否有有效的 token
   */
  @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM password_reset_tokens WHERE UserID = :userID AND Invalidated = 0 AND ExpiresAt > GETDATE()", nativeQuery = true)
  boolean hasValidTokenByUserID(@Param("userID") UUID userID);
}

