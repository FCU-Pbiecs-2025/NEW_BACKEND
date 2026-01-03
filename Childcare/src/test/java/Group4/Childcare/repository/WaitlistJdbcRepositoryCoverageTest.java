package Group4.Childcare.repository;

import Group4.Childcare.Repository.WaitlistJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WaitlistJdbcRepository 分支覆蓋率測試
 * 專門測試以下方法的所有分支以達到 90% 以上覆蓋率：
 * 1. findWaitlistByInstitution(String, String)
 * 2. getLotteryApplicantsByPriority(UUID)
 * 3. manualAdmit(UUID, String, UUID)
 * 4. getCurrentStudentsCount(UUID)
 * 5. findSuitableClass(LocalDate, List)
 * 6. getAcceptedCountByPriority(UUID)
 * 7. hasClassCapacity(UUID)
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class WaitlistJdbcRepositoryCoverageTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private WaitlistJdbcRepository repository;

    private UUID testInstitutionId;
    private UUID testApplicationId;
    private UUID testClassId;
    private String testNationalId;

    @BeforeEach
    void setUp() {
        testInstitutionId = UUID.randomUUID();
        testApplicationId = UUID.randomUUID();
        testClassId = UUID.randomUUID();
        testNationalId = "A123456789";
    }

    // ===========================================================================================
    // 1. findWaitlistByInstitution(String, String) - 測試所有分支
    // ===========================================================================================

    @Test
    void testFindWaitlistByInstitution_BothParametersNonNull() {
        // 測試 institutionId 和 name 都不為 null 的分支
        List<Map<String, Object>> mockResults = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("ApplicationID", testApplicationId.toString());
        row.put("Name", "測試學生");
        row.put("BirthDate", java.sql.Date.valueOf(LocalDate.now().minusYears(2)));
        mockResults.add(row);

        doReturn(mockResults).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        // When
        List<Map<String, Object>> result = repository.findWaitlistByInstitution(
                testInstitutionId.toString(), "測試");

        // Then
        assertNotNull(result);
        verify(jdbcTemplate).queryForList(contains("InstitutionID"), any(Object[].class));
    }

    @Test
    void testFindWaitlistByInstitution_InstitutionIdNull_NameNonNull() {
        // 測試 institutionId 為 null，name 不為 null
        List<Map<String, Object>> mockResults = new ArrayList<>();
        doReturn(mockResults).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        List<Map<String, Object>> result = repository.findWaitlistByInstitution(null, "測試");

        assertNotNull(result);
        verify(jdbcTemplate).queryForList(contains("LIKE"), any(Object[].class));
    }

    @Test
    void testFindWaitlistByInstitution_InstitutionIdEmpty_NameNonNull() {
        // 測試 institutionId 為空字串
        List<Map<String, Object>> mockResults = new ArrayList<>();
        doReturn(mockResults).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        List<Map<String, Object>> result = repository.findWaitlistByInstitution("   ", "測試");

        assertNotNull(result);
    }

    @Test
    void testFindWaitlistByInstitution_BothParametersNull() {
        // 測試兩個參數都為 null
        List<Map<String, Object>> mockResults = new ArrayList<>();
        doReturn(mockResults).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        List<Map<String, Object>> result = repository.findWaitlistByInstitution(null, null);

        assertNotNull(result);
    }

    @Test
    void testFindWaitlistByInstitution_BirthDateAsSqlDate() {
        // 測試 BirthDate 為 java.sql.Date 類型
        Map<String, Object> row = new HashMap<>();
        row.put("BirthDate", java.sql.Date.valueOf("2020-03-15"));
        List<Map<String, Object>> mockResults = Collections.singletonList(row);

        doReturn(mockResults).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        List<Map<String, Object>> result = repository.findWaitlistByInstitution(null, null);

        assertNotNull(result.get(0).get("Age"));
        assertTrue(result.get(0).get("Age").toString().contains("歲"));
    }

    @Test
    void testFindWaitlistByInstitution_BirthDateAsString() {
        // 測試 BirthDate 為 String 類型
        Map<String, Object> row = new HashMap<>();
        row.put("BirthDate", "2020-06-20");
        List<Map<String, Object>> mockResults = Collections.singletonList(row);

        doReturn(mockResults).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        List<Map<String, Object>> result = repository.findWaitlistByInstitution(null, null);

        assertNotNull(result.get(0).get("Age"));
        assertTrue(result.get(0).get("Age").toString().contains("歲"));
    }

    @Test
    void testFindWaitlistByInstitution_BirthDateNull() {
        // 測試 BirthDate 為 null 的分支
        Map<String, Object> row = new HashMap<>();
        row.put("BirthDate", null);
        List<Map<String, Object>> mockResults = Collections.singletonList(row);

        doReturn(mockResults).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        List<Map<String, Object>> result = repository.findWaitlistByInstitution(null, null);

        assertEquals("", result.get(0).get("Age"));
    }

    @Test
    void testFindWaitlistByInstitution_BirthDateInvalidType() {
        // 測試 BirthDate 為無效類型（else 分支）
        Map<String, Object> row = new HashMap<>();
        row.put("BirthDate", 12345); // Integer type (invalid)
        List<Map<String, Object>> mockResults = Collections.singletonList(row);

        doReturn(mockResults).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        List<Map<String, Object>> result = repository.findWaitlistByInstitution(null, null);

        assertEquals("", result.get(0).get("Age"));
    }

    @Test
    void testFindWaitlistByInstitution_BirthDateParseException() {
        // 測試日期解析異常（catch 分支）
        Map<String, Object> row = new HashMap<>();
        row.put("BirthDate", "invalid-date-format");
        List<Map<String, Object>> mockResults = Collections.singletonList(row);

        doReturn(mockResults).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        List<Map<String, Object>> result = repository.findWaitlistByInstitution(null, null);

        assertEquals("", result.get(0).get("Age"));
    }

    // ===========================================================================================
    // 2. getLotteryApplicantsByPriority(UUID) - 測試所有分支
    // ===========================================================================================

    @Test
    void testGetLotteryApplicantsByPriority_IdentityType1() {
        // 測試 identityType == 1 分支
        Map<String, Object> applicant = new HashMap<>();
        applicant.put("IdentityType", 1);
        applicant.put("Name", "第一序位學生");

        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(Collections.singletonList(applicant));

        Map<Integer, List<Map<String, Object>>> result =
                repository.getLotteryApplicantsByPriority(testInstitutionId);

        assertEquals(1, result.get(1).size());
        assertEquals(0, result.get(2).size());
        assertEquals(0, result.get(3).size());
    }

    @Test
    void testGetLotteryApplicantsByPriority_IdentityType2() {
        // 測試 identityType == 2 分支
        Map<String, Object> applicant = new HashMap<>();
        applicant.put("IdentityType", 2);
        applicant.put("Name", "第二序位學生");

        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(Collections.singletonList(applicant));

        Map<Integer, List<Map<String, Object>>> result =
                repository.getLotteryApplicantsByPriority(testInstitutionId);

        assertEquals(0, result.get(1).size());
        assertEquals(1, result.get(2).size());
        assertEquals(0, result.get(3).size());
    }

    @Test
    void testGetLotteryApplicantsByPriority_IdentityTypeOther() {
        // 測試 identityType 為其他值（else 分支，第三序位）
        Map<String, Object> applicant = new HashMap<>();
        applicant.put("IdentityType", 3);
        applicant.put("Name", "第三序位學生");

        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(Collections.singletonList(applicant));

        Map<Integer, List<Map<String, Object>>> result =
                repository.getLotteryApplicantsByPriority(testInstitutionId);

        assertEquals(0, result.get(1).size());
        assertEquals(0, result.get(2).size());
        assertEquals(1, result.get(3).size());
    }

    @Test
    void testGetLotteryApplicantsByPriority_IdentityTypeNull() {
        // 測試 identityType 為 null（使用三元運算符的 null 分支）
        Map<String, Object> applicant = new HashMap<>();
        applicant.put("IdentityType", null);
        applicant.put("Name", "無身分別學生");

        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(Collections.singletonList(applicant));

        Map<Integer, List<Map<String, Object>>> result =
                repository.getLotteryApplicantsByPriority(testInstitutionId);

        // null 會被轉為 0，進入 else 分支（第三序位）
        assertEquals(1, result.get(3).size());
    }

    @Test
    void testGetLotteryApplicantsByPriority_MixedIdentityTypes() {
        // 測試混合多種身分別
        List<Map<String, Object>> applicants = new ArrayList<>();

        Map<String, Object> a1 = new HashMap<>();
        a1.put("IdentityType", 1);
        applicants.add(a1);

        Map<String, Object> a2 = new HashMap<>();
        a2.put("IdentityType", 2);
        applicants.add(a2);

        Map<String, Object> a3 = new HashMap<>();
        a3.put("IdentityType", 3);
        applicants.add(a3);

        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(applicants);

        Map<Integer, List<Map<String, Object>>> result =
                repository.getLotteryApplicantsByPriority(testInstitutionId);

        assertEquals(1, result.get(1).size());
        assertEquals(1, result.get(2).size());
        assertEquals(1, result.get(3).size());
    }

    // ===========================================================================================
    // 3. getCurrentStudentsCount(UUID) - 測試所有分支
    // ===========================================================================================

    @Test
    void testGetCurrentStudentsCount_ReturnsNonNull() {
        // 測試 result != null 分支
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(50);

        int result = repository.getCurrentStudentsCount(testInstitutionId);

        assertEquals(50, result);
    }

    @Test
    void testGetCurrentStudentsCount_ReturnsNull() {
        // 測試 result == null 分支（返回 0）
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(null);

        int result = repository.getCurrentStudentsCount(testInstitutionId);

        assertEquals(0, result);
    }

    // ===========================================================================================
    // 4. getAcceptedCountByPriority(UUID) - 測試所有分支
    // ===========================================================================================

    @Test
    void testGetAcceptedCountByPriority_IdentityType1() {
        // 測試 identityType == 1 分支
        Map<String, Object> row = new HashMap<>();
        row.put("IdentityType", 1);
        row.put("Count", 5);

        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(Collections.singletonList(row));

        Map<Integer, Integer> result = repository.getAcceptedCountByPriority(testInstitutionId);

        assertEquals(5, result.get(1));
        assertEquals(0, result.get(2));
        assertEquals(0, result.get(3));
    }

    @Test
    void testGetAcceptedCountByPriority_IdentityType2() {
        // 測試 identityType == 2 分支
        Map<String, Object> row = new HashMap<>();
        row.put("IdentityType", 2);
        row.put("Count", 10);

        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(Collections.singletonList(row));

        Map<Integer, Integer> result = repository.getAcceptedCountByPriority(testInstitutionId);

        assertEquals(0, result.get(1));
        assertEquals(10, result.get(2));
        assertEquals(0, result.get(3));
    }

    @Test
    void testGetAcceptedCountByPriority_IdentityTypeOther() {
        // 測試 identityType 為其他值（else 分支）
        Map<String, Object> row = new HashMap<>();
        row.put("IdentityType", 3);
        row.put("Count", 15);

        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(Collections.singletonList(row));

        Map<Integer, Integer> result = repository.getAcceptedCountByPriority(testInstitutionId);

        assertEquals(0, result.get(1));
        assertEquals(0, result.get(2));
        assertEquals(15, result.get(3));
    }

    @Test
    void testGetAcceptedCountByPriority_IdentityTypeNull() {
        // 測試 identityTypeObj == null 的分支
        Map<String, Object> row = new HashMap<>();
        row.put("IdentityType", null);
        row.put("Count", 5);

        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(Collections.singletonList(row));

        Map<Integer, Integer> result = repository.getAcceptedCountByPriority(testInstitutionId);

        // null 情況下，外層 if 條件不滿足，不會更新 countMap
        assertEquals(0, result.get(1));
        assertEquals(0, result.get(2));
        assertEquals(0, result.get(3));
    }

    @Test
    void testGetAcceptedCountByPriority_CountNull() {
        // 測試 countObj == null 的分支
        Map<String, Object> row = new HashMap<>();
        row.put("IdentityType", 1);
        row.put("Count", null);

        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(Collections.singletonList(row));

        Map<Integer, Integer> result = repository.getAcceptedCountByPriority(testInstitutionId);

        // null 情況下不會更新
        assertEquals(0, result.get(1));
    }

    @Test
    void testGetAcceptedCountByPriority_MixedIdentityTypes() {
        // 測試多種身分別混合
        List<Map<String, Object>> rows = new ArrayList<>();

        Map<String, Object> r1 = new HashMap<>();
        r1.put("IdentityType", 1);
        r1.put("Count", 3);
        rows.add(r1);

        Map<String, Object> r2 = new HashMap<>();
        r2.put("IdentityType", 2);
        r2.put("Count", 7);
        rows.add(r2);

        Map<String, Object> r3 = new HashMap<>();
        r3.put("IdentityType", 3);
        r3.put("Count", 12);
        rows.add(r3);

        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(rows);

        Map<Integer, Integer> result = repository.getAcceptedCountByPriority(testInstitutionId);

        assertEquals(3, result.get(1));
        assertEquals(7, result.get(2));
        assertEquals(12, result.get(3));
    }

    // ===========================================================================================
    // 5. findSuitableClass(LocalDate, List) - 測試所有分支
    // ===========================================================================================

    @Test
    void testFindSuitableClass_BirthDateNull() {
        // 測試 birthDate == null 分支
        UUID result = repository.findSuitableClass(null, Collections.emptyList());
        assertNull(result);
    }

    @Test
    void testFindSuitableClass_ClassesNull() {
        // 測試 classes == null 分支
        UUID result = repository.findSuitableClass(LocalDate.now(), null);
        assertNull(result);
    }

    @Test
    void testFindSuitableClass_ClassesEmpty() {
        // 測試 classes.isEmpty() 分支
        UUID result = repository.findSuitableClass(LocalDate.now(), Collections.emptyList());
        assertNull(result);
    }

    @Test
    void testFindSuitableClass_MatchingClass() {
        // 測試找到合適班級（所有條件都滿足）
        LocalDate birthDate = LocalDate.now().minusMonths(18); // 18個月大

        List<Map<String, Object>> classes = new ArrayList<>();
        Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("ClassID", testClassId.toString());
        classInfo.put("MinAgeDescription", 12); // 12個月
        classInfo.put("MaxAgeDescription", 24); // 24個月
        classInfo.put("Capacity", 20);
        classInfo.put("CurrentStudents", 15);
        classes.add(classInfo);

        UUID result = repository.findSuitableClass(birthDate, classes);

        assertEquals(testClassId, result);
    }

    @Test
    void testFindSuitableClass_MinAgeNull() {
        // 測試 minAgeObj == null 分支
        LocalDate birthDate = LocalDate.now().minusMonths(18);

        Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("ClassID", testClassId.toString());
        classInfo.put("MinAgeDescription", null); // null
        classInfo.put("MaxAgeDescription", 24);
        classInfo.put("Capacity", 20);
        classInfo.put("CurrentStudents", 15);

        UUID result = repository.findSuitableClass(birthDate, Collections.singletonList(classInfo));

        assertNull(result); // 不滿足條件，繼續下一個
    }

    @Test
    void testFindSuitableClass_MaxAgeNull() {
        // 測試 maxAgeObj == null 分支
        LocalDate birthDate = LocalDate.now().minusMonths(18);

        Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("ClassID", testClassId.toString());
        classInfo.put("MinAgeDescription", 12);
        classInfo.put("MaxAgeDescription", null); // null
        classInfo.put("Capacity", 20);
        classInfo.put("CurrentStudents", 15);

        UUID result = repository.findSuitableClass(birthDate, Collections.singletonList(classInfo));

        assertNull(result);
    }

    @Test
    void testFindSuitableClass_CapacityNull() {
        // 測試 capacityObj == null 分支
        LocalDate birthDate = LocalDate.now().minusMonths(18);

        Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("ClassID", testClassId.toString());
        classInfo.put("MinAgeDescription", 12);
        classInfo.put("MaxAgeDescription", 24);
        classInfo.put("Capacity", null); // null
        classInfo.put("CurrentStudents", 15);

        UUID result = repository.findSuitableClass(birthDate, Collections.singletonList(classInfo));

        assertNull(result);
    }

    @Test
    void testFindSuitableClass_CurrentStudentsNull() {
        // 測試 currentStudentsObj == null 分支
        LocalDate birthDate = LocalDate.now().minusMonths(18);

        Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("ClassID", testClassId.toString());
        classInfo.put("MinAgeDescription", 12);
        classInfo.put("MaxAgeDescription", 24);
        classInfo.put("Capacity", 20);
        classInfo.put("CurrentStudents", null); // null

        UUID result = repository.findSuitableClass(birthDate, Collections.singletonList(classInfo));

        assertNull(result);
    }

    @Test
    void testFindSuitableClass_AgeBelowMinAge() {
        // 測試 ageInMonths < minAge（不滿足年齡條件）
        LocalDate birthDate = LocalDate.now().minusMonths(10); // 10個月

        Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("ClassID", testClassId.toString());
        classInfo.put("MinAgeDescription", 12); // 需要12個月以上
        classInfo.put("MaxAgeDescription", 24);
        classInfo.put("Capacity", 20);
        classInfo.put("CurrentStudents", 15);

        UUID result = repository.findSuitableClass(birthDate, Collections.singletonList(classInfo));

        assertNull(result);
    }

    @Test
    void testFindSuitableClass_AgeAboveMaxAge() {
        // 測試 ageInMonths >= maxAge（不滿足年齡上限）
        LocalDate birthDate = LocalDate.now().minusMonths(30); // 30個月

        Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("ClassID", testClassId.toString());
        classInfo.put("MinAgeDescription", 12);
        classInfo.put("MaxAgeDescription", 24); // 最大24個月
        classInfo.put("Capacity", 20);
        classInfo.put("CurrentStudents", 15);

        UUID result = repository.findSuitableClass(birthDate, Collections.singletonList(classInfo));

        assertNull(result);
    }

    @Test
    void testFindSuitableClass_NoCapacity() {
        // 測試 currentStudents >= capacity（班級已滿）
        LocalDate birthDate = LocalDate.now().minusMonths(18);

        Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("ClassID", testClassId.toString());
        classInfo.put("MinAgeDescription", 12);
        classInfo.put("MaxAgeDescription", 24);
        classInfo.put("Capacity", 20);
        classInfo.put("CurrentStudents", 20); // 已滿

        UUID result = repository.findSuitableClass(birthDate, Collections.singletonList(classInfo));

        assertNull(result);
    }

    @Test
    void testFindSuitableClass_ClassIdNull() {
        // 測試 classIdObj == null 分支
        LocalDate birthDate = LocalDate.now().minusMonths(18);

        Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("ClassID", null); // null
        classInfo.put("MinAgeDescription", 12);
        classInfo.put("MaxAgeDescription", 24);
        classInfo.put("Capacity", 20);
        classInfo.put("CurrentStudents", 15);

        UUID result = repository.findSuitableClass(birthDate, Collections.singletonList(classInfo));

        assertNull(result);
    }

    @Test
    void testFindSuitableClass_MultipleClasses_FirstMatch() {
        // 測試多個班級，返回第一個符合的
        LocalDate birthDate = LocalDate.now().minusMonths(18);

        List<Map<String, Object>> classes = new ArrayList<>();

        // 第一個班級不符合（年齡不符）
        Map<String, Object> c1 = new HashMap<>();
        c1.put("ClassID", UUID.randomUUID().toString());
        c1.put("MinAgeDescription", 24);
        c1.put("MaxAgeDescription", 36);
        c1.put("Capacity", 20);
        c1.put("CurrentStudents", 10);
        classes.add(c1);

        // 第二個班級符合
        Map<String, Object> c2 = new HashMap<>();
        c2.put("ClassID", testClassId.toString());
        c2.put("MinAgeDescription", 12);
        c2.put("MaxAgeDescription", 24);
        c2.put("Capacity", 20);
        c2.put("CurrentStudents", 10);
        classes.add(c2);

        UUID result = repository.findSuitableClass(birthDate, classes);

        assertEquals(testClassId, result);
    }

    // ===========================================================================================
    // 6. hasClassCapacity(UUID) - 測試所有分支
    // ===========================================================================================

    @Test
    void testHasClassCapacity_HasCapacity() {
        // 測試 result != null && result == 1（有空位）
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(1);

        boolean result = repository.hasClassCapacity(testClassId);

        assertTrue(result);
    }

    @Test
    void testHasClassCapacity_NoCapacity() {
        // 測試 result != null && result == 0（沒空位）
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(0);

        boolean result = repository.hasClassCapacity(testClassId);

        assertFalse(result);
    }

    @Test
    void testHasClassCapacity_ResultNull() {
        // 測試 result == null 分支
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(null);

        boolean result = repository.hasClassCapacity(testClassId);

        assertFalse(result);
    }

    @Test
    void testHasClassCapacity_ResultOtherValue() {
        // 測試 result 為其他數值（不是 0 或 1）
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(2);

        boolean result = repository.hasClassCapacity(testClassId);

        assertFalse(result); // 只有 1 才返回 true
    }

    // ===========================================================================================
    // 7. manualAdmit(UUID, String, UUID) - 測試所有分支
    // ===========================================================================================

    @Test
    void testManualAdmit_NoCapacity() {
        // 測試 !hasClassCapacity(classId) 分支（沒空位，返回 false）
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(0); // 沒空位

        boolean result = repository.manualAdmit(testApplicationId, testNationalId, testClassId);

        assertFalse(result);
        verify(jdbcTemplate, never()).update(contains("UPDATE application_participants"),
                any(), any(), any(), any());
    }

    @Test
    void testManualAdmit_Success() {
        // 測試成功錄取（有空位，更新成功）
        // Mock hasClassCapacity
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(1); // 有空位

        // Mock update application_participants
        when(jdbcTemplate.update(contains("UPDATE application_participants"),
                anyString(), any(LocalDateTime.class), anyString(), anyString()))
                .thenReturn(1); // 更新成功

        // Mock updateClassCurrentStudents
        when(jdbcTemplate.update(contains("UPDATE classes"), anyInt(), anyString()))
                .thenReturn(1);

        boolean result = repository.manualAdmit(testApplicationId, testNationalId, testClassId);

        assertTrue(result);
        verify(jdbcTemplate).update(contains("UPDATE application_participants"),
                anyString(), any(LocalDateTime.class), anyString(), anyString());
        verify(jdbcTemplate).update(contains("UPDATE classes"), eq(1), anyString());
    }

    @Test
    void testManualAdmit_UpdateFailed() {
        // 測試更新失敗（updated == 0，返回 false）
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(1); // 有空位

        when(jdbcTemplate.update(contains("UPDATE application_participants"),
                anyString(), any(LocalDateTime.class), anyString(), anyString()))
                .thenReturn(0); // 更新失敗

        boolean result = repository.manualAdmit(testApplicationId, testNationalId, testClassId);

        assertFalse(result);
        // 不應該更新班級學生數
        verify(jdbcTemplate, never()).update(contains("UPDATE classes"), anyInt(), anyString());
    }

    @Test
    void testManualAdmit_UpdateReturnsNegative() {
        // 測試邊界情況：update 返回負數
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(1);

        when(jdbcTemplate.update(contains("UPDATE application_participants"),
                anyString(), any(LocalDateTime.class), anyString(), anyString()))
                .thenReturn(-1); // 異常情況

        boolean result = repository.manualAdmit(testApplicationId, testNationalId, testClassId);

        assertFalse(result);
    }
}

