package Group4.Childcare.service;

import Group4.Childcare.Model.Users;
import Group4.Childcare.Repository.UserJdbcRepository;
import Group4.Childcare.Service.UsersService;
import Group4.Childcare.DTO.UserSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class UsersServiceExpandedTest {

    @Mock
    private UserJdbcRepository repository;

    @InjectMocks
    private UsersService service;

    private Users testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        testUser = new Users();
        testUser.setUserID(testUserId);
        testUser.setAccount("testuser");
        testUser.setPassword("password123");
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setAccountStatus((byte) 1);
    }

    @Test
    void testCreateUser_Success() {
        when(repository.save(any(Users.class))).thenReturn(testUser);

        Users result = service.createUser(testUser);

        assertNotNull(result);
        assertEquals("testuser", result.getAccount());
        verify(repository).save(testUser);
    }

    @Test
    void testGetUserById_Found() {
        when(repository.findById(testUserId)).thenReturn(Optional.of(testUser));

        Optional<Users> result = service.getUserById(testUserId);

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getAccount());
    }

    @Test
    void testGetUserById_NotFound() {
        when(repository.findById(testUserId)).thenReturn(Optional.empty());

        Optional<Users> result = service.getUserById(testUserId);

        assertFalse(result.isPresent());
    }

    @Test
    void testGetAllUsers_Success() {
        List<Users> users = Arrays.asList(testUser);
        when(repository.findAll()).thenReturn(users);

        List<Users> result = service.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetAllUsers_Empty() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<Users> result = service.getAllUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdateUser_Success() {
        testUser.setName("Updated Name");
        when(repository.save(any(Users.class))).thenReturn(testUser);

        Users result = service.updateUser(testUserId, testUser);

        assertNotNull(result);
        assertEquals(testUserId, result.getUserID());
    }

    @Test
    void testGetUsersWithOffsetAndInstitutionNameJdbc() {
        List<UserSummaryDTO> summaries = new ArrayList<>();
        when(repository.findWithOffsetAndInstitutionName(0, 10)).thenReturn(summaries);

        List<UserSummaryDTO> result = service.getUsersWithOffsetAndInstitutionNameJdbc(0, 10);

        assertNotNull(result);
    }

    @Test
    void testGetTotalCount() {
        when(repository.countTotal()).thenReturn(100L);

        long result = service.getTotalCount();

        assertEquals(100L, result);
    }

    @Test
    void testSaveUsingJdbc() {
        when(repository.save(any(Users.class))).thenReturn(testUser);

        Users result = service.saveUsingJdbc(testUser);

        assertNotNull(result);
    }

    @Test
    void testUpdateAccountStatus_Success() {
        when(repository.updateAccountStatus(testUserId, 1)).thenReturn(1);
        when(repository.findById(testUserId)).thenReturn(Optional.of(testUser));

        Users result = service.updateAccountStatus(testUserId, 1);

        assertNotNull(result);
    }

    @Test
    void testUpdateUserProfile_AllFields() {
        when(repository.updateProfile(testUserId, "New Name", "new@email.com", "0912345678", "New Address"))
                .thenReturn(1);

        int result = service.updateUserProfile(testUserId, "New Name", "new@email.com", "0912345678", "New Address");

        assertEquals(1, result);
    }

    @Test
    void testUpdateUserProfile_PartialFields() {
        when(repository.updateProfile(testUserId, "New Name", null, null, null))
                .thenReturn(1);

        int result = service.updateUserProfile(testUserId, "New Name", null, null, null);

        assertEquals(1, result);
    }

    @Test
    void testSearchUsersWithOffset() {
        List<UserSummaryDTO> results = new ArrayList<>();
        when(repository.searchUsersWithOffset("test", 0, 10)).thenReturn(results);

        List<UserSummaryDTO> result = service.searchUsersWithOffset("test", 0, 10);

        assertNotNull(result);
    }

    @Test
    void testGetSearchCount() {
        when(repository.countSearchUsers("test")).thenReturn(5L);

        long result = service.getSearchCount("test");

        assertEquals(5L, result);
    }

    @Test
    void testSearchUsersByAccountWithOffset() {
        List<UserSummaryDTO> results = new ArrayList<>();
        when(repository.searchUsersByAccountWithOffset("test", 0, 10)).thenReturn(results);

        List<UserSummaryDTO> result = service.searchUsersByAccountWithOffset("test", 0, 10);

        assertNotNull(result);
    }

    @Test
    void testGetSearchTotalCount() {
        when(repository.countSearchUsersByAccount("test")).thenReturn(10L);

        long result = service.getSearchTotalCount("test");

        assertEquals(10L, result);
    }

    @Test
    void testIsAccountExists_True() {
        when(repository.findByAccount("testuser")).thenReturn(Optional.of(testUser));

        boolean result = service.isAccountExists("testuser");

        assertTrue(result);
    }

    @Test
    void testIsAccountExists_False() {
        when(repository.findByAccount("newuser")).thenReturn(Optional.empty());

        boolean result = service.isAccountExists("newuser");

        assertFalse(result);
    }

    @Test
    void testIsAccountExists_NullInput() {
        boolean result = service.isAccountExists(null);

        assertFalse(result);
    }

    @Test
    void testIsAccountExists_EmptyInput() {
        boolean result = service.isAccountExists("");

        assertFalse(result);
    }

    @Test
    void testIsEmailExists_True() {
        when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        boolean result = service.isEmailExists("test@example.com");

        assertTrue(result);
    }

    @Test
    void testIsEmailExists_False() {
        when(repository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        boolean result = service.isEmailExists("new@example.com");

        assertFalse(result);
    }

    @Test
    void testIsEmailExists_NullInput() {
        boolean result = service.isEmailExists(null);

        assertFalse(result);
    }

    @Test
    void testIsEmailExists_EmptyInput() {
        boolean result = service.isEmailExists("");

        assertFalse(result);
    }

    @Test
    void testSearchCitizenUsersByAccountWithOffset() {
        List<UserSummaryDTO> results = new ArrayList<>();
        when(repository.searchCitizenUsersByAccountWithOffset("citizen", 0, 10)).thenReturn(results);

        List<UserSummaryDTO> result = service.searchCitizenUsersByAccountWithOffset("citizen", 0, 10);

        assertNotNull(result);
    }

    @Test
    void testGetSearchCitizenTotalCount() {
        when(repository.countSearchCitizenUsersByAccount("citizen")).thenReturn(15L);

        long result = service.getSearchCitizenTotalCount("citizen");

        assertEquals(15L, result);
    }

    @Test
    void testIsAccountExists_WithWhitespace() {
        when(repository.findByAccount("testuser")).thenReturn(Optional.of(testUser));

        boolean result = service.isAccountExists("  testuser  ");

        assertTrue(result);
        verify(repository).findByAccount("testuser");
    }

    @Test
    void testIsEmailExists_WithWhitespace() {
        when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        boolean result = service.isEmailExists("  test@example.com  ");

        assertTrue(result);
        verify(repository).findByEmail("test@example.com");
    }

    @Test
    void testIsAccountExists_Exception() {
        when(repository.findByAccount(anyString())).thenThrow(new RuntimeException("Database error"));

        boolean result = service.isAccountExists("testuser");

        assertFalse(result);
    }

    @Test
    void testIsEmailExists_Exception() {
        when(repository.findByEmail(anyString())).thenThrow(new RuntimeException("Database error"));

        boolean result = service.isEmailExists("test@example.com");

        assertFalse(result);
    }
}
