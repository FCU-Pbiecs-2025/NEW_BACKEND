package Group4.Childcare.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSummaryDTO {
    private UUID applicationID;
    private LocalDate applicationDate;
    private String reason;
    private String status; // 從 application_participants 表中獲取
}
