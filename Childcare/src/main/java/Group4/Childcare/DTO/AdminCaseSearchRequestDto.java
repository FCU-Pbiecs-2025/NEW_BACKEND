package Group4.Childcare.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCaseSearchRequestDto {
  /** 公托機構ID (全部時為 null) */
  private UUID institutionId;

  /** 班級ID (全部時為 null) */
  private UUID classId;

  /** 流水案號（精確查詢） */
  private Long caseNumber;

  /** 身分證字號 / 護照號碼 */
  private String applicantNationalId;

  /** 身分別 (applications.IdentityType) */
  private String identityType;

  /** 案件狀態 (application_participants.Status) */
  private String caseStatus;




}
