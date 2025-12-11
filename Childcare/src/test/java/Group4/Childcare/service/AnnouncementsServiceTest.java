package Group4.Childcare.service;

import Group4.Childcare.DTO.AnnouncementSummaryDTO;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AnnouncementsService 單元測試
 *
 * 測試範圍：
 * 1. create() - 創建公告
 * 2. getById() - 根據ID查詢公告
 * 3. getAll() - 查詢所有公告
 * 4. update() - 更新公告
 * 5. getSummaryAll() - 查詢所有公告摘要
 * 6. getAdminActiveBackend() - 查詢後台活動公告
 * 7. getAnnouncementsWithOffsetJdbc() - 分頁查詢公告
 * 8. getTotalCount() - 取得公告總數
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementsServiceTest {

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
        testAnnouncement.setType((byte) 1);
        testAnnouncement.setStatus((byte) 1);
        testAnnouncement.setStartDate(LocalDate.now());
        testAnnouncement.setEndDate(LocalDate.now().plusDays(7));
    }

    @Test
    void testCreate_Success() {
        // Given
        when(jdbcRepository.save(any(Announcements.class))).thenReturn(testAnnouncement);

        // When
        Announcements result = service.create(testAnnouncement);

        // Then
        assertNotNull(result);
        assertEquals(testAnnouncementId, result.getAnnouncementID());
        assertEquals("測試公告", result.getTitle());
        assertEquals("測試內容", result.getContent());
        verify(jdbcRepository, times(1)).save(testAnnouncement);
    }

    @Test
    void testGetById_Success() {
        // Given
        when(jdbcRepository.findById(testAnnouncementId)).thenReturn(Optional.of(testAnnouncement));

        // When
        Optional<Announcements> result = service.getById(testAnnouncementId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testAnnouncementId, result.get().getAnnouncementID());
        assertEquals("測試公告", result.get().getTitle());
        verify(jdbcRepository, times(1)).findById(testAnnouncementId);
    }

    @Test
    void testGetById_NotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(jdbcRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Optional<Announcements> result = service.getById(nonExistentId);

        // Then
        assertFalse(result.isPresent());
        verify(jdbcRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void testGetAll_Success() {
        // Given
        List<Announcements> announcementList = Arrays.asList(testAnnouncement);
        when(jdbcRepository.findAll()).thenReturn(announcementList);

        // When
        List<Announcements> result = service.getAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAnnouncementId, result.get(0).getAnnouncementID());
        verify(jdbcRepository, times(1)).findAll();
    }

    @Test
    void testUpdate_Success() {
        // Given
        testAnnouncement.setTitle("更新後的標題");
        when(jdbcRepository.save(any(Announcements.class))).thenReturn(testAnnouncement);

        // When
        Announcements result = service.update(testAnnouncementId, testAnnouncement);

        // Then
        assertNotNull(result);
        assertEquals(testAnnouncementId, result.getAnnouncementID());
        assertEquals("更新後的標題", result.getTitle());
        verify(jdbcRepository, times(1)).save(testAnnouncement);
    }

    @Test
    void testGetSummaryAll_Success() {
        // Given
        AnnouncementSummaryDTO summaryDTO = new AnnouncementSummaryDTO(
                testAnnouncementId,
                "測試公告",
                "測試內容",
                LocalDate.now(),
                "/attachments/test.pdf",
                (byte) 1
        );
        List<AnnouncementSummaryDTO> summaryList = Arrays.asList(summaryDTO);
        when(jdbcRepository.findSummaryData()).thenReturn(summaryList);

        // When
        List<AnnouncementSummaryDTO> result = service.getSummaryAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("測試公告", result.get(0).getTitle());
        verify(jdbcRepository, times(1)).findSummaryData();
    }

    @Test
    void testGetAdminActiveBackend_Success() {
        // Given
        AnnouncementSummaryDTO summaryDTO = new AnnouncementSummaryDTO(
                testAnnouncementId,
                "活動公告",
                "活動內容",
                LocalDate.now(),
                "/attachments/activity.pdf",
                (byte) 2
        );
        List<AnnouncementSummaryDTO> activeList = Arrays.asList(summaryDTO);
        when(jdbcRepository.findAdminActiveBackend()).thenReturn(activeList);

        // When
        List<AnnouncementSummaryDTO> result = service.getAdminActiveBackend();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("活動公告", result.get(0).getTitle());
        assertEquals((byte) 2, result.get(0).getType());
        verify(jdbcRepository, times(1)).findAdminActiveBackend();
    }

    @Test
    void testGetAnnouncementsWithOffsetJdbc_Success() {
        // Given
        int offset = 0;
        int pageSize = 8;
        List<Announcements> announcementList = Arrays.asList(testAnnouncement);
        when(jdbcRepository.findWithOffset(offset, pageSize)).thenReturn(announcementList);

        // When
        List<Announcements> result = service.getAnnouncementsWithOffsetJdbc(offset);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAnnouncementId, result.get(0).getAnnouncementID());
        verify(jdbcRepository, times(1)).findWithOffset(offset, pageSize);
    }

    @Test
    void testGetTotalCount_Success() {
        // Given
        long expectedCount = 25L;
        when(jdbcRepository.countTotal()).thenReturn(expectedCount);

        // When
        long result = service.getTotalCount();

        // Then
        assertEquals(expectedCount, result);
        verify(jdbcRepository, times(1)).countTotal();
    }

    @Test
    void testGetAnnouncementsWithOffsetJdbc_EmptyResult() {
        // Given
        int offset = 100;
        when(jdbcRepository.findWithOffset(eq(offset), eq(8))).thenReturn(Arrays.asList());

        // When
        List<Announcements> result = service.getAnnouncementsWithOffsetJdbc(offset);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(jdbcRepository, times(1)).findWithOffset(offset, 8);
    }
}

