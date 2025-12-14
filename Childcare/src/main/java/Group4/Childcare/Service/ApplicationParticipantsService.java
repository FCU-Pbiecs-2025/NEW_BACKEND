package Group4.Childcare.Service;

import Group4.Childcare.Model.ApplicationParticipants;
import Group4.Childcare.Repository.ApplicationParticipantsJdbcRepository;
import Group4.Childcare.Repository.ApplicationsJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApplicationParticipantsService {
    private final ApplicationParticipantsJdbcRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationsJdbcRepository applicationsJdbcRepository;

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired
    public ApplicationParticipantsService(ApplicationParticipantsJdbcRepository repository) {
        this.repository = repository;
    }

    public ApplicationParticipants create(ApplicationParticipants entity) {
        return repository.save(entity);
    }

    public Optional<ApplicationParticipants> getById(UUID id) {
        return repository.findById(id);
    }

    public List<ApplicationParticipants> getAll() {
        return repository.findAll();
    }

    public ApplicationParticipants update(UUID id, ApplicationParticipants entity) {
        entity.setApplicationID(id);
        return repository.save(entity);
    }

    public ApplicationParticipants updateParticipant(UUID participantID, String status, String reason, UUID classID) {
        Optional<ApplicationParticipants> existingEntity = repository.findById(participantID);
        if (existingEntity.isPresent()) {
            ApplicationParticipants participant = existingEntity.get();
            participant.setStatus(status);
            participant.setReason(reason);
            participant.setClassID(classID);
            return repository.save(participant);
        } else {
            throw new RuntimeException("Participant not found with ID: " + participantID);
        }
    }

    /**
     * 更新參與者狀態，支持動態currentOrder更新和自動遞補
     *
     * 功能：
     * 1. 當狀態變為"候補中"時，自動指派下一個候補序號
     * 2. 當狀態從"候補中"變為其他狀態（如已錄取）時：
     *    - 將該個案的currentOrder設為null
     *    - 自動遞補：同機構後面所有的currentOrder減1
     *
     * @param applicationID 申請案件ID
     * @param nationalID 參與者身分證
     * @param status 新的參與者狀態
     * @param reason 審核原因或備註
     * @param classID 班級ID
     * @return 更新後的參與者資訊
     */
    public ApplicationParticipants updateParticipantWithDynamicOrder(
            UUID applicationID, String nationalID, String status, String reason, UUID classID) {

        System.out.println("[DEBUG updateParticipantWithDynamicOrder] ApplicationID: " + applicationID +
                         ", NationalID: " + nationalID + ", Status: " + status);

        // 查詢該參與者的當前狀態和 CurrentOrder
        String getCurrentInfoSql = "SELECT Status, CurrentOrder, ParticipantType FROM application_participants WHERE ApplicationID = ? AND NationalID = ?";
        String oldStatus = null;
        Integer oldCurrentOrder = null;
        Boolean isChild = null;

        try {
            java.util.Map<String, Object> currentInfo = jdbcTemplate.queryForMap(getCurrentInfoSql, applicationID.toString(), nationalID);
            oldStatus = (String) currentInfo.get("Status");
            Object currentOrderObj = currentInfo.get("CurrentOrder");
            if (currentOrderObj != null) {
                oldCurrentOrder = ((Number) currentOrderObj).intValue();
            }
            Object participantTypeObj = currentInfo.get("ParticipantType");
            if (participantTypeObj != null) {
                if (participantTypeObj instanceof Boolean) {
                    isChild = !(Boolean) participantTypeObj;
                } else if (participantTypeObj instanceof Number) {
                    isChild = ((Number) participantTypeObj).intValue() == 0;
                }
            }

            System.out.println("[DEBUG] 查詢當前資料 - 舊狀態: " + oldStatus + ", 舊CurrentOrder: " + oldCurrentOrder + ", isChild: " + isChild);
        } catch (Exception ex) {
            System.out.println("[ERROR] 無法查詢當前資料: " + ex.getMessage());
        }

        Integer currentOrder = null;

        // 情況1: 如果狀態改為"候補中"，設置新的 CurrentOrder
        if (status != null && "候補中".equals(status)) {
            System.out.println("[DEBUG] 狀態為候補中，開始處理 CurrentOrder");

            if (isChild != null && isChild) {
                // 獲取該申請案件的InstitutionID
                String getInstitutionIdSql = "SELECT InstitutionID FROM applications WHERE ApplicationID = ?";
                UUID institutionId = null;
                try {
                    String institutionIdStr = jdbcTemplate.queryForObject(getInstitutionIdSql, String.class, applicationID.toString());
                    if (institutionIdStr != null) {
                        institutionId = UUID.fromString(institutionIdStr);
                        System.out.println("[DEBUG] InstitutionID: " + institutionId);
                    }
                } catch (Exception ex) {
                    System.out.println("[ERROR] 無法獲取 InstitutionID: " + ex.getMessage());
                }

                if (institutionId != null) {
                    // 查詢同機構的最大CurrentOrder值
                    String getMaxOrderSql =
                        "SELECT MAX(ap.CurrentOrder) FROM application_participants ap " +
                        "INNER JOIN applications a ON ap.ApplicationID = a.ApplicationID " +
                        "WHERE a.InstitutionID = ? " +
                        "AND ap.CurrentOrder IS NOT NULL " +
                        "AND ap.ParticipantType = 0";

                    Integer maxOrder = null;
                    try {
                        maxOrder = jdbcTemplate.queryForObject(getMaxOrderSql, Integer.class, institutionId.toString());
                        System.out.println("[DEBUG] 查詢到的最大 CurrentOrder: " + maxOrder);
                    } catch (Exception ex) {
                        System.out.println("[DEBUG] 無法查詢最大 CurrentOrder (可能沒有記錄): " + ex.getMessage());
                    }

                    if (maxOrder == null) {
                        currentOrder = 1;
                        System.out.println("[DEBUG] 設置 CurrentOrder = 1 (首個候補)");
                    } else {
                        currentOrder = maxOrder + 1;
                        System.out.println("[DEBUG] 設置 CurrentOrder = " + currentOrder + " (maxOrder + 1)");
                    }
                }
            } else {
                System.out.println("[DEBUG] 非幼兒記錄，不設置 CurrentOrder");
            }
        }
        // 情況2: 如果原本是"候補中"且有 CurrentOrder，現在改為其他狀態（如已錄取），需要遞補後面的 CurrentOrder
        else if (oldStatus != null && "候補中".equals(oldStatus) && oldCurrentOrder != null && isChild != null && isChild) {
            System.out.println("[DEBUG] 從候補中變更為其他狀態，需要遞補後面的 CurrentOrder");

            // 獲取該申請案件的InstitutionID
            String getInstitutionIdSql = "SELECT InstitutionID FROM applications WHERE ApplicationID = ?";
            UUID institutionId = null;
            try {
                String institutionIdStr = jdbcTemplate.queryForObject(getInstitutionIdSql, String.class, applicationID.toString());
                if (institutionIdStr != null) {
                    institutionId = UUID.fromString(institutionIdStr);
                    System.out.println("[DEBUG] InstitutionID: " + institutionId);
                }
            } catch (Exception ex) {
                System.out.println("[ERROR] 無法獲取 InstitutionID: " + ex.getMessage());
            }

            if (institutionId != null) {
                // 將該個案後面所有的 CurrentOrder 減 1
                String updateFollowingOrdersSql =
                    "UPDATE application_participants " +
                    "SET CurrentOrder = CurrentOrder - 1 " +
                    "WHERE ParticipantType = 0 " +
                    "AND CurrentOrder > ? " +
                    "AND ApplicationID IN ( " +
                    "  SELECT ApplicationID FROM applications WHERE InstitutionID = ? " +
                    ")";

                try {
                    int updatedCount = jdbcTemplate.update(updateFollowingOrdersSql, oldCurrentOrder, institutionId.toString());
                    System.out.println("[DEBUG] 遞補完成：將 CurrentOrder > " + oldCurrentOrder + " 的 " + updatedCount + " 筆記錄減 1");
                } catch (Exception ex) {
                    System.out.println("[ERROR] 遞補 CurrentOrder 失敗: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }

            // 將當前個案的 CurrentOrder 設為 null（因為已不在候補狀態）
            currentOrder = null;
            System.out.println("[DEBUG] 將當前個案的 CurrentOrder 設為 null");
        }

        // 執行更新
        String updateSql = "UPDATE application_participants SET Status = ?, Reason = ?, ReviewDate = ?, CurrentOrder = ? WHERE ApplicationID = ? AND NationalID = ?";
        java.sql.Timestamp reviewTs = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());
        try {
            int rowsAffected = jdbcTemplate.update(updateSql, status, reason, reviewTs, currentOrder, applicationID.toString(), nationalID);
            System.out.println("[DEBUG] 更新完成，影響行數: " + rowsAffected + ", 新 CurrentOrder: " + currentOrder);
        } catch (Exception ex) {
            System.out.println("[ERROR] 更新失敗: " + ex.getMessage());
            ex.printStackTrace();
        }

        // 更新ClassID如果提供了
        if (classID != null) {
            String updateClassSql = "UPDATE application_participants SET ClassID = ? WHERE ApplicationID = ? AND NationalID = ?";
            try {
                jdbcTemplate.update(updateClassSql, classID.toString(), applicationID.toString(), nationalID);
                System.out.println("[DEBUG] ClassID 更新完成");
            } catch (Exception ex) {
                System.out.println("[ERROR] ClassID 更新失敗: " + ex.getMessage());
            }
        }

        // 查詢並返回更新後的參與者
        List<ApplicationParticipants> participants = repository.findByApplicationIDAndNationalID(applicationID, nationalID);
        if (!participants.isEmpty()) {
            ApplicationParticipants updatedParticipant = participants.get(0);

            // 🔔 發送郵件通知
            sendStatusChangeEmail(applicationID, nationalID, status, reason, currentOrder);

            return updatedParticipant;
        } else {
            throw new RuntimeException("Participant not found after update: ApplicationID=" + applicationID + ", NationalID=" + nationalID);
        }
    }

    /**
     * 發送狀態變更郵件通知
     */
    private void sendStatusChangeEmail(UUID applicationID, String nationalID, String status, String reason, Integer currentOrder) {
        if (emailService == null) {
            System.out.println("⚠️ [sendStatusChangeEmail] EmailService 未配置，跳過郵件發送");
            return;
        }

        try {
            System.out.println("🔔 [sendStatusChangeEmail] 準備發送郵件:");
            System.out.println("  ApplicationID: " + applicationID);
            System.out.println("  NationalID: " + nationalID);
            System.out.println("  狀態: " + status);

            // 1. 從 users 表查詢申請人郵件
            Optional<String> emailOpt = applicationsJdbcRepository.getUserEmailByApplicationId(applicationID);
            if (emailOpt.isEmpty() || emailOpt.get().isEmpty()) {
                System.err.println("❌ 無法找到申請人郵件地址: applicationId=" + applicationID);
                return;
            }
            String applicantEmail = emailOpt.get();
            System.out.println("  收件人: " + applicantEmail);

            // 2. 查詢案件相關資訊
            String querySql =
                "SELECT " +
                "  u.Name AS ApplicantName, " +
                "  ap.Name AS ChildName, " +
                "  i.InstitutionName, " +
                "  a.CaseNumber, " +
                "  a.ApplicationDate " +
                "FROM application_participants ap " +
                "INNER JOIN applications a ON ap.ApplicationID = a.ApplicationID " +
                "LEFT JOIN users u ON a.UserID = u.UserID " +
                "LEFT JOIN institutions i ON a.InstitutionID = i.InstitutionID " +
                "WHERE ap.ApplicationID = ? AND ap.NationalID = ?";

            java.util.Map<String, Object> caseInfo = jdbcTemplate.queryForMap(querySql, applicationID.toString(), nationalID);

            String applicantName = (String) caseInfo.get("ApplicantName");
            String childName = (String) caseInfo.get("ChildName");
            String institutionName = (String) caseInfo.get("InstitutionName");
            Object caseNumberObj = caseInfo.get("CaseNumber");
            Long caseNumber = caseNumberObj != null ? ((Number) caseNumberObj).longValue() : null;
            Object appDateObj = caseInfo.get("ApplicationDate");
            String applicationDate = appDateObj != null ? appDateObj.toString() : "";

            System.out.println("  申請人: " + applicantName);
            System.out.println("  幼兒: " + childName);
            System.out.println("  機構: " + institutionName);

            // 3. 發送郵件
            emailService.sendApplicationStatusChangeEmail(
                    applicantEmail,
                    applicantName != null ? applicantName : "",
                    childName != null ? childName : "",
                    institutionName != null ? institutionName : "",
                    caseNumber,
                    applicationDate,
                    status,
                    currentOrder,
                    reason
            );

            System.out.println("✅ [sendStatusChangeEmail] 郵件發送成功: " + applicantEmail + " (狀態: " + status + ")");

        } catch (Exception e) {
            System.err.println("❌ [sendStatusChangeEmail] 郵件發送失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 撤銷申請並自動遞補 CurrentOrder
     *
     * 功能：
     * 1. 將申請案的狀態設為"已撤銷"
     * 2. 如果該申請案有 CurrentOrder（候補序號）：
     *    - 將該申請案的 CurrentOrder 設為 null
     *    - 自動遞補：同機構後面所有的 CurrentOrder 減 1
     * 3. 如果 CurrentOrder 為 null，則不影響其他申請案
     *
     * @param applicationID 申請案件ID
     * @param nationalID 參與者身分證
     * @param reason 撤銷原因
     * @return 更新後的參與者資訊
     */
    public ApplicationParticipants cancelApplicationWithOrderRecalculation(
            UUID applicationID, String nationalID, String reason) {

        System.out.println("[DEBUG cancelApplicationWithOrderRecalculation] ApplicationID: " + applicationID +
                         ", NationalID: " + nationalID);

        // 1. 查詢該參與者的當前 CurrentOrder 和 ParticipantType
        String getCurrentInfoSql = "SELECT CurrentOrder, ParticipantType FROM application_participants WHERE ApplicationID = ? AND NationalID = ?";
        Integer currentOrder = null;
        Boolean isChild = null;

        try {
            java.util.Map<String, Object> currentInfo = jdbcTemplate.queryForMap(getCurrentInfoSql, applicationID.toString(), nationalID);
            Object currentOrderObj = currentInfo.get("CurrentOrder");
            if (currentOrderObj != null) {
                currentOrder = ((Number) currentOrderObj).intValue();
            }
            Object participantTypeObj = currentInfo.get("ParticipantType");
            if (participantTypeObj != null) {
                if (participantTypeObj instanceof Boolean) {
                    isChild = !(Boolean) participantTypeObj;
                } else if (participantTypeObj instanceof Number) {
                    isChild = ((Number) participantTypeObj).intValue() == 0;
                }
            }

            System.out.println("[DEBUG] 查詢當前資料 - CurrentOrder: " + currentOrder + ", isChild: " + isChild);
        } catch (Exception ex) {
            System.out.println("[ERROR] 無法查詢當前資料: " + ex.getMessage());
            throw new RuntimeException("無法查詢申請案資料: " + ex.getMessage());
        }

        // 2. 如果有 CurrentOrder 且是幼兒記錄，需要遞補後面的 CurrentOrder
        if (currentOrder != null && isChild != null && isChild) {
            System.out.println("[DEBUG] 該申請案有候補序號 " + currentOrder + "，開始遞補後面的申請案");

            // 獲取該申請案件的 InstitutionID
            String getInstitutionIdSql = "SELECT InstitutionID FROM applications WHERE ApplicationID = ?";
            UUID institutionId = null;
            try {
                String institutionIdStr = jdbcTemplate.queryForObject(getInstitutionIdSql, String.class, applicationID.toString());
                if (institutionIdStr != null) {
                    institutionId = UUID.fromString(institutionIdStr);
                    System.out.println("[DEBUG] InstitutionID: " + institutionId);
                }
            } catch (Exception ex) {
                System.out.println("[ERROR] 無法獲取 InstitutionID: " + ex.getMessage());
            }

            if (institutionId != null) {
                // 將該個案後面所有的 CurrentOrder 減 1
                String updateFollowingOrdersSql =
                    "UPDATE application_participants " +
                    "SET CurrentOrder = CurrentOrder - 1 " +
                    "WHERE ParticipantType = 0 " +
                    "AND CurrentOrder > ? " +
                    "AND ApplicationID IN ( " +
                    "  SELECT ApplicationID FROM applications WHERE InstitutionID = ? " +
                    ")";

                try {
                    int updatedCount = jdbcTemplate.update(updateFollowingOrdersSql, currentOrder, institutionId.toString());
                    System.out.println("[DEBUG] 遞補完成：將 CurrentOrder > " + currentOrder + " 的 " + updatedCount + " 筆記錄減 1");
                } catch (Exception ex) {
                    System.out.println("[ERROR] 遞補 CurrentOrder 失敗: " + ex.getMessage());
                    ex.printStackTrace();
                    throw new RuntimeException("遞補候補序號失敗: " + ex.getMessage());
                }
            }
        } else if (currentOrder == null) {
            System.out.println("[DEBUG] 該申請案無候補序號，不需要遞補");
        } else {
            System.out.println("[DEBUG] 該申請案非幼兒記錄，不需要遞補");
        }

        // 3. 更新申請案狀態為"已撤銷"，並將 CurrentOrder 設為 null
        String updateSql = "UPDATE application_participants SET Status = '撤銷申請通過', Reason = ?, ReviewDate = ?, CurrentOrder = NULL WHERE ApplicationID = ? AND NationalID = ?";
        java.sql.Timestamp reviewTs = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());
        try {
            int rowsAffected = jdbcTemplate.update(updateSql, reason, reviewTs, applicationID.toString(), nationalID);
            System.out.println("[DEBUG] 申請案撤銷完成，影響行數: " + rowsAffected);
        } catch (Exception ex) {
            System.out.println("[ERROR] 更新失敗: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("撤銷申請案失敗: " + ex.getMessage());
        }

        // 4. 查詢並返回更新後的參與者
        List<ApplicationParticipants> participants = repository.findByApplicationIDAndNationalID(applicationID, nationalID);
        if (!participants.isEmpty()) {
            ApplicationParticipants updatedParticipant = participants.get(0);

            // 🔔 發送郵件通知
            sendStatusChangeEmail(applicationID, nationalID, "已撤銷", reason, null);

            return updatedParticipant;
        } else {
            throw new RuntimeException("Participant not found after cancel: ApplicationID=" + applicationID + ", NationalID=" + nationalID);
        }
    }

    /**
     * 計算指定 NationalID 且 ParticipantType = false (幼兒) 的總案件數
     * @param nationalID 幼兒身分證字號
     * @return 該幼兒的總案件數
     */
    public int countApplicationsByChildNationalID(String nationalID) {
        return repository.countApplicationsByChildNationalID(nationalID);
    }

}
