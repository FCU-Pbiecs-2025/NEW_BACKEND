package Group4.Childcare.Service;

import Group4.Childcare.Model.Applications;
import Group4.Childcare.DTO.ApplicationSummaryDTO;
import Group4.Childcare.Repository.ApplicationsJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import Group4.Childcare.DTO.ApplicationApplyDTO;
import Group4.Childcare.DTO.ApplicationParticipantDTO;
import Group4.Childcare.Model.ApplicationParticipants;
import Group4.Childcare.Repository.ApplicationParticipantsJdbcRepository;
import java.time.LocalDate;
import Group4.Childcare.DTO.ApplicationSummaryWithDetailsDTO;
import Group4.Childcare.DTO.ApplicationCaseDTO;
import Group4.Childcare.DTO.CaseOffsetListDTO;
import Group4.Childcare.DTO.CaseOffsetListDTO;
import Group4.Childcare.DTO.CaseEditUpdateDTO;
import Group4.Childcare.DTO.UserApplicationDetailsDTO;

@Service
public class ApplicationsService {
    @Autowired
    private ApplicationsJdbcRepository applicationsJdbcRepository;
    @Autowired
    private ApplicationParticipantsJdbcRepository applicationParticipantsRepository;
    @Autowired
    private Group4.Childcare.Service.FileService fileService;
    @Autowired(required = false)
    private EmailService emailService;

    @Transactional
    public Applications create(Applications entity) {
        return applicationsJdbcRepository.save(entity);
    }

    /**
     * 生成案件編號
     * 格式：YYYYMMDD + 4位流水號，如 202412040001
     * 流水號為符合日期格式的總案件數 + 1
     * @return 新的案件編號
     */
    public Long generateCaseNumber() {
        java.time.LocalDate today = java.time.LocalDate.now();
        // 生成日期前綴：YYYYMMDD
        long datePrefix = today.getYear() * 10000L + today.getMonthValue() * 100L + today.getDayOfMonth();

        // 查詢符合日期格式的總案件數
        long totalCount = applicationsJdbcRepository.countCaseNumberWithDateFormat();

        // 流水號 = 總案件數 + 1，格式化為4位數
        long sequenceNumber = totalCount + 1;

        // 組合案件編號：YYYYMMDD + 4位流水號
        return datePrefix * 10000 + sequenceNumber;
    }

    public Optional<Applications> getById(UUID id) {
        return applicationsJdbcRepository.findById(id);
    }

    public List<Applications> getAll() {
        return applicationsJdbcRepository.findAll();
    }

    @Transactional
    public Applications update(UUID id, Applications entity) {
        entity.setApplicationID(id);
        return applicationsJdbcRepository.save(entity);
    }

    public List<ApplicationSummaryDTO> getSummaryByUserID(UUID userID) {
        return applicationsJdbcRepository.findSummaryByUserID(userID);
    }

    // New: expose JDBC offset query
    public List<ApplicationSummaryWithDetailsDTO> getSummariesWithOffset(int offset, int limit) {
        return applicationsJdbcRepository.findSummariesWithOffset(offset, limit);
    }

    public void apply(ApplicationApplyDTO dto) {
        Applications app = new Applications();
        app.setApplicationID(UUID.randomUUID());
        app.setApplicationDate(LocalDate.now());
        // 身分類別
        app.setIdentityType((byte)("低收入戶".equals(dto.identityType) ? 1 : "中低收入戶".equals(dto.identityType) ? 2 : 0));
        // 附件路徑（多檔名以逗號分隔）
        if (dto.attachmentFiles != null && !dto.attachmentFiles.isEmpty()) {
            app.setAttachmentPath(String.join(",", dto.attachmentFiles));
        }
        applicationsJdbcRepository.save(app);
        // 申請人與家長資料
        if (dto.participants != null) {
            for (ApplicationParticipantDTO p : dto.participants) {
                ApplicationParticipants entity = new ApplicationParticipants();
                entity.setApplicationID(app.getApplicationID());
                // 支持 "家長"/"幼兒" 文字或 1/0 數字格式
                boolean isParent = "家長".equals(p.participantType) || "1".equals(p.participantType);
                entity.setParticipantType(isParent);
                entity.setNationalID(p.nationalID);
                entity.setName(p.name);
                entity.setGender("男".equals(p.gender));
                entity.setRelationShip(p.relationShip);
                entity.setOccupation(p.occupation);
                entity.setPhoneNumber(p.phoneNumber);
                entity.setHouseholdAddress(p.householdAddress);
                entity.setMailingAddress(p.mailingAddress);
                entity.setEmail(p.email);
                entity.setBirthDate(p.birthDate != null && !p.birthDate.isEmpty() ? LocalDate.parse(p.birthDate) : null);
                entity.setIsSuspended(p.isSuspended);
                entity.setSuspendEnd(p.suspendEnd != null && !p.suspendEnd.isEmpty() ? LocalDate.parse(p.suspendEnd) : null);
                entity.setCurrentOrder(p.currentOrder);
                entity.setStatus(p.status);
                entity.setClassID(p.classID != null && !p.classID.isEmpty() ? UUID.fromString(p.classID) : null);
                // participant-level review fields
                entity.setReviewDate(p.reviewDate);
                applicationParticipantsRepository.save(entity);
            }
        }
    }

    // New method to get total applications count
    public long getTotalApplicationsCount() {
        return applicationsJdbcRepository.count();
    }

    // New method to map an Applications entity to ApplicationSummaryWithDetailsDTO
    public Optional<ApplicationSummaryWithDetailsDTO> getApplicationSummaryWithDetailsById(UUID id) {
        return applicationsJdbcRepository.findApplicationSummaryWithDetailsById(id);

    }

    // New method to search applications with optional parameters
    public List<ApplicationSummaryWithDetailsDTO> searchApplications(String institutionID, String institutionName, String applicationID) {
        // repository expects: (institutionID, institutionName, caseNumber, nationalID)
        return applicationsJdbcRepository.searchApplications(institutionID, institutionName, applicationID, null);
    }

    // New method to search applications with optional parameters (institution + caseNumber + nationalID)
    public List<ApplicationSummaryWithDetailsDTO> searchApplications(String institutionID, String institutionName, String caseNumber, String nationalID) {
        return applicationsJdbcRepository.searchApplications(institutionID, institutionName, caseNumber, nationalID);
    }

    // New method to search applications with optional parameters (institution + caseNumber + nationalID)
    public List<ApplicationSummaryWithDetailsDTO> revokesearchApplications(String institutionID, String institutionName, String caseNumber, String nationalID) {
        return applicationsJdbcRepository.revokesearchApplications(institutionID, institutionName, caseNumber, nationalID);
    }

    // JDBC 方式查詢單一個案 - changed to return ApplicationCaseDTO
    public Optional<ApplicationCaseDTO> getApplicationByIdJdbc(UUID id, String nationalID) {
        return applicationsJdbcRepository.findApplicationCaseById(id, nationalID, null);
    }

    // Update single participant's status and reason, optionally set reviewDate
    public void updateParticipantStatusReason(UUID id, String nationalID, String status, String reason, java.time.LocalDateTime reviewDate) {
        applicationsJdbcRepository.updateParticipantStatusReason(id, nationalID, status, reason, reviewDate);
    }

    /**
     * 更新審核狀態並發送通知郵件
     * @param applicationId 案件 ID
     * @param nationalID 幼兒身分證字號
     * @param newStatus 新狀態
     * @param reason 備註說明（可為 null）
     * @param reviewDate 審核日期
     */
    @Transactional
    public void updateStatusAndSendEmail(
            UUID applicationId,
            String nationalID,
            String newStatus,
            String reason,
            java.time.LocalDateTime reviewDate) {

        // 1. 更新審核狀態
        updateParticipantStatusReason(applicationId, nationalID, newStatus, reason, reviewDate);

        // 2. 查詢案件詳情以獲取發送郵件所需的信息
        try {
            // 2.1 從 users 表查詢申請人郵件
            Optional<String> emailOpt = applicationsJdbcRepository.getUserEmailByApplicationId(applicationId);
            if (emailOpt.isEmpty() || emailOpt.get() == null || emailOpt.get().isEmpty()) {
                System.err.println("❌ 無法找到申請人郵件地址: applicationId=" + applicationId);
                return;
            }
            String applicantEmail = emailOpt.get();

            // 2.2 查詢案件詳情
            Optional<ApplicationCaseDTO> caseOpt = getApplicationByIdJdbc(applicationId, nationalID);
            if (caseOpt.isEmpty()) {
                System.err.println("❌ 無法找到案件: " + applicationId);
                return;
            }

            ApplicationCaseDTO caseDto = caseOpt.get();

            // 3. 獲取申請人姓名（從家長列表中取得第一位）
            String applicantName = "";
            if (caseDto.parents != null && !caseDto.parents.isEmpty()) {
                applicantName = caseDto.parents.get(0).name;
            }

            // 4. 獲取幼兒信息（第一個 child）
            String childName = "";
            Integer currentOrder = null;
            if (caseDto.children != null && !caseDto.children.isEmpty()) {
                ApplicationParticipantDTO child = caseDto.children.get(0);
                childName = child.name;
                currentOrder = child.currentOrder;
            }

            // 5. 獲取機構名稱和案件編號
            String institutionName = caseDto.institutionName != null ? caseDto.institutionName : "";
            Long caseNumber = caseDto.caseNumber;
            String applicationDate = caseDto.applicationDate != null ? caseDto.applicationDate.toString() : "";

            // 6. 發送郵件（如果 emailService 可用）
            System.out.println("🔔 準備發送郵件通知:");
            System.out.println("  狀態: " + newStatus);
            System.out.println("  收件人: " + applicantEmail);
            System.out.println("  申請人: " + applicantName);
            System.out.println("  幼兒: " + childName);

            if (emailService != null) {
                try {
                    emailService.sendApplicationStatusChangeEmail(
                            applicantEmail,
                            applicantName,
                            childName,
                            institutionName,
                            caseNumber,
                            applicationDate,
                            newStatus,
                            currentOrder,
                            reason
                    );
                    System.out.println("✅ 審核狀態變更通知郵件已發送成功: " + applicantEmail + " (狀態: " + newStatus + ")");
                } catch (Exception emailError) {
                    System.err.println("❌ 郵件發送失敗 (狀態: " + newStatus + "): " + emailError.getMessage());
                    emailError.printStackTrace();
                }
            } else {
                System.out.println("⚠️ EmailService 未配置，郵件未發送");
                System.out.println("郵件摘要:");
                System.out.println("  收件人: " + applicantEmail);
                System.out.println("  申請人: " + applicantName);
                System.out.println("  幼兒: " + childName);
                System.out.println("  機構: " + institutionName);
                System.out.println("  案件編號: " + caseNumber);
                System.out.println("  狀態: " + newStatus);
                System.out.println("  序號: " + currentOrder);
            }

        } catch (Exception e) {
            System.err.println("❌ updateStatusAndSendEmail 整體流程出錯: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新案件資料（包含參與者信息）
     * @param id 案件 ID
     */
    @Transactional
    public void updateApplicationCase(UUID id, ApplicationCaseDTO dto) {
        applicationsJdbcRepository.updateApplicationCase(id, dto);
    }

    /**
     * 查詢案件列表（分頁）
     * @param offset 分頁起始位置
     * @param limit 每頁筆數
     * @param status 審核狀態（可選）
     * @param institutionId 機構ID（可選）
     * @param applicationId 案件ID（可選）
     * @param classId 班級ID（可選）
     * @param childNationalId 幼兒身分證字號（可選）
     * @param caseNumber 案件流水號（可選）
     * @param identityType 身分別（可選）
     * @return List<CaseOffsetListDTO>
     */
    public List<CaseOffsetListDTO> getCaseListWithOffset(int offset, int limit, String status, UUID institutionId,
                                                         UUID applicationId, UUID classId, String childNationalId,
                                                         Long caseNumber, String identityType) {
        return applicationsJdbcRepository.findCaseListWithOffset(offset, limit, status, institutionId,
                applicationId, classId, childNationalId,
                caseNumber, identityType);
    }

    /**
     * 查詢案件列表的總筆數
     * @param status 審核狀態（可選）
     * @param institutionId 機構ID（可選）
     * @param applicationId 案件ID（可選）
     * @param classId 班級ID（可選）
     * @param childNationalId 幼兒身分證字號（可選）
     * @param caseNumber 案件流水號（可選）
     * @param identityType 身分別（可選）
     * @return 總筆數
     */
    public long countCaseList(String status, UUID institutionId, UUID applicationId, UUID classId,
                              String childNationalId, Long caseNumber, String identityType) {
        return applicationsJdbcRepository.countCaseList(status, institutionId, applicationId, classId,
                childNationalId, caseNumber, identityType);
    }

    /**
     * 根據幼兒身分證字號查詢案件並自動讀取檔案列表
     * @param childrenNationalID 幼兒身分證字號
     * @return CaseEditUpdateDTO（包含檔案列表和參與者信息）或 Optional.empty()
     */
    public Optional<CaseEditUpdateDTO> getCaseByChildrenNationalId(String childrenNationalID) {
        // 查詢是否存在這個身分證字號對應的案件
        List<CaseEditUpdateDTO> results = applicationsJdbcRepository.findByNationalID(childrenNationalID);

        if (results.isEmpty()) {
            return Optional.empty();
        }

        // 取第一個找到的案件記錄
        CaseEditUpdateDTO result = results.getFirst();

        // 自動讀取檔案列表並設置到四個路徑字段
        if (result.getApplicationID() != null) {
            List<String> files = fileService.getFilesByApplicationId(result.getApplicationID());
            // 將檔案列表對應到 attachmentPath, attachmentPath1, attachmentPath2, attachmentPath3
            if (files.size() > 0) {
                result.setAttachmentPath(files.get(0));
            }
            if (files.size() > 1) {
                result.setAttachmentPath1(files.get(1));
            }
            if (files.size() > 2) {
                result.setAttachmentPath2(files.get(2));
            }
            if (files.size() > 3) {
                result.setAttachmentPath3(files.get(3));
            }
        }

        // 查詢該案件的所有參與者（家長和幼兒）
        Optional<ApplicationCaseDTO> caseDto = applicationsJdbcRepository.findApplicationCaseById(result.getApplicationID(), childrenNationalID, result.getParticipantID());
        if (caseDto.isPresent()) {
            ApplicationCaseDTO applicationCase = caseDto.get();
            result.setParents(applicationCase.parents);
            result.setChildren(applicationCase.children);
            // User 信息已經在 findByNationalID 中正確設置，無需覆蓋
        }

        return Optional.of(result);
    }

    /**
     * 根據 UserID 查詢使用者申請詳細資料
     * 包含 applications、application_participants、cancellation、user 表的聯合查詢
     * @param userID 使用者ID
     * @return 包含申請詳細資料的清單
     */
    public List<UserApplicationDetailsDTO> getUserApplicationDetails(UUID userID) {
        return applicationsJdbcRepository.findUserApplicationDetails(userID);
    }

    /**
     * 根據 ParticipantID 查詢案件並自動讀取檔案列表
     * @param participantID 參與者ID（幼兒）
     * @return CaseEditUpdateDTO（包含檔案列表和參與者信息）或 Optional.empty()
     */
    public Optional<CaseEditUpdateDTO> getCaseByParticipantId(UUID participantID) {
        // 根據 ParticipantID 直接查詢案件信息
        Optional<CaseEditUpdateDTO> resultOpt = applicationsJdbcRepository.findCaseByParticipantId(participantID);

        if (resultOpt.isEmpty()) {
            return Optional.empty();
        }

        CaseEditUpdateDTO result = resultOpt.get();

        // 自動讀取檔案列表並設置到四個路徑字段
        if (result.getApplicationID() != null) {
            List<String> files = fileService.getFilesByApplicationId(result.getApplicationID());
            // 將檔案列表對應到 attachmentPath, attachmentPath1, attachmentPath2, attachmentPath3
            if (files.size() > 0) {
                result.setAttachmentPath(files.get(0));
            }
            if (files.size() > 1) {
                result.setAttachmentPath1(files.get(1));
            }
            if (files.size() > 2) {
                result.setAttachmentPath2(files.get(2));
            }
            if (files.size() > 3) {
                result.setAttachmentPath3(files.get(3));
            }
        }

        // 查詢該案件的所有參與者（家長和幼兒）
        Optional<ApplicationCaseDTO> caseDto = applicationsJdbcRepository.findApplicationCaseByParticipantId(participantID);
        if (caseDto.isPresent()) {
            ApplicationCaseDTO applicationCase = caseDto.get();
            result.setParents(applicationCase.parents);
            result.setChildren(applicationCase.children);
        }

        return Optional.of(result);
    }

}
