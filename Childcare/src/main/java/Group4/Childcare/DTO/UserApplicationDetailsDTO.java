package Group4.Childcare.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserApplicationDetailsDTO {
    private UUID applicationID;        // from applications table
    private LocalDate applicationDate; // from applications table
    private UUID institutionID;        // from applications table
    private String caseNumber;         // from applications table (a.CaseNumber)
    private String institutionName;    // from institutions table (i.InstitutionName)
    private String childname;          // from application_participants table (ap.Name)
    private LocalDate birthDate;       // from application_participants table
    private String status;             // from application_participants table
    private int currentOrder;          // from application_participants table (ap.CurrentOrder)
    private String childNationalID;    // from application_participants table (ap.NationalID)
    private String reason;             // from application_participants table (ap.Reason)
    private UUID cancellationID;       // from cancellation table (c.CancellationID)
    private String username;           // from users table (u.Name)
}
