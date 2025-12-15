package Group4.Childcare.repository;

import Group4.Childcare.Model.Announcements;
import Group4.Childcare.Repository.AnnouncementsJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AnnouncementsJdbcRepository 單元測試
 * 測試公告管理相關的資料庫操作
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AnnouncementsJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AnnouncementsJdbcRepository announcementsRepository;

    private UUID testAnnouncementId;

    @BeforeEach
    void setUp() {
        testAnnouncementId = UUID.randomUUID();
    }

    // ==================== save Tests ====================

    @Test
    void testSave_NewAnnouncement_Success() {
        // Given
        Announcements announcement = createTestAnnouncement();
        announcement.setAnnouncementID(null);

        when(jdbcTemplate.update(anyString(), any(), anyString(), anyString(), anyByte(),
                any(LocalDateTime.class), any(LocalDateTime.class), anyString()))
            .thenReturn(1);

        // When
        Announcements result = announcementsRepository.save(announcement);

        // Then
        assertNotNull(result);
        assertNotNull(result.getAnnouncementID());
    }

    @Test
    void testSave_ExistingAnnouncement_Update_Success() {
        // Given
        Announcements announcement = createTestAnnouncement();
        announcement.setAnnouncementID(testAnnouncementId);

        when(jdbcTemplate.update(anyString(), anyString(), anyString(), anyByte(),
                any(LocalDateTime.class), any(LocalDateTime.class), anyString(), any()))
            .thenReturn(1);

        // When
        Announcements result = announcementsRepository.save(announcement);

        // Then
        assertNotNull(result);
        assertEquals(testAnnouncementId, result.getAnnouncementID());
    }

    // ==================== findById Tests ====================

    @Test
    void testFindById_Success() {
        // Given
        Announcements mockAnnouncement = createTestAnnouncement();
        mockAnnouncement.setAnnouncementID(testAnnouncementId);

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(testAnnouncementId.toString())))
            .thenReturn(mockAnnouncement);

        // When
        Optional<Announcements> result = announcementsRepository.findById(testAnnouncementId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testAnnouncementId, result.get().getAnnouncementID());
        assertEquals("重要公告", result.get().getTitle());
    }

    @Test
    void testFindById_NotFound() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
            .thenThrow(new RuntimeException("Not found"));

        // When
        Optional<Announcements> result = announcementsRepository.findById(testAnnouncementId);

        // Then
        assertFalse(result.isPresent());
    }

    // ==================== findAll Tests ====================

    @Test
    void testFindAll_Success() {
        // Given
        Announcements announcement1 = createTestAnnouncement();
        announcement1.setAnnouncementID(UUID.randomUUID());
        announcement1.setTitle("公告一");

        Announcements announcement2 = createTestAnnouncement();
        announcement2.setAnnouncementID(UUID.randomUUID());
        announcement2.setTitle("公告二");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Arrays.asList(announcement1, announcement2));

        // When
        List<Announcements> result = announcementsRepository.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("公告一", result.get(0).getTitle());
        assertEquals("公告二", result.get(1).getTitle());
    }

    @Test
    void testFindAll_EmptyResult() {
        // Given
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Collections.emptyList());

        // When
        List<Announcements> result = announcementsRepository.findAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== count Tests ====================

    @Test
    void testCount_Success() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(10L);

        // When
        long count = announcementsRepository.count();

        // Then
        assertEquals(10L, count);
    }

    @Test
    void testCount_Zero() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(0L);

        // When
        long count = announcementsRepository.count();

        // Then
        assertEquals(0L, count);
    }

    // ==================== existsById Tests ====================

    @Test
    void testExistsById_True() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testAnnouncementId.toString())))
            .thenReturn(1);

        // When
        boolean exists = announcementsRepository.existsById(testAnnouncementId);

        // Then
        assertTrue(exists);
    }

    @Test
    void testExistsById_False() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testAnnouncementId.toString())))
            .thenReturn(0);

        // When
        boolean exists = announcementsRepository.existsById(testAnnouncementId);

        // Then
        assertFalse(exists);
    }

    // ==================== deleteById Tests ====================

    @Test
    void testDeleteById_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), eq(testAnnouncementId.toString())))
            .thenReturn(1);

        // When
        announcementsRepository.deleteById(testAnnouncementId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testAnnouncementId.toString()));
    }

    // ==================== Helper Methods ====================

    private Announcements createTestAnnouncement() {
        Announcements announcement = new Announcements();
        announcement.setAnnouncementID(testAnnouncementId);
        announcement.setTitle("重要公告");
        announcement.setContent("這是一則重要公告內容");
        announcement.setType((byte) 1);
        announcement.setAttachmentPath("/uploads/announcement.pdf");
        return announcement;
    }
}

