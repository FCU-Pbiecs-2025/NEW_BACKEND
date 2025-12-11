package Group4.Childcare.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionSummaryDTO {
    private UUID institutionID;
    private String institutionName;
    private String address;
    private String phoneNumber;
}
