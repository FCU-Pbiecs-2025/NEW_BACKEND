package Group4.Childcare.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor

public class ApplicationParticipantDTO {

  //ap、revoke共用，絕對不能改
    public UUID participantID;
    public UUID applicationID;
    public String participantType;
    public String nationalID;
    public String name;
    public String gender;
    public String relationShip;
    public String occupation;
    public String phoneNumber;
    public String householdAddress;
    public String mailingAddress;
    public String email;
    public String birthDate;
    public Boolean isSuspended;
    public String suspendEnd;
    public Integer currentOrder;
    public String status;
    public String reason;
    public String classID;
    public LocalDateTime reviewDate;
}

