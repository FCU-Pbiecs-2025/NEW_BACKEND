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

    // ========== searchUsersWithOffset Tests ==========

    @Test
    void testSearchUsersWithOffset_Valid() {
        List<UserSummaryDTO> results = new ArrayList<>();
        when(repository.searchUsersWithOffset("test", 0, 10)).thenReturn(results);
        List<UserSummaryDTO> result = service.searchUsersWithOffset("test", 0, 10);
        assertNotNull(result);
        verify(repository).searchUsersWithOffset("test", 0, 10);
    }

    @Test
    void testSearchUsersWithOffset_NullOrEmpty() {
        when(repository.findWithOffsetAndInstitutionName(0, 10)).thenReturn(new ArrayList<>());
        
        service.searchUsersWithOffset(null, 0, 10);
        service.searchUsersWithOffset("  ", 0, 10);
        
        verify(repository, times(2)).findWithOffsetAndInstitutionName(0, 10);
    }

    @Test
    void testSearchUsersWithOffset_Exception() {
        when(repository.searchUsersWithOffset(anyString(), anyInt(), anyInt())).thenThrow(new RuntimeException("DB Error"));
        assertThrows(RuntimeException.class, () -> service.searchUsersWithOffset("test", 0, 10));
    }

    // ========== getSearchCount Tests ==========

    @Test
    void testGetSearchCount_Valid() {
        when(repository.countSearchUsers("test")).thenReturn(5L);
        long result = service.getSearchCount("test");
        assertEquals(5L, result);
    }

    @Test
    void testGetSearchCount_NullOrEmpty() {
        when(repository.countTotal()).thenReturn(100L);
        
        assertEquals(100L, service.getSearchCount(null));
        assertEquals(100L, service.getSearchCount(""));
        
        verify(repository, times(2)).countTotal();
    }

    @Test
    void testGetSearchCount_Exception() {
        when(repository.countSearchUsers(anyString())).thenThrow(new RuntimeException("DB Error"));
        assertEquals(0L, service.getSearchCount("test"));
    }

    // ========== searchUsersByAccountWithOffset Tests ==========

    @Test
    void testSearchUsersByAccountWithOffset_Valid() {
        List<UserSummaryDTO> results = new ArrayList<>();
        when(repository.searchUsersByAccountWithOffset("test", 0, 10)).thenReturn(results);
        List<UserSummaryDTO> result = service.searchUsersByAccountWithOffset("test", 0, 10);
        assertNotNull(result);
        verify(repository).searchUsersByAccountWithOffset("test", 0, 10);
    }

    @Test
    void testSearchUsersByAccountWithOffset_NullOrEmpty() {
        when(repository.findWithOffsetAndInstitutionName(0, 10)).thenReturn(new ArrayList<>());
        
        service.searchUsersByAccountWithOffset(null, 0, 10);
        service.searchUsersByAccountWithOffset(" ", 0, 10);
        
        verify(repository, times(2)).findWithOffsetAndInstitutionName(0, 10);
    }

    @Test
    void testSearchUsersByAccountWithOffset_Exception() {
        when(repository.searchUsersByAccountWithOffset(anyString(), anyInt(), anyInt())).thenThrow(new RuntimeException("DB Error"));
        assertThrows(RuntimeException.class, () -> service.searchUsersByAccountWithOffset("test", 0, 10));
    }

    // ========== getSearchTotalCount Tests ==========

    @Test
    void testGetSearchTotalCount_Valid() {
        when(repository.countSearchUsersByAccount("test")).thenReturn(10L);
        long result = service.getSearchTotalCount("test");
        assertEquals(10L, result);
    }

    @Test
    void testGetSearchTotalCount_NullOrEmpty() {
        when(repository.countTotal()).thenReturn(100L);
        
        assertEquals(100L, service.getSearchTotalCount(null));
        assertEquals(100L, service.getSearchTotalCount("  "));
        
        verify(repository, times(2)).countTotal();
    }

    @Test
    void testGetSearchTotalCount_Exception() {
        when(repository.countSearchUsersByAccount(anyString())).thenThrow(new RuntimeException("DB Error"));
        assertEquals(0L, service.getSearchTotalCount("test"));
    }

    // ========== searchCitizenUsersByAccountWithOffset Tests ==========

    @Test
    void testSearchCitizenUsersByAccountWithOffset_Valid() {
        List<UserSummaryDTO> results = new ArrayList<>();
        when(repository.searchCitizenUsersByAccountWithOffset("citizen", 0, 10)).thenReturn(results);
        List<UserSummaryDTO> result = service.searchCitizenUsersByAccountWithOffset("citizen", 0, 10);
        assertNotNull(result);
        verify(repository).searchCitizenUsersByAccountWithOffset("citizen", 0, 10);
    }

    @Test
    void testSearchCitizenUsersByAccountWithOffset_NullOrEmpty() {
        when(repository.searchCitizenUsersByAccountWithOffset("", 0, 10)).thenReturn(new ArrayList<>());
        
        service.searchCitizenUsersByAccountWithOffset(null, 0, 10);
        service.searchCitizenUsersByAccountWithOffset("", 0, 10);
        
        verify(repository, times(2)).searchCitizenUsersByAccountWithOffset("", 0, 10);
    }

    @Test
    void testSearchCitizenUsersByAccountWithOffset_Exception() {
        when(repository.searchCitizenUsersByAccountWithOffset(anyString(), anyInt(), anyInt())).thenThrow(new RuntimeException("DB Error"));
        assertThrows(RuntimeException.class, () -> service.searchCitizenUsersByAccountWithOffset("citizen", 0, 10));
    }

    // ========== getSearchCitizenTotalCount Tests ==========

    @Test
    void testGetSearchCitizenTotalCount_Valid() {
        when(repository.countSearchCitizenUsersByAccount("citizen")).thenReturn(15L);
        long result = service.getSearchCitizenTotalCount("citizen");
        assertEquals(15L, result);
    }

    @Test
    void testGetSearchCitizenTotalCount_NullOrEmpty() {
        when(repository.countSearchCitizenUsersByAccount("")).thenReturn(20L);
        
        assertEquals(20L, service.getSearchCitizenTotalCount(null));
        assertEquals(20L, service.getSearchCitizenTotalCount(" "));
        
        verify(repository, times(2)).countSearchCitizenUsersByAccount("");
    }

    @Test
    void testGetSearchCitizenTotalCount_Exception() {
        when(repository.countSearchCitizenUsersByAccount(anyString())).thenThrow(new RuntimeException("DB Error"));
        assertEquals(0L, service.getSearchCitizenTotalCount("citizen"));
    }

    // ========== updateAccountStatus Tests ==========

    @Test
    void testUpdateAccountStatus_Success() {
        when(repository.updateAccountStatus(testUserId, 1)).thenReturn(1);
        when(repository.findById(testUserId)).thenReturn(Optional.of(testUser));
        Users result = service.updateAccountStatus(testUserId, 1);
        assertNotNull(result);
    }

    @Test
    void testUpdateAccountStatus_RowsZero() {
        when(repository.updateAccountStatus(testUserId, 1)).thenReturn(0);
        assertThrows(RuntimeException.class, () -> service.updateAccountStatus(testUserId, 1));
    }

    @Test
    void testUpdateAccountStatus_UserNotFoundAfterUpdate() {
        when(repository.updateAccountStatus(testUserId, 1)).thenReturn(1);
        when(repository.findById(testUserId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateAccountStatus(testUserId, 1));
    }

    @Test
    void testUpdateAccountStatus_Exception() {
        when(repository.updateAccountStatus(any(), anyInt())).thenThrow(new RuntimeException("DB Error"));
        assertThrows(RuntimeException.class, () -> service.updateAccountStatus(testUserId, 1));
    }

    // ========== Other Tests ==========

    @Test
    void testCreateUser_Success() {
        when(repository.save(any(Users.class))).thenReturn(testUser);
        Users result = service.createUser(testUser);
        assertNotNull(result);
        assertEquals("testuser", result.getAccount());
    }

    @Test
    void testGetUserById_Found() {
        when(repository.findById(testUserId)).thenReturn(Optional.of(testUser));
        Optional<Users> result = service.getUserById(testUserId);
        assertTrue(result.isPresent());
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
    void testUpdateUser_Success() {
        testUser.setName("Updated Name");
        when(repository.save(any(Users.class))).thenReturn(testUser);
        Users result = service.updateUser(testUserId, testUser);
        assertNotNull(result);
        assertEquals(testUserId, result.getUserID());
    }

    @Test
    void testGetUsersWithOffsetAndInstitutionNameJdbc_Exception() {
        when(repository.findWithOffsetAndInstitutionName(anyInt(), anyInt())).thenThrow(new RuntimeException("DB Error"));
        assertThrows(RuntimeException.class, () -> service.getUsersWithOffsetAndInstitutionNameJdbc(0, 10));
    }

    @Test
    void testGetTotalCount_Exception() {
        when(repository.countTotal()).thenThrow(new RuntimeException("DB Error"));
        assertEquals(0L, service.getTotalCount());
    }

    @Test
    void testSaveUsingJdbc_Null() {
        assertThrows(IllegalArgumentException.class, () -> service.saveUsingJdbc(null));
    }

    @Test
    void testSaveUsingJdbc_Exception() {
        when(repository.save(any())).thenThrow(new RuntimeException("DB Error"));
        assertThrows(RuntimeException.class, () -> service.saveUsingJdbc(testUser));
    }

    @Test
    void testUpdateUserProfile_Exception() {
        when(repository.updateProfile(any(), any(), any(), any(), any())).thenThrow(new RuntimeException("DB Error"));
        assertThrows(RuntimeException.class, () -> service.updateUserProfile(testUserId, "Name", "Email", "Phone", "Address"));
    }

    @Test
    void testIsAccountExists_NullOrEmpty() {
        assertFalse(service.isAccountExists(null));
        assertFalse(service.isAccountExists(""));
    }

    @Test
    void testIsEmailExists_NullOrEmpty() {
        assertFalse(service.isEmailExists(null));
        assertFalse(service.isEmailExists(" "));
    }
}
