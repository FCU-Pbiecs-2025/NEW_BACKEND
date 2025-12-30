package Group4.Childcare.repository;

import Group4.Childcare.Repository.WaitlistJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WaitlistJdbcRepository 單元測試
 * 測試候補名單管理相關的資料庫操作
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class WaitlistJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private WaitlistJdbcRepository waitlistRepository;

    private String testInstitutionId;
    private UUID testApplicationId;

    @BeforeEach
    void setUp() {
        testInstitutionId = UUID.randomUUID().toString();
        testApplicationId = UUID.randomUUID();
    }

    // ==================== findWaitlistByInstitution Tests ====================

    @Test
    void testFindWaitlistByInstitution_WithAllParameters_Success() {
        // Given
        String name = "王小明";
        List<Map<String, Object>> mockResults = new ArrayList<>();
        mockResults.add(createTestWaitlistMap());

        // Use doReturn to avoid type ambiguity
        doReturn(mockResults).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        // When
        List<Map<String, Object>> result = waitlistRepository.findWaitlistByInstitution(
                testInstitutionId, name);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() >= 1);
    }

    @Test
    void testFindWaitlistByInstitution_WithNullName_Success() {
        // Given
        List<Map<String, Object>> mockResults = new ArrayList<>();
        mockResults.add(createTestWaitlistMap());
        mockResults.add(createTestWaitlistMap());

        doReturn(mockResults).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        // When
        List<Map<String, Object>> result = waitlistRepository.findWaitlistByInstitution(
                testInstitutionId, null);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    void testFindWaitlistByInstitution_EmptyResult() {
        // Given
        doReturn(Collections.emptyList()).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        // When
        List<Map<String, Object>> result = waitlistRepository.findWaitlistByInstitution(
                testInstitutionId, null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindWaitlistByInstitution_DateParsing() {
        // Given
        // 1. String Date
        Map<String, Object> r1 = createTestWaitlistMap();
        r1.put("BirthDate", "2020-01-01"); // String format

        // 2. Unknown Type
        Map<String, Object> r2 = createTestWaitlistMap();
        r2.put("BirthDate", 12345L); // invalid type

        // 3. Invalid String Format
        Map<String, Object> r3 = createTestWaitlistMap();
        r3.put("BirthDate", "invalid-date");

        // 4. Null Date
        Map<String, Object> r4 = createTestWaitlistMap();
        r4.put("BirthDate", null);

        List<Map<String, Object>> mockResults = new ArrayList<>();
        mockResults.add(r1);
        mockResults.add(r2);
        mockResults.add(r3);
        mockResults.add(r4);

        doReturn(mockResults).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        // When
        List<Map<String, Object>> result = waitlistRepository.findWaitlistByInstitution(testInstitutionId, null);

        // Then
        assertNotNull(result);
        assertEquals(4, result.size());

        // r1: String parsed correctly
        assertNotNull(result.get(0).get("Age"));
        assertNotEquals("", result.get(0).get("Age"));

        // r2: Unknown type -> Age is empty string
        assertEquals("", result.get(1).get("Age"));

        // r3: Invalid string -> catch exception -> Age is empty string
        assertEquals("", result.get(2).get("Age"));

        // r4: Null -> Age is empty string
        assertEquals("", result.get(3).get("Age"));
    }

    // ==================== getWaitlistApplicants Tests ====================

    @Test
    void testGetWaitlistApplicants_Success() {
        // Given
        List<Map<String, Object>> mockResults = Collections.singletonList(createTestApplicantMap());

        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(mockResults);

        // When
        List<Map<String, Object>> result = waitlistRepository.getWaitlistApplicants(testApplicationId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetWaitlistApplicants_EmptyResult() {
        // Given
        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        // When
        List<Map<String, Object>> result = waitlistRepository.getWaitlistApplicants(testApplicationId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getNextWaitlistOrder Tests ====================

    @Test
    void testGetNextWaitlistOrder_ReturnsNextOrder() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(6);

        // When
        int nextOrder = waitlistRepository.getNextWaitlistOrder(testApplicationId);

        // Then
        assertEquals(6, nextOrder);
    }

    @Test
    void testGetNextWaitlistOrder_ReturnsOne_WhenNoData() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(null);

        // When
        int nextOrder = waitlistRepository.getNextWaitlistOrder(testApplicationId);

        // Then
        assertEquals(1, nextOrder);
    }

    // ==================== updateApplicantOrder Tests ====================

    @Test
    void testUpdateApplicantOrder_Success() {
        // Given
        String nationalId = "A123456789";
        int newOrder = 5;
        String status = "候補中";
        LocalDateTime reviewDate = LocalDateTime.now();

        when(jdbcTemplate.update(anyString(), anyInt(), anyString(), any(LocalDateTime.class),
                anyString(), anyString()))
                .thenReturn(1);

        // When
        waitlistRepository.updateApplicantOrder(testApplicationId, nationalId, newOrder, status, reviewDate);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), anyInt(), anyString(),
                any(LocalDateTime.class), anyString(), anyString());
    }

    // ==================== batchUpdateApplicants Tests ====================

    @Test
    void testBatchUpdateApplicants_Success() {
        // Given
        List<Map<String, Object>> applicants = Arrays.asList(
                createTestApplicantUpdateMap(),
                createTestApplicantUpdateMap());

        when(jdbcTemplate.batchUpdate(anyString(), anyList()))
                .thenReturn(new int[] { 1, 1 });

        // When
        waitlistRepository.batchUpdateApplicants(applicants);

        // Then
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), anyList());
    }

    @Test
    void testBatchUpdateApplicants_EmptyList() {
        // Given
        List<Map<String, Object>> applicants = Collections.emptyList();

        when(jdbcTemplate.batchUpdate(anyString(), anyList()))
                .thenReturn(new int[0]);

        // When
        waitlistRepository.batchUpdateApplicants(applicants);

        // Then
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), anyList());
    }

    // ==================== getLotteryApplicantsByPriority Tests
    // ====================

    @Test
    void testGetLotteryApplicantsByPriority_Success() {
        // Given
        List<Map<String, Object>> mockResults = Arrays.asList(
                createTestLotteryApplicantMap(1),
                createTestLotteryApplicantMap(2));

        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(mockResults);

        // When
        Map<Integer, List<Map<String, Object>>> result = waitlistRepository
                .getLotteryApplicantsByPriority(testApplicationId);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey(1));
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> createTestWaitlistMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("ApplicationID", testApplicationId.toString());
        map.put("Name", "王小明");
        map.put("CaseNumber", "12345");
        map.put("BirthDate", java.sql.Date.valueOf("2020-05-15"));
        map.put("IdentityType", 1);
        map.put("CurrentOrder", 3);
        map.put("InstitutionName", "測試幼兒園");
        map.put("Age", "3歲6個月");
        return map;
    }

    private Map<String, Object> createTestApplicantMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("ApplicationID", testApplicationId.toString());
        map.put("NationalID", "A123456789");
        map.put("Name", "王小明");
        map.put("BirthDate", java.sql.Date.valueOf("2020-05-15"));
        map.put("CurrentOrder", 3);
        map.put("Status", "候補中");
        map.put("IdentityType", 1);
        map.put("ClassID", UUID.randomUUID().toString());
        return map;
    }

    private Map<String, Object> createTestApplicantUpdateMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("ApplicationID", testApplicationId.toString());
        map.put("NationalID", "A123456789");
        map.put("CurrentOrder", 3);
        map.put("Status", "候補中");
        map.put("Reason", null);
        map.put("ClassID", UUID.randomUUID().toString());
        map.put("ReviewDate", LocalDateTime.now());
        return map;
    }

    private Map<String, Object> createTestLotteryApplicantMap(int identityType) {
        Map<String, Object> map = new HashMap<>();
        map.put("ApplicationID", testApplicationId.toString());
        map.put("NationalID", "A123456789");
        map.put("Name", "王小明");
        map.put("ChildName", "王小明");
        map.put("BirthDate", java.sql.Date.valueOf("2020-05-15"));
        map.put("CurrentOrder", 3);
        map.put("Status", "候補中");
        map.put("IdentityType", identityType);
        map.put("ClassID", UUID.randomUUID().toString());
        map.put("Email", "test@example.com");
        map.put("ApplicantName", "王爸爸");
        map.put("InstitutionName", "測試幼兒園");
        map.put("CaseNumber", "12345");
        map.put("ApplicationDate", java.sql.Date.valueOf("2024-01-01"));
        return map;
    }

    // ==================== resetAllWaitlistOrders Tests ====================

    @Test
    void testResetAllWaitlistOrders_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), anyString())).thenReturn(1);

        // When
        waitlistRepository.resetAllWaitlistOrders(UUID.fromString(testInstitutionId));

        // Then
        verify(jdbcTemplate).update(contains("UPDATE ap SET ap.CurrentOrder = 0"), eq(testInstitutionId));
    }

    // ==================== getTotalCapacity Tests ====================

    @Test
    void testGetTotalCapacity_Success() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(50);

        // When
        int result = waitlistRepository.getTotalCapacity(UUID.fromString(testInstitutionId));

        // Then
        assertEquals(50, result);
    }

    @Test
    void testGetTotalCapacity_Null() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(null);

        // When
        int result = waitlistRepository.getTotalCapacity(UUID.fromString(testInstitutionId));

        // Then
        assertEquals(0, result);
    }

    // ==================== getCurrentStudentsCount Tests ====================

    @Test
    void testGetCurrentStudentsCount_Success() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(30);

        // When
        int result = waitlistRepository.getCurrentStudentsCount(UUID.fromString(testInstitutionId));

        // Then
        assertEquals(30, result);
    }

    // ==================== updateClassCurrentStudents Tests ====================

    @Test
    void testUpdateClassCurrentStudents_Success() {
        // Given
        UUID classId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), anyInt(), anyString())).thenReturn(1);

        // When
        waitlistRepository.updateClassCurrentStudents(classId, 1);

        // Then
        verify(jdbcTemplate).update(contains("UPDATE classes SET CurrentStudents"), eq(1), eq(classId.toString()));
    }

    // ==================== logSkippedAdmission Tests ====================

    @Test
    void testLogSkippedAdmission_Success() {
        // Given
        String nationalId = "A123";
        String reason = "Skipped";

        // When
        waitlistRepository.logSkippedAdmission(testApplicationId, nationalId, reason);

        // Then
        verify(jdbcTemplate).update(contains("SET Reason = ?"), eq(reason), eq(testApplicationId.toString()),
                eq(nationalId));
    }

    // ==================== hasClassCapacity Tests ====================

    @Test
    void testHasClassCapacity_True() {
        // Given
        UUID classId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(1);

        // When
        boolean result = waitlistRepository.hasClassCapacity(classId);

        // Then
        assertTrue(result);
    }

    @Test
    void testHasClassCapacity_False() {
        // Given
        UUID classId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);

        // When
        boolean result = waitlistRepository.hasClassCapacity(classId);

        // Then
        assertFalse(result);
    }

    // ==================== checkAdmissionOrderViolation Tests ====================

    @Test
    void testCheckAdmissionOrderViolation() {
        // Given
        when(jdbcTemplate.queryForList(anyString(), anyString(), anyInt())).thenReturn(Collections.emptyList());

        // When
        List<Map<String, Object>> result = waitlistRepository
                .checkAdmissionOrderViolation(UUID.fromString(testInstitutionId), 5);

        // Then
        verify(jdbcTemplate).queryForList(contains("CurrentOrder < ?"), eq(testInstitutionId), eq(5));
        assertNotNull(result);
    }

    // ==================== getAcceptedCountByPriority Tests ====================

    @Test
    void testGetAcceptedCountByPriority_Success() {
        // Given
        List<Map<String, Object>> mockResults = new ArrayList<>();
        Map<String, Object> r1 = new HashMap<>();
        r1.put("IdentityType", 1);
        r1.put("Count", 5);
        Map<String, Object> r2 = new HashMap<>();
        r2.put("IdentityType", 2);
        r2.put("Count", 3);
        Map<String, Object> r3 = new HashMap<>();
        r3.put("IdentityType", 3);
        r3.put("Count", 2);
        mockResults.add(r1);
        mockResults.add(r2);
        mockResults.add(r3);

        when(jdbcTemplate.queryForList(anyString(), anyString())).thenReturn(mockResults);

        // When
        Map<Integer, Integer> result = waitlistRepository
                .getAcceptedCountByPriority(UUID.fromString(testInstitutionId));

        // Then
        assertEquals(5, result.get(1));
        assertEquals(3, result.get(2));
        assertEquals(2, result.get(3));
    }

    // ==================== getClassInfo Tests ====================

    @Test
    void testGetClassInfo() {
        // Given
        when(jdbcTemplate.queryForList(anyString(), anyString())).thenReturn(Collections.emptyList());

        // When
        waitlistRepository.getClassInfo(UUID.fromString(testInstitutionId));

        // Then
        verify(jdbcTemplate).queryForList(contains("SELECT ClassID, ClassName"), eq(testInstitutionId));
    }

    // ==================== findSuitableClass Tests ====================

    @Test
    void testFindSuitableClass_Match() {
        // Given
        java.time.LocalDate birthDate = java.time.LocalDate.now().minusMonths(20);
        List<Map<String, Object>> classes = new ArrayList<>();
        Map<String, Object> cls = new HashMap<>();
        cls.put("ClassID", UUID.randomUUID().toString());
        cls.put("MinAgeDescription", 12);
        cls.put("MaxAgeDescription", 24);
        cls.put("Capacity", 10);
        cls.put("CurrentStudents", 5);
        classes.add(cls);

        // When
        UUID classId = waitlistRepository.findSuitableClass(birthDate, classes);

        // Then
        assertNotNull(classId);
        assertEquals(UUID.fromString(cls.get("ClassID").toString()), classId);
    }

    @Test
    void testFindSuitableClass_NoMatch_Capacity() {
        // Given
        java.time.LocalDate birthDate = java.time.LocalDate.now().minusMonths(20);
        List<Map<String, Object>> classes = new ArrayList<>();
        Map<String, Object> cls = new HashMap<>();
        cls.put("ClassID", UUID.randomUUID().toString());
        cls.put("MinAgeDescription", 12);
        cls.put("MaxAgeDescription", 24);
        cls.put("Capacity", 10);
        cls.put("CurrentStudents", 10); // Full
        classes.add(cls);

        // When
        UUID classId = waitlistRepository.findSuitableClass(birthDate, classes);

        // Then
        assertNull(classId);
    }

    // ==================== manualAdmit Tests ====================

    @Test
    void testManualAdmit_Success() {
        // Given
        UUID classId = UUID.randomUUID();
        String nationalId = "A123";

        // Mock hasClassCapacity query
        when(jdbcTemplate.queryForObject(contains("Capacity > CurrentStudents"), eq(Integer.class),
                eq(classId.toString())))
                .thenReturn(1);

        // Mock update applicant
        when(jdbcTemplate.update(contains("SET Status = '已錄取'"), anyString(), any(LocalDateTime.class), anyString(),
                anyString()))
                .thenReturn(1);

        // When
        boolean result = waitlistRepository.manualAdmit(testApplicationId, nationalId, classId);

        // Then
        assertTrue(result);
        // Verify class student count update requested
        verify(jdbcTemplate).update(contains("UPDATE classes SET CurrentStudents"), eq(1), eq(classId.toString()));
    }

    @Test
    void testManualAdmit_Fail_NoCapacity() {
        // Given
        UUID classId = UUID.randomUUID();

        when(jdbcTemplate.queryForObject(contains("Capacity > CurrentStudents"), eq(Integer.class),
                eq(classId.toString())))
                .thenReturn(0);

        // When
        boolean result = waitlistRepository.manualAdmit(testApplicationId, "A123", classId);

        // Then
        assertFalse(result);
        verify(jdbcTemplate, never()).update(contains("SET Status = '已錄取'"), any(), any(), any(), any());
    }
}
