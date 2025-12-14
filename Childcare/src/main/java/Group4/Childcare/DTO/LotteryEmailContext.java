package Group4.Childcare.DTO;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class LotteryEmailContext {
    private UUID applicationId;
    private String email;
    private String applicantName;
    private String institutionName;
    private Long caseNumber;
    private LocalDate applicationDate;
}
