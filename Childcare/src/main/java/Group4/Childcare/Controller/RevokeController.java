// 此控制器處理撤銷申請相關的 API 請求
package Group4.Childcare.Controller;

// 匯入撤銷服務、DTO 及 Spring 相關註解
import Group4.Childcare.Service.RevokeService;
import Group4.Childcare.DTO.RevokeApplicationDTO;
import Group4.Childcare.DTO.RevokeSearchRequest;
import Group4.Childcare.DTO.RevokeDetailResponse;
import Group4.Childcare.DTO.ApplicationParticipantDTO;
import Group4.Childcare.DTO.UpdateConfirmDateRequest;
import Group4.Childcare.DTO.CreateRevokeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// 標註為 REST 控制器，路徑為 /revoke
@RestController
@RequestMapping("/revoke")
public class RevokeController {
    // 注入撤銷服務
    @Autowired
    private RevokeService revokeService;

    // 取得撤銷申請列表的 API，支援分頁
    @GetMapping("/applications")
    public ResponseEntity<Object> getRevokedApplications(
            @RequestParam(defaultValue = "0") int offset, // 分頁起始位置
            @RequestParam(defaultValue = "10") int size, // 每頁筆數
            @RequestParam(required = false) String institutionID, // 機構ID（可選）
            @RequestParam(required = false) String caseNumber, // 案件編號（可選）
            @RequestParam(required = false) String nationalID) { // 身分證字號（可選）
        // 參數驗證，offset 不可小於 0，size 必須大於 0
        if (offset < 0 || size <= 0) {
            return ResponseEntity.badRequest().build();
        }
        final int MAX_SIZE = 100; // 最大每頁筆數限制
        if (size > MAX_SIZE) size = MAX_SIZE;

        // 計算分頁頁碼，維持 repository page*size 的模式
        int page = offset / size;
        // 取得撤銷申請資料
        List<RevokeApplicationDTO> content = revokeService.getRevokedApplications(page, size, institutionID, caseNumber, nationalID);
        // 取得撤銷申請總筆數
        long totalElements = revokeService.getTotalRevokedApplications(institutionID, caseNumber, nationalID);
        // 計算總頁數
        int totalPages = (int) Math.ceil((double) totalElements / size);
        // 判斷是否有下一頁
        boolean hasNext = offset + size < totalElements;

        // 組合回應資料
        Map<String, Object> response = Map.of(
                "offset", offset,
                "size", size,
                "totalPages", totalPages,
                "hasNext", hasNext,
                "content", content,
                "totalElements", totalElements
        );
        // 回傳 200 OK 與資料
        return ResponseEntity.ok(response);
    }

    // 搜尋撤銷申請的 API，根據 CaseNumber 和 NationalID 進行搜尋，支援分頁
    @GetMapping("/search")
    public ResponseEntity<Object> searchRevokedApplications(
            @RequestParam(required = false) String caseNumber,
            @RequestParam(required = false) String nationalID,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String institutionID) {
        // 參數驗證
        if (offset < 0 || size <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "offset 必須 >= 0 且 size 必須 > 0"));
        }
        final int MAX_SIZE = 100;
        if (size > MAX_SIZE) size = MAX_SIZE;
        int page = offset / size;

        // 呼叫服務層搜尋
        List<RevokeApplicationDTO> content = revokeService.searchRevokedApplicationsPaged(
                caseNumber != null ? caseNumber.trim() : null,
                nationalID != null ? nationalID.trim() : null,
                page, size,
                institutionID != null ? institutionID.trim() : null
        );
        long totalElements = revokeService.countSearchRevokedApplications(
                caseNumber != null ? caseNumber.trim() : null,
                nationalID != null ? nationalID.trim() : null,
                institutionID != null ? institutionID.trim() : null
        );
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = offset + size < totalElements;

        Map<String, Object> response = Map.of(
                "offset", offset,
                "size", size,
                "totalPages", totalPages,
                "hasNext", hasNext,
                "content", content,
                "totalElements", totalElements
        );
        return ResponseEntity.ok(response);
    }

    // 新增：POST API 根據 CancellationID 與 NationalID 做兩次查詢並回傳兩個 DTO
    @PostMapping("/details")
    public ResponseEntity<Object> getRevokeDetails(@RequestBody RevokeSearchRequest req) {
        String cancellationID = req != null ? req.getCancellationID() : null;
        String nationalID = req != null ? req.getNationalID() : null;
        if (cancellationID == null || cancellationID.isEmpty() || nationalID == null || nationalID.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "cancellationID 和 nationalID 為必填"));
        }
        try {
            // 第一次查詢：透過 cancellation LEFT JOIN application_participants 取 participantType == 2 的家長資料
            List<ApplicationParticipantDTO> parents = revokeService.getParentsByCancellation(cancellationID);
            // 取得撤銷 summary
            RevokeApplicationDTO revokeInfo = revokeService.getRevokeByCancellationID(cancellationID);
            // 第二次查詢：依據 CancellationID 與 NationalID 查 applications 與 application_participants 裡的資料
            ApplicationParticipantDTO appDetail = revokeService.getApplicationDetailByCancellationAndNationalID(cancellationID, nationalID);

            RevokeDetailResponse resp = new RevokeDetailResponse(revokeInfo, parents, appDetail);
            return ResponseEntity.ok(resp);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return ResponseEntity.status(404).body(Map.of("error", "找不到對應資料"));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    // PUT: 更新撤銷聲請的確認日期
    @PutMapping("/confirm-date")
    public ResponseEntity<Object> updateConfirmDate(@RequestBody UpdateConfirmDateRequest req) {
        String cancellationID = req != null ? req.getCancellationID() : null;
        LocalDate confirmDate = req != null ? req.getConfirmDate() : null;
        if (cancellationID == null || cancellationID.isEmpty() || confirmDate == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "cancellationID 與 confirmDate 為必填"));
        }
        try {
            int updated = revokeService.updateConfirmDate(cancellationID, confirmDate);
            if (updated > 0) {
                return ResponseEntity.ok(Map.of("success", true, "updated", updated));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "找不到對應資料"));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    // PUT: 更新 application_participants 的 Status（根據 ApplicationID 與 NationalID）
    @PutMapping("/update-participant-status")
    public ResponseEntity<Object> updateParticipantStatus(@RequestBody Map<String, String> req) {
        String applicationID = req != null ? req.get("ApplicationID") : null;
        String nationalID = req != null ? req.get("NationalID") : null;
        String status = req != null ? req.get("Status") : null;
        if (applicationID == null || applicationID.isEmpty() || nationalID == null || nationalID.isEmpty() || status == null || status.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "ApplicationID, NationalID 與 Status 為必填"));
        }
        try {
            int updated = revokeService.updateApplicationParticipantStatus(applicationID, nationalID, status);
            if (updated > 0) {
                return ResponseEntity.ok(Map.of("success", true, "updated", updated));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "找不到對應資料"));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    // 新增 POST API：建立一筆 cancellation 紀錄
    @PostMapping("/create")
    public ResponseEntity<Object> createCancellation(@RequestBody CreateRevokeRequest req) {
        if (req == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Request body required"));
        }
        String nationalID = req.getNationalID() != null ? req.getNationalID().trim() : null;
        String abandonReason = req.getAbandonReason() != null ? req.getAbandonReason().trim() : null;
        String applicationID = req.getApplicationID() != null ? req.getApplicationID().trim() : null;
        String caseNumber = req.getCaseNumber() != null ? req.getCaseNumber().trim() : null;
        if (nationalID == null || nationalID.isEmpty() || abandonReason == null || abandonReason.isEmpty() || applicationID == null || applicationID.isEmpty() || caseNumber == null || caseNumber.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "nationalID, abandonReason, applicationID, caseNumber 為必填"));
        }
        try {
            revokeService.createCancellation(applicationID, abandonReason, nationalID, caseNumber);
            LocalDate cancellationDate = LocalDate.now();
            return ResponseEntity.ok(Map.of("success", true, "cancellationDate", cancellationDate));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

}
