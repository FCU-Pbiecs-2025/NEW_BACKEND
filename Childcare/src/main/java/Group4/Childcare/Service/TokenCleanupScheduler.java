package Group4.Childcare.Service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

// 暫時註解 - 因為資料庫中不存在 password_reset_tokens 資料表
/*
@Component
public class TokenCleanupScheduler {

    @Autowired
    private PasswordResetService passwordResetService;

    // Run hourly
    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void cleanup() {
        passwordResetService.cleanupExpired();
    }
}
*/

