package Group4.Childcare.repository;

import Group4.Childcare.Model.FamilyInfo;
import Group4.Childcare.Model.Users;
import Group4.Childcare.Repository.FamilyInfoJdbcRepository;
import Group4.Childcare.Repository.UserJdbcRepository;
import Group4.Childcare.DTO.UserSummaryDTO;
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

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class UserJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private FamilyInfoJdbcRepository familyInfoJdbcRepository;

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

    // ==================== save Tests ====================

    @Test
    void testSave_NewUser_Success() {
        // Given: UserID is null, so it should insert
        testUser.setUserID(null);
        UUID familyInfoId = UUID.randomUUID();
        testUser.setFamilyInfoID(familyInfoId);

        // Mock checks
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testUser.getAccount()))).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testUser.getEmail()))).thenReturn(0);

        // Mock insert
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any()))
                .thenReturn(1);

        // When
        Users result = repository.save(testUser);

        // Then
        assertNotNull(result.getUserID());
        verify(jdbcTemplate).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void testSave_NewUser_AutoCreateFamilyInfo() {
        // Given: UserID null, FamilyInfoID null
        testUser.setUserID(null);
        testUser.setFamilyInfoID(null);

        // Mock FamilyInfo creation
        FamilyInfo newFamily = new FamilyInfo();
        newFamily.setFamilyInfoID(UUID.randomUUID());
        when(familyInfoJdbcRepository.save(any(FamilyInfo.class))).thenReturn(newFamily);

        // Mock checks and insert
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any()))
                .thenReturn(1);

        // When
        Users result = repository.save(testUser);

        // Then
        assertNotNull(result.getFamilyInfoID());
        assertEquals(newFamily.getFamilyInfoID(), result.getFamilyInfoID());
        verify(familyInfoJdbcRepository).save(any(FamilyInfo.class));
    }

    @Test
    void testSave_UpdateUser_Success() {
        // Given: UserID exists
        testUser.setFamilyInfoID(UUID.randomUUID()); // Fix: Set FamilyInfoID to avoid NPE
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any()))
                .thenReturn(1); // update returns 1

        // When
        Users result = repository.save(testUser);

        // Then
        assertEquals(testUserId, result.getUserID());
        // Verify update SQL (14 params for update)
        verify(jdbcTemplate).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void testInsert_DuplicateAccount_ThrowsRuntime() {
        testUser.setUserID(null);
        testUser.setFamilyInfoID(UUID.randomUUID());

        // existsByAccount -> true (count > 0)
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testUser.getAccount()))).thenReturn(1);

        assertThrows(RuntimeException.class, () -> repository.save(testUser));
    }

    @Test
    void testInsert_DuplicateEmail_ThrowsRuntime() {
        testUser.setUserID(null);
        testUser.setFamilyInfoID(UUID.randomUUID());

        // existsByAccount -> false, existsByEmail -> true
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testUser.getAccount()))).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testUser.getEmail()))).thenReturn(1);

        assertThrows(RuntimeException.class, () -> repository.save(testUser));
    }

    @Test
    void testSave_NullUser_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }

    @Test
    void testSave_MissingRequiredFields_ThrowsException() {
        testUser.setAccount(null);
        assertThrows(IllegalArgumentException.class, () -> repository.save(testUser));

        testUser.setAccount("valid");
        testUser.setPassword(null);
        assertThrows(IllegalArgumentException.class, () -> repository.save(testUser));
    }

    // ==================== find & exists Tests ====================

    @Test
    void testFindById_Found() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(testUserId.toString())))
                .thenReturn(testUser);
        Optional<Users> result = repository.findById(testUserId);
        assertTrue(result.isPresent());
    }

    @Test
    void testFindByAccount_Found() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(testUser.getAccount())))
                .thenReturn(testUser);
        Optional<Users> result = repository.findByAccount(testUser.getAccount());
        assertTrue(result.isPresent());
    }

    @Test
    void testFindByEmail_Found() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(testUser.getEmail())))
                .thenReturn(testUser);
        Optional<Users> result = repository.findByEmail(testUser.getEmail());
        assertTrue(result.isPresent());
    }

    @Test
    void testExistsByAccount_True() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("acc"))).thenReturn(1);
        assertTrue(repository.existsByAccount("acc"));
    }

    // ==================== Partial Update Tests ====================

    @Test
    void testUpdateProfile() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), anyString())).thenReturn(1);

        int rows = repository.updateProfile(testUserId, "New Name", "new@mail.com", "0999888777", "New Addr");
        assertEquals(1, rows);
    }

    @Test
    void testUpdateProfile_NoUpdates() {
        // All null -> should return 0 and not call jdbc
        int rows = repository.updateProfile(testUserId, null, null, null, null);
        assertEquals(0, rows);
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void testUpdateAccountStatus() {
        when(jdbcTemplate.update(anyString(), eq(2), eq(testUserId.toString()))).thenReturn(1);
        int rows = repository.updateAccountStatus(testUserId, 2);
        assertEquals(1, rows);
    }

    // ==================== Search methods ====================

    @Test
    void testSearchUsersWithOffset() {
        List<UserSummaryDTO> list = new ArrayList<>();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt()))
                .thenReturn(list);

        List<UserSummaryDTO> result = repository.searchUsersWithOffset("term", 0, 10);
        assertNotNull(result);
    }

    @Test
    void testCountSearchUsers() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString(), anyString(), anyString(),
                anyString()))
                .thenReturn(5L);
        long count = repository.countSearchUsers("term");
        assertEquals(5L, count);
    }

    @Test
    void testSearchUsersByAccountWithOffset() {
        List<UserSummaryDTO> list = new ArrayList<>();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyInt(), anyInt()))
                .thenReturn(list);
        List<UserSummaryDTO> result = repository.searchUsersByAccountWithOffset("acc", 0, 10);
        assertNotNull(result);
    }

    @Test
    void testSearchCitizenUsersByAccountWithOffset() {
        List<UserSummaryDTO> list = new ArrayList<>();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyInt(), anyInt()))
                .thenReturn(list);
        List<UserSummaryDTO> result = repository.searchCitizenUsersByAccountWithOffset("acc", 0, 10);
        assertNotNull(result);
    }

}
