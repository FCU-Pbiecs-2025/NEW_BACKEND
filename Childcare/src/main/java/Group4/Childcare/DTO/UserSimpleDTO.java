package Group4.Childcare.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSimpleDTO {
  //使用在CaseEditUpdateDTO，前端是個案管理編輯.vue作為使用
  @JsonProperty("userID")
  private String userID;
  @JsonProperty("name")
  private String name;
  @JsonProperty("gender")
  private String gender;
  @JsonProperty("birthDate")
  private String birthDate;
  @JsonProperty("mailingAddress")
  private String mailingAddress;
  @JsonProperty("email")
  private String email;
  @JsonProperty("phoneNumber")
  private String phoneNumber;
  @JsonProperty("nationalID")
  private String nationalID;
}
