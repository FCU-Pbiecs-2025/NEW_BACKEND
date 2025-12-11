package Group4.Childcare.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens", schema = "dbo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

  @Id
  @Column(name = "TokenID")
  private UUID tokenID;

  @Column(name = "UserID", nullable = false)
  private UUID userID;

  @Column(name = "TokenHash", nullable = false, length = 255)
  private String tokenHash;

  @Column(name = "CreatedAt", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "ExpiresAt", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "UsedAt")
  private LocalDateTime usedAt;

  @Column(name = "Invalidated", nullable = false)
  private Boolean invalidated = false;
}

