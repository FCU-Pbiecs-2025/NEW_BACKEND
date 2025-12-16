package Group4.Childcare.Controller;

import Group4.Childcare.DTO.LotteryRequest;
import Group4.Childcare.DTO.LotteryResult;
import Group4.Childcare.DTO.ManualAdmissionRequest;
import Group4.Childcare.Repository.WaitlistJdbcRepository;
import Group4.Childcare.Service.EmailService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/waitlist")
public class WaitlistController {
    private final WaitlistJdbcRepository waitlistJdbcRepository;
    private final EmailService emailService;

    @Autowired
    public WaitlistController(WaitlistJdbcRepository waitlistJdbcRepository, EmailService emailService) {
        this.waitlistJdbcRepository = waitlistJdbcRepository;
        this.emailService = emailService;
    }

    // 查詢候補名單，可依機構ID與姓名模糊查詢
    @GetMapping("/by-institution")
    public List<Map<String, Object>> getWaitlistByInstitution(
            @RequestParam(required = false) String institutionId,
            @RequestParam(required = false) String name) {
        return waitlistJdbcRepository.findWaitlistByInstitution(institutionId, name);
    }

    /**
     * 抽籤功能（抽籤時期）
     * POST /waitlist/lottery
     */
    @PostMapping("/lottery")
    //@PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<LotteryResult> conductLottery(@RequestBody LotteryRequest request) {
        try {
            UUID institutionId = request.getInstitutionId();

            // 1. 獲取機構總收托人數和目前就讀中人數
            int totalCapacity = waitlistJdbcRepository.getTotalCapacity(institutionId);
            int currentStudents = waitlistJdbcRepository.getCurrentStudentsCount(institutionId);
            int availableSlots = totalCapacity - currentStudents;

            // 檢查是否有空缺名額
            if (availableSlots <= 0) {
                LotteryResult errorResult = new LotteryResult();
                errorResult.setSuccess(false);
                errorResult.setMessage(String.format("目前無空缺名額，無法進行抽籤。總容量=%d，就讀中=%d",
                    totalCapacity, currentStudents));
                return ResponseEntity.badRequest().body(errorResult);
            }

            // 2. 計算法定序位名額（基於總容量的比例）
            int firstPriorityLegalQuota = (int) Math.floor(totalCapacity * 0.2);
            int secondPriorityLegalQuota = (int) Math.floor(totalCapacity * 0.1);
            int thirdPriorityLegalQuota = totalCapacity - firstPriorityLegalQuota - secondPriorityLegalQuota;

            // 3. 統計機構內各序位已錄取人數
            Map<Integer, Integer> acceptedCountByPriority = waitlistJdbcRepository.getAcceptedCountByPriority(institutionId);
            int firstPriorityAcceptedCount = acceptedCountByPriority.get(1);
            int secondPriorityAcceptedCount = acceptedCountByPriority.get(2);
            int thirdPriorityAcceptedCount = acceptedCountByPriority.get(3);

            // 4. 計算實際序位名額（法定名額 - 已錄取人數）
            int firstPriorityQuota = Math.max(0, firstPriorityLegalQuota - firstPriorityAcceptedCount);
            int secondPriorityQuota = Math.max(0, secondPriorityLegalQuota - secondPriorityAcceptedCount);
            int thirdPriorityQuota = Math.max(0, thirdPriorityLegalQuota - thirdPriorityAcceptedCount);

            // 重置所有候補順位（洗牌）- 無論是否有名額都執行
            waitlistJdbcRepository.resetAllWaitlistOrders(institutionId);

            // 4. 獲取所有申請人並按身分別分組
            Map<Integer, List<Map<String, Object>>> applicantsByPriority =
                    waitlistJdbcRepository.getLotteryApplicantsByPriority(institutionId);

            List<Map<String, Object>> priority1 = applicantsByPriority.get(1);
            List<Map<String, Object>> priority2 = applicantsByPriority.get(2);
            List<Map<String, Object>> priority3 = applicantsByPriority.get(3);

            // 獲取班級資訊（包含各班級剩餘空位）
            List<Map<String, Object>> classes = waitlistJdbcRepository.getClassInfo(institutionId);

            // 5. 進行三階段抽籤
            List<Map<String, Object>> acceptedList = new ArrayList<>();
            List<Map<String, Object>> waitlist = new ArrayList<>();
            List<Map<String, Object>> classFullWaitlist = new ArrayList<>(); // 班級已滿無法錄取者

            // === 第一序位抽籤 ===
            List<Map<String, Object>> priority1Selected = new ArrayList<>();
            List<Map<String, Object>> priority1NotSelected = new ArrayList<>();

            if (firstPriorityQuota <= 0) {
                // 第一序位已滿額，所有申請人併入第二序位池
                priority1NotSelected.addAll(priority1);
            } else {
                Collections.shuffle(priority1); // 隨機洗牌
                if (priority1.size() <= firstPriorityQuota) {
                    // Case A: 全部正取
                    priority1Selected.addAll(priority1);
                } else {
                    // Case B: 抽籤決定正取
                    for (int i = 0; i < firstPriorityQuota; i++) {
                        priority1Selected.add(priority1.get(i));
                    }
                    for (int i = firstPriorityQuota; i < priority1.size(); i++) {
                        priority1NotSelected.add(priority1.get(i));
                    }
                }
            }

            // === 第二序位抽籤 ===
            // 第一序位未錄取者加入第二序位池
            priority2.addAll(priority1NotSelected);

            List<Map<String, Object>> priority2Selected = new ArrayList<>();
            List<Map<String, Object>> priority2NotSelected = new ArrayList<>();

            int priority2Available = secondPriorityQuota + (firstPriorityQuota - priority1Selected.size());

            if (priority2Available <= 0) {
                // 第二序位已滿額，所有申請人併入第三序位池
                priority2NotSelected.addAll(priority2);
            } else {
                Collections.shuffle(priority2);
                if (priority2.size() <= priority2Available) {
                    // Case A: 全部正取
                    priority2Selected.addAll(priority2);
                } else {
                    // Case B: 抽籤決定正取
                    for (int i = 0; i < priority2Available; i++) {
                        priority2Selected.add(priority2.get(i));
                    }
                    for (int i = priority2Available; i < priority2.size(); i++) {
                        priority2NotSelected.add(priority2.get(i));
                    }
                }
            }

            // === 第三序位抽籤 ===
            // 第一、二序位未錄取者加入第三序位池
            priority3.addAll(priority2NotSelected);

            List<Map<String, Object>> priority3Selected = new ArrayList<>();
            List<Map<String, Object>> priority3NotSelected = new ArrayList<>();

            int priority3Available = thirdPriorityQuota +
                (firstPriorityQuota - priority1Selected.size()) +
                (secondPriorityQuota - priority2Selected.size());

            if (priority3Available <= 0) {
                // 第三序位已滿額，所有申請人成為備取
                priority3NotSelected.addAll(priority3);
            } else {
                Collections.shuffle(priority3);
                if (priority3.size() <= priority3Available) {
                    // Case A: 全部正取
                    priority3Selected.addAll(priority3);
                } else {
                    // Case B: 抽籤決定正取
                    for (int i = 0; i < priority3Available; i++) {
                        priority3Selected.add(priority3.get(i));
                    }
                    for (int i = priority3Available; i < priority3.size(); i++) {
                        priority3NotSelected.add(priority3.get(i));
                    }
                }
            }

            // 6. 依序位順序進行班級分配（序位 1→2→3）
            int firstAccepted = 0;
            int secondAccepted = 0;
            int thirdAccepted = 0;

            int lotteryOrder = 1; // 記錄抽籤順序

            // 第一序位正取者進行班級分配
            for (Map<String, Object> applicant : priority1Selected) {
                applicant.put("LotteryOrder", lotteryOrder++); // 記錄抽籤順序
                if (assignClassAndAdmit(applicant, classes, "第一序位正取", acceptedList, classFullWaitlist)) {
                    firstAccepted++;
                }
            }

            // 第二序位正取者進行班級分配
            for (Map<String, Object> applicant : priority2Selected) {
                applicant.put("LotteryOrder", lotteryOrder++);
                if (assignClassAndAdmit(applicant, classes, "第二序位正取", acceptedList, classFullWaitlist)) {
                    secondAccepted++;
                }
            }

            // 第三序位正取者進行班級分配
            for (Map<String, Object> applicant : priority3Selected) {
                applicant.put("LotteryOrder", lotteryOrder++);
                if (assignClassAndAdmit(applicant, classes, "第三序位正取", acceptedList, classFullWaitlist)) {
                    thirdAccepted++;
                }
            }

            // 第三序位未抽中者成為備取（記錄抽籤順序）
            for (Map<String, Object> applicant : priority3NotSelected) {
                applicant.put("Status", "候補中");
                applicant.put("ReviewDate", LocalDateTime.now());
                applicant.put("LotteryOrder", lotteryOrder++);
                waitlist.add(applicant);
            }

            // 7. 排序邏輯：只有候補者需要 CurrentOrder
            // 依照抽籤順序排序所有候補者（包含班級已滿和未抽中）
            List<Map<String, Object>> allWaitlist = new ArrayList<>();
            allWaitlist.addAll(classFullWaitlist); // 班級已滿無法錄取者
            allWaitlist.addAll(waitlist); // 未抽中備取者

            // 依照 LotteryOrder 排序（班級已滿者參考當初抽出來的排序）
            allWaitlist.sort((a, b) -> {
                Integer orderA = (Integer) a.get("LotteryOrder");
                Integer orderB = (Integer) b.get("LotteryOrder");
                return orderA.compareTo(orderB);
            });

            // 分配 CurrentOrder（只有候補者需要）
            int currentOrder = 1;
            for (Map<String, Object> applicant : allWaitlist) {
                applicant.put("CurrentOrder", currentOrder++);
            }

            // 已錄取者不需要 CurrentOrder（設為 NULL 或不設定）
            for (Map<String, Object> applicant : acceptedList) {
                applicant.put("CurrentOrder", null); // 已錄取者不需要順序
            }

            // 8. 批量更新資料庫
            List<Map<String, Object>> allUpdates = new ArrayList<>();
            allUpdates.addAll(acceptedList);      // 已錄取者（CurrentOrder = null）
            allUpdates.addAll(allWaitlist);       // 所有候補者（有 CurrentOrder）
            waitlistJdbcRepository.batchUpdateApplicants(allUpdates);

            // 8.5. 非同步發送郵件通知（不會阻塞回應）
            System.out.println("📧 啟動非同步郵件發送流程...");
            sendLotteryNotificationEmails(acceptedList, allWaitlist);
            System.out.println("✅ 抽籤完成，郵件將在背景發送");
            // 9. 返回結果
            LotteryResult result = new LotteryResult();
            result.setSuccess(true);
            result.setMessage(String.format(
                "抽籤完成。總容量=%d，就讀中=%d，剩餘空位=%d。" +
                "法定名額：第一序位=%d（已錄取%d，本次可錄取%d），第二序位=%d（已錄取%d，本次可錄取%d），第三序位=%d（已錄取%d，本次可錄取%d）。" +
                "本次實際錄取=%d",
                totalCapacity, currentStudents, availableSlots,
                firstPriorityLegalQuota, firstPriorityAcceptedCount, firstPriorityQuota,
                secondPriorityLegalQuota, secondPriorityAcceptedCount, secondPriorityQuota,
                thirdPriorityLegalQuota, thirdPriorityAcceptedCount, thirdPriorityQuota,
                acceptedList.size()));
            result.setTotalProcessed(allUpdates.size());
            result.setFirstPriorityAccepted(firstAccepted);
            result.setSecondPriorityAccepted(secondAccepted);
            result.setThirdPriorityAccepted(thirdAccepted);
            result.setWaitlisted(allWaitlist.size());
            result.setAcceptedList(acceptedList);
            result.setWaitlistList(allWaitlist);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            LotteryResult errorResult = new LotteryResult();
            errorResult.setSuccess(false);
            errorResult.setMessage("抽籤失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * 分配班級並錄取（檢查年齡班級限制）
     */
    private boolean assignClassAndAdmit(Map<String, Object> applicant, List<Map<String, Object>> classes,
                                       String priorityType, List<Map<String, Object>> acceptedList,
                                       List<Map<String, Object>> classFullWaitlist) {
        Object birthDateObj = applicant.get("BirthDate");
        if (birthDateObj == null) {
            // 無法確認年齡，無法分配班級，列入候補
            applicant.put("Status", "候補中");
            applicant.put("Reason", priorityType + "-無出生日期");
            applicant.put("ReviewDate", LocalDateTime.now());
            classFullWaitlist.add(applicant);
            return false;
        }

        LocalDate birthDate = null;
        if (birthDateObj instanceof java.sql.Date) {
            birthDate = ((java.sql.Date) birthDateObj).toLocalDate();
        } else if (birthDateObj instanceof LocalDate) {
            birthDate = (LocalDate) birthDateObj;
        }

        // 尋找適合的班級
        UUID classId = waitlistJdbcRepository.findSuitableClass(birthDate, classes);

        if (classId != null && waitlistJdbcRepository.hasClassCapacity(classId)) {
            // 有適合的班級且有空位，錄取
            applicant.put("Status", "已錄取");
            applicant.put("Reason", priorityType);
            applicant.put("ClassID", classId.toString());
            applicant.put("ReviewDate", LocalDateTime.now());
            acceptedList.add(applicant);

            // 更新班級學生數（記憶體中的 classes 也要更新）
            waitlistJdbcRepository.updateClassCurrentStudents(classId, 1);

            // 同步更新記憶體中的班級資訊
            for (Map<String, Object> classInfo : classes) {
                if (classId.toString().equals(classInfo.get("ClassID").toString())) {
                    int current = ((Number) classInfo.get("CurrentStudents")).intValue();
                    classInfo.put("CurrentStudents", current + 1);
                    break;
                }
            }

            return true;
        } else {
            // 班級已滿或無適合班級，列入候補
            String reasonDetail = classId == null ?
                priorityType + "-無適合年齡班級" :
                priorityType + "-班級已滿";
            applicant.put("Status", "候補中");
            applicant.put("Reason", reasonDetail);
            applicant.put("ReviewDate", LocalDateTime.now());
            classFullWaitlist.add(applicant);
            return false;
        }
    }

    /**
     * 非抽籤時期：機構手動錄取
     * POST /waitlist/manual-admit
     */
    @PostMapping("/manual-admit")
    @Transactional
    public ResponseEntity<Map<String, Object>> manualAdmit(@RequestBody ManualAdmissionRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 檢查是否違反順序錄取
            List<Map<String, Object>> applicants = waitlistJdbcRepository.getWaitlistApplicants(
                    request.getApplicationId());

            int targetOrder = -1;
            for (Map<String, Object> applicant : applicants) {
                String nationalId = (String) applicant.get("NationalID");
                if (nationalId.equals(request.getNationalId())) {
                    targetOrder = (Integer) applicant.get("CurrentOrder");
                    break;
                }
            }

            if (targetOrder > 1) {
                // 檢查前面是否還有未錄取的人
                List<Map<String, Object>> violations = waitlistJdbcRepository.checkAdmissionOrderViolation(
                        request.getApplicationId(), targetOrder);

                if (!violations.isEmpty()) {
                    // 記錄違反順序錄取（將詳細資訊記錄到 Status）
                    waitlistJdbcRepository.logSkippedAdmission(
                            request.getApplicationId(),
                            request.getNationalId(),
                            "已錄取（違反順序-跳過" + violations.size() + "位候補）");

                    response.put("warning", "注意：前面還有 " + violations.size() + " 位候補者未錄取");
                    response.put("skippedApplicants", violations);
                }
            }

            // 執行錄取
            boolean success = waitlistJdbcRepository.manualAdmit(
                    request.getApplicationId(),
                    request.getNationalId(),
                    request.getClassId());

            if (success) {
                response.put("success", true);
                response.put("message", "錄取成功");
            } else {
                response.put("success", false);
                response.put("message", "錄取失敗：班級已滿");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "錄取失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 審核通過後自動分配候補順序（非抽籤時期）
     * POST /waitlist/assign-order
     */
    @PostMapping("/assign-order")
    @Transactional
    public ResponseEntity<Map<String, Object>> assignWaitlistOrder(
            @RequestParam UUID institutionId,
            @RequestParam UUID applicationId,
            @RequestParam String nationalId) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 獲取下一個候補順序
            int nextOrder = waitlistJdbcRepository.getNextWaitlistOrder(institutionId);

            // 更新申請人狀態
            waitlistJdbcRepository.updateApplicantOrder(
                    applicationId,
                    nationalId,
                    nextOrder,
                    "錄取候補中",
                    LocalDateTime.now());

            response.put("success", true);
            response.put("message", "已分配候補順序");
            response.put("currentOrder", nextOrder);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "分配順序失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 重置抽籤（每年 7/31 執行）
     * POST /waitlist/reset-lottery
     */
    @PostMapping("/reset-lottery")
    @Transactional
    public ResponseEntity<Map<String, Object>> resetLottery(@RequestParam UUID institutionId) {
        Map<String, Object> response = new HashMap<>();

        try {
            waitlistJdbcRepository.resetAllWaitlistOrders(institutionId);

            response.put("success", true);
            response.put("message", "已重置所有候補順位");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "重置失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 查詢機構的候補統計資訊
     * GET /waitlist/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getWaitlistStatistics(@RequestParam UUID institutionId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 獲取各序位人數
            Map<Integer, List<Map<String, Object>>> applicantsByPriority =
                    waitlistJdbcRepository.getLotteryApplicantsByPriority(institutionId);

            int totalCapacity = waitlistJdbcRepository.getTotalCapacity(institutionId);
            List<Map<String, Object>> classInfo = waitlistJdbcRepository.getClassInfo(institutionId);

            response.put("totalCapacity", totalCapacity);
            response.put("firstPriorityCount", applicantsByPriority.get(1).size());
            response.put("secondPriorityCount", applicantsByPriority.get(2).size());
            response.put("thirdPriorityCount", applicantsByPriority.get(3).size());
            response.put("firstPriorityQuota", (int) Math.floor(totalCapacity * 0.2));
            response.put("secondPriorityQuota", (int) Math.floor(totalCapacity * 0.1));
            response.put("classInfo", classInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "查詢失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 發送抽籤通知郵件給錄取者和候補者
     */
    private void sendLotteryNotificationEmails(List<Map<String, Object>> acceptedList,
                                               List<Map<String, Object>> waitlist) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int successCount = 0;
        int failCount = 0;

        System.out.println("📧 開始發送抽籤通知郵件...");
        System.out.println("  錄取人數: " + acceptedList.size());
        System.out.println("  候補人數: " + waitlist.size());

        // 發送郵件給錄取者
        for (Map<String, Object> applicant : acceptedList) {
            try {
                String email = (String) applicant.get("Email");
                String applicantName = (String) applicant.get("ApplicantName");
                String childName = (String) applicant.get("ChildName");
                String institutionName = (String) applicant.get("InstitutionName");
                Object caseNumberObj = applicant.get("CaseNumber");
                Long caseNumber = caseNumberObj != null ? ((Number) caseNumberObj).longValue() : null;
                String status = (String) applicant.get("Status");
                String reason = (String) applicant.get("Reason");

                // 轉換申請日期
                Object applicationDateObj = applicant.get("ApplicationDate");
                String applicationDate = "";
                if (applicationDateObj instanceof LocalDateTime) {
                    applicationDate = ((LocalDateTime) applicationDateObj).format(dateFormatter);
                } else if (applicationDateObj instanceof java.sql.Timestamp) {
                    applicationDate = ((java.sql.Timestamp) applicationDateObj).toLocalDateTime().format(dateFormatter);
                } else if (applicationDateObj instanceof String) {
                    applicationDate = (String) applicationDateObj;
                }

                if (email != null && !email.isEmpty()) {
                    emailService.sendApplicationStatusChangeEmail(
                        email,
                        applicantName,
                        childName,
                        institutionName,
                        caseNumber,
                        applicationDate,
                        status,
                        null, // 錄取者不需要顯示序號
                        reason
                    );
                    successCount++;
                    System.out.println("  ✅ 已排程發送錄取通知給: " + applicantName + " (" + email + ")");
                } else {
                    System.out.println("  ⚠️ 無法發送郵件給: " + applicantName + " (無 Email)");
                    failCount++;
                }
            } catch (Exception e) {
                System.err.println("  ❌ 處理申請人資料時發生錯誤: " + e.getMessage());
                failCount++;
            }
        }

        // 發送郵件給候補者
        for (Map<String, Object> applicant : waitlist) {
            try {
                String email = (String) applicant.get("Email");
                String applicantName = (String) applicant.get("ApplicantName");
                String childName = (String) applicant.get("ChildName");
                String institutionName = (String) applicant.get("InstitutionName");
                Object caseNumberObj = applicant.get("CaseNumber");
                Long caseNumber = caseNumberObj != null ? ((Number) caseNumberObj).longValue() : null;
                String status = (String) applicant.get("Status");
                Integer currentOrder = (Integer) applicant.get("CurrentOrder");

                // 轉換申請日期
                Object applicationDateObj = applicant.get("ApplicationDate");
                String applicationDate = "";
                if (applicationDateObj instanceof LocalDateTime) {
                    applicationDate = ((LocalDateTime) applicationDateObj).format(dateFormatter);
                } else if (applicationDateObj instanceof java.sql.Timestamp) {
                    applicationDate = ((java.sql.Timestamp) applicationDateObj).toLocalDateTime().format(dateFormatter);
                } else if (applicationDateObj instanceof String) {
                    applicationDate = (String) applicationDateObj;
                }

                if (email != null && !email.isEmpty()) {
                    emailService.sendApplicationStatusChangeEmail(
                        email,
                        applicantName,
                        childName,
                        institutionName,
                        caseNumber,
                        applicationDate,
                        status,
                        currentOrder, // 候補者需要顯示目前序號
                        "抽籤結果：候補名單"
                    );
                    successCount++;
                    System.out.println("  ✅ 已發送候補通知給: " + applicantName + " (序號: " + currentOrder + ", " + email + ")");
                } else {
                    System.out.println("  ⚠️ 無法發送郵件給: " + applicantName + " (無 Email)");
                    failCount++;
                }
            } catch (MessagingException e) {
                System.err.println("  ❌ 發送郵件失敗: " + e.getMessage());
                failCount++;
            } catch (Exception e) {
                System.err.println("  ❌ 處理申請人資料時發生錯誤: " + e.getMessage());
                failCount++;
            }
        }

        System.out.println("📧 郵件發送完成！");
        System.out.println("  成功: " + successCount + " 封");
        System.out.println("  失敗: " + failCount + " 封");
    }
}
