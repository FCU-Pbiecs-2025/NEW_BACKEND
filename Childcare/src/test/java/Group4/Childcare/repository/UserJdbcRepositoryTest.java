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

import java.sql.ResultSet;
import java.sql.SQLException;
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
        testUser.setPassword("password123");
        testUser.setAccountStatus((byte) 1);
        testUser.setPermissionType((byte) 2);
        testUser.setGender(true);
        testUser.setPhoneNumber("0912345678");
        testUser.setMailingAddress("測試地址");
        testUser.setBirthDate(LocalDate.of(1990, 1, 1));
        testUser.setNationalID("A123456789");
    }

    // ==================== save() Tests ====================

    @Test
    void testSave_NullUser_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }

    @Test
    void testSave_NullAccount_ThrowsException() {
        testUser.setAccount(null);
        assertThrows(IllegalArgumentException.class, () -> repository.save(testUser));
    }

    @Test
    void testSave_EmptyAccount_ThrowsException() {
        testUser.setAccount("  ");
        assertThrows(IllegalArgumentException.class, () -> repository.save(testUser));
    }

    @Test
    void testSave_NullPassword_ThrowsException() {
        testUser.setPassword(null);
        assertThrows(IllegalArgumentException.class, () -> repository.save(testUser));
    }

    @Test
    void testSave_EmptyPassword_ThrowsException() {
        testUser.setPassword("");
        assertThrows(IllegalArgumentException.class, () -> repository.save(testUser));
    }

    @Test
    void testSave_NewUser_AutoCreateFamilyInfo() {
        testUser.setUserID(null);
        testUser.setFamilyInfoID(null);

        FamilyInfo newFamily = new FamilyInfo();
        newFamily.setFamilyInfoID(UUID.randomUUID());
        when(familyInfoJdbcRepository.save(any(FamilyInfo.class))).thenReturn(newFamily);

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any()))
                .thenReturn(1);

        Users result = repository.save(testUser);
        assertNotNull(result.getFamilyInfoID());
        assertEquals(newFamily.getFamilyInfoID(), result.getFamilyInfoID());
    }

    @Test
    void testSave_ExistingFamilyInfoID_Success() {
        testUser.setUserID(null);
        testUser.setFamilyInfoID(UUID.randomUUID());

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        Users result = repository.save(testUser);
        assertNotNull(result);
        verify(familyInfoJdbcRepository, never()).save(any());
    }

    @Test
    void testSave_NewUser_NullOptionalFields_Success() {
        testUser.setUserID(null);
        testUser.setFamilyInfoID(UUID.randomUUID());
        testUser.setBirthDate(null);
        testUser.setInstitutionID(null);

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        Users result = repository.save(testUser);
        assertNotNull(result);
    }

    @Test
    void testSave_UpdateUser_NullOptionalFields_Success() {
        testUser.setFamilyInfoID(UUID.randomUUID());
        testUser.setBirthDate(null);
        testUser.setInstitutionID(null);

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        Users result = repository.save(testUser);
        assertNotNull(result);
    }

    @Test
    void testSave_UpdateUser_Success() {
        testUser.setFamilyInfoID(UUID.randomUUID());
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any()))
                .thenReturn(1);

        Users result = repository.save(testUser);
        assertEquals(testUserId, result.getUserID());
    }

    @Test
    void testSave_UpdateUser_NoRowsAffected() {
        testUser.setFamilyInfoID(UUID.randomUUID());
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any()))
                .thenReturn(0);

        Users result = repository.save(testUser);
        assertNotNull(result);
    }

    @Test
    void testSave_UpdateUser_Exception() {
        testUser.setFamilyInfoID(UUID.randomUUID());
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB Error"));

        assertThrows(RuntimeException.class, () -> repository.save(testUser));
    }

    @Test
    void testInsert_DuplicateAccount_ThrowsException() {
        testUser.setUserID(null);
        testUser.setFamilyInfoID(UUID.randomUUID());
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testUser.getAccount()))).thenReturn(1);
        assertThrows(RuntimeException.class, () -> repository.save(testUser));
    }

    @Test
    void testInsert_DuplicateEmail_ThrowsException() {
        testUser.setUserID(null);
        testUser.setFamilyInfoID(UUID.randomUUID());
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testUser.getAccount()))).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testUser.getEmail()))).thenReturn(1);
        assertThrows(RuntimeException.class, () -> repository.save(testUser));
    }

    @Test
    void testInsert_Exception() {
        testUser.setUserID(null);
        testUser.setFamilyInfoID(UUID.randomUUID());
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Insert Error"));

        assertThrows(RuntimeException.class, () -> repository.save(testUser));
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
    void testFindById_NotFound() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                .thenThrow(new RuntimeException("Not found"));
        Optional<Users> result = repository.findById(testUserId);
        assertFalse(result.isPresent());
    }

    @Test
    void testFindAll() {
        List<Users> list = new ArrayList<>();
        list.add(testUser);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(list);
        List<Users> result = repository.findAll();
        assertEquals(1, result.size());
    }

    @Test
    void testDeleteById() {
        repository.deleteById(testUserId);
        verify(jdbcTemplate).update(anyString(), eq(testUserId.toString()));
    }

    @Test
    void testDelete() {
        repository.delete(testUser);
        verify(jdbcTemplate).update(anyString(), eq(testUserId.toString()));
    }

    @Test
    void testExistsById_True() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(testUserId.toString()))).thenReturn(1);
        assertTrue(repository.existsById(testUserId));
    }

    @Test
    void testExistsById_False() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        assertFalse(repository.existsById(testUserId));
    }

    @Test
    void testCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(10L);
        assertEquals(10L, repository.count());
    }

    @Test
    void testCount_Null() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);
        assertEquals(0L, repository.count());
    }

    @Test
    void testFindByAccount_Found() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("A123456789")))
                .thenReturn(testUser);
        Optional<Users> result = repository.findByAccount("A123456789");
        assertTrue(result.isPresent());
    }

    @Test
    void testFindByAccount_NotFound() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                .thenThrow(new RuntimeException("Not found"));
        Optional<Users> result = repository.findByAccount("A123456789");
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByEmail_Found() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("test@example.com")))
                .thenReturn(testUser);
        Optional<Users> result = repository.findByEmail("test@example.com");
        assertTrue(result.isPresent());
    }

    @Test
    void testFindByEmail_NotFound() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString()))
                .thenThrow(new RuntimeException("Not found"));
        Optional<Users> result = repository.findByEmail("test@example.com");
        assertFalse(result.isPresent());
    }

    @Test
    void testExistsByAccount_True() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("acc"))).thenReturn(1);
        assertTrue(repository.existsByAccount("acc"));
    }

    @Test
    void testExistsByAccount_False() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        assertFalse(repository.existsByAccount("acc"));
    }

    @Test
    void testExistsByAccount_Null() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(null);
        assertFalse(repository.existsByAccount("acc"));
    }

    @Test
    void testExistsByAccount_Exception() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenThrow(new RuntimeException("Error"));
        assertFalse(repository.existsByAccount("acc"));
    }

    @Test
    void testExistsByEmail_True() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("email"))).thenReturn(1);
        assertTrue(repository.existsByEmail("email"));
    }

    @Test
    void testExistsByEmail_False() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        assertFalse(repository.existsByEmail("email"));
    }

    @Test
    void testExistsByEmail_Null() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(null);
        assertFalse(repository.existsByEmail("email"));
    }

    @Test
    void testExistsByEmail_Exception() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenThrow(new RuntimeException("Error"));
        assertFalse(repository.existsByEmail("email"));
    }

    // ==================== updateProfile & updateAccountStatus Tests ====================

    @Test
    void testUpdateProfile_AllFields_Success() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        int result = repository.updateProfile(testUserId, "New Name", "new@mail.com", "0987", "New Addr");
        assertEquals(1, result);
    }

    @Test
    void testUpdateProfile_NullName_Success() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        int result = repository.updateProfile(testUserId, null, "new@mail.com", "0987", "New Addr");
        assertEquals(1, result);
    }

    @Test
    void testUpdateProfile_NullEmail_Success() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        int result = repository.updateProfile(testUserId, "Name", null, "0987", "New Addr");
        assertEquals(1, result);
    }

    @Test
    void testUpdateProfile_NullPhone_Success() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        int result = repository.updateProfile(testUserId, "Name", "mail", null, "New Addr");
        assertEquals(1, result);
    }

    @Test
    void testUpdateProfile_NullAddress_Success() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        int result = repository.updateProfile(testUserId, "Name", "mail", "0987", null);
        assertEquals(1, result);
    }

    @Test
    void testUpdateProfile_NullNameAndEmail_Success() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        int result = repository.updateProfile(testUserId, null, null, "0987", "New Addr");
        assertEquals(1, result);
    }

    @Test
    void testUpdateProfile_OnlyAddress_Success() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        int result = repository.updateProfile(testUserId, null, null, null, "New Addr");
        assertEquals(1, result);
    }

    @Test
    void testUpdateProfile_PartialFields_Success() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        int result = repository.updateProfile(testUserId, null, "new@mail.com", null, "New Addr");
        assertEquals(1, result);
    }

    @Test
    void testUpdateProfile_NoFields_ReturnsZero() {
        int result = repository.updateProfile(testUserId, null, null, null, null);
        assertEquals(0, result);
    }

    @Test
    void testUpdateProfile_Exception() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenThrow(new RuntimeException("Error"));
        assertThrows(RuntimeException.class, () -> repository.updateProfile(testUserId, "Name", null, null, null));
    }

    @Test
    void testUpdateAccountStatus_Success() {
        when(jdbcTemplate.update(anyString(), any(Object.class), any(Object.class))).thenReturn(1);
        int result = repository.updateAccountStatus(testUserId, 2);
        assertEquals(1, result);
    }

    @Test
    void testUpdateAccountStatus_Exception() {
        when(jdbcTemplate.update(anyString(), any(Object.class), any(Object.class))).thenThrow(new RuntimeException("Error"));
        assertThrows(RuntimeException.class, () -> repository.updateAccountStatus(testUserId, 1));
    }

    // ==================== Search Tests ====================

    @Test
    void testSearchUsersWithOffset() {
        List<UserSummaryDTO> list = new ArrayList<>();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(list);
        List<UserSummaryDTO> result = repository.searchUsersWithOffset("term", 0, 10);
        assertNotNull(result);
    }

    @Test
    void testCountSearchUsers() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any(), any(), any())).thenReturn(5L);
        assertEquals(5L, repository.countSearchUsers("term"));
    }

    @Test
    void testCountSearchUsers_Null() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any(), any(), any())).thenReturn(null);
        assertEquals(0L, repository.countSearchUsers("term"));
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
    void testCountSearchUsersByAccount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(3L);
        assertEquals(3L, repository.countSearchUsersByAccount("acc"));
    }

    @Test
    void testCountSearchUsersByAccount_Null() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(null);
        assertEquals(0L, repository.countSearchUsersByAccount("acc"));
    }

    @Test
    void testSearchCitizenUsersByAccountWithOffset() {
        List<UserSummaryDTO> list = new ArrayList<>();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyInt(), anyInt()))
                .thenReturn(list);
        List<UserSummaryDTO> result = repository.searchCitizenUsersByAccountWithOffset("acc", 0, 10);
        assertNotNull(result);
    }

    @Test
    void testCountSearchCitizenUsersByAccount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(4L);
        assertEquals(4L, repository.countSearchCitizenUsersByAccount("acc"));
    }

    @Test
    void testCountSearchCitizenUsersByAccount_Null() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(null);
        assertEquals(0L, repository.countSearchCitizenUsersByAccount("acc"));
    }

    @Test
    void testFindWithOffsetAndInstitutionName() {
        List<UserSummaryDTO> list = new ArrayList<>();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt())).thenReturn(list);
        List<UserSummaryDTO> result = repository.findWithOffsetAndInstitutionName(0, 10);
        assertNotNull(result);
    }

    @Test
    void testCountTotal() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(100L);
        assertEquals(100L, repository.countTotal());
    }

    // ==================== RowMapper Tests ====================

    @Test
    void testUsersRowMapper_FullData() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        UUID userId = UUID.randomUUID();
        UUID familyInfoId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        LocalDate birthDate = LocalDate.of(1990, 1, 1);

        when(rs.getString("UserID")).thenReturn(userId.toString());
        when(rs.getString("Account")).thenReturn("testAccount");
        when(rs.getString("Password")).thenReturn("testPass");
        when(rs.getByte("AccountStatus")).thenReturn((byte) 1);
        when(rs.getByte("PermissionType")).thenReturn((byte) 2);
        when(rs.getString("Name")).thenReturn("Test Name");
        when(rs.getBoolean("Gender")).thenReturn(true);
        when(rs.getString("PhoneNumber")).thenReturn("0912345678");
        when(rs.getString("MailingAddress")).thenReturn("Test Address");
        when(rs.getString("Email")).thenReturn("test@mail.com");
        when(rs.getDate("BirthDate")).thenReturn(java.sql.Date.valueOf(birthDate));
        when(rs.getString("FamilyInfoID")).thenReturn(familyInfoId.toString());
        when(rs.getString("InstitutionID")).thenReturn(institutionId.toString());
        when(rs.getString("NationalID")).thenReturn("A123456789");

        final RowMapper<Users>[] capturedMapper = new RowMapper[1];
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString())).thenAnswer(invocation -> {
            capturedMapper[0] = invocation.getArgument(1);
            return null;
        });

        repository.findByAccount("testAccount");
        assertNotNull(capturedMapper[0]);

        Users result = capturedMapper[0].mapRow(rs, 1);
        assertNotNull(result);
        assertEquals(userId, result.getUserID());
        assertEquals(birthDate, result.getBirthDate());
        assertEquals(familyInfoId, result.getFamilyInfoID());
        assertEquals(institutionId, result.getInstitutionID());
    }

    @Test
    void testUsersRowMapper_NullOptionalFields() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        UUID userId = UUID.randomUUID();

        when(rs.getString("UserID")).thenReturn(userId.toString());
        when(rs.getString("Account")).thenReturn("testAccount");
        when(rs.getDate("BirthDate")).thenReturn(null);
        when(rs.getString("FamilyInfoID")).thenReturn(null);
        when(rs.getString("InstitutionID")).thenReturn(null);
        when(rs.getString("NationalID")).thenReturn(null);

        final RowMapper<Users>[] capturedMapper = new RowMapper[1];
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyString())).thenAnswer(invocation -> {
            capturedMapper[0] = invocation.getArgument(1);
            return null;
        });

        repository.findByAccount("testAccount");
        Users result = capturedMapper[0].mapRow(rs, 1);

        assertNotNull(result);
        assertNull(result.getBirthDate());
        assertNull(result.getFamilyInfoID());
        assertNull(result.getInstitutionID());
        assertNull(result.getNationalID());
    }

    @Test
    void testUserSummaryRowMapper() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        UUID userId = UUID.randomUUID();

        when(rs.getString("UserID")).thenReturn(userId.toString());
        when(rs.getString("Account")).thenReturn("testAccount");
        when(rs.getByte("PermissionType")).thenReturn((byte) 1);
        when(rs.getByte("AccountStatus")).thenReturn((byte) 1);
        when(rs.getString("InstitutionName")).thenReturn("Test Institution");

        final RowMapper<UserSummaryDTO>[] capturedMapper = new RowMapper[1];
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyInt(), anyInt())).thenAnswer(invocation -> {
            capturedMapper[0] = invocation.getArgument(1);
            return Collections.emptyList();
        });

        repository.searchUsersByAccountWithOffset("test", 0, 10);
        UserSummaryDTO result = capturedMapper[0].mapRow(rs, 1);

        assertNotNull(result);
        assertEquals(userId, result.getUserID());
        assertEquals("Test Institution", result.getInstitutionName());
    }
}