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

import java.time.LocalDate;
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

        // ==================== NEW COVERAGE TESTS ====================

        @Test
        void testInsertWithAttachment_Success() {
                // Given
                Announcements announcement = createTestAnnouncement();
                announcement.setAnnouncementID(null); // Force insert

                when(jdbcTemplate.update(anyString(), any(), anyString(), anyString(), anyByte(),
                                any(), any(), any(), any(), any(), any(), any(), anyString())) // 12 args
                                .thenReturn(1);

                // When
                Announcements result = announcementsRepository.insertWithAttachment(announcement);

                // Then
                assertNotNull(result);
                verify(jdbcTemplate).update(anyString(), any(), anyString(), anyString(), anyByte(),
                                any(), any(), any(), any(), any(), any(), any(), anyString());
        }

        @Test
        void testFindSummaryData() {
                // Given
                when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                                .thenReturn(Collections.emptyList());

                // When
                announcementsRepository.findSummaryData();

                // Then
                verify(jdbcTemplate).query((String) contains(
                                "SELECT AnnouncementID, Title, Content, StartDate, AttachmentPath, Type"),
                                any(RowMapper.class));
        }

        @Test
        void testFindfrontSummaryData() {
                // Given
                when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                                .thenReturn(Collections.emptyList());

                // When
                announcementsRepository.findfrontSummaryData();

                // Then
                verify(jdbcTemplate).query((String) contains("WHERE Type = 1 AND Status = 1"), any(RowMapper.class));
        }

        @Test
        void testFindWithOffset() {
                // Given
                when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                                .thenReturn(Collections.emptyList());

                // When
                announcementsRepository.findWithOffset(0, 10);

                // Then
                // Check offset/limit in SQL string (simple check)
                verify(jdbcTemplate).query((String) argThat(
                                sql -> ((String) sql).contains("OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY")),
                                any(RowMapper.class));
        }

        @Test
        void testCountTotal() {
                // Given
                when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                                .thenReturn(5L);

                // When
                long count = announcementsRepository.countTotal();

                // Then
                assertEquals(5L, count);
                verify(jdbcTemplate).queryForObject((String) contains("WHERE Type = 1 AND Status = 1"), eq(Long.class));
        }

        @Test
        void testFindAdminActiveRaw() {
                // Given
                when(jdbcTemplate.queryForList(anyString()))
                                .thenReturn(Collections.emptyList());

                // When
                announcementsRepository.findAdminActiveRaw();

                // Then
                verify(jdbcTemplate).queryForList((String) contains("WHERE Type = 2"));
        }

        @Test
        void testFindAdminActiveSummaries() {
                // Given
                when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                                .thenReturn(Collections.emptyList());

                // When
                announcementsRepository.findAdminActiveSummaries();

                // Then
                // Ambiguous call fix: Use specialized matcher or cast
                // verify(jdbcTemplate).query(sql, rowMapper) -> ambiguous between (String,
                // RowMapper) and (PreparedStatementCreator, RowMapper)
                // because `contains` returns string matcher which matches both? No.
                // It's likely because verifies are tricky with overloaded methods.
                // Let's use explicit verify arguments.
                verify(jdbcTemplate).query((String) contains("order by StartDate DESC"), any(RowMapper.class));
        }

        @Test
        void testRowMapperLogic() throws java.sql.SQLException {
                // Given
                java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                when(rs.getString("AnnouncementID")).thenReturn(testAnnouncementId.toString());
                when(rs.getString("Title")).thenReturn("Title");
                when(rs.getString("Content")).thenReturn("Content");
                when(rs.getByte("Type")).thenReturn((byte) 1);
                when(rs.getDate("StartDate")).thenReturn(java.sql.Date.valueOf("2024-01-01"));
                when(rs.getDate("EndDate")).thenReturn(java.sql.Date.valueOf("2024-12-31"));
                when(rs.getByte("Status")).thenReturn((byte) 1);
                when(rs.getString("CreatedUser")).thenReturn("User");
                when(rs.getTimestamp("CreatedTime")).thenReturn(java.sql.Timestamp.valueOf(LocalDateTime.now()));
                when(rs.getString("UpdatedUser")).thenReturn("User");
                when(rs.getTimestamp("UpdatedTime")).thenReturn(java.sql.Timestamp.valueOf(LocalDateTime.now()));
                when(rs.getString("AttachmentPath")).thenReturn("/path");

                // Use findAll to trigger RowMapper usage
                // Note: Generic verification with captor is complex because RowMapper is a
                // private static final field
                // passed to query. We can capture it.
                when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());

                // When
                announcementsRepository.findAll();

                // Then
                org.mockito.ArgumentCaptor<RowMapper<Announcements>> captor = org.mockito.ArgumentCaptor
                                .forClass(RowMapper.class);
                verify(jdbcTemplate).query(anyString(), captor.capture());

                RowMapper<Announcements> mapper = captor.getValue();
                Announcements result = mapper.mapRow(rs, 1);

                assertNotNull(result);
                assertEquals(testAnnouncementId, result.getAnnouncementID());
                assertEquals("Title", result.getTitle());
                assertEquals(LocalDate.of(2024, 1, 1), result.getStartDate());
        }

        @Test
        void testSummaryRowMapperLogic() throws java.sql.SQLException {
                // Given
                java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                when(rs.getString("AnnouncementID")).thenReturn(testAnnouncementId.toString());
                when(rs.getString("Title")).thenReturn("Title");
                when(rs.getString("Content")).thenReturn("Content");
                when(rs.getDate("StartDate")).thenReturn(java.sql.Date.valueOf("2024-01-01"));
                when(rs.getString("AttachmentPath")).thenReturn("/path");
                when(rs.getObject("Type")).thenReturn((byte) 1);
                when(rs.getByte("Type")).thenReturn((byte) 1);

                // Trigger findSummaryData
                when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());

                // When
                announcementsRepository.findSummaryData();

                // Then
                org.mockito.ArgumentCaptor<RowMapper<Group4.Childcare.DTO.AnnouncementSummaryDTO>> captor = org.mockito.ArgumentCaptor
                                .forClass(RowMapper.class);
                verify(jdbcTemplate).query(contains("SELECT AnnouncementID"), captor.capture());

                RowMapper<Group4.Childcare.DTO.AnnouncementSummaryDTO> mapper = captor.getValue();
                Group4.Childcare.DTO.AnnouncementSummaryDTO result = mapper.mapRow(rs, 1);

                assertNotNull(result);
                assertEquals("Title", result.getTitle());
                assertEquals(LocalDate.of(2024, 1, 1), result.getStartDate());
        }

        @Test
        void testFindAdminActiveBackend() {
                // Given
                when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(java.sql.Date.class)))
                                .thenReturn(Collections.emptyList());

                // When
                announcementsRepository.findAdminActiveBackend();

                // Then
                verify(jdbcTemplate).query((String) contains("WHERE Status = 1 AND Type = 2"), any(RowMapper.class),
                                any(java.sql.Date.class));
        }
}
