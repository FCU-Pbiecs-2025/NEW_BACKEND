package Group4.Childcare.DTO;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseEditUpdateDTO {

  /** 案件編號 (applications.CaseNumber) */
  @JsonProperty("caseNumber")
  private Long caseNumber;

  /** 聲請日期 (applications.ApplicationDate) */
  @JsonProperty("applyDate")
  private LocalDate applyDate;

  /** 身分別 (applications.IdentityType) */
  @JsonProperty("identityType")
  private Integer identityType;

  /** 機構ID (applications.InstitutionID) */
  @JsonProperty("institutionId")
  private UUID institutionId;

  /** 機構名稱 (institutions.InstitutionName) */
  @JsonProperty("institutionName")
  private String institutionName;


  /** 選定的班級名稱 (classes.ClassName) */
  @JsonProperty("selectedClass")
  private String selectedClass;

  /** 候補序號 (application_participants.CurrentOrder) */
  @JsonProperty("currentOrder")
  private Integer currentOrder;

  /** 審核日期 (application_participants.ReviewDate) */
  @JsonProperty("reviewDate")
  private LocalDateTime reviewDate;

  /** 案件ID (applications.ApplicationID) */
  @JsonProperty("applicationID")
  private UUID applicationID;

  /** 參與者ID (application_participants.ParticipantID) - 用於篩選幼兒 */
  @JsonProperty("participantID")
  private UUID participantID;

  /** 申請人信息 (users model) */
  @JsonProperty("user")
  private UserSimpleDTO user;

  /** 家長信息陣列 */
  @JsonProperty("parents")
  private List<ApplicationParticipantDTO> parents;

  /** 幼兒信息陣列 */
  @JsonProperty("children")
  private List<ApplicationParticipantDTO> children;

  @JsonProperty("attachmentPath")
  private String attachmentPath;
  @JsonProperty("attachmentPath1")
  private String attachmentPath1;
  @JsonProperty("attachmentPath2")
  private String attachmentPath2;
  @JsonProperty("attachmentPath3")
  private String attachmentPath3;
}
