package Group4.Childcare.DTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor

public class ApplicationCaseDTO {
    // 申請編號
    public UUID applicationId;
    // 申請日期
    public LocalDate applicationDate;
    // 申請機構名稱
    public String institutionName;
    // 案件流水號
    public Long caseNumber;

    // ★ 新增附件欄位
    public String attachmentPath;
    public String attachmentPath1;
    public String attachmentPath2;
    public String attachmentPath3;

    // 原因/備註
    public String reason;
    // 申請人（家長）清單
    public List<ApplicationParticipantDTO> parents;
    // 兒童（被申請入托）清單
    public List<ApplicationParticipantDTO> children;

    public Byte identityType;
}
