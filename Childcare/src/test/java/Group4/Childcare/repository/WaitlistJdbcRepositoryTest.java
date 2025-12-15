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
            createTestApplicantUpdateMap()
        );

        when(jdbcTemplate.batchUpdate(anyString(), anyList()))
            .thenReturn(new int[]{1, 1});

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

    // ==================== getLotteryApplicantsByPriority Tests ====================

    @Test
    void testGetLotteryApplicantsByPriority_Success() {
        // Given
        List<Map<String, Object>> mockResults = Arrays.asList(
            createTestLotteryApplicantMap(1),
            createTestLotteryApplicantMap(2)
        );

        when(jdbcTemplate.queryForList(anyString(), anyString()))
            .thenReturn(mockResults);

        // When
        Map<Integer, List<Map<String, Object>>> result =
            waitlistRepository.getLotteryApplicantsByPriority(testApplicationId);

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
}

