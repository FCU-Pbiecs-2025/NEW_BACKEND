package Group4.Childcare.repository;

import Group4.Childcare.Model.ApplicationParticipants;
import Group4.Childcare.Repository.ApplicationParticipantsJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ApplicationParticipantsJdbcRepository 單元測試
 * 測試申請參與者管理相關的資料庫操作
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ApplicationParticipantsJdbcRepositoryTest {

        @Mock
        private JdbcTemplate jdbcTemplate;

        @InjectMocks
        private ApplicationParticipantsJdbcRepository participantsRepository;

        private UUID testParticipantId;
        private UUID testApplicationId;
        private UUID testClassId;

        @BeforeEach
        void setUp() {
                testParticipantId = UUID.randomUUID();
                testApplicationId = UUID.randomUUID();
                testClassId = UUID.randomUUID();
        }

        // ==================== save Tests ====================

        @Test
        void testSave_NewParticipant_WithNullId_Success() {
                // Given
                ApplicationParticipants participant = createTestParticipant();
                participant.setParticipantID(null); // New participant

                when(jdbcTemplate.update(anyString(), anyString(), anyString(), anyBoolean(), anyString(),
                                anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString(), any(), anyBoolean(), any(), anyInt(), anyString(),
                                any(), any(), any()))
                                .thenReturn(1);

                // When
                ApplicationParticipants result = participantsRepository.save(participant);

                // Then
                assertNotNull(result);
                assertNotNull(result.getParticipantID()); // ID should be generated
        }

        @Test
        void testSave_ExistingParticipant_Update_Success() {
                // Given
                ApplicationParticipants participant = createTestParticipant();
                participant.setParticipantID(testParticipantId);

                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                                .thenReturn(1); // exists

                when(jdbcTemplate.update(anyString(), anyBoolean(), anyString(), anyString(), anyBoolean(),
                                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                                any(), anyBoolean(), any(), anyInt(), anyString(), any(), any(), anyString()))
                                .thenReturn(1);

                // When
                ApplicationParticipants result = participantsRepository.save(participant);

                // Then
                assertNotNull(result);
                assertEquals(testParticipantId, result.getParticipantID());
        }

        @Test
        void testSave_ParticipantWithId_NotExists_Insert() {
                // Given
                ApplicationParticipants participant = createTestParticipant();
                participant.setParticipantID(testParticipantId);

                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                                .thenReturn(0); // not exists

                when(jdbcTemplate.update(anyString(), anyString(), anyString(), anyBoolean(), anyString(),
                                anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString(), any(), anyBoolean(), any(), anyInt(), anyString(),
                                any(), any(), any()))
                                .thenReturn(1);

                // When
                ApplicationParticipants result = participantsRepository.save(participant);

                // Then
                assertNotNull(result);
                assertEquals(testParticipantId, result.getParticipantID());
        }

        // ==================== findById Tests ====================

        @Test
        void testFindById_Success() {
                // Given
                ApplicationParticipants mockParticipant = createTestParticipant();
                mockParticipant.setParticipantID(testParticipantId);

                when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                                .thenReturn(mockParticipant);

                // When
                Optional<ApplicationParticipants> result = participantsRepository.findById(testApplicationId);

                // Then
                assertTrue(result.isPresent());
                assertEquals("王小明", result.get().getName());
        }

        @Test
        void testFindById_NotFound() {
                // Given
                when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                                .thenThrow(new RuntimeException("Not found"));

                // When
                Optional<ApplicationParticipants> result = participantsRepository.findById(testApplicationId);

                // Then
                assertFalse(result.isPresent());
        }

        // ==================== findAll Tests ====================

        @Test
        void testFindAll_Success() {
                // Given
                ApplicationParticipants participant1 = createTestParticipant();
                participant1.setParticipantID(UUID.randomUUID());
                participant1.setName("王小明");

                ApplicationParticipants participant2 = createTestParticipant();
                participant2.setParticipantID(UUID.randomUUID());
                participant2.setName("李小華");

                when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                                .thenReturn(Arrays.asList(participant1, participant2));

                // When
                List<ApplicationParticipants> result = participantsRepository.findAll();

                // Then
                assertNotNull(result);
                assertEquals(2, result.size());
                assertEquals("王小明", result.get(0).getName());
                assertEquals("李小華", result.get(1).getName());
        }

        @Test
        void testFindAll_EmptyResult() {
                // Given
                when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                                .thenReturn(Collections.emptyList());

                // When
                List<ApplicationParticipants> result = participantsRepository.findAll();

                // Then
                assertNotNull(result);
                assertTrue(result.isEmpty());
        }

        // ==================== findByApplicationIDAndNationalID Tests
        // ====================

        @Test
        void testFindByApplicationIDAndNationalID_Success() {
                // Given
                ApplicationParticipants participant = createTestParticipant();
                participant.setApplicationID(testApplicationId);
                String nationalID = "A123456789";

                when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(testApplicationId.toString()),
                                eq(nationalID)))
                                .thenReturn(Collections.singletonList(participant));

                // When
                List<ApplicationParticipants> result = participantsRepository
                                .findByApplicationIDAndNationalID(testApplicationId, nationalID);

                // Then
                assertNotNull(result);
                assertEquals(1, result.size());
                assertEquals(testApplicationId, result.get(0).getApplicationID());
        }

        @Test
        void testFindByApplicationIDAndNationalID_NotFound() {
                // Given
                when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyString()))
                                .thenReturn(Collections.emptyList());

                // When
                List<ApplicationParticipants> result = participantsRepository
                                .findByApplicationIDAndNationalID(testApplicationId, "A123456789");

                // Then
                assertNotNull(result);
                assertTrue(result.isEmpty());
        }

        // ==================== existsById Tests ====================

        @Test
        void testExistsById_True() {
                // Given
                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testParticipantId.toString())))
                                .thenReturn(1);

                // When
                boolean exists = participantsRepository.existsById(testParticipantId);

                // Then
                assertTrue(exists);
                verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Integer.class),
                                eq(testParticipantId.toString()));
        }

        @Test
        void testExistsById_False() {
                // Given
                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testParticipantId.toString())))
                                .thenReturn(0);

                // When
                boolean exists = participantsRepository.existsById(testParticipantId);

                // Then
                assertFalse(exists);
        }

        // ==================== deleteById Tests ====================

        @Test
        void testDeleteById_Success() {
                // Given
                when(jdbcTemplate.update(anyString(), eq(testParticipantId.toString())))
                                .thenReturn(1);

                // When
                participantsRepository.deleteById(testParticipantId);

                // Then
                verify(jdbcTemplate, times(1)).update(anyString(), eq(testParticipantId.toString()));
        }

        @Test
        void testDeleteById_NotFound() {
                // Given
                when(jdbcTemplate.update(anyString(), eq(testParticipantId.toString())))
                                .thenReturn(0);

                // When
                participantsRepository.deleteById(testParticipantId);

                // Then
                verify(jdbcTemplate, times(1)).update(anyString(), eq(testParticipantId.toString()));
        }

        // ==================== count Tests ====================

        @Test
        void testCount_Success() {
                // Given
                when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                                .thenReturn(42L);

                // When
                long count = participantsRepository.count();

                // Then
                assertEquals(42L, count);
                verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Long.class));
        }

        @Test
        void testCount_Zero() {
                // Given
                when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                                .thenReturn(0L);

                // When
                long count = participantsRepository.count();

                // Then
                assertEquals(0L, count);
        }

        // ==================== Helper Methods ====================

        private ApplicationParticipants createTestParticipant() {
                ApplicationParticipants participant = new ApplicationParticipants();
                participant.setApplicationID(testApplicationId);
                participant.setParticipantType(false); // 0 = child
                participant.setNationalID("A123456789");
                participant.setName("王小明");
                participant.setGender(true);
                participant.setRelationShip("子女");
                participant.setOccupation(null);
                participant.setPhoneNumber("0912345678");
                participant.setHouseholdAddress("台北市信義區信義路一段");
                participant.setMailingAddress("台北市信義區信義路一段");
                participant.setEmail("test@example.com");
                participant.setBirthDate(LocalDate.of(2020, 3, 15));
                participant.setIsSuspended(false);
                participant.setSuspendEnd(null);
                participant.setCurrentOrder(0);
                participant.setStatus("審核中");
                participant.setReason(null);
                participant.setClassID(testClassId);
                return participant;
        }

        // ==================== delete (Entity) Tests ====================

        @Test
        void testDelete_Success() {
                // Given
                ApplicationParticipants participant = createTestParticipant();
                participant.setParticipantID(testParticipantId);

                when(jdbcTemplate.update(anyString(), eq(testApplicationId.toString())))
                                .thenReturn(1);

                // When
                participantsRepository.delete(participant);

                // Then
                verify(jdbcTemplate, times(1)).update(anyString(), eq(testApplicationId.toString()));
        }

        // ==================== countApplicationsByChildNationalID Tests
        // ====================

        @Test
        void testCountApplicationsByChildNationalID_ReturnsCount() {
                // Given
                String nationalID = "A123456789";
                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(nationalID)))
                                .thenReturn(3);

                // When
                long count = participantsRepository.countApplicationsByChildNationalID(nationalID);

                // Then
                assertEquals(3L, count);
                verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Integer.class), eq(nationalID));
        }

        @Test
        void testCountApplicationsByChildNationalID_ReturnsZero() {
                // Given
                String nationalID = "B987654321";
                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(nationalID)))
                                .thenReturn(0);

                // When
                long count = participantsRepository.countApplicationsByChildNationalID(nationalID);

                // Then
                assertEquals(0L, count);
        }

        @Test
        void testCountApplicationsByChildNationalID_ReturnsZeroWhenNull() {
                // Given
                String nationalID = "C111222333";
                when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(nationalID)))
                                .thenReturn(null);

                // When
                long count = participantsRepository.countApplicationsByChildNationalID(nationalID);

                // Then
                assertEquals(0L, count);
        }
}
