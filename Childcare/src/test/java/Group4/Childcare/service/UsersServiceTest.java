package Group4.Childcare.service;

import Group4.Childcare.DTO.UserSummaryDTO;
import Group4.Childcare.Model.Users;
import Group4.Childcare.Repository.UserJdbcRepository;
import Group4.Childcare.Service.UsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UsersService 單元測試
 *
 * 測試範圍：
 * 1. createUser() - 創建使用者
 * 2. getUserById() - 根據ID查詢使用者
 * 3. getAllUsers() - 查詢所有使用者
 * 4. updateUser() - 更新使用者
 * 5. getUsersWithOffsetAndInstitutionNameJdbc() - 分頁查詢使用者
 * 6. getTotalCount() - 取得使用者總數
 * 7. saveUsingJdbc() - 使用JDBC保存使用者
 * 8. updateAccountStatus() - 更新帳號狀態
 * 9. updateUserProfile() - 更新使用者資料
 */
@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    @Mock
    private UserJdbcRepository repository;

    @InjectMocks
    private UsersService service;

    private Users testUser;
    private UUID testUserId;
    private UUID testFamilyInfoId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testFamilyInfoId = UUID.randomUUID();

        testUser = new Users();
        testUser.setUserID(testUserId);
        testUser.setAccount("testuser@example.com");
        testUser.setPassword("password123");
        testUser.setName("測試使用者");
        testUser.setEmail("testuser@example.com");
        testUser.setPhoneNumber("0912345678");
        testUser.setMailingAddress("台北市測試路123號");
        testUser.setAccountStatus((byte) 1);
        testUser.setFamilyInfoID(testFamilyInfoId);
    }

    @Test
    void testCreateUser_Success() {
        // Given
        when(repository.save(any(Users.class))).thenReturn(testUser);

        // When
        Users result = service.createUser(testUser);

        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserID());
        assertEquals("testuser@example.com", result.getAccount());
        assertEquals("測試使用者", result.getName());
        verify(repository, times(1)).save(testUser);
    }

    @Test
    void testGetUserById_Success() {
        // Given
        when(repository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        Optional<Users> result = service.getUserById(testUserId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUserId, result.get().getUserID());
        assertEquals("testuser@example.com", result.get().getAccount());
        verify(repository, times(1)).findById(testUserId);
    }

    @Test
    void testGetUserById_NotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Optional<Users> result = service.getUserById(nonExistentId);

        // Then
        assertFalse(result.isPresent());
        verify(repository, times(1)).findById(nonExistentId);
    }

    @Test
    void testGetAllUsers_Success() {
        // Given
        List<Users> userList = Arrays.asList(testUser);
        when(repository.findAll()).thenReturn(userList);

        // When
        List<Users> result = service.getAllUsers();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUserId, result.get(0).getUserID());
        verify(repository, times(1)).findAll();
    }

    @Test
    void testUpdateUser_Success() {
        // Given
        testUser.setName("更新後的名稱");
        when(repository.save(any(Users.class))).thenReturn(testUser);

        // When
        Users result = service.updateUser(testUserId, testUser);

        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserID());
        assertEquals("更新後的名稱", result.getName());
        verify(repository, times(1)).save(testUser);
    }

    @Test
    void testGetUsersWithOffsetAndInstitutionNameJdbc_Success() {
        // Given
        int offset = 0;
        int size = 10;
        UserSummaryDTO summaryDTO = new UserSummaryDTO();
        summaryDTO.setUserID(testUserId);
        summaryDTO.setAccount("testuser@example.com");
        summaryDTO.setInstitutionName("測試機構");
        summaryDTO.setPermissionType((byte) 1);
        summaryDTO.setAccountStatus((byte) 1);

        List<UserSummaryDTO> summaryList = Arrays.asList(summaryDTO);
        when(repository.findWithOffsetAndInstitutionName(offset, size)).thenReturn(summaryList);

        // When
        List<UserSummaryDTO> result = service.getUsersWithOffsetAndInstitutionNameJdbc(offset, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser@example.com", result.get(0).getAccount());
        assertEquals("測試機構", result.get(0).getInstitutionName());
        verify(repository, times(1)).findWithOffsetAndInstitutionName(offset, size);
    }

    @Test
    void testGetUsersWithOffsetAndInstitutionNameJdbc_Exception() {
        // Given
        int offset = 0;
        int size = 10;
        when(repository.findWithOffsetAndInstitutionName(offset, size))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            service.getUsersWithOffsetAndInstitutionNameJdbc(offset, size);
        });
        verify(repository, times(1)).findWithOffsetAndInstitutionName(offset, size);
    }

    @Test
    void testGetTotalCount_Success() {
        // Given
        long expectedCount = 50L;
        when(repository.countTotal()).thenReturn(expectedCount);

        // When
        long result = service.getTotalCount();

        // Then
        assertEquals(expectedCount, result);
        verify(repository, times(1)).countTotal();
    }

    @Test
    void testGetTotalCount_Exception() {
        // Given
        when(repository.countTotal()).thenThrow(new RuntimeException("Database error"));

        // When
        long result = service.getTotalCount();

        // Then
        assertEquals(0, result);
        verify(repository, times(1)).countTotal();
    }

    @Test
    void testSaveUsingJdbc_Success() {
        // Given
        when(repository.save(any(Users.class))).thenReturn(testUser);

        // When
        Users result = service.saveUsingJdbc(testUser);

        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserID());
        verify(repository, times(1)).save(testUser);
    }

    @Test
    void testSaveUsingJdbc_NullUser() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            service.saveUsingJdbc(null);
        });
        verify(repository, never()).save(any());
    }

    @Test
    void testSaveUsingJdbc_Exception() {
        // Given
        when(repository.save(any(Users.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            service.saveUsingJdbc(testUser);
        });
        verify(repository, times(1)).save(testUser);
    }

    @Test
    void testUpdateAccountStatus_Success() {
        // Given
        Integer newStatus = 0;
        when(repository.updateAccountStatus(testUserId, newStatus)).thenReturn(1);
        testUser.setAccountStatus((byte) 0);
        when(repository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        Users result = service.updateAccountStatus(testUserId, newStatus);

        // Then
        assertNotNull(result);
        assertEquals((byte) 0, result.getAccountStatus());
        verify(repository, times(1)).updateAccountStatus(testUserId, newStatus);
        verify(repository, times(1)).findById(testUserId);
    }

    @Test
    void testUpdateAccountStatus_UserNotFound() {
        // Given
        int newStatus = 0;
        when(repository.updateAccountStatus(testUserId, newStatus)).thenReturn(0);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            service.updateAccountStatus(testUserId, newStatus);
        });
        verify(repository, times(1)).updateAccountStatus(testUserId, newStatus);
    }

    @Test
    void testUpdateUserProfile_Success() {
        // Given
        String newName = "更新的名字";
        String newEmail = "newemail@example.com";
        String newPhone = "0987654321";
        String newAddress = "新北市新路456號";
        when(repository.updateProfile(testUserId, newName, newEmail, newPhone, newAddress)).thenReturn(1);

        // When
        int result = service.updateUserProfile(testUserId, newName, newEmail, newPhone, newAddress);

        // Then
        assertEquals(1, result);
        verify(repository, times(1)).updateProfile(testUserId, newName, newEmail, newPhone, newAddress);
    }

    @Test
    void testUpdateUserProfile_Exception() {
        // Given
        String newName = "更新的名字";
        when(repository.updateProfile(eq(testUserId), eq(newName), any(), any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            service.updateUserProfile(testUserId, newName, null, null, null);
        });
        verify(repository, times(1)).updateProfile(testUserId, newName, null, null, null);
    }
}

