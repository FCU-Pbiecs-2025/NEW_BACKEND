package Group4.Childcare.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Repository
public class WaitlistJdbcRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public WaitlistJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findWaitlistByInstitution(String institutionId, String name) {
        StringBuilder sql = new StringBuilder("SELECT a.[ApplicationID], ap.[Name], ap.[BirthDate], a.[IdentityType], ap.[CurrentOrder] " +
                "FROM [dbo].[applications] a " +
                "LEFT JOIN [dbo].[application_participants] ap ON a.[ApplicationID] = ap.[ApplicationID] " +
                "WHERE ap.[Status] = '候補中' AND ap.[ParticipantType] = 0 ");
        // 動態組合條件
        List<Object> paramsList = new java.util.ArrayList<>();
        if (institutionId != null && !institutionId.trim().isEmpty()) {
            sql.append("AND a.[InstitutionID] = ? ");
            paramsList.add(institutionId);
        }
        if (name != null && !name.trim().isEmpty()) {
            sql.append("AND ap.[Name] LIKE ? ");
            paramsList.add("%" + name + "%");
        }
        sql.append("order by a.InstitutionID , ap.CurrentOrder");
        Object[] params = paramsList.toArray();
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params);
        // 處理 BirthDate 轉換為幾歲幾個月
        for (Map<String, Object> row : results) {
            Object birthDateObj = row.get("BirthDate");
            if (birthDateObj != null) {
                try {
                    LocalDate birthDate;
                    if (birthDateObj instanceof java.sql.Date) {
                        birthDate = ((java.sql.Date) birthDateObj).toLocalDate();
                    } else if (birthDateObj instanceof String) {
                        birthDate = LocalDate.parse((String) birthDateObj, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    } else {
                        row.put("Age", "");
                        continue;
                    }
                    LocalDate now = LocalDate.now();
                    Period period = Period.between(birthDate, now);
                    String age = period.getYears() + "歲" + period.getMonths() + "個月";
                    row.put("Age", age);
                } catch (Exception e) {
                    row.put("Age", "");
                }
            } else {
                row.put("Age", "");
            }
        }
        return results;
    }

    /**
     * 獲取機構的所有待審核通過的候補申請人（非抽籤時期用）
     */
    public List<Map<String, Object>> getWaitlistApplicants(UUID institutionId) {
        String sql = "SELECT ap.ApplicationID, ap.NationalID, ap.Name, ap.BirthDate, " +
                "ap.CurrentOrder, ap.Status, a.IdentityType, ap.ClassID " +
                "FROM application_participants ap " +
                "LEFT JOIN applications a ON ap.ApplicationID = a.ApplicationID " +
                "WHERE a.InstitutionID = ? AND ap.ParticipantType = 0 AND ap.Status = '候補中' " +
                "ORDER BY ap.CurrentOrder ASC";
        return jdbcTemplate.queryForList(sql, institutionId.toString());
    }

    /**
     * 獲取下一個候補順序號
     */
    public int getNextWaitlistOrder(UUID institutionId) {
        String sql = "SELECT ISNULL(MAX(ap.CurrentOrder), 0) + 1 " +
                "FROM application_participants ap " +
                "LEFT JOIN applications a ON ap.ApplicationID = a.ApplicationID " +
                "WHERE a.InstitutionID = ? AND ap.ParticipantType = 0";
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, institutionId.toString());
        return result != null ? result : 1;
    }

    /**
     * 更新申請人的候補順序和狀態
     */
    @Transactional
    public void updateApplicantOrder(UUID applicationId, String nationalId, int newOrder, String status, LocalDateTime reviewDate) {
        String sql = "UPDATE application_participants " +
                "SET CurrentOrder = ?, Status = ?, ReviewDate = ? " +
                "WHERE ApplicationID = ? AND NationalID = ?";
        jdbcTemplate.update(sql, newOrder, status, reviewDate,
                applicationId.toString(), nationalId);
    }

    /**
     * 批量更新申請人狀態和順序
     */
    @Transactional
    public void batchUpdateApplicants(List<Map<String, Object>> applicants) {
        String sql = "UPDATE application_participants " +
                "SET CurrentOrder = ?, Status = ?, Reason = ?, ClassID = ?, ReviewDate = ? " +
                "WHERE ApplicationID = ? AND NationalID = ?";

        List<Object[]> batchArgs = new ArrayList<>();
        for (Map<String, Object> applicant : applicants) {
            Object[] args = new Object[]{
                    applicant.get("CurrentOrder"),
                    applicant.get("Status"),
                    applicant.get("Reason"),
                    applicant.get("ClassID"),
                    applicant.get("ReviewDate"),
                    applicant.get("ApplicationID"),
                    applicant.get("NationalID")
            };
            batchArgs.add(args);
        }
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    /**
     * 獲取抽籤時期的所有申請人（按身分別分組）
     */
    public Map<Integer, List<Map<String, Object>>> getLotteryApplicantsByPriority(UUID institutionId) {
        String sql = "SELECT ap.ApplicationID, ap.NationalID, ap.Name, ap.BirthDate, " +
                "ap.CurrentOrder, ap.Status, a.IdentityType, ap.ClassID " +
                "FROM application_participants ap " +
                "LEFT JOIN applications a ON ap.ApplicationID = a.ApplicationID " +
                "WHERE a.InstitutionID = ? AND ap.ParticipantType = 0 " +
                "AND ap.Status = '候補中' " +
                "ORDER BY a.IdentityType, ap.CurrentOrder";

        List<Map<String, Object>> allApplicants = jdbcTemplate.queryForList(sql, institutionId.toString());

        // 按身分別分組 (1: 第一序位, 2: 第二序位, 其他: 第三序位)
        Map<Integer, List<Map<String, Object>>> grouped = new HashMap<>();
        grouped.put(1, new ArrayList<>());
        grouped.put(2, new ArrayList<>());
        grouped.put(3, new ArrayList<>());

        for (Map<String, Object> applicant : allApplicants) {
            Object identityTypeObj = applicant.get("IdentityType");
            int identityType = identityTypeObj != null ? ((Number) identityTypeObj).intValue() : 0;

            if (identityType == 1) {
                grouped.get(1).add(applicant);
            } else if (identityType == 2) {
                grouped.get(2).add(applicant);
            } else {
                grouped.get(3).add(applicant);
            }
        }

        return grouped;
    }

    /**
     * 重置所有候補順位（抽籤前執行）
     */
    @Transactional
    public void resetAllWaitlistOrders(UUID institutionId) {
        String sql = "UPDATE ap SET ap.CurrentOrder = 0 " +
                "FROM application_participants ap " +
                "LEFT JOIN applications a ON ap.ApplicationID = a.ApplicationID " +
                "WHERE a.InstitutionID = ? AND ap.ParticipantType = 0 " +
                "AND ap.Status = '候補中'";
        jdbcTemplate.update(sql, institutionId.toString());
    }

    /**
     * 獲取機構總收托人數
     */
    public int getTotalCapacity(UUID institutionId) {
        String sql = "SELECT ISNULL(SUM(Capacity), 0) FROM classes WHERE InstitutionID = ?";
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, institutionId.toString());
        return result != null ? result : 0;
    }

    /**
     * 獲取機構目前就讀中學生總數
     */
    public int getCurrentStudentsCount(UUID institutionId) {
        String sql = "SELECT ISNULL(SUM(CurrentStudents), 0) FROM classes WHERE InstitutionID = ?";
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, institutionId.toString());
        return result != null ? result : 0;
    }

    /**
     * 統計機構內各序位已錄取人數
     * @return Map<序位類型, 已錄取人數>
     */
    public Map<Integer, Integer> getAcceptedCountByPriority(UUID institutionId) {
        String sql = "SELECT a.IdentityType, COUNT(*) AS Count " +
                "FROM application_participants ap " +
                "LEFT JOIN applications a ON ap.ApplicationID = a.ApplicationID " +
                "WHERE a.InstitutionID = ? AND ap.ParticipantType = 0 AND ap.Status = '已錄取' " +
                "GROUP BY a.IdentityType";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, institutionId.toString());

        Map<Integer, Integer> countMap = new HashMap<>();
        countMap.put(1, 0); // 第一序位
        countMap.put(2, 0); // 第二序位
        countMap.put(3, 0); // 第三序位

        for (Map<String, Object> row : results) {
            Object identityTypeObj = row.get("IdentityType");
            Object countObj = row.get("Count");

            if (identityTypeObj != null && countObj != null) {
                int identityType = ((Number) identityTypeObj).intValue();
                int count = ((Number) countObj).intValue();

                if (identityType == 1) {
                    countMap.put(1, count);
                } else if (identityType == 2) {
                    countMap.put(2, count);
                } else {
                    countMap.put(3, count);
                }
            }
        }

        return countMap;
    }

    /**
     * 獲取各班級資訊
     */
    public List<Map<String, Object>> getClassInfo(UUID institutionId) {
        String sql = "SELECT ClassID, ClassName, Capacity, CurrentStudents, " +
                "MinAgeDescription, MaxAgeDescription " +
                "FROM classes WHERE InstitutionID = ?";
        return jdbcTemplate.queryForList(sql, institutionId.toString());
    }

    /**
     * 根據幼童年齡計算適合的班級
     */
    public UUID findSuitableClass(LocalDate birthDate, List<Map<String, Object>> classes) {
        if (birthDate == null || classes == null || classes.isEmpty()) {
            return null;
        }

        LocalDate now = LocalDate.now();
        Period period = Period.between(birthDate, now);
        int ageInMonths = period.getYears() * 12 + period.getMonths();

        for (Map<String, Object> classInfo : classes) {
            Object minAgeObj = classInfo.get("MinAgeDescription");
            Object maxAgeObj = classInfo.get("MaxAgeDescription");
            Object capacityObj = classInfo.get("Capacity");
            Object currentStudentsObj = classInfo.get("CurrentStudents");

            if (minAgeObj != null && maxAgeObj != null && capacityObj != null && currentStudentsObj != null) {
                // MinAgeDescription 和 MaxAgeDescription 已經是月，不需要再乘以12
                int minAge = ((Number) minAgeObj).intValue();
                int maxAge = ((Number) maxAgeObj).intValue();
                int capacity = ((Number) capacityObj).intValue();
                int currentStudents = ((Number) currentStudentsObj).intValue();

                // 檢查年齡是否符合且班級有空位
                // 範例：幼童20個月，小班MinAge=12, MaxAge=24 → 12 <= 20 < 24
                if (ageInMonths >= minAge && ageInMonths < maxAge && currentStudents < capacity) {
                    Object classIdObj = classInfo.get("ClassID");
                    if (classIdObj != null) {
                        return UUID.fromString(classIdObj.toString());
                    }
                }
            }
        }

        return null;
    }

    /**
     * 檢查班級是否還有空位
     */
    public boolean hasClassCapacity(UUID classId) {
        String sql = "SELECT CASE WHEN Capacity > CurrentStudents THEN 1 ELSE 0 END " +
                "FROM classes WHERE ClassID = ?";
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, classId.toString());
        return result != null && result == 1;
    }

    /**
     * 更新班級目前學生數
     */
    @Transactional
    public void updateClassCurrentStudents(UUID classId, int increment) {
        String sql = "UPDATE classes SET CurrentStudents = CurrentStudents + ? WHERE ClassID = ?";
        jdbcTemplate.update(sql, increment, classId.toString());
    }

    /**
     * 記錄跳過錄取的情況（用於查核）
     */
    @Transactional
    public void logSkippedAdmission(UUID applicationId, String nationalId, String reason) {
        // 這裡可以記錄到日誌表或更新 Reason 欄位
        String sql = "UPDATE application_participants " +
                "SET Reason = ? " +
                "WHERE ApplicationID = ? AND NationalID = ?";
        jdbcTemplate.update(sql, reason, applicationId.toString(), nationalId);
    }

    /**
     * 非抽籤時期：機構手動錄取
     */
    @Transactional
    public boolean manualAdmit(UUID applicationId, String nationalId, UUID classId) {
        // 檢查班級是否有空位
        if (!hasClassCapacity(classId)) {
            return false;
        }

        // 更新申請人狀態為已錄取
        String sql = "UPDATE application_participants " +
                "SET Status = '已錄取', ClassID = ?, ReviewDate = ? " +
                "WHERE ApplicationID = ? AND NationalID = ?";
        int updated = jdbcTemplate.update(sql, classId.toString(), LocalDateTime.now(),
                applicationId.toString(), nationalId);

        if (updated > 0) {
            // 更新班級學生數
            updateClassCurrentStudents(classId, 1);
            return true;
        }

        return false;
    }

    /**
     * 檢查是否違反順序錄取
     */
    public List<Map<String, Object>> checkAdmissionOrderViolation(UUID institutionId, int admittedOrder) {
        String sql = "SELECT ap.ApplicationID, ap.NationalID, ap.Name, ap.CurrentOrder, ap.Status " +
                "FROM application_participants ap " +
                "LEFT JOIN applications a ON ap.ApplicationID = a.ApplicationID " +
                "WHERE a.InstitutionID = ? AND ap.ParticipantType = 0 " +
                "AND ap.CurrentOrder < ? AND ap.Status = '候補中'";
        return jdbcTemplate.queryForList(sql, institutionId.toString(), admittedOrder);
    }
}
