package Group4.Childcare.DTO;

import Group4.Childcare.Model.ChildInfo;
import Group4.Childcare.Model.ParentInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFamilyInfoDTO {
  private UUID userID;
  private Byte accountStatus;
  private Byte permissionType;
  private String name;
  private Boolean gender;
  private String phoneNumber;
  private String mailingAddress;
  private String email;
  private LocalDate birthDate;
  private UUID familyInfoID;
  private UUID institutionID;
  private String nationalID;
  private List<ParentInfo> Parents;
  private List<ChildInfo> Children;

}
