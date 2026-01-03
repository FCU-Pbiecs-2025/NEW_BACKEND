package Group4.Childcare.service;

import Group4.Childcare.Model.Announcements;
import Group4.Childcare.Repository.AnnouncementsJdbcRepository;
import Group4.Childcare.Service.AnnouncementsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementsServiceExpandedTest {

    @Mock
    private AnnouncementsJdbcRepository jdbcRepository;

    @InjectMocks
    private AnnouncementsService service;

    private Announcements testAnnouncement;
    private UUID testAnnouncementId;

    @BeforeEach
    void setUp() {
        testAnnouncementId = UUID.randomUUID();
        testAnnouncement = new Announcements();
        testAnnouncement.setAnnouncementID(testAnnouncementId);
        testAnnouncement.setTitle("測試公告");
        testAnnouncement.setContent("測試內容");
    }

    // ========== createAnnouncementJdbc Tests ==========

    @Test
    void testCreateAnnouncementJdbc_WithId() {
        // Given
        when(jdbcRepository.insertWithAttachment(any(Announcements.class))).thenReturn(testAnnouncement);

        // When
        Announcements result = service.createAnnouncementJdbc(testAnnouncement);

        // Then
        assertNotNull(result);
        assertEquals(testAnnouncementId, result.getAnnouncementID());
        verify(jdbcRepository).insertWithAttachment(testAnnouncement);
    }

    @Test
    void testCreateAnnouncementJdbc_WithoutId() {
        // Given
        testAnnouncement.setAnnouncementID(null);
        when(jdbcRepository.insertWithAttachment(any(Announcements.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Announcements result = service.createAnnouncementJdbc(testAnnouncement);

        // Then
        assertNotNull(result);
        assertNotNull(result.getAnnouncementID());
        verify(jdbcRepository).insertWithAttachment(any(Announcements.class));
    }

    // ========== delete Tests ==========

    @Test
    void testDelete_Success() {
        // Given
        when(jdbcRepository.existsById(testAnnouncementId)).thenReturn(true);
        doNothing().when(jdbcRepository).deleteById(testAnnouncementId);

        // When
        boolean result = service.delete(testAnnouncementId);

        // Then
        assertTrue(result);
        verify(jdbcRepository).existsById(testAnnouncementId);
        verify(jdbcRepository).deleteById(testAnnouncementId);
    }

    @Test
    void testDelete_NotFound() {
        // Given
        when(jdbcRepository.existsById(testAnnouncementId)).thenReturn(false);

        // When
        boolean result = service.delete(testAnnouncementId);

        // Then
        assertFalse(result);
        verify(jdbcRepository).existsById(testAnnouncementId);
        verify(jdbcRepository, never()).deleteById(any());
    }

    @Test
    void testDelete_Exception() {
        // Given
        when(jdbcRepository.existsById(testAnnouncementId)).thenReturn(true);
        doThrow(new RuntimeException("Database error")).when(jdbcRepository).deleteById(testAnnouncementId);

        // When
        boolean result = service.delete(testAnnouncementId);

        // Then
        assertFalse(result);
        verify(jdbcRepository).existsById(testAnnouncementId);
        verify(jdbcRepository).deleteById(testAnnouncementId);
    }

    // ========== Additional Tests for other methods to ensure overall coverage ==========

    @Test
    void testUpdateWithJdbc() {
        // Given
        when(jdbcRepository.save(any(Announcements.class))).thenReturn(testAnnouncement);

        // When
        Announcements result = service.updateWithJdbc(testAnnouncementId, testAnnouncement);

        // Then
        assertNotNull(result);
        assertEquals(testAnnouncementId, result.getAnnouncementID());
        verify(jdbcRepository).save(testAnnouncement);
    }

    @Test
    void testGetfrontSummaryAll() {
        // Given
        when(jdbcRepository.findfrontSummaryData()).thenReturn(java.util.Collections.emptyList());

        // When
        service.getfrontSummaryAll();

        // Then
        verify(jdbcRepository).findfrontSummaryData();
    }
}
