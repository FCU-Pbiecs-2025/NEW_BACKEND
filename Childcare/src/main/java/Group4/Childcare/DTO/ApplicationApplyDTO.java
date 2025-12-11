package Group4.Childcare.DTO;

import java.util.List;

public class ApplicationApplyDTO {
    // 申請主檔欄位
    public String identityType;
    public List<String> attachmentFiles;
    // 申請人與家長資料
    public List<ApplicationParticipantDTO> participants;
}
