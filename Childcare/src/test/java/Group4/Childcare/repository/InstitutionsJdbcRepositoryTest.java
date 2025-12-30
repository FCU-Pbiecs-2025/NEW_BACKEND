package Group4.Childcare.repository;

import Group4.Childcare.Model.Institutions;
import Group4.Childcare.DTO.InstitutionSummaryDTO;
import Group4.Childcare.DTO.InstitutionSimpleDTO;
import Group4.Childcare.Repository.InstitutionsJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InstitutionsJdbcRepository 單元測試
 * 測試機構管理相關的資料庫操作
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class InstitutionsJdbcRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private InstitutionsJdbcRepository repository;

  private UUID testInstitutionId;
  private Institutions testInstitution;

  @BeforeEach
  void setUp() {
    testInstitutionId = UUID.randomUUID();

    testInstitution = new Institutions();
    testInstitution.setInstitutionID(testInstitutionId);
    testInstitution.setInstitutionName("新竹縣測試托育機構");
    testInstitution.setContactPerson("張三");
    testInstitution.setPhoneNumber("03-1234567");
    testInstitution.setAddress("新竹縣竹北市測試路123號");
    testInstitution.setEmail("test@institution.com");
  }

  // ===== 測試 save (新增機構) =====
  @Test
  void testSave_Success() {
    // Given - UPDATE has 19 parameters (18 fields + WHERE InstitutionID)
    when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(1);

    // When
    Institutions result = repository.save(testInstitution);

    // Then
    assertNotNull(result);
    assertEquals(testInstitutionId, result.getInstitutionID());
    assertEquals("新竹縣測試托育機構", result.getInstitutionName());
  }

  @Test
  void testSave_CallsInsertWhenIdIsNull() {
    // Given - Set ID to null to trigger INSERT (19 parameters)
    testInstitution.setInstitutionID(null);

    // Mock update to return 1 (success) - INSERT also uses 19 parameters
    when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(1);

    // When
    Institutions result = repository.save(testInstitution);

    // Then
    assertNotNull(result);
    assertNotNull(result.getInstitutionID()); // ID should be auto-generated
    assertEquals("新竹縣測試托育機構", result.getInstitutionName());
    // The successful save with auto-generated ID proves INSERT was called
  }

  // ===== 測試 findById (根據ID查詢) =====
  @Test
  void testFindById_Success() {
    // Given - findById uses queryForObject, not query
    when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
        .thenReturn(testInstitution);

    // When
    Optional<Institutions> result = repository.findById(testInstitutionId);

    // Then
    assertTrue(result.isPresent());
    assertEquals(testInstitutionId, result.get().getInstitutionID());
    assertEquals("新竹縣測試托育機構", result.get().getInstitutionName());
    verify(jdbcTemplate, times(1)).queryForObject(anyString(), any(RowMapper.class), anyString());
  }

  @Test
  void testFindById_ReturnsEmpty_WhenNotFound() {
    // Given - queryForObject throws exception when no data found
    when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
        .thenThrow(new RuntimeException("No data found"));

    // When
    Optional<Institutions> result = repository.findById(UUID.randomUUID());

    // Then
    assertFalse(result.isPresent());
  }

  // ===== 測試 findAll (查詢所有機構) =====
  @Test
  void testFindAll_ReturnsAllInstitutions() {
    // Given
    Institutions institution2 = new Institutions();
    institution2.setInstitutionID(UUID.randomUUID());
    institution2.setInstitutionName("台北市測試機構");

    List<Institutions> mockList = Arrays.asList(testInstitution, institution2);
    when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
        .thenReturn(mockList);

    // When
    List<Institutions> result = repository.findAll();

    // Then
    assertNotNull(result);
    assertEquals(2, result.size());
    verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class));
  }

  // ===== 測試 findAllWithPagination (分頁查詢) =====
  @Test
  void testFindAllWithPagination_ReturnsCorrectPage() {
    // Given
    int offset = 0;
    int size = 10;
    List<Institutions> mockList = Collections.singletonList(testInstitution);
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(offset), eq(size)))
        .thenReturn(mockList);

    // When
    List<Institutions> result = repository.findAllWithPagination(offset, size);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class), eq(offset), eq(size));
  }

  // ===== 測試 findByInstitutionIDWithPagination (根據機構ID分頁查詢) =====
  @Test
  void testFindByInstitutionIDWithPagination_ReturnsCorrectPage() {
    // Given
    int offset = 0;
    int size = 10;
    List<Institutions> mockList = Collections.singletonList(testInstitution);
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), eq(offset), eq(size)))
        .thenReturn(mockList);

    // When
    List<Institutions> result = repository.findByInstitutionIDWithPagination(testInstitutionId, offset, size);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(testInstitutionId, result.get(0).getInstitutionID());
    verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class), anyString(), eq(offset), eq(size));
  }

  // ===== 測試 findAllWithSearchAndPagination (搜尋+分頁) =====
  @Test
  void testFindAllWithSearchAndPagination_ReturnsMatchingResults() {
    // Given
    String search = "測試";
    int offset = 0;
    int size = 10;
    List<Institutions> mockList = Collections.singletonList(testInstitution);
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyString(), anyString(), eq(offset),
        eq(size)))
        .thenReturn(mockList);

    // When
    List<Institutions> result = repository.findAllWithSearchAndPagination(search, offset, size);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class), anyString(), anyString(), anyString(),
        eq(offset), eq(size));
  }

  // ===== 測試 count (計算總數) =====
  @Test
  void testCount_ReturnsCorrectCount() {
    // Given
    Long expectedCount = 15L;
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
        .thenReturn(expectedCount);

    // When
    long result = repository.count();

    // Then
    assertEquals(expectedCount, result);
    verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Long.class));
  }

  // ===== 測試 countByInstitutionID (根據機構ID計數) =====
  @Test
  void testCountByInstitutionID_ReturnsCorrectCount() {
    // Given
    Long expectedCount = 1L;
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString()))
        .thenReturn(expectedCount);

    // When
    long result = repository.countByInstitutionID(testInstitutionId);

    // Then
    assertEquals(expectedCount, result);
    verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Long.class), anyString());
  }

  // ===== 測試 countAllWithSearch (搜尋計數) =====
  @Test
  void testCountAllWithSearch_ReturnsMatchingCount() {
    // Given
    String search = "測試";
    Long expectedCount = 5L;
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString(), anyString(), anyString()))
        .thenReturn(expectedCount);

    // When
    long result = repository.countAllWithSearch(search);

    // Then
    assertEquals(expectedCount, result);
    verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Long.class), anyString(), anyString(), anyString());
  }

  // ===== 測試 save (更新機構) =====
  @Test
  void testSave_UpdatesExisting() {
    // Given - UPDATE has 19 parameters (18 fields + WHERE InstitutionID)
    testInstitution.setInstitutionName("更新後的機構名稱");
    testInstitution.setContactPerson("李四");

    // Mock the update call - use lenient stubbing to avoid strict argument matching
    lenient().when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(1);

    // When
    Institutions result = repository.save(testInstitution);

    // Then
    assertNotNull(result);
    assertEquals("更新後的機構名稱", result.getInstitutionName());
    assertEquals("李四", result.getContactPerson());
    assertNotNull(result.getUpdatedTime()); // UpdatedTime should be auto-generated
  }

  // ===== 測試 deleteById (刪除機構) =====
  @Test
  void testDeleteById_Success() {
    // Given
    when(jdbcTemplate.update(anyString(), anyString()))
        .thenReturn(1);

    // When
    repository.deleteById(testInstitutionId);

    // Then
    verify(jdbcTemplate, times(1)).update(anyString(), eq(testInstitutionId.toString()));
  }

  // ===== 測試 existsById (檢查是否存在) =====
  @Test
  void testExistsById_ReturnsTrue_WhenExists() {
    // Given
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
        .thenReturn(1);

    // When
    boolean result = repository.existsById(testInstitutionId);

    // Then
    assertTrue(result);
  }

  @Test
  void testExistsById_ReturnsFalse_WhenNotExists() {
    // Given
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
        .thenReturn(0);

    // When
    boolean result = repository.existsById(UUID.randomUUID());

    // Then
    assertFalse(result);
  }

  // ===== 測試邊界情況 =====
  @Test
  void testFindAllWithPagination_WithZeroSize() {
    // Given
    int offset = 0;
    int size = 0;
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(offset), eq(size)))
        .thenReturn(Collections.emptyList());

    // When
    List<Institutions> result = repository.findAllWithPagination(offset, size);

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void testFindAllWithSearchAndPagination_WithEmptySearch() {
    // Given
    String search = "";
    int offset = 0;
    int size = 10;
    List<Institutions> mockList = Collections.singletonList(testInstitution);
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyString(), anyString(), eq(offset),
        eq(size)))
        .thenReturn(mockList);

    // When
    List<Institutions> result = repository.findAllWithSearchAndPagination(search, offset, size);

    // Then
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  void testSave_WithNullOptionalFields() {
    // Given
    Institutions institutionWithNulls = new Institutions();
    institutionWithNulls.setInstitutionID(testInstitutionId);
    institutionWithNulls.setInstitutionName("最小機構");
    institutionWithNulls.setContactPerson(null);
    institutionWithNulls.setPhoneNumber(null);
    institutionWithNulls.setEmail(null);

    // Mock update with 19 parameters (18 fields + WHERE InstitutionID)
    when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(),
        any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(1);

    // When
    Institutions result = repository.save(institutionWithNulls);

    // Then
    assertNotNull(result);
    assertEquals("最小機構", result.getInstitutionName());
    assertNull(result.getContactPerson());
  }

  // ==================== NEW TESTS - Additional Coverage ====================

  // ===== 測試 delete (實體刪除) =====
  @Test
  void testDelete_Success() {
    // Given
    when(jdbcTemplate.update(anyString(), anyString()))
        .thenReturn(1);

    // When
    repository.delete(testInstitution);

    // Then
    verify(jdbcTemplate, times(1)).update(anyString(), eq(testInstitutionId.toString()));
  }

  // ===== 測試 findAllActive (查詢啟用機構) =====
  @Test
  void testFindAllActive_ReturnsOnlyActiveInstitutions() {
    // Given
    testInstitution.setAccountStatus((byte) 1);
    List<Institutions> mockList = Collections.singletonList(testInstitution);
    when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
        .thenReturn(mockList);

    // When
    List<Institutions> result = repository.findAllActive();

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals((byte) 1, result.get(0).getAccountStatus());
    verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class));
  }

  @Test
  void testFindAllActive_ReturnsEmpty_WhenNoActiveInstitutions() {
    // Given
    when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
        .thenReturn(Collections.emptyList());

    // When
    List<Institutions> result = repository.findAllActive();

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  // ===== 測試 findSummaryData (簡要資料) =====
  @Test
  void testFindSummaryData_ReturnsInstitutionSummaries() {
    // Given
    List<InstitutionSummaryDTO> mockSummaries = new ArrayList<>();
    InstitutionSummaryDTO summary = new InstitutionSummaryDTO();
    summary.setInstitutionID(testInstitutionId);
    summary.setInstitutionName("測試機構");
    mockSummaries.add(summary);

    when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
        .thenReturn(mockSummaries);

    // When
    List<InstitutionSummaryDTO> result = repository.findSummaryData();

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(testInstitutionId, result.get(0).getInstitutionID());
    verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class));
  }

  // ===== 測試 findAllSimple (簡化資料：ID和名稱) =====
  @Test
  void testFindAllSimple_ReturnsSimpleDTOs() {
    // Given
    List<InstitutionSimpleDTO> mockSimpleList = new ArrayList<>();
    InstitutionSimpleDTO simpleDTO = new InstitutionSimpleDTO();
    simpleDTO.setInstitutionID(testInstitutionId);
    simpleDTO.setInstitutionName("測試機構");
    mockSimpleList.add(simpleDTO);

    when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
        .thenReturn(mockSimpleList);

    // When
    List<InstitutionSimpleDTO> result = repository.findAllSimple();

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(testInstitutionId, result.get(0).getInstitutionID());
    assertEquals("測試機構", result.get(0).getInstitutionName());
    verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class));
  }

  @Test
  void testFindAllSimple_ReturnsEmpty_WhenNoActiveInstitutions() {
    // Given
    when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
        .thenReturn(Collections.emptyList());

    // When
    List<InstitutionSimpleDTO> result = repository.findAllSimple();

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  // ==================== Search & Pagination Tests (Additional)
  // ====================

  @Test
  void testFindByInstitutionIDWithSearchAndPagination_Success() {
    // Given
    String search = "SearchTerm";
    int offset = 0;
    int limit = 10;
    List<Institutions> mockList = Collections.singletonList(testInstitution);
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyString(), anyString(), anyString(),
        eq(offset), eq(limit)))
        .thenReturn(mockList);

    // When
    List<Institutions> result = repository.findByInstitutionIDWithSearchAndPagination(testInstitutionId, search, offset,
        limit);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(jdbcTemplate).query(contains("WHERE InstitutionID = ?"), any(RowMapper.class),
        eq(testInstitutionId.toString()), eq("%SearchTerm%"), eq("%SearchTerm%"), eq("%SearchTerm%"), eq(offset),
        eq(limit));
  }

  @Test
  void testCountByInstitutionIDWithSearch_ReturnsCount() {
    // Given
    String search = "SearchTerm";
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(5L);

    // When
    long count = repository.countByInstitutionIDWithSearch(testInstitutionId, search);

    // Then
    assertEquals(5L, count);
    verify(jdbcTemplate).queryForObject(contains("WHERE InstitutionID = ?"), eq(Long.class),
        eq(testInstitutionId.toString()), eq("%SearchTerm%"), eq("%SearchTerm%"), eq("%SearchTerm%"));
  }

  @Test
  void testFindAllWithNameSearchAndPagination_Success() {
    // Given
    String name = "NameSearch";
    int offset = 0;
    int limit = 10;
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), eq(offset), eq(limit)))
        .thenReturn(Collections.singletonList(testInstitution));

    // When
    List<Institutions> result = repository.findAllWithNameSearchAndPagination(name, offset, limit);

    // Then
    assertNotNull(result);
    verify(jdbcTemplate).query(contains("WHERE InstitutionName LIKE ?"), any(RowMapper.class), eq("%NameSearch%"),
        eq(offset), eq(limit));
  }

  @Test
  void testCountAllWithNameSearch_ReturnsCount() {
    // Given
    String name = "NameSearch";
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString()))
        .thenReturn(8L);

    // When
    long count = repository.countAllWithNameSearch(name);

    // Then
    assertEquals(8L, count);
    verify(jdbcTemplate).queryForObject(contains("WHERE InstitutionName LIKE ?"), eq(Long.class), eq("%NameSearch%"));
  }

  @Test
  void testFindByInstitutionIDWithNameSearchAndPagination_Success() {
    // Given
    String name = "NameSearch";
    int offset = 0;
    int limit = 10;
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyString(), eq(offset), eq(limit)))
        .thenReturn(Collections.singletonList(testInstitution));

    // When
    List<Institutions> result = repository.findByInstitutionIDWithNameSearchAndPagination(testInstitutionId, name,
        offset, limit);

    // Then
    assertNotNull(result);
    verify(jdbcTemplate).query(contains("WHERE InstitutionID = ? AND InstitutionName LIKE ?"), any(RowMapper.class),
        eq(testInstitutionId.toString()), eq("%NameSearch%"), eq(offset), eq(limit));
  }

  @Test
  void testCountByInstitutionIDWithNameSearch_ReturnsCount() {
    // Given
    String name = "NameSearch";
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString(), anyString()))
        .thenReturn(3L);

    // When
    long count = repository.countByInstitutionIDWithNameSearch(testInstitutionId, name);

    // Then
    assertEquals(3L, count);
    verify(jdbcTemplate).queryForObject(contains("WHERE InstitutionID = ? AND InstitutionName LIKE ?"), eq(Long.class),
        eq(testInstitutionId.toString()), eq("%NameSearch%"));
  }

  // ==================== RowMapper Logic Tests ====================

  @Test
  void testInstitutionsRowMapper_Logic() throws java.sql.SQLException {
    // Given
    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
    when(rs.getString("InstitutionID")).thenReturn(testInstitutionId.toString());
    when(rs.getString("InstitutionName")).thenReturn("Mapper Test Inst");
    when(rs.getString("ContactPerson")).thenReturn("Contact");
    when(rs.getString("Address")).thenReturn("Addr");
    when(rs.getString("PhoneNumber")).thenReturn("123");
    when(rs.getString("Fax")).thenReturn("456");
    when(rs.getString("Email")).thenReturn("email@test.com");
    when(rs.getString("RelatedLinks")).thenReturn("links");
    when(rs.getString("Description")).thenReturn("desc");
    when(rs.getString("ResponsiblePerson")).thenReturn("Resp");
    when(rs.getString("ImagePath")).thenReturn("img.jpg");
    when(rs.getString("CreatedUser")).thenReturn("UserC");
    when(rs.getString("UpdatedUser")).thenReturn("UserU");

    // Mock Timestamps
    java.sql.Timestamp nowTs = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());
    when(rs.getTimestamp("CreatedTime")).thenReturn(nowTs);
    when(rs.getTimestamp("UpdatedTime")).thenReturn(nowTs);

    when(rs.getBigDecimal("Latitude")).thenReturn(java.math.BigDecimal.ONE);
    when(rs.getBigDecimal("Longitude")).thenReturn(java.math.BigDecimal.TEN);
    when(rs.getBoolean("InstitutionsType")).thenReturn(true);
    when(rs.getInt("AccountStatus")).thenReturn(1);

    // Capture the mapper
    org.mockito.ArgumentCaptor<RowMapper<Institutions>> mapperCaptor = org.mockito.ArgumentCaptor
        .forClass(RowMapper.class);
    when(jdbcTemplate.query(anyString(), mapperCaptor.capture())).thenReturn(Collections.emptyList());

    // Trigger capture (using findAll)
    repository.findAll();
    RowMapper<Institutions> mapper = mapperCaptor.getValue();

    // When
    Institutions result = mapper.mapRow(rs, 1);

    // Then
    assertNotNull(result);
    assertEquals(testInstitutionId, result.getInstitutionID());
    assertEquals("Mapper Test Inst", result.getInstitutionName());
    assertEquals("Contact", result.getContactPerson());
    assertNotNull(result.getCreatedTime());
    assertNotNull(result.getUpdatedTime());
    assertEquals(java.math.BigDecimal.ONE, result.getLatitude());
    assertTrue(result.getInstitutionsType());
    assertEquals(1, result.getAccountStatus());
  }

  @Test
  void testInstitutionsRowMapper_Logic_Nulls() throws java.sql.SQLException {
    // Given
    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
    when(rs.getString("InstitutionID")).thenReturn(testInstitutionId.toString());
    when(rs.getString("InstitutionName")).thenReturn("Null Test");

    // Null timestamps
    when(rs.getTimestamp("CreatedTime")).thenReturn(null);
    when(rs.getTimestamp("UpdatedTime")).thenReturn(null);

    // Other fields null
    when(rs.getString("ContactPerson")).thenReturn(null);
    when(rs.getBigDecimal("Latitude")).thenReturn(null);

    org.mockito.ArgumentCaptor<RowMapper<Institutions>> mapperCaptor = org.mockito.ArgumentCaptor
        .forClass(RowMapper.class);
    when(jdbcTemplate.query(anyString(), mapperCaptor.capture())).thenReturn(Collections.emptyList());

    repository.findAll();
    RowMapper<Institutions> mapper = mapperCaptor.getValue();

    // When
    Institutions result = mapper.mapRow(rs, 1);

    // Then
    assertNotNull(result);
    assertEquals("Null Test", result.getInstitutionName());
    assertNull(result.getCreatedTime());
    assertNull(result.getUpdatedTime());
    assertNull(result.getContactPerson());
    assertNull(result.getLatitude());
  }

  @Test
  void testSummaryRowMapper_Logic() throws java.sql.SQLException {
    // Given
    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
    when(rs.getString("InstitutionID")).thenReturn(testInstitutionId.toString());
    when(rs.getString("InstitutionName")).thenReturn("Summary Test");
    when(rs.getString("Address")).thenReturn("Summary Addr");
    when(rs.getString("PhoneNumber")).thenReturn("999");

    org.mockito.ArgumentCaptor<RowMapper<InstitutionSummaryDTO>> mapperCaptor = org.mockito.ArgumentCaptor
        .forClass(RowMapper.class);
    when(jdbcTemplate.query(anyString(), mapperCaptor.capture())).thenReturn(Collections.emptyList());

    repository.findSummaryData();
    RowMapper<InstitutionSummaryDTO> mapper = mapperCaptor.getValue();

    // When
    InstitutionSummaryDTO result = mapper.mapRow(rs, 1);

    // Then
    assertNotNull(result);
    assertEquals(testInstitutionId, result.getInstitutionID());
    assertEquals("Summary Test", result.getInstitutionName());
    assertEquals("999", result.getPhoneNumber());
  }
}
