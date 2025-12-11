package Group4.Childcare.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LotteryResult {
    private boolean success;
    private String message;
    private int totalProcessed;
    private int firstPriorityAccepted;
    private int secondPriorityAccepted;
    private int thirdPriorityAccepted;
    private int waitlisted;
    private List<Map<String, Object>> acceptedList;
    private List<Map<String, Object>> waitlistList;
}

