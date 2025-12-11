package Group4.Childcare.DTO;

import lombok.Data;
import java.util.UUID;

@Data
public class LotteryRequest {
    private UUID institutionId;
    private boolean isLotteryPeriod; // true: 抽籤時期, false: 非抽籤時期
}


