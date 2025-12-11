package Group4.Childcare.DTO;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data

public class ApplicationSummaryWithDetailsDTO {
    private UUID applicationID;
    private LocalDate applicationDate;
    private String name;
    private String institutionName;
    private String status;
    private String InstitutionID;
    private String PName;
    private String NationalID;
    private String ParticipantType;
    private Long caseNumber;
}
