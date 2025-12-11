package Group4.Childcare.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateRevokeRequest {
    @JsonProperty("NationalID")
    public String nationalID;

    @JsonProperty("AbandonReason")
    public String abandonReason;

    @JsonProperty("ApplicationID")
    public String applicationID;

    @JsonProperty("CaseNumber")
    public String caseNumber;

    public CreateRevokeRequest() {}

    public CreateRevokeRequest(String nationalID, String abandonReason, String applicationID, String caseNumber) {
        this.nationalID = nationalID;
        this.abandonReason = abandonReason;
        this.applicationID = applicationID;
        this.caseNumber = caseNumber;
    }

    public String getNationalID() { return nationalID; }
    public String getAbandonReason() { return abandonReason; }
    public String getApplicationID() { return applicationID; }
    public String getCaseNumber() { return caseNumber; }

    public void setNationalID(String nationalID) { this.nationalID = nationalID; }
    public void setAbandonReason(String abandonReason) { this.abandonReason = abandonReason; }
    public void setApplicationID(String applicationID) { this.applicationID = applicationID; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
}
