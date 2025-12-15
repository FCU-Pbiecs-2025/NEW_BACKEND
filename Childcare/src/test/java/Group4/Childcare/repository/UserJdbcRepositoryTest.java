package Group4.Childcare.repository;

import Group4.Childcare.Model.Users;
import Group4.Childcare.Repository.UserJdbcRepository;
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
 * UserJdbcRepository 單元測試
 * 測試使用者管理相關的資料庫操作
 */
@ExtendWith(MockitoExtension.class)
class UserJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Group4.Childcare.Repository.FamilyInfoJdbcRepository familyInfoJdbcRepository;

    @InjectMocks
    private UserJdbcRepository repository;

    private UUID testUserId;
    private Users testUser;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        testUser = new Users();
        testUser.setUserID(testUserId);
        testUser.setAccount("A123456789");
        testUser.setName("測試使用者");
        testUser.setEmail("test@example.com");
        testUser.setPhoneNumber("0912345678");
        testUser.setPassword("hashedPassword123");
        testUser.setAccountStatus((byte) 1);
        testUser.setPermissionType((byte) 1);
        testUser.setGender(true);
        testUser.setBirthDate(LocalDate.of(1990, 1, 1));
    }

    // ===== 測試 save (新增使用者) =====
    @Test
    void testSave_Success() {
        // Given
        UUID familyInfoId = UUID.randomUUID();
        testUser.setFamilyInfoID(familyInfoId);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        // When
        Users result = repository.save(testUser);

        // Then
        assertNotNull(result);
        assertEquals("A123456789", result.getAccount());
        verify(jdbcTemplate, times(1)).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ===== 測試 findById (根據ID查詢) =====
    @Test
    void testFindById_Success() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(testUser);

        // When
        Optional<Users> result = repository.findById(testUserId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUserId, result.get().getUserID());
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), any(RowMapper.class), anyString());
    }

    @Test
    void testFindById_ReturnsEmpty_WhenNotFound() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                .thenThrow(new RuntimeException("Not found"));

        // When
        Optional<Users> result = repository.findById(UUID.randomUUID());

        // Then
        assertFalse(result.isPresent());
    }

    // ===== 測試 findAll (查詢所有使用者) =====
    @Test
    void testFindAll_ReturnsAllUsers() {
        // Given
        List<Users> mockUsers = Arrays.asList(testUser, testUser);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(mockUsers);

        // When
        List<Users> result = repository.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class));
    }

    // ===== 測試 count (計算總數) =====
    @Test
    void testCount_ReturnsCorrectCount() {
        // Given
        Long expectedCount = 100L;
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(expectedCount);

        // When
        long result = repository.count();

        // Then
        assertEquals(expectedCount, result);
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(Long.class));
    }

    // ===== 測試 deleteById (刪除使用者) =====
    @Test
    void testDeleteById_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), anyString()))
                .thenReturn(1);

        // When
        repository.deleteById(testUserId);

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), eq(testUserId.toString()));
    }
}

