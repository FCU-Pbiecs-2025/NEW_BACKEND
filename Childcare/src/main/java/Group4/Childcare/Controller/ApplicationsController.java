package Group4.Childcare.Controller;

import Group4.Childcare.Model.Applications;
import Group4.Childcare.DTO.ApplicationSummaryDTO;
import Group4.Childcare.DTO.ApplicationSummaryWithDetailsDTO;
import Group4.Childcare.DTO.ApplicationCaseDTO;
import Group4.Childcare.DTO.AdminCaseSearchRequestDto;
import Group4.Childcare.DTO.CaseOffsetListDTO;
import Group4.Childcare.DTO.CaseEditUpdateDTO;
import Group4.Childcare.DTO.UserApplicationDetailsDTO;
import Group4.Childcare.DTO.ApplicationParticipantDTO;
import Group4.Childcare.Model.ApplicationParticipants;
import Group4.Childcare.Service.ApplicationsService;
import Group4.Childcare.Service.FileService;
import Group4.Childcare.Service.ApplicationParticipantsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/applications")
public class ApplicationsController {
    private final ApplicationsService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FileService fileService;

    @Autowired
    private ApplicationParticipantsService applicationParticipantsService;

    @Autowired
    public ApplicationsController(ApplicationsService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Applications> create(@RequestBody Applications entity) {
        return ResponseEntity.ok(service.create(entity));
    }


    @PutMapping("/{id}")
    public ResponseEntity<Applications> update(@PathVariable UUID id, @RequestBody Applications entity) {
        Applications original = service.getById(id).orElseThrow();
        // 只更新有傳的欄位（部分更新）
        if (entity.getApplicationDate() != null) original.setApplicationDate(entity.getApplicationDate());
        if (entity.getCaseNumber() != null) original.setCaseNumber(entity.getCaseNumber());
        if (entity.getInstitutionID() != null) original.setInstitutionID(entity.getInstitutionID());
        if (entity.getUserID() != null) original.setUserID(entity.getUserID());
        if (entity.getIdentityType() != null) original.setIdentityType(entity.getIdentityType());
        if (entity.getAttachmentPath() != null) original.setAttachmentPath(entity.getAttachmentPath());
        if (entity.getAttachmentPath1() != null) original.setAttachmentPath1(entity.getAttachmentPath1());
        if (entity.getAttachmentPath2() != null) original.setAttachmentPath2(entity.getAttachmentPath2());
        if (entity.getAttachmentPath3() != null) original.setAttachmentPath3(entity.getAttachmentPath3());
        // 其他欄位如有需要可依此類推
        return ResponseEntity.ok(service.update(id, original));
    }

    @GetMapping("/application-status/{userID}")
    public ResponseEntity<List<ApplicationSummaryDTO>> getSummaryByUserID(@PathVariable UUID userID) {
        return ResponseEntity.ok(service.getSummaryByUserID(userID));
    }

    // New endpoint to expose JDBC offset API
    @GetMapping("/offset")
    public ResponseEntity<Object> getWithOffset(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int size) {
        // basic validation and sanitization
        if (offset < 0) {
            return ResponseEntity.badRequest().build();
        }
        if (size <= 0) {
            return ResponseEntity.badRequest().build();
        }
        // cap size to prevent abuse
        final int MAX_SIZE = 100;
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }

        // Fetch content and total count
        List<ApplicationSummaryWithDetailsDTO> content = service.getSummariesWithOffset(offset, size);
        long totalElements = service.getTotalApplicationsCount(); // Assume this method exists in the service
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = offset + size < totalElements;

        // Build response with corrected field placement
        Map<String, Object> response = Map.of(
                "totalPages", totalPages,
                "hasNext", hasNext,
                "offset", offset,
                "size", size,
                "content", content,
                "totalElements", totalElements
        );

        return ResponseEntity.ok(response);
    }


    /**
     * 給查詢卡片使用的API
     * */
    @GetMapping("/search")
    public ResponseEntity<List<ApplicationSummaryWithDetailsDTO>> searchApplications(
            @RequestParam(required = false) String institutionID,
            @RequestParam(required = false) String institutionName,
            @RequestParam(required = false) String CaseNumber,
            @RequestParam(required = false) String NationalID
    ) {
        List<ApplicationSummaryWithDetailsDTO> result = service.searchApplications(institutionID, institutionName, CaseNumber, NationalID);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/revoke_search")
    public ResponseEntity<List<ApplicationSummaryWithDetailsDTO>> searchRevokeApplications(
            @RequestParam(required = false) String institutionID,
            @RequestParam(required = false) String institutionName,
            @RequestParam(required = false) String CaseNumber,
            @RequestParam(required = false) String NationalID
    ) {
        List<ApplicationSummaryWithDetailsDTO> result = service.revokesearchApplications(institutionID, institutionName, CaseNumber, NationalID);
        return ResponseEntity.ok(result);
    }

    /**
     * 根據 ParticipantID 查詢案件詳情
     *
     * 功能說明：
     * 使用 ParticipantID（參與者ID）直接查詢特定幼兒在某案件中的詳細信息
     * 此方法精確可靠，可以區分同一幼兒的多筆申請
     *
     * 端點: GET /applications/case?participantID=xxx
     *
     * 參數說明:
     * - participantID: 參與者ID（UUID 格式，必需）
     *
     * 功能流程:
     * 1. 根據 ParticipantID 查詢應用程序參與者信息
     * 2. 取得關聯的 ApplicationID
     * 3. 自動讀取該應用的所有附件文件
     * 4. 查詢該案件的所有參與者（家長和幼兒）
     * 5. 申請人資料從 users 表取得
     *
     * 回傳值:
     * - 200 OK - 返回 CaseEditUpdateDTO 包含案件全部信息和所有參與者
     * - 400 Bad Request - 缺少或無效的 participantID 參數
     * - 404 Not Found - 找不到該 participantID 對應的案件
     * - 500 Internal Server Error - 伺服器錯誤
     *
     * 成功回應 (200 OK):
     * {
     *   "caseNumber": 1764427242183,
     *   "applyDate": "2025-11-29",
     *   "identityType": 2,
     *   "institutionId": "e09f1689-17a4-46f7-ae95-160a368147af",
     *   "institutionName": "新竹縣東正社區公共托育家園",
     *   "selectedClass": "小班",
     *   "currentOrder": 1,
     *   "reviewDate": null,
     *   "applicationID": "4286bfa6-fcfd-40d4-afb2-2c16e4dd5eec",
     *   "participantID": "550e8400-e29b-41d4-a716-446655440001",
     *   "user": {
     *     "userID": "4B051688-5751-45EB-A63E-CF6ADE991332",
     *     "name": "李小寶",
     *     "gender": "M",
     *     "nationalID": "E567890123",
     *     "birthDate": "2021-03-15",
     *     "mailingAddress": "台北市大安區仁愛路200號",
     *     "email": null,
     *     "phoneNumber": null
     *   },
     *   "parents": [
     *     {
     *       "participantID": "550e8400-e29b-41d4-a716-446655440002",
     *       "participantType": "家長",
     *       "nationalID": "C345678901",
     *       "name": "李美玲",
     *       "gender": "女",
     *       "relationShip": "母親",
     *       "occupation": "教師",
     *       "phoneNumber": "0934567890",
     *       "householdAddress": "台北市大安區仁愛路200號",
     *       "mailingAddress": "台北市大安區仁愛路200號",
     *       "email": "li@parent.com",
     *       "birthDate": "1990-05-10",
     *       "isSuspended": false,
     *       "suspendEnd": null,
     *       "currentOrder": null,
     *       "status": null,
     *       "reason": null,
     *       "classID": null,
     *       "reviewDate": null
     *     }
     *   ],
     *   "children": [
     *     {
     *       "participantID": "550e8400-e29b-41d4-a716-446655440001",
     *       "participantType": "幼兒",
     *       "nationalID": "E567890123",
     *       "name": "李小寶",
     *       "gender": "男",
     *       "relationShip": null,
     *       "occupation": null,
     *       "phoneNumber": null,
     *       "householdAddress": "台北市大安區仁愛路200號",
     *       "mailingAddress": "台北市大安區仁愛路200號",
     *       "email": null,
     *       "birthDate": "2021-03-15",
     *       "isSuspended": false,
     *       "suspendEnd": null,
     *       "currentOrder": 1,
     *       "status": "審核中",
     *       "reason": null,
     *       "classID": "3A384085-F1A5-4DAC-901A-B8EA1A4A9B72",
     *       "reviewDate": null
     *     }
     *   ],
     *   "attachmentPath": "450b4b86-e5aa-4acd-92de-28d43811fe62_螢幕擷取畫面.png",
     *   "attachmentPath1": null,
     *   "attachmentPath2": null,
     *   "attachmentPath3": null
     * }
     *
     * 使用範例:
     * GET /applications/case?participantID=550e8400-e29b-41d4-a716-446655440001
     */
    @GetMapping("/case")
    public ResponseEntity<?> getCaseByParticipantId(@RequestParam(required = true) UUID participantID) {
        if (participantID == null) {
            return ResponseEntity.badRequest().body("Missing or invalid participantID parameter");
        }

        try {
            Optional<CaseEditUpdateDTO> result = service.getCaseByParticipantId(participantID);
            return result.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Error retrieving case: " + ex.getMessage());
        }
    }

    /**
     * 提交新的申請案件（包含案件資訊和附件檔案）
     *
     * 功能說明：
     * 1. 接收 CaseEditUpdateDTO 格式的申請資料（JSON）
     * 2. 支持上傳最多 4 個附件檔案
     * 3. 建立案件資訊並將檔案儲存到 IdentityResource/{applicationID}/ 目錄
     * 4. 驗證每個幼兒的 nationalID 總案件數不得超過 2 件
     * 5. 返回建立成功的完整案件資訊
     *
     * RequestParam 說明：
     *  - file (可選): 第一個附件檔案
     *  - file1 (可選): 第二個附件檔案
     *  - file2 (可選): 第三個附件檔案
     *  - file3 (可選): 第四個附件檔案
     *
     * RequestBody (CaseEditUpdateDTO):
     *  - caseNumber, applyDate, identityType, institutionId, institutionName: 案件基本資訊
     *  - selectedClass, currentOrder: 班級和序號資訊
     *  - User: 申請人信息（UserSimpleDTO）
     *  - parents: 家長列表
     *  - children: 幼兒列表
     *  - attachmentPath, attachmentPath1, attachmentPath2, attachmentPath3: 附件路徑（由系統設定）
     *
     * 驗證規則：
     *  - 每個幼兒的身分證字號（nationalID）在系統中的總申請案件數不得超過 2 件
     *  - 若超過限制，將返回 400 Bad Request 並說明錯誤訊息
     *
     * 回傳值：
     *  - 200 OK + 完整的 CaseEditUpdateDTO（包含自動設置的 applicationID、attachmentPath 等）
     *  - 400 Bad Request + 錯誤訊息（當幼兒申請案件數超過限制時）
     *  - 500 Internal Server Error + 錯誤訊息（當發生系統錯誤時）
     *
     * 使用範例：
     * POST /applications/case/submit
     * Content-Type: multipart/form-data
     *
     * 參數：
     {
     "caseNumber": 1,
     "applyDate": "2025-11-27",
     "identityType": 1,
     "institutionId": "550e8400-e29b-41d4-a716-446655440000",
     "institutionName": "逢甲幼兒園",
     "selectedClass": "CLASS001",
     "currentOrder": 1,
     "User": {
     "UserID": "550e8400-e29b-41d4-a716-446655440001",
     "Name": "王小明",
     "Gender": "M",
     "BirthDate": "1990-01-15",
     "MailingAddress": "台中市西屯區豐樂路123號",
     "email": "wang@example.com",
     "PhoneNumber": "0912345678",
     "NationalID": "A123456789"
     },
     "parents": [
     {
     "participantType": "PARENT",
     "nationalID": "A123456789",
     "name": "王小明",
     "gender": "M",
     "relationShip": "父親",
     "occupation": "工程師",
     "phoneNumber": "0912345678",
     "householdAddress": "台中市西屯區豐樂路123號",
     "mailingAddress": "台中市西屯區豐樂路123號",
     "email": "wang@example.com",
     "birthDate": "1990-01-15",
     "isSuspended": false,
     "suspendEnd": null,
     "currentOrder": 1,
     "status": "pending",
     "reason": null,
     "classID": null
     },
     {
     "participantType": "PARENT",
     "nationalID": "B987654321",
     "name": "王美美",
     "gender": "F",
     "relationShip": "母親",
     "occupation": "護理師",
     "phoneNumber": "0987654321",
     "householdAddress": "台中市西屯區豐樂路123號",
     "mailingAddress": "台中市西屯區豐樂路123號",
     "email": "wang.meimei@example.com",
     "birthDate": "1992-03-20",
     "isSuspended": false,
     "suspendEnd": null,
     "currentOrder": 2,
     "status": "pending",
     "reason": null,
     "classID": null
     }
     ],
     "children": [
     {
     "participantType": "CHILD",
     "nationalID": "C987654321",
     "name": "王小美",
     "gender": "F",
     "relationShip": "女兒",
     "occupation": null,
     "phoneNumber": null,
     "householdAddress": "台中市西屯區豐樂路123號",
     "mailingAddress": "台中市西屯區豐樂路123號",
     "email": null,
     "birthDate": "2021-06-10",
     "isSuspended": false,
     "suspendEnd": null,
     "currentOrder": 1,
     "status": "pending",
     "reason": null,
     "classID": "CLASS001"
     }
     ],
     "attachmentPath": null,
     "attachmentPath1": null,
     "attachmentPath2": null,
     "attachmentPath3": null
     }


     * */
    @PostMapping("/case/submit")
    public ResponseEntity<?> submitApplicationCase(
            @RequestPart(value = "caseDto") CaseEditUpdateDTO caseDto,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "file1", required = false) MultipartFile file1,
            @RequestPart(value = "file2", required = false) MultipartFile file2,
            @RequestPart(value = "file3", required = false) MultipartFile file3) {

        if (caseDto == null) {
            return ResponseEntity.badRequest().body("Missing or invalid caseDto parameter");
        }

        try {
            Applications newApplication = new Applications();

            // 🔍 Debug：檢查 Controller 收到的 DTO 與 userID 映射
            System.out.println("=== submitApplicationCase DEBUG START ===");
            System.out.println("Raw caseDto = " + caseDto);
            if (caseDto.getUser() == null) {
                System.out.println("caseDto.getUser() = null");
            } else {
                System.out.println("caseDto.getUser() = " + caseDto.getUser());
                try {
                    System.out.println("caseDto.getUser().getUserID() = " + caseDto.getUser().getUserID());
                } catch (Exception e) {
                    System.out.println("Error reading caseDto.getUser().getUserID(): " + e.getMessage());
                }
            }

            // 生成唯一的 Application ID
            UUID applicationId = UUID.randomUUID();
            newApplication.setApplicationID(applicationId);

            // 從 caseDto 中設置必要的資訊
            newApplication.setApplicationDate(caseDto.getApplyDate() != null ?
                    caseDto.getApplyDate() : java.time.LocalDate.now());

            // 如果前端沒有傳入 caseNumber，則自動生成
            // 格式：YYYYMMDD + 4位流水號，如 202412040001
            if (caseDto.getCaseNumber() == null) {
                Long generatedCaseNumber = service.generateCaseNumber();
                newApplication.setCaseNumber(generatedCaseNumber);
                System.out.println("Generated CaseNumber: " + generatedCaseNumber);
            } else {
                newApplication.setCaseNumber(caseDto.getCaseNumber());
            }

            newApplication.setInstitutionID(caseDto.getInstitutionId());
            newApplication.setIdentityType(caseDto.getIdentityType() != null ?
                    caseDto.getIdentityType().byteValue() : (byte)0);

            // 如果有 User 資訊，設定 UserID
            if (caseDto.getUser() != null) {
                try {
                    String userIdStr = caseDto.getUser().getUserID();
                    System.out.println("Raw userIdStr from DTO = " + userIdStr);
                    if (userIdStr != null && !userIdStr.trim().isEmpty()) {
                        UUID userId = UUID.fromString(userIdStr.trim());
                        newApplication.setUserID(userId);
                    } else {
                        System.out.println("userIdStr is null or empty");
                    }
                } catch (IllegalArgumentException ex) {
                    System.err.println("Failed to parse UserID: " + ex.getMessage());
                }
            } else {
                System.out.println("caseDto.getUser() is null, skip mapping userID");
            }

            System.out.println("newApplication.getUserID() AFTER mapping = " + newApplication.getUserID());
            System.out.println("=== submitApplicationCase DEBUG END ===");

            // 先儲存 Application（此時還沒有 attachmentPath 資訊）
            try {
                System.out.println("🔵 BEFORE service.create() - Application data:");
                System.out.println("  ApplicationID: " + newApplication.getApplicationID());
                System.out.println("  CaseNumber: " + newApplication.getCaseNumber());
                System.out.println("  ApplicationDate: " + newApplication.getApplicationDate());
                System.out.println("  InstitutionID: " + newApplication.getInstitutionID());
                System.out.println("  UserID: " + newApplication.getUserID());
                System.out.println("  IdentityType: " + newApplication.getIdentityType());

                newApplication = service.create(newApplication);

                System.out.println("✅ SUCCESS: Application saved to database!");
                System.out.println("  Saved ApplicationID: " + newApplication.getApplicationID());
            } catch (Exception ex) {
                System.err.println("❌ FAILED to create Application: " + ex.getMessage());
                ex.printStackTrace();
                return ResponseEntity.status(500).body("Failed to create application: " + ex.getMessage());
            }

            // 設置 Application ID 到 caseDto
            caseDto.setApplicationID(applicationId);

            // 儲存檔案（不再由 FileService 額外建立資料夾，若需要會在 getFolderPath/createDirectories 自動建立）
            List<MultipartFile> files = new ArrayList<>();
            if (file != null && !file.isEmpty()) files.add(file);
            if (file1 != null && !file1.isEmpty()) files.add(file1);
            if (file2 != null && !file2.isEmpty()) files.add(file2);
            if (file3 != null && !file3.isEmpty()) files.add(file3);

            for (int i = 0; i < files.size(); i++) {
                MultipartFile uploadedFile = files.get(i);
                try {
                    String originalFileName = uploadedFile.getOriginalFilename();
                    if (originalFileName == null || originalFileName.trim().isEmpty()) {
                        originalFileName = "attachment";
                    }
                    String fileName = UUID.randomUUID() + "_" + originalFileName;
                    Path filePath = fileService.getFolderPath(applicationId).resolve(fileName);

                    // 直接寫檔，不再自動建立父目錄；若目錄不存在將拋出錯誤，方便你自行管理資料夾結構
                    Files.copy(uploadedFile.getInputStream(), filePath);

                    // 設置對應的 attachmentPath 到 DTO 與 Entity，之後會一起寫入 DB
                    switch (i) {
                        case 0:
                            caseDto.setAttachmentPath(fileName);
                            newApplication.setAttachmentPath(fileName);
                            break;
                        case 1:
                            caseDto.setAttachmentPath1(fileName);
                            newApplication.setAttachmentPath1(fileName);
                            break;
                        case 2:
                            caseDto.setAttachmentPath2(fileName);
                            newApplication.setAttachmentPath2(fileName);
                            break;
                        case 3:
                            caseDto.setAttachmentPath3(fileName);
                            newApplication.setAttachmentPath3(fileName);
                            break;
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to save file " + i + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Failed to save file " + i + ": " + ex.getMessage());
                }
            }

            // 如果有任何附件路徑被設定，更新一次 Application 以寫入 DB
            if (newApplication.getAttachmentPath() != null ||
                    newApplication.getAttachmentPath1() != null ||
                    newApplication.getAttachmentPath2() != null ||
                    newApplication.getAttachmentPath3() != null) {
                try {
                    System.out.println("🔵 Updating Application with attachment paths:");
                    System.out.println("  AttachmentPath: " + newApplication.getAttachmentPath());
                    System.out.println("  AttachmentPath1: " + newApplication.getAttachmentPath1());
                    System.out.println("  AttachmentPath2: " + newApplication.getAttachmentPath2());
                    System.out.println("  AttachmentPath3: " + newApplication.getAttachmentPath3());

                    service.update(applicationId, newApplication); // 使用 update 而不是 create
                    System.out.println("✅ SUCCESS: Attachment paths updated!");
                } catch (Exception ex) {
                    System.err.println("❌ FAILED to update Application attachments: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }

            // 假設此處已經完成 newApplication = service.create(newApplication);
            // 並且已經取得 applicationId 並設到 caseDto.setApplicationID(applicationId);

            // === 新增：建立 application_participants 資料（家長 + 幼兒） ===
            // 這裡不要重新宣告 applicationId，直接使用前面建立的變數

            // 建立通用方法把 DTO 轉成 Entity 並存檔
            java.util.function.BiConsumer<ApplicationParticipantDTO, Boolean> saveParticipant = (dto, isParent) -> {
                if (dto == null) return;

                System.out.println("🔵 Creating participant: " + (isParent ? "PARENT" : "CHILD"));
                System.out.println("  Name: " + dto.name);
                System.out.println("  NationalID: " + dto.nationalID);

                ApplicationParticipants participant = new ApplicationParticipants();
                participant.setParticipantID(java.util.UUID.randomUUID());
                participant.setApplicationID(applicationId);
                participant.setParticipantType(isParent); // true = 家長, false = 幼兒
                participant.setNationalID(dto.nationalID);
                participant.setName(dto.name);

                // gender: 依你現有慣例，"男"/"M" 視為 true，其餘視為 false
                if (dto.gender != null) {
                    String g = dto.gender.trim();
                    boolean genderBool = "男".equals(g) || "M".equalsIgnoreCase(g) || "1".equals(g);
                    participant.setGender(genderBool);
                } else {
                    participant.setGender(null);
                }

                participant.setRelationShip(dto.relationShip);
                participant.setOccupation(dto.occupation);
                participant.setPhoneNumber(dto.phoneNumber);
                participant.setHouseholdAddress(dto.householdAddress);
                participant.setMailingAddress(dto.mailingAddress);
                participant.setEmail(dto.email);

                // 生日與停權結束日字串轉 LocalDate（格式預期為 yyyy-MM-dd）
                try {
                    if (dto.birthDate != null && !dto.birthDate.isEmpty()) {
                        participant.setBirthDate(java.time.LocalDate.parse(dto.birthDate));
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse birthDate for participant: " + dto.birthDate + ", " + e.getMessage());
                }
                participant.setIsSuspended(dto.isSuspended);
                try {
                    if (dto.suspendEnd != null && !dto.suspendEnd.isEmpty()) {
                        participant.setSuspendEnd(java.time.LocalDate.parse(dto.suspendEnd));
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse suspendEnd for participant: " + dto.suspendEnd + ", " + e.getMessage());
                }

                // 直接使用 DTO 傳入的 CurrentOrder 值（不自動分配）
                participant.setCurrentOrder(dto.currentOrder);

                participant.setStatus(dto.status);
                participant.setReason(dto.reason);

                // classID 轉 UUID
                try {
                    if (dto.classID != null && !dto.classID.isEmpty()) {
                        participant.setClassID(java.util.UUID.fromString(dto.classID));
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse classID for participant: " + dto.classID + ", " + e.getMessage());
                }

                // reviewDate 直接帶入（DTO 已是 LocalDateTime）
                participant.setReviewDate(dto.reviewDate);

                try {
                    System.out.println("  🔵 Saving participant to database...");
                    System.out.println("    ParticipantID: " + participant.getParticipantID());
                    System.out.println("    ApplicationID: " + participant.getApplicationID());
                    System.out.println("    ParticipantType: " + participant.getParticipantType());
                    System.out.println("    Status: " + participant.getStatus());

                    applicationParticipantsService.create(participant);

                    System.out.println("  ✅ SUCCESS: Participant saved!");
                } catch (Exception ex) {
                    System.err.println("  ❌ FAILED to save ApplicationParticipant: " + ex.getMessage());
                    ex.printStackTrace();
                }
            };

            // 先存家長（parents）
            System.out.println("🔵 Starting to save PARENTS...");
            if (caseDto.getParents() != null) {
                System.out.println("  Total parents to save: " + caseDto.getParents().size());
                for (ApplicationParticipantDTO parentDto : caseDto.getParents()) {
                    saveParticipant.accept(parentDto, true);
                }
            } else {
                System.out.println("  No parents to save (null)");
            }

            // 再存幼兒（children）
            System.out.println("🔵 Starting to save CHILDREN...");
            if (caseDto.getChildren() != null) {
                System.out.println("  Total children to save: " + caseDto.getChildren().size());

                // 🔍 檢查每個幼兒的 nationalID 總案件數是否超過 2 件
                for (ApplicationParticipantDTO childDto : caseDto.getChildren()) {
                    if (childDto.nationalID != null && !childDto.nationalID.trim().isEmpty()) {
                        int existingCount = applicationParticipantsService.countApplicationsByChildNationalID(childDto.nationalID);
                        System.out.println("  🔍 幼兒 " + childDto.name + " (身分證: " + childDto.nationalID + ") 目前已有 " + existingCount + " 件申請");

                        if (existingCount >= 2) {
                            String errorMsg = "幼兒 " + childDto.name + " (身分證: " + childDto.nationalID + ") 的申請案件已達上限 2 件，無法再提交新申請";
                            System.err.println("  ❌ " + errorMsg);
                            return ResponseEntity.status(400).body(errorMsg);
                        }
                    }
                }

                for (ApplicationParticipantDTO childDto : caseDto.getChildren()) {
                    saveParticipant.accept(childDto, false);
                }
            } else {
                System.out.println("  No children to save (null)");
            }

            System.out.println("✅ All participants saved successfully!");
            // === 建立 participants 完成 ===

            return ResponseEntity.ok(caseDto);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body("Error submitting application case: " + ex.getMessage());
        }
    }

    /**
     * 申請審核 reviewEdit.vue 畫面使用的案件明細 API
     *
     * <p>用途：</p>
     * <ul>
     *   <li>依 ApplicationID 取得單一案件的完整資訊</li>
     *   <li>包含：案件基本資料、家長/幼兒參與者清單、附件檔案欄位</li>
     * </ul>
     *
     * <p>HTTP Method / 路徑：</p>
     * <pre>
     *   GET /applications/{id}
     * </pre>
     *
     * <p>Path Variable：</p>
     * <ul>
     *   <li><b>id</b> (UUID)：applications.ApplicationID</li>
     * </ul>
     *
     * <p>Query Parameter（可選）：</p>
     * <ul>
     *   <li><b>NationalID</b> (String)：
     *     若提供，則 children 清單只會回傳該幼兒（parents 仍回傳全部家長）。
     *   </li>
     * </ul>
     *
     * <p>回傳型別：</p>
     * <pre>
     *   200 OK  -> ApplicationCaseDTO JSON
     *   404 Not Found -> 找不到指定 ApplicationID 的案件
     * </pre>
     *
     * <p>ApplicationCaseDTO 結構重點：</p>
     * <ul>
     *   <li><b>applicationId</b> (UUID)：案件 ID (applications.ApplicationID)</li>
     *   <li><b>applicationDate</b> (LocalDate)：申請日期 (applications.ApplicationDate)</li>
     *   <li><b>institutionName</b> (String)：機構名稱 (institutions.InstitutionName)</li>
     *   <li><b>attachmentPath</b> ~ <b>attachmentPath3</b> (String)：
     *     對應 applications.AttachmentPath ~ AttachmentPath3 的檔名（不含路徑）。
     *   </li>
     *   <li><b>parents</b> (ApplicationParticipantDTO[])：家長清單（ParticipantType=家長）</li>
     *   <li><b>children</b> (ApplicationParticipantDTO[])：幼兒清單（ParticipantType=幼兒）
     *     - 若有提供 NationalID query，children 只包含該幼兒
     *   </li>
     * </ul>
     *
     * <p>附件檔案實際 URL 組合方式（搭配 WebConfig）：</p>
     * <ul>
     *   <li>WebConfig 將實體資料夾 <code>IdentityResource</code> 映射為 <code>/identity-files/**</code></li>
     *   <li>若檔案實際存放於：<code>{projectRoot}/IdentityResource/{檔名}</code></li>
     *   <li>前端可用下列方式組 URL：</li>
     * </ul>
     * <pre>
     *   // 範例：DTO 回傳
     *   {
     *     "applicationId": "4286bfa6-fcfd-40d4-afb2-2c16e4dd5eec",
     *     "attachmentPath": "a_file_1.jpg"
     *   }
     *
     *   // 對應可存取 URL
     *   http://localhost:8080/identity-files/a_file_1.jpg
     * </pre>
     *
     * <p>使用範例：</p>
     * <pre>
     *   // 取得整個案件（所有家長與幼兒）
     *   GET /applications/4286bfa6-fcfd-40d4-afb2-2c16e4dd5eec
     *
     *   // 只關注某一幼兒（例如身分證 E567890123），children 陣列只回傳該幼兒
     *   GET /applications/4286bfa6-fcfd-40d4-afb2-2c16e4dd5eec?NationalID=E567890123
     * </pre>
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getApplicationById(@PathVariable UUID id,
                                                @RequestParam(required = false, value = "NationalID") String nationalID) {
        Optional<ApplicationCaseDTO> opt = service.getApplicationByIdJdbc(id, nationalID);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 更新單一申請（包含參與者資料與審核欄位）
     * 快速審核單個參與者的狀態
     * 使用方式分為兩種：
     * 1. 更新單個參與者：提供 NationalID 參數（只更新該參與者的 status、reason、reviewDate）
     * 2. 批量更新：不提供 NationalID，直接傳遞 ApplicationCaseDTO JSON body
     *
     * RequestParam 說明：
     *  - id (PathVariable): 申請編號 (必填)
     *  - status (必填): 參與者狀態。若 JSON body 中的參與者沒有 status，則使用此參數補上
     *  - reason: 拒絕原因 (可為 null)
     *  - reviewDate: 審核日期 (會被忽略，後端直接設為當下時間)
     *  - NationalID: 國民ID (可選)
     *    - 若有提供：只更新該身分證號碼的參與者的 status、reason、reviewDate
     *    - 若無提供：批量更新所有參與者（包含 parents 和 children）
     *
     * JSON body (ApplicationCaseDTO):
     *  - parents: 家長列表 (批量更新時使用)
     *  - children: 幼兒列表 (批量更新時使用)
     *  - reason: 拒絕原因 (可為 null)
     *  - applicationId, applicationDate, institutionName: 只讀欄位 (會被查詢時覆蓋)
     *
     * CurrentOrder 自動管理機制（僅針對幼兒 ParticipantType=0）：
     *  - 當狀態改為「候補中」：
     *    1. 自動查詢同機構的最大 CurrentOrder 值
     *    2. 設置為 maxOrder + 1（若無記錄則設為 1）
     *  - 當狀態從「候補中」改為其他狀態（如「已錄取」）：
     *    1. 將該幼兒的 CurrentOrder 設為 null
     *    2. 自動遞補：將同機構所有 CurrentOrder > 當前值的幼兒順序減 1
     *  - 注意：家長（ParticipantType=1）不會設置 CurrentOrder
     *
     * 回傳值：
     *  - 若提供 NationalID：回傳 ApplicationCaseDTO (只包含該參與者)，其中 parents 包含所有家長，children 只包含指定身分證的幼兒
     *  - 若未提供 NationalID：回傳 HTTP 204 No Content
     *
     * 範例使用：
     *  1. 單一幼兒審核為候補：PUT /applications/{id}/case?NationalID=A123456789&status=候補中
     *     → 系統自動分配 CurrentOrder（如 5）
     *  2. 將候補中的幼兒改為已錄取：PUT /applications/{id}/case?NationalID=A123456789&status=已錄取
     *     → 系統自動將後面的候補（CurrentOrder 6,7,8...）遞補為 5,6,7...
     * */
    @PutMapping("/{id}/case")
    public ResponseEntity<?> updateApplicationCase(
            @PathVariable UUID id,
            @RequestBody(required = false) ApplicationCaseDTO dto,
            @RequestParam(required = false, value = "reviewDate") String reviewDateParam,
            @RequestParam(required = false, value = "reason") String reason,
            @RequestParam(required = false, value = "status") String statusParam,
            @RequestParam(required = false, value = "NationalID") String nationalID) {
        // Basic validation
        if (id == null) return ResponseEntity.badRequest().body("Missing application id");

        try {
            // If NationalID provided, we treat this as "update single participant's status/reason"
            if (nationalID != null && !nationalID.isEmpty()) {
                if (statusParam == null || statusParam.isEmpty()) {
                    return ResponseEntity.badRequest().body("Missing required parameter: status (provide as query param when NationalID is used)");
                }
                // reviewDate: server sets to now
                java.time.LocalDateTime now = java.time.LocalDateTime.now();

                // ✅ 新增：調用更新狀態並發送郵件的方法
                service.updateStatusAndSendEmail(id, nationalID, statusParam, reason, now);

                // return updated single application DTO (filter by nationalID so children contains only that child)
                Optional<ApplicationCaseDTO> opt = service.getApplicationByIdJdbc(id, nationalID);
                return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
            }

            // Otherwise handle full update path (existing behavior)
            if (dto == null) dto = new ApplicationCaseDTO();

            // For update: statusParam is required (used to fill participant status if missing)
            String finalStatus = null;
            if (statusParam != null && !statusParam.isEmpty()) finalStatus = statusParam;

            if (finalStatus == null) {
                return ResponseEntity.badRequest().body("Missing required parameter: status (provide as query param)");
            }

            // set server-side reviewDate to now (ignore any incoming reviewDateParam)
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            // reference reviewDateParam to avoid unused-parameter warning
            if (reviewDateParam != null) {
                try { java.time.LocalDateTime.parse(reviewDateParam); } catch (Exception ex) { /* ignored */ }
            }

            // reason: prefer request param if provided, otherwise keep body.reason
            if (reason != null) dto.reason = reason;

            // Set reviewDate for participants and fill missing status with provided status
            if (dto.parents != null) {
                for (Group4.Childcare.DTO.ApplicationParticipantDTO p : dto.parents) {
                    if (p != null) {
                        p.reviewDate = now;
                        if (p.status == null || p.status.isEmpty()) {
                            p.status = finalStatus;
                        }
                    }
                }
            }
            if (dto.children != null) {
                for (Group4.Childcare.DTO.ApplicationParticipantDTO p : dto.children) {
                    if (p != null) {
                        p.reviewDate = now;
                        if (p.status == null || p.status.isEmpty()) {
                            p.status = finalStatus;
                        }
                    }
                }
            }

            // Update application case
            service.updateApplicationCase(id, dto);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Failed to update application case: " + ex.getMessage());
        }
    }

    /**
     * AI聊天機器人使用
     * 後台案件搜尋 API
     * 支援多條件查詢：機構、班級、流水案號、申請人身分證、身分別、案件狀態
     *
     * @param searchDto 包含查詢條件的 AdminCaseSearchRequestDto
     * @return 查詢結果列表
     */
    @GetMapping("/admin/search")
    public ResponseEntity<List<Map<String, Object>>> adminSearchCases(@RequestBody AdminCaseSearchRequestDto searchDto) {
        StringBuilder sql = new StringBuilder(
                "SELECT " +
                        "  a.ApplicationID, " +
                        "  a.CaseNumber, " +
                        "  a.ApplicationDate, " +
                        "  a.IdentityType, " +
                        "  i.InstitutionName, " +
                        "  c.ClassName, " +
                        "  ap.NationalID AS ApplicantNationalID, " +
                        "  u.Name AS ApplicantName, " +
                        "  ap.NationalID AS ChildNationalID, " +
                        "  ap.Name AS ChildName, " +
                        "  ap.Status AS CaseStatus, " +
                        "  ap.ReviewDate, " +
                        "  ap.CurrentOrder " +
                        "FROM applications a " +
                        "LEFT JOIN institutions i ON a.InstitutionID = i.InstitutionID " +
                        "LEFT JOIN users u ON a.UserID = u.UserID " +
                        "LEFT JOIN application_participants ap ON a.ApplicationID = ap.ApplicationID " +
                        "LEFT JOIN classes c ON ap.ClassID = c.ClassID " +
                        "WHERE ap.ParticipantType = 0 "  // 只查詢幼兒記錄
        );

        List<Object> params = new ArrayList<>();

        // 機構篩選 (applications.InstitutionID)
        if (searchDto.getInstitutionId() != null) {
            sql.append("AND a.InstitutionID = ? ");
            params.add(searchDto.getInstitutionId().toString());
        }

        // 班級篩選 (application_participants.ClassID)
        if (searchDto.getClassId() != null) {
            sql.append("AND ap.ClassID = ? ");
            params.add(searchDto.getClassId().toString());
        }

        // 流水案號篩選 (applications.CaseNumber)
        if (searchDto.getCaseNumber() != null) {
            sql.append("AND a.CaseNumber = ? ");
            params.add(searchDto.getCaseNumber());
        }

        // 幼兒身分證字號 (application_participants.NationalID)
        if (searchDto.getApplicantNationalId() != null && !searchDto.getApplicantNationalId().isEmpty()) {
            sql.append("AND ap.NationalID = ? ");
            params.add(searchDto.getApplicantNationalId());
        }

        // 身分別 (applications.IdentityType)
        if (searchDto.getIdentityType() != null && !searchDto.getIdentityType().isEmpty()) {
            sql.append("AND a.IdentityType = ? ");
            params.add(searchDto.getIdentityType());
        }

        // 案件狀態 (application_participants.Status，且 ParticipantType = 0)
        if (searchDto.getCaseStatus() != null && !searchDto.getCaseStatus().isEmpty()) {
            sql.append("AND ap.Status = ? ");
            params.add(searchDto.getCaseStatus());
        }

        sql.append("ORDER BY a.ApplicationDate DESC, a.CaseNumber ASC");

        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * 沒再用的API
     * 後台案件搜尋 API - GET 方式
     * 支援查詢參數形式的搜尋
     */
    @GetMapping("/case/search")
    public ResponseEntity<List<Map<String, Object>>> adminSearchCasesGet(
            @RequestParam(required = false) String institutionId,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) Long caseNumber,
            @RequestParam(required = false) String applicantNationalId,
            @RequestParam(required = false) String identityType,
            @RequestParam(required = false) String caseStatus) {

        AdminCaseSearchRequestDto dto = new AdminCaseSearchRequestDto();

        if (institutionId != null && !institutionId.isEmpty()) {
            try {
                dto.setInstitutionId(UUID.fromString(institutionId));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(null);
            }
        }

        if (classId != null && !classId.isEmpty()) {
            try {
                dto.setClassId(UUID.fromString(classId));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(null);
            }
        }

        dto.setCaseNumber(caseNumber);
        dto.setApplicantNationalId(applicantNationalId);
        dto.setIdentityType(identityType);
        dto.setCaseStatus(caseStatus);

        return adminSearchCases(dto);
    }

    /**
     * 取得案件列表（分頁）
     *個案管理列表 以及 其查詢卡片使用
     * @param offset 分頁起始位置（預設: 0）
     * @param size 每頁筆數（預設: 10）
     * @param status 審核狀態篩選（可選）
     * @param institutionId 機構ID篩選（可選）
     * @param applicationId 案件ID篩選（可選）
     * @param classId 班級ID篩選（可選）
     * @param childNationalId 申請之幼兒身分證字號篩選（可選）
     * @param caseNumber 案件流水號篩選（可選）
     * @param identityType 身分別篩選（可選）
     * @return 包含分頁資訊和案件列表的回應
     *
     * {
     *     "totalElements": 6,
     *     "content": [
     *         {
     *             "caseNumber": 1764571014066,
     *             "applicationId": "d0e85fa5-56f7-43fa-ba0c-bbd320d50d68",
     *             "applicationDate": "2025-12-01",
     *             "institutionName": "新竹縣東正社區公共托育家園",
     *             "childNationalId": "E567890123",
     *             "childName": "李小寶",
     *             "childBirthDate": "2021-03-15",
     *             "currentOrder": null,
     *             "reviewStatus": "審核中",
     *             "className": null,
     *             "applicantNationalName": "李美玲",
     *             "applicantNationalId": "C345678901",
     *             "identityType": "1",
     *             "caseStatus": "審核中"
     *         },
     *         {
     *             "caseNumber": 1764571012981,
     *             "applicationId": "33bf0cbf-e2e7-4d63-9ff9-9166c5e446be",
     *             "applicationDate": "2025-12-01",
     *             "institutionName": "新竹縣東正社區公共托育家園",
     *             "childNationalId": "E567890123",
     *             "childName": "李小寶",
     *             "childBirthDate": "2021-03-15",
     *             "currentOrder": null,
     *             "reviewStatus": "審核中",
     *             "className": null,
     *             "applicantNationalName": "李美玲",
     *             "applicantNationalId": "C345678901",
     *             "identityType": "1",
     *             "caseStatus": "審核中"
     *         },
     *         {
     *             "caseNumber": 1764427013142,
     *             "applicationId": "1fee23ea-cec6-49b2-9f43-d5fd8ea2ed1f",
     *             "applicationDate": "2025-11-29",
     *             "institutionName": "新竹縣公設民營松柏托嬰中心",
     *             "childNationalId": "E567890123",
     *             "childName": "李小寶",
     *             "childBirthDate": "2021-03-15",
     *             "currentOrder": null,
     *             "reviewStatus": "通過",
     *             "className": null,
     *             "applicantNationalName": "李美玲",
     *             "applicantNationalId": "C345678901",
     *             "identityType": "3",
     *             "caseStatus": "通過"
     *         },
     *         {
     *             "caseNumber": 1764427242183,
     *             "applicationId": "4286bfa6-fcfd-40d4-afb2-2c16e4dd5eec",
     *             "applicationDate": "2025-11-29",
     *             "institutionName": "新竹縣東正社區公共托育家園",
     *             "childNationalId": "E567890123",
     *             "childName": "李小寶",
     *             "childBirthDate": "2021-03-15",
     *             "currentOrder": null,
     *             "reviewStatus": "審核中",
     *             "className": null,
     *             "applicantNationalName": "李美玲",
     *             "applicantNationalId": "C345678901",
     *             "identityType": "2",
     *             "caseStatus": "審核中"
     *         },
     *         {
     *             "caseNumber": 1764427118154,
     *             "applicationId": "f5d3966d-43d6-4f93-990a-a096a4b8cc86",
     *             "applicationDate": "2025-11-29",
     *             "institutionName": "新竹縣東正社區公共托育家園",
     *             "childNationalId": "E567890123",
     *             "childName": "李小寶",
     *             "childBirthDate": "2021-03-15",
     *             "currentOrder": null,
     *             "reviewStatus": "通過",
     *             "className": null,
     *             "applicantNationalName": "李美玲",
     *             "applicantNationalId": "C345678901",
     *             "identityType": "1",
     *             "caseStatus": "通過"
     *         },
     *         {
     *             "caseNumber": 1004,
     *             "applicationId": "112e7e08-136d-4439-82ad-d1f355942af3",
     *             "applicationDate": "2024-04-05",
     *             "institutionName": "新竹縣公設民營嘉豐托嬰中心",
     *             "childNationalId": "H890123456",
     *             "childName": "林小美",
     *             "childBirthDate": "2022-01-05",
     *             "currentOrder": 3,
     *             "reviewStatus": "撤銷申請審核中",
     *             "className": "小班",
     *             "applicantNationalName": "李美玲",
     *             "applicantNationalId": "C345678901",
     *             "identityType": "2",
     *             "caseStatus": "撤銷申請審核中"
     *         },
     *         {
     *             "caseNumber": 1004,
     *             "applicationId": "112e7e08-136d-4439-82ad-d1f355942af3",
     *             "applicationDate": "2024-04-05",
     *             "institutionName": "新竹縣公設民營嘉豐托嬰中心",
     *             "childNationalId": "Q789012345",
     *             "childName": "林小強",
     *             "childBirthDate": "2023-03-12",
     *             "currentOrder": 4,
     *             "reviewStatus": "審核中",
     *             "className": "小班",
     *             "applicantNationalName": "李美玲",
     *             "applicantNationalId": "C345678901",
     *             "identityType": "2",
     *             "caseStatus": "審核中"
     *         }
     *     ],
     *     "hasNext": false,
     *     "size": 10,
     *     "offset": 0,
     *     "totalPages": 1
     * }
     */
    @GetMapping("/cases/list")
    public ResponseEntity<Map<String, Object>> getCasesList(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String institutionId,
            @RequestParam(required = false) String applicationId,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String childNationalId,
            @RequestParam(required = false) Long caseNumber,
            @RequestParam(required = false) String identityType) {

        // 基本驗證
        if (offset < 0) {
            return ResponseEntity.badRequest().build();
        }
        if (size <= 0) {
            return ResponseEntity.badRequest().build();
        }

        // 限制最大頁面大小防止濫用
        final int MAX_SIZE = 100;
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }

        // 轉換 institutionId 參數
        UUID institutionUUID = null;
        if (institutionId != null && !institutionId.isEmpty()) {
            try {
                institutionUUID = UUID.fromString(institutionId);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid institutionId format"));
            }
        }

        // 轉換 applicationId 參數
        UUID applicationUUID = null;
        if (applicationId != null && !applicationId.isEmpty()) {
            try {
                applicationUUID = UUID.fromString(applicationId);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid applicationId format"));
            }
        }

        // 轉換 classId 參數
        UUID classUUID = null;
        if (classId != null && !classId.isEmpty()) {
            try {
                classUUID = UUID.fromString(classId);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid classId format"));
            }
        }

        // 取得案件列表和總筆數
        List<CaseOffsetListDTO> content = service.getCaseListWithOffset(offset, size, status, institutionUUID,
                applicationUUID, classUUID, childNationalId,
                caseNumber, identityType);
        long totalElements = service.countCaseList(status, institutionUUID, applicationUUID, classUUID,
                childNationalId, caseNumber, identityType);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = offset + size < totalElements;

        // 構建回應
        Map<String, Object> response = Map.of(
                "content", content,
                "offset", offset,
                "size", size,
                "totalElements", totalElements,
                "totalPages", totalPages,
                "hasNext", hasNext
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 根據 UserID 取得使用者申請詳細資料
     * 使用 JDBC 查詢 applications、application_participants、cancellation、users 表
     *
     * @param userID 使用者ID
     * @return 包含申請詳細資料的清單
     */
    @GetMapping("/user/{userID}/details")
    public ResponseEntity<List<UserApplicationDetailsDTO>> getUserApplicationDetails(@PathVariable UUID userID) {
        if (userID == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<UserApplicationDetailsDTO> result = service.getUserApplicationDetails(userID);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }


}