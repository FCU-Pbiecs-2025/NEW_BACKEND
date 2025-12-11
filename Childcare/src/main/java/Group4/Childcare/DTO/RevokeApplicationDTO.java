package Group4.Childcare.DTO;

import java.util.UUID;
import java.time.LocalDateTime;

public class RevokeApplicationDTO {
    private UUID cancellationID;
    private UUID applicationID;
    private LocalDateTime cancellationDate;
    private UUID userID;
    private String userName;
    private UUID institutionID;
    private String institutionName;
    private String abandonReason;
    private String nationalID;
    private String caseNumber;

    public RevokeApplicationDTO(UUID cancellationID, UUID applicationID, LocalDateTime cancellationDate, UUID userID, String userName, UUID institutionID, String institutionName, String abandonReason, String nationalID, String caseNumber) {
        this.cancellationID = cancellationID;
        this.applicationID = applicationID;
        this.cancellationDate = cancellationDate;
        this.userID = userID;
        this.userName = userName;
        this.institutionID = institutionID;
        this.institutionName = institutionName;
        this.abandonReason = abandonReason;
        this.nationalID = nationalID;
        this.caseNumber = caseNumber;
    }

    public UUID getCancellationID() { return cancellationID; }
    public UUID getApplicationID() { return applicationID; }
    public LocalDateTime getCancellationDate() { return cancellationDate; }
    public UUID getUserID() { return userID; }
    public String getUserName() { return userName; }
    public UUID getInstitutionID() { return institutionID; }
    public String getInstitutionName() { return institutionName; }
    public String getAbandonReason() { return abandonReason; }
    public String getNationalID() { return nationalID; }
    public String getCaseNumber() { return caseNumber; }
}
