package Group4.Childcare.DTO;

import java.time.LocalDate;

public class UpdateConfirmDateRequest {
    private String cancellationID;
    private LocalDate confirmDate;

    public String getCancellationID() {
        return cancellationID;
    }
    public void setCancellationID(String cancellationID) {
        this.cancellationID = cancellationID;
    }
    public LocalDate getConfirmDate() {
        return confirmDate;
    }
    public void setConfirmDate(LocalDate confirmDate) {
        this.confirmDate = confirmDate;
    }
}

