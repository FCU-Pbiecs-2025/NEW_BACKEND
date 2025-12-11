package Group4.Childcare.Service;

import Group4.Childcare.DTO.RevokeApplicationDTO;
import Group4.Childcare.DTO.RevokeDetailResponse;
import Group4.Childcare.DTO.ApplicationParticipantDTO;
import Group4.Childcare.Repository.RevokesJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.LocalDate;

@Service
public class RevokeService {
    @Autowired
    private RevokesJdbcRepository revokesJdbcRepository;

    public List<RevokeApplicationDTO> getRevokedApplications(int page, int size, String institutionID, String caseNumber, String nationalID) {
        return revokesJdbcRepository.findRevokedApplications(page, size, institutionID, caseNumber, nationalID);
    }

    public long getTotalRevokedApplications(String institutionID, String caseNumber, String nationalID) {
        return revokesJdbcRepository.countRevokedApplications(institutionID, caseNumber, nationalID);
    }

    // 分頁搜尋撤銷申請
    public List<RevokeApplicationDTO> searchRevokedApplicationsPaged(String caseNumber, String nationalID, int page, int size, String institutionID) {
        return revokesJdbcRepository.searchRevokedApplicationsPaged(caseNumber, nationalID, page, size, institutionID);
    }

    // 搜尋撤銷申請總數
    public long countSearchRevokedApplications(String caseNumber, String nationalID, String institutionID) {
        return revokesJdbcRepository.countSearchRevokedApplications(caseNumber, nationalID, institutionID);
    }

    // 取得撤銷資料
    public RevokeApplicationDTO getRevokeByCancellationID(String cancellationID) {
        return revokesJdbcRepository.getRevokeByCancellationID(cancellationID);
    }

    // 取得指定撤銷的家長資料 (participantType == 2)
    public List<ApplicationParticipantDTO> getParentsByCancellation(String cancellationID) {
        return revokesJdbcRepository.getParentsByCancellation(cancellationID);
    }

    // 取得 applications 與 application_participants 的詳細資料
    public ApplicationParticipantDTO getApplicationDetailByCancellationAndNationalID(String cancellationID, String nationalID) {
        return revokesJdbcRepository.getApplicationDetailByCancellationAndNationalID(cancellationID, nationalID);
    }

    // 新增：更新撤銷聲請的確認日期
    public int updateConfirmDate(String cancellationID, LocalDate confirmDate) {
        return revokesJdbcRepository.updateConfirmDate(cancellationID, confirmDate);
    }

    // 新增：更新 application_participants 的 Status
    public int updateApplicationParticipantStatus(String applicationID, String nationalID, String status) {
        return revokesJdbcRepository.updateApplicationParticipantStatus(applicationID, nationalID, status);
    }

    // 新增：建立一筆 cancellation 並回傳 CancellationID
    public void createCancellation(String applicationID, String abandonReason, String nationalID, String caseNumber) {
        LocalDate today = LocalDate.now();
        revokesJdbcRepository.insertCancellation(applicationID, abandonReason, nationalID, today, caseNumber);
    }
}
