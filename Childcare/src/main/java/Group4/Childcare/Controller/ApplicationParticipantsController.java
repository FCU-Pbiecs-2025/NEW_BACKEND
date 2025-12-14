package Group4.Childcare.Controller;

import Group4.Childcare.Model.ApplicationParticipants;
import Group4.Childcare.Service.ApplicationParticipantsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/application-participants")
public class ApplicationParticipantsController {
    private final ApplicationParticipantsService service;

    @Autowired
    public ApplicationParticipantsController(ApplicationParticipantsService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApplicationParticipants> create(@RequestBody ApplicationParticipants entity) {
        return ResponseEntity.ok(service.create(entity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationParticipants> getById(@PathVariable UUID id) {
        Optional<ApplicationParticipants> entity = service.getById(id);
        return entity.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ApplicationParticipants>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    /**
     * 更新參與者資訊，支持兩種模式：
     *
     * 模式1：legacy模式（只傳participantID）
     *   - 直接更新參與者資訊，不涉及currentOrder動態計算
     *   - 適用於簡單的狀態/備註更新
     *
     * 模式2：新模式（傳applicationID + nationalID）
     *   - 支持動態currentOrder計算和自動遞補
     *   - 當狀態改為"候補中"時：自動指派下一個候補序號
     *   - 當狀態從"候補中"改為其他狀態（如已錄取）時：
     *     * 將該個案的currentOrder設為null
     *     * 自動遞補：同機構後面所有的currentOrder減1
     *   - 例：錄取11號個案後，12號會變成11號、13號變成12號...
     *
     * @param participantID 參與者ID（PathVariable，兩種模式都需要）
     * @param applicationID 申請案件ID（RequestParam，新模式需要）
     * @param nationalID 參與者身分證（RequestParam，新模式需要）
     * @param status 參與者狀態（候補中/已錄取/需要補件/已退件等）
     * @param reason 審核原因或備註
     * @param classID 班級ID
     * @return 更新後的參與者資訊
     */
    @PutMapping("/{participantID}")
    public ResponseEntity<?> update(
            @PathVariable UUID participantID,
            @RequestParam(required = false) UUID applicationID,
            @RequestParam(required = false) String nationalID,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) UUID classID) {

        try {
            // 模式2：新模式 - 使用applicationID + nationalID，支持動態currentOrder計算
            if (applicationID != null && nationalID != null && !nationalID.isEmpty()) {
                System.out.println("[DEBUG] 使用新模式：applicationID=" + applicationID + ", nationalID=" + nationalID);
                ApplicationParticipants result = service.updateParticipantWithDynamicOrder(
                    applicationID, nationalID, status, reason, classID);
                return ResponseEntity.ok(result);
            }

            // 模式1：legacy模式 - 只使用participantID，簡單更新
            System.out.println("[DEBUG] 使用legacy模式：participantID=" + participantID);
            ApplicationParticipants result = service.updateParticipant(participantID, status, reason, classID);
            return ResponseEntity.ok(result);

        } catch (RuntimeException ex) {
            return ResponseEntity.status(404).body("錯誤: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("更新失敗: " + ex.getMessage());
        }
    }

    /**
     * 撤銷申請並自動遞補 CurrentOrder
     *
     * 功能說明：
     * 1. 將申請案的狀態設為"已撤銷"
     * 2. 如果該申請案有 CurrentOrder（候補序號）：
     *    - 將該申請案的 CurrentOrder 設為 null
     *    - 自動遞補：同機構後面所有的 CurrentOrder 減 1
     * 3. 如果 CurrentOrder 為 null，則不影響其他申請案
     * 4. 自動發送撤銷通知郵件給申請人
     *
     * 使用範例：
     * POST /application-participants/cancel
     * {
     *   "applicationID": "550e8400-e29b-41d4-a716-446655440000",
     *   "nationalID": "A123456789",
     *   "reason": "家長主動撤銷申請"
     * }
     *
     * @param requestBody 包含 applicationID, nationalID, reason 的 JSON 物件
     * @return 撤銷結果，包含更新後的參與者資訊和遞補統計
     */
    @PostMapping("/cancel")
    @Transactional
    public ResponseEntity<Map<String, Object>> cancelApplication(@RequestBody Map<String, String> requestBody) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 取得請求參數
            String applicationIDStr = requestBody.get("ApplicationID");
            String nationalID = requestBody.get("NationalID");
            String reason = requestBody.get("reason");

            // 驗證必填參數
            if (applicationIDStr == null || applicationIDStr.isEmpty()) {
                response.put("success", false);
                response.put("message", "ApplicationID 為必填欄位");
                return ResponseEntity.badRequest().body(response);
            }

            if (nationalID == null || nationalID.isEmpty()) {
                response.put("success", false);
                response.put("message", "NationalID 為必填欄位");
                return ResponseEntity.badRequest().body(response);
            }

            UUID applicationID = UUID.fromString(applicationIDStr);

            // 如果沒有提供原因，使用預設值
            if (reason == null || reason.isEmpty()) {
                reason = "使用者撤銷申請";
            }

            System.out.println("📋 [cancelApplication API] 收到撤銷申請請求:");
            System.out.println("  ApplicationID: " + applicationID);
            System.out.println("  NationalID: " + nationalID);
            System.out.println("  Reason: " + reason);

            // 呼叫 Service 層方法執行撤銷邏輯
            ApplicationParticipants canceledParticipant = service.cancelApplicationWithOrderRecalculation(
                applicationID,
                nationalID,
                reason
            );

            // 返回成功結果（與原本格式一致）
            response.put("success", true);
            response.put("message", "撤銷審核通過");

            System.out.println("✅ [cancelApplication API] 撤銷成功");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            // UUID 格式錯誤
            response.put("success", false);
            response.put("message", "參數格式錯誤: " + ex.getMessage());
            System.err.println("❌ [cancelApplication API] 參數格式錯誤: " + ex.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (RuntimeException ex) {
            // 業務邏輯錯誤（如找不到記錄）
            response.put("success", false);
            response.put("message", "撤銷失敗: " + ex.getMessage());
            System.err.println("❌ [cancelApplication API] 業務邏輯錯誤: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception ex) {
            // 其他未預期的錯誤
            response.put("success", false);
            response.put("message", "系統錯誤: " + ex.getMessage());
            System.err.println("❌ [cancelApplication API] 系統錯誤: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
