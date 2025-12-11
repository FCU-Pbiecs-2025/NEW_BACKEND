package Group4.Childcare.DTO;

public class RevokeSearchRequest {
    public String cancellationID;
    public String nationalID;

    public RevokeSearchRequest() {}

    public RevokeSearchRequest(String cancellationID, String nationalID) {
        this.cancellationID = cancellationID;
        this.nationalID = nationalID;
    }

    public String getCancellationID() { return cancellationID; }
    public String getNationalID() { return nationalID; }
    public void setCancellationID(String cancellationID) { this.cancellationID = cancellationID; }
    public void setNationalID(String nationalID) { this.nationalID = nationalID; }
}

