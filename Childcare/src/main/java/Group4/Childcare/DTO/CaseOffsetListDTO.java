package Group4.Childcare.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseOffsetListDTO {
  // 案件編號 (applications.CaseNumber)
  private Long caseNumber;
  // 幼兒ParticipantID (application_participants.ParticipantID)
  private UUID ParticipantID;
  // 聲請日期 (applications.ApplicationDate)
  private LocalDate applicationDate;

  // 申請機構名稱 (institutions.InstitutionName)
  private String institutionName;

  // 幼兒身分證字號/護照號碼 (application_participants.NationalID)
  private String childNationalId;

  // 幼兒姓名 (application_participants.Name)
  private String childName;

  // 幼兒出生年月日 (application_participants.BirthDate)
  private LocalDate childBirthDate;

  // 當前排序 (application_participants.CurrentOrder)
  private Integer currentOrder;

  // 審核狀態 (application_participants.Status)
  private String reviewStatus;

  /** 班級ID (classes.ClassName) */
  private String className;


  /** 申請人姓名(user) */
  private String applicantNationalName;

  /** 申請人身分證字號(user) */
  private String applicantNationalId;

  /** 身分別 (applications) */
  private String identityType;

  /** 案件狀態 (application_participants.Status) */
  private String caseStatus;




}
