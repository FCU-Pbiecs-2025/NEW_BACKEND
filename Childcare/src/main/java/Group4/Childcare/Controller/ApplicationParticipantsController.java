package Group4.Childcare.Controller;

import Group4.Childcare.Model.ApplicationParticipants;
import Group4.Childcare.Service.ApplicationParticipantsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
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
}
