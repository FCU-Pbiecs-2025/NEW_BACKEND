package Group4.Childcare.repository;

import Group4.Childcare.Model.Announcements;
import Group4.Childcare.Repository.AnnouncementsJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AnnouncementsJdbcRepository 分支覆蓋率測試
 * 專門測試 RowMapper 和分支邏輯以提升覆蓋率到 90% 以上
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AnnouncementsJdbcRepositoryCoverageTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AnnouncementsJdbcRepository repository;

    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
    }

    // ===========================================================================================
    // 1. ANNOUNCEMENTS_ROW_MAPPER 分支測試 (10個分支)
    // ===========================================================================================

    @Test
    void testAnnouncementsRowMapper_AllFieldsNonNull() throws SQLException {
        // 捕獲 RowMapper 並測試所有分支
        ArgumentCaptor<RowMapper<Announcements>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);

        when(jdbcTemplate.query(anyString(), mapperCaptor.capture()))
            .thenReturn(Collections.emptyList());

        repository.findAll();

        RowMapper<Announcements> mapper = mapperCaptor.getValue();

        // Mock ResultSet with all fields non-null
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("AnnouncementID")).thenReturn(testId.toString());
        when(rs.getString("Title")).thenReturn("Test Title");
        when(rs.getString("Content")).thenReturn("Test Content");
        when(rs.getByte("Type")).thenReturn((byte) 1);
        when(rs.getDate("StartDate")).thenReturn(java.sql.Date.valueOf(LocalDate.now()));
        when(rs.getDate("EndDate")).thenReturn(java.sql.Date.valueOf(LocalDate.now().plusDays(7)));
        when(rs.getByte("Status")).thenReturn((byte) 1);
        when(rs.getString("CreatedUser")).thenReturn("admin");
        when(rs.getTimestamp("CreatedTime")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
        when(rs.getString("UpdatedUser")).thenReturn("admin");
        when(rs.getTimestamp("UpdatedTime")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
        when(rs.getString("AttachmentPath")).thenReturn("/path/to/file.pdf");

        // When
        Announcements result = mapper.mapRow(rs, 1);

        // Then - 驗證所有 if (field != null) 分支都被執行
        assertNotNull(result);
        assertEquals(testId, result.getAnnouncementID());
        assertEquals("Test Title", result.getTitle());
        assertEquals("Test Content", result.getContent());
        assertNotNull(result.getStartDate());
        assertNotNull(result.getEndDate());
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdatedTime());
        assertEquals("/path/to/file.pdf", result.getAttachmentPath());
    }

    @Test
    void testAnnouncementsRowMapper_AllNullableFieldsNull() throws SQLException {
        // 測試所有可為 null 的字段都是 null 的情況
        ArgumentCaptor<RowMapper<Announcements>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);

        when(jdbcTemplate.query(anyString(), mapperCaptor.capture()))
            .thenReturn(Collections.emptyList());

        repository.findAll();

        RowMapper<Announcements> mapper = mapperCaptor.getValue();

        // Mock ResultSet with nullable fields as null
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("AnnouncementID")).thenReturn(null);  // null branch
        when(rs.getString("Title")).thenReturn("Title");
        when(rs.getString("Content")).thenReturn("Content");
        when(rs.getByte("Type")).thenReturn((byte) 1);
        when(rs.getDate("StartDate")).thenReturn(null);  // null branch
        when(rs.getDate("EndDate")).thenReturn(null);  // null branch
        when(rs.getByte("Status")).thenReturn((byte) 1);
        when(rs.getString("CreatedUser")).thenReturn("user");
        when(rs.getTimestamp("CreatedTime")).thenReturn(null);  // null branch
        when(rs.getString("UpdatedUser")).thenReturn("user");
        when(rs.getTimestamp("UpdatedTime")).thenReturn(null);  // null branch
        when(rs.getString("AttachmentPath")).thenReturn(null);

        // When
        Announcements result = mapper.mapRow(rs, 1);

        // Then - 驗證所有 null 分支都被執行
        assertNotNull(result);
        assertNull(result.getAnnouncementID());  // null branch covered
        assertNull(result.getStartDate());  // null branch covered
        assertNull(result.getEndDate());  // null branch covered
        assertNull(result.getCreatedTime());  // null branch covered
        assertNull(result.getUpdatedTime());  // null branch covered
    }

    @Test
    void testAnnouncementsRowMapper_MixedNullFields() throws SQLException {
        // 測試混合場景（部分 null，部分非 null）
        ArgumentCaptor<RowMapper<Announcements>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);

        when(jdbcTemplate.query(anyString(), mapperCaptor.capture()))
            .thenReturn(Collections.emptyList());

        repository.findAll();

        RowMapper<Announcements> mapper = mapperCaptor.getValue();

        // Mock ResultSet with mixed null/non-null
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("AnnouncementID")).thenReturn(testId.toString());  // non-null
        when(rs.getString("Title")).thenReturn("Title");
        when(rs.getString("Content")).thenReturn("Content");
        when(rs.getByte("Type")).thenReturn((byte) 1);
        when(rs.getDate("StartDate")).thenReturn(java.sql.Date.valueOf(LocalDate.now()));  // non-null
        when(rs.getDate("EndDate")).thenReturn(null);  // null
        when(rs.getByte("Status")).thenReturn((byte) 1);
        when(rs.getString("CreatedUser")).thenReturn("user");
        when(rs.getTimestamp("CreatedTime")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));  // non-null
        when(rs.getString("UpdatedUser")).thenReturn("user");
        when(rs.getTimestamp("UpdatedTime")).thenReturn(null);  // null
        when(rs.getString("AttachmentPath")).thenReturn(null);

        // When
        Announcements result = mapper.mapRow(rs, 1);

        // Then
        assertNotNull(result);
        assertNotNull(result.getAnnouncementID());
        assertNotNull(result.getStartDate());
        assertNull(result.getEndDate());
        assertNotNull(result.getCreatedTime());
        assertNull(result.getUpdatedTime());
    }

    // ===========================================================================================
    // 2. SUMMARY_ROW_MAPPER 分支測試 (4個分支)
    // ===========================================================================================

    @Test
    void testSummaryRowMapper_AllFieldsNonNull() throws SQLException {
        // 捕獲 SUMMARY_ROW_MAPPER
        ArgumentCaptor<RowMapper<?>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);

        when(jdbcTemplate.query(anyString(), mapperCaptor.capture()))
            .thenReturn(Collections.emptyList());

        repository.findSummaryData();

        RowMapper<?> mapper = mapperCaptor.getValue();

        // Mock ResultSet
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("AnnouncementID")).thenReturn(testId.toString());
        when(rs.getString("Title")).thenReturn("Title");
        when(rs.getString("Content")).thenReturn("Content");
        when(rs.getDate("StartDate")).thenReturn(java.sql.Date.valueOf(LocalDate.now()));  // non-null
        when(rs.getString("AttachmentPath")).thenReturn("/path");
        when(rs.getObject("Type")).thenReturn((byte) 1);  // non-null
        when(rs.getByte("Type")).thenReturn((byte) 1);

        // When
        Object result = mapper.mapRow(rs, 1);

        // Then
        assertNotNull(result);
    }

    @Test
    void testSummaryRowMapper_NullableFieldsNull() throws SQLException {
        // 測試 StartDate 為 null 和 Type 為 null
        ArgumentCaptor<RowMapper<?>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);

        when(jdbcTemplate.query(anyString(), mapperCaptor.capture()))
            .thenReturn(Collections.emptyList());

        repository.findSummaryData();

        RowMapper<?> mapper = mapperCaptor.getValue();

        // Mock ResultSet
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("AnnouncementID")).thenReturn(testId.toString());
        when(rs.getString("Title")).thenReturn("Title");
        when(rs.getString("Content")).thenReturn("Content");
        when(rs.getDate("StartDate")).thenReturn(null);  // null branch
        when(rs.getString("AttachmentPath")).thenReturn(null);
        when(rs.getObject("Type")).thenReturn(null);  // null branch

        // When
        Object result = mapper.mapRow(rs, 1);

        // Then
        assertNotNull(result);
    }

    // ===========================================================================================
    // 3. save() 方法分支測試 (2個分支)
    // ===========================================================================================

    @Test
    void testSave_WithNullId_CallsInsert() {
        // 測試 if (announcement.getAnnouncementID() == null) 分支
        Announcements announcement = new Announcements();
        announcement.setAnnouncementID(null);  // null ID
        announcement.setTitle("Test");
        announcement.setContent("Content");
        announcement.setType((byte) 1);
        announcement.setStatus((byte) 1);

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(1);

        // When
        Announcements result = repository.save(announcement);

        // Then
        assertNotNull(result.getAnnouncementID());  // UUID 被自動生成
        verify(jdbcTemplate).update(contains("INSERT"), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testSave_WithExistingId_CallsUpdate() {
        // 測試 else 分支（ID 不為 null）
        Announcements announcement = new Announcements();
        announcement.setAnnouncementID(testId);  // existing ID
        announcement.setTitle("Test");
        announcement.setContent("Content");
        announcement.setType((byte) 1);
        announcement.setStatus((byte) 1);

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(1);

        // When
        Announcements result = repository.save(announcement);

        // Then
        assertEquals(testId, result.getAnnouncementID());
        verify(jdbcTemplate).update(contains("UPDATE"), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ===========================================================================================
    // 4. insert() / insertWithAttachment() / update() 三元運算符測試 (6個分支)
    // ===========================================================================================

    @Test
    void testInsert_WithNonNullId() {
        // 測試 announcement.getAnnouncementID() != null ? ... : null (true 分支)
        Announcements announcement = new Announcements();
        announcement.setAnnouncementID(testId);  // non-null
        announcement.setTitle("Test");
        announcement.setContent("Content");
        announcement.setType((byte) 1);
        announcement.setStatus((byte) 1);

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(1);

        // When
        repository.save(announcement);

        // Then - verify the ID was converted to string
        verify(jdbcTemplate).update(anyString(), eq(testId.toString()), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testInsertWithAttachment_WithNullId() {
        // 測試三元運算符的 null 分支
        Announcements announcement = new Announcements();
        announcement.setAnnouncementID(null);  // null
        announcement.setTitle("Test");
        announcement.setContent("Content");
        announcement.setType((byte) 1);
        announcement.setStatus((byte) 1);
        announcement.setAttachmentPath("/path");

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(1);

        // When
        repository.insertWithAttachment(announcement);

        // Then - verify null was passed (or converted UUID)
        verify(jdbcTemplate).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testUpdate_WithNonNullId() {
        // 測試 update 方法的三元運算符 (ID 不為 null)
        Announcements announcement = new Announcements();
        announcement.setAnnouncementID(testId);
        announcement.setTitle("Updated");
        announcement.setContent("Content");
        announcement.setType((byte) 1);
        announcement.setStatus((byte) 1);

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(1);

        // When
        repository.save(announcement);

        // Then
        verify(jdbcTemplate).update(contains("UPDATE"), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(testId.toString()));
    }

    // ===========================================================================================
    // 5. existsById() 分支測試 (4個分支)
    // ===========================================================================================

    @Test
    void testExistsById_CountIsNull() {
        // 測試 count != null (false 分支) && count > 0
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
            .thenReturn(null);

        // When
        boolean result = repository.existsById(testId);

        // Then
        assertFalse(result);  // count == null -> false
    }

    @Test
    void testExistsById_CountIsZero() {
        // 測試 count != null (true) && count > 0 (false)
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
            .thenReturn(0);

        // When
        boolean result = repository.existsById(testId);

        // Then
        assertFalse(result);  // count == 0 -> false
    }

    @Test
    void testExistsById_CountIsPositive() {
        // 測試 count != null (true) && count > 0 (true)
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
            .thenReturn(1);

        // When
        boolean result = repository.existsById(testId);

        // Then
        assertTrue(result);  // count > 0 -> true
    }

    @Test
    void testExistsById_CountIsNegative() {
        // 測試 count < 0 的邊界情況
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
            .thenReturn(-1);

        // When
        boolean result = repository.existsById(testId);

        // Then
        assertFalse(result);  // count <= 0 -> false
    }

    // ===========================================================================================
    // 6. count() 分支測試 (2個分支)
    // ===========================================================================================

    @Test
    void testCount_ReturnsNull() {
        // 測試 count != null ? count : 0 (false 分支)
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(null);

        // When
        long result = repository.count();

        // Then
        assertEquals(0L, result);  // null -> 0
    }

    @Test
    void testCount_ReturnsNonNull() {
        // 測試 count != null ? count : 0 (true 分支)
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(10L);

        // When
        long result = repository.count();

        // Then
        assertEquals(10L, result);
    }

    // ===========================================================================================
    // 7. countTotal() 分支測試 (2個分支)
    // ===========================================================================================

    @Test
    void testCountTotal_ReturnsNull() {
        // 測試 count != null ? count : 0 (false 分支)
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(null);

        // When
        long result = repository.countTotal();

        // Then
        assertEquals(0L, result);  // null -> 0
    }

    @Test
    void testCountTotal_ReturnsNonNull() {
        // 測試 count != null ? count : 0 (true 分支)
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(5L);

        // When
        long result = repository.countTotal();

        // Then
        assertEquals(5L, result);
    }

    // ===========================================================================================
    // 8. findById() 異常處理測試
    // ===========================================================================================

    @Test
    void testFindById_ThrowsException_ReturnsEmpty() {
        // 測試 try-catch 的 catch 分支
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
            .thenThrow(new RuntimeException("Not found"));

        // When
        Optional<Announcements> result = repository.findById(testId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testFindById_Success_ReturnsPresent() {
        // 測試 try-catch 的 try 分支成功
        Announcements announcement = new Announcements();
        announcement.setAnnouncementID(testId);
        announcement.setTitle("Found");

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
            .thenReturn(announcement);

        // When
        Optional<Announcements> result = repository.findById(testId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testId, result.get().getAnnouncementID());
    }

    // ===========================================================================================
    // 9. 其他方法覆蓋率測試
    // ===========================================================================================

    @Test
    void testFindAll() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Collections.emptyList());

        List<Announcements> result = repository.findAll();

        assertNotNull(result);
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class));
    }

    @Test
    void testDeleteById() {
        when(jdbcTemplate.update(anyString(), anyString()))
            .thenReturn(1);

        repository.deleteById(testId);

        verify(jdbcTemplate).update(contains("DELETE"), eq(testId.toString()));
    }

    @Test
    void testDelete() {
        Announcements announcement = new Announcements();
        announcement.setAnnouncementID(testId);

        when(jdbcTemplate.update(anyString(), anyString()))
            .thenReturn(1);

        repository.delete(announcement);

        verify(jdbcTemplate).update(contains("DELETE"), eq(testId.toString()));
    }

    @Test
    void testFindWithOffset() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Collections.emptyList());

        List<Announcements> result = repository.findWithOffset(0, 10);

        assertNotNull(result);
        verify(jdbcTemplate).query(contains("OFFSET"), any(RowMapper.class));
    }

    @Test
    void testFindfrontSummaryData() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Collections.emptyList());

        repository.findfrontSummaryData();

        verify(jdbcTemplate).query(contains("Type = 1"), any(RowMapper.class));
    }

    @Test
    void testFindAdminActiveRaw() {
        when(jdbcTemplate.queryForList(anyString()))
            .thenReturn(Collections.emptyList());

        repository.findAdminActiveRaw();

        verify(jdbcTemplate).queryForList(contains("Type = 2"));
    }

    @Test
    void testFindAdminActiveSummaries() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Collections.emptyList());

        repository.findAdminActiveSummaries();

        verify(jdbcTemplate).query(contains("order by StartDate"), any(RowMapper.class));
    }

    @Test
    void testFindAdminActiveBackend() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
            .thenReturn(Collections.emptyList());

        repository.findAdminActiveBackend();

        verify(jdbcTemplate).query(contains("Type = 2"), any(RowMapper.class), any(java.sql.Date.class));
    }
}

