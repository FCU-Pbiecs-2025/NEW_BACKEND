package Group4.Childcare.controller;

import Group4.Childcare.Model.Users;
import Group4.Childcare.DTO.UserSummaryDTO;
import Group4.Childcare.Service.UsersService;
import Group4.Childcare.Service.ChildInfoService;
import Group4.Childcare.Service.ParentInfoService;
import Group4.Childcare.Controller.UsersController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UsersController 單元測試
 *
 * 測試範圍：
 * 1. createUser() - 創建使用者
 * 2. getUserById() - 根據ID查詢使用者
 * 3. getAllUsers() - 查詢所有使用者
 * 4. getUsersWithOffsetAndInstitutionNameJdbc() - 分頁查詢使用者
 * 5. updateUser() - 更新使用者
 */
@ExtendWith(MockitoExtension.class)
class UsersControllerTest {

    @Mock
    private UsersService usersService;

    @Mock
    private ChildInfoService childInfoService;

    @Mock
    private ParentInfoService parentInfoService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsersController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Users testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        testUserId = UUID.randomUUID();
        testUser = new Users();
        testUser.setUserID(testUserId);
        testUser.setAccount("testuser@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setName("測試使用者");
        testUser.setEmail("testuser@example.com");
        testUser.setPhoneNumber("0912345678");
        testUser.setMailingAddress("台北市測試路123號");
        testUser.setAccountStatus((byte) 1);
        testUser.setPermissionType((byte) 2);
    }

    // ============================================================
    // changePassword 測試
    // ============================================================

    @Test
    void testChangePassword_Success() throws Exception {
        // Given
        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(usersService.updateUser(eq(testUserId), any(Users.class))).thenReturn(testUser);

        Map<String, String> passwordRequest = Map.of("newPassword", "newPassword123");

        // When & Then
        mockMvc.perform(put("/users/{id}/password", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("密碼修改成功")));

        verify(passwordEncoder).encode("newPassword123");
        verify(usersService).updateUser(eq(testUserId), any(Users.class));
    }

    @Test
    void testChangePassword_NewPasswordEmpty() throws Exception {
        Map<String, String> passwordRequest = Map.of("newPassword", " ");

        mockMvc.perform(put("/users/{id}/password", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("新密碼不能為空")));
    }

    @Test
    void testChangePassword_NewPasswordNull() throws Exception {
        // 模擬 newPassword 為 null 的情況
        // 注意：Map.of 不允許 value 為 null，所以改用 HashMap
        Map<String, String> passwordRequest = new HashMap<>();
        passwordRequest.put("newPassword", null);

        mockMvc.perform(put("/users/{id}/password", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("新密碼不能為空")));
    }

    @Test
    void testChangePassword_NewPasswordTooShort() throws Exception {
        Map<String, String> passwordRequest = Map.of("newPassword", "12345");

        mockMvc.perform(put("/users/{id}/password", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("密碼長度至少為 6 個字元")));
    }

    @Test
    void testChangePassword_UserNotFound() throws Exception {
        when(usersService.getUserById(testUserId)).thenReturn(Optional.empty());
        Map<String, String> passwordRequest = Map.of("newPassword", "newPassword123");

        mockMvc.perform(put("/users/{id}/password", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("使用者不存在")));
    }

    @Test
    void testChangePassword_PermissionDenied() throws Exception {
        testUser.setPermissionType((byte) 1); // 設定為非 admin
        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        Map<String, String> passwordRequest = Map.of("newPassword", "newPassword123");

        mockMvc.perform(put("/users/{id}/password", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("只有管理員可以修改密碼")));
    }

    @Test
    void testChangePassword_PermissionNull() throws Exception {
        testUser.setPermissionType(null); // 設定為 null
        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        Map<String, String> passwordRequest = Map.of("newPassword", "newPassword123");

        mockMvc.perform(put("/users/{id}/password", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("只有管理員可以修改密碼")));
    }

    @Test
    void testChangePassword_ServiceException() throws Exception {
        when(usersService.getUserById(testUserId)).thenThrow(new RuntimeException("DB Error"));
        Map<String, String> passwordRequest = Map.of("newPassword", "newPassword123");

        mockMvc.perform(put("/users/{id}/password", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("密碼修改失敗，請稍後再試")));
    }

    // ... (原有的測試)
    @Test
    void testCreateUser_Success() throws Exception {
        // Given
        when(usersService.createUser(any(Users.class))).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userID", is(testUserId.toString())))
                .andExpect(jsonPath("$.account", is("testuser@example.com")))
                .andExpect(jsonPath("$.name", is("測試使用者")));

        verify(usersService, times(1)).createUser(any(Users.class));
    }

    @Test
    void testGetUserById_Success() throws Exception {
        // Given
        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/users/{id}", testUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userID", is(testUserId.toString())))
                .andExpect(jsonPath("$.account", is("testuser@example.com")))
                .andExpect(jsonPath("$.name", is("測試使用者")));

        verify(usersService, times(1)).getUserById(testUserId);
    }

    @Test
    void testGetUserById_NotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(usersService.getUserById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/users/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(usersService, times(1)).getUserById(nonExistentId);
    }

    @Test
    void testGetAllUsers_Success() throws Exception {
        // Given
        Users anotherUser = new Users();
        anotherUser.setUserID(UUID.randomUUID());
        anotherUser.setAccount("anotheruser@example.com");
        anotherUser.setName("另一個使用者");
        List<Users> users = Arrays.asList(testUser, anotherUser);
        when(usersService.getAllUsers()).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].account", is("testuser@example.com")))
                .andExpect(jsonPath("$[1].account", is("anotheruser@example.com")));

        verify(usersService, times(1)).getAllUsers();
    }

    @Test
    void testGetAllUsers_EmptyList() throws Exception {
        // Given
        when(usersService.getAllUsers()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(usersService, times(1)).getAllUsers();
    }

    @Test
    void testGetUsersWithOffsetAndInstitutionNameJdbc_Success() throws Exception {
        // Given
        int offset = 0;
        int size = 10;
        UserSummaryDTO summaryDTO = new UserSummaryDTO();
        summaryDTO.setUserID(testUserId);
        summaryDTO.setAccount("testuser@example.com");
        summaryDTO.setInstitutionName("測試機構");
        summaryDTO.setPermissionType((byte) 2);
        summaryDTO.setAccountStatus((byte) 1);

        List<UserSummaryDTO> users = Arrays.asList(summaryDTO);
        when(usersService.getUsersWithOffsetAndInstitutionNameJdbc(offset, size)).thenReturn(users);
        when(usersService.getTotalCount()).thenReturn(1L);

        // When & Then
        mockMvc.perform(get("/users/offset")
                .param("offset", String.valueOf(offset))
                .param("size", String.valueOf(size))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offset", is(offset)))
                .andExpect(jsonPath("$.size", is(size)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].account", is("testuser@example.com")));

        verify(usersService, times(1)).getUsersWithOffsetAndInstitutionNameJdbc(offset, size);
        verify(usersService, times(1)).getTotalCount();
    }

    @Test
    void testUpdateUser_Success() throws Exception {
        // Given
        testUser.setName("更新後的名字");
        testUser.setEmail("newemail@example.com");
        when(usersService.updateUser(any(UUID.class), any(Users.class))).thenReturn(testUser);

        // When & Then
        mockMvc.perform(put("/users/{id}", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("更新後的名字")))
                .andExpect(jsonPath("$.email", is("newemail@example.com")));

        verify(usersService, times(1)).updateUser(any(UUID.class), any(Users.class));
    }

    @Test
    void testGetUserWithFamilyInfo_Success() throws Exception {
        // Given
        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/users/{id}", testUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionType", is(2)));

        verify(usersService, times(1)).getUserById(testUserId);
    }

    // ===== updateAccountStatus 測試 =====
    @Test
    void testUpdateAccountStatus_Success() throws Exception {
        when(usersService.updateAccountStatus(eq(testUserId), eq(1))).thenReturn(testUser);

        mockMvc.perform(put("/users/{id}/status", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountStatus\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testUpdateAccountStatus_MissingAccountStatus() throws Exception {
        mockMvc.perform(put("/users/{id}/status", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void testUpdateAccountStatus_InvalidValue() throws Exception {
        mockMvc.perform(put("/users/{id}/status", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountStatus\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("accountStatus must be 1 or 2")));
    }

    @Test
    void testUpdateAccountStatus_StringValue() throws Exception {
        when(usersService.updateAccountStatus(eq(testUserId), eq(2))).thenReturn(testUser);

        mockMvc.perform(put("/users/{id}/status", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountStatus\": \"2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testUpdateAccountStatus_InvalidStringValue() throws Exception {
        mockMvc.perform(put("/users/{id}/status", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountStatus\": \"invalid\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateAccountStatus_Exception() throws Exception {
        when(usersService.updateAccountStatus(any(UUID.class), any(Integer.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(put("/users/{id}/status", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountStatus\": 1}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ===== updateUserProfile 測試 =====
    @Test
    void testUpdateUserProfile_Success() throws Exception {
        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(usersService.updateUserProfile(eq(testUserId), any(), any(), any(), any())).thenReturn(1);

        mockMvc.perform(put("/users/{id}/profile", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"新名字\", \"email\": \"new@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testUpdateUserProfile_UserNotFound() throws Exception {
        when(usersService.getUserById(testUserId)).thenReturn(Optional.empty());

        mockMvc.perform(put("/users/{id}/profile", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"新名字\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateUserProfile_NoFieldsUpdated() throws Exception {
        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(usersService.updateUserProfile(eq(testUserId), any(), any(), any(), any())).thenReturn(0);

        mockMvc.perform(put("/users/{id}/profile", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"新名字\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void testUpdateUserProfile_Exception() throws Exception {
        when(usersService.getUserById(testUserId)).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(put("/users/{id}/profile", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"新名字\"}"))
                .andExpect(status().isInternalServerError());
    }

    // ===== searchUsersByAccount 測試 =====
    @Test
    void testSearchUsersByAccount_Success() throws Exception {
        mockMvc.perform(get("/users/search")
                .param("account", "test")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    // ===== searchCitizenUsersByAccount 測試 =====
    @Test
    void testSearchCitizenUsersByAccount_Success() throws Exception {
        UserSummaryDTO dto = new UserSummaryDTO();
        dto.setUserID(testUserId);
        when(usersService.searchCitizenUsersByAccountWithOffset(eq("test"), eq(0), eq(10)))
                .thenReturn(Arrays.asList(dto));
        when(usersService.getSearchCitizenTotalCount(eq("test"))).thenReturn(1L);

        mockMvc.perform(get("/users/SEARCH3")
                .param("account", "test"))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchCitizenUsersByAccount_EmptyAccount() throws Exception {
        UserSummaryDTO dto = new UserSummaryDTO();
        when(usersService.searchCitizenUsersByAccountWithOffset(eq(""), eq(0), eq(10))).thenReturn(Arrays.asList(dto));
        when(usersService.getSearchCitizenTotalCount(eq(""))).thenReturn(1L);

        mockMvc.perform(get("/users/SEARCH3"))
                .andExpect(status().isOk());
    }

    // ==================== 新增測試用例以提升覆蓋率 ====================

    // ===== getUsersByOffsetJdbc 邊界測試 =====
    @Test
    void testGetUsersByOffsetJdbc_InvalidOffset() throws Exception {
        mockMvc.perform(get("/users/offset")
                .param("offset", "-1")
                .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid offset/size")));
    }

    @Test
    void testGetUsersByOffsetJdbc_InvalidSize() throws Exception {
        mockMvc.perform(get("/users/offset")
                .param("offset", "0")
                .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid offset/size")));
    }

    @Test
    void testGetUsersByOffsetJdbc_LargeSize() throws Exception {
        // 測試 size 超過 MAX_SIZE 會被限制
        when(usersService.getUsersWithOffsetAndInstitutionNameJdbc(eq(0), eq(100))).thenReturn(Arrays.asList());
        when(usersService.getTotalCount()).thenReturn(0L);

        mockMvc.perform(get("/users/offset")
                .param("offset", "0")
                .param("size", "200")) // 超過 MAX_SIZE=100
                .andExpect(status().isOk());
    }

    @Test
    void testGetUsersByOffsetJdbc_Exception() throws Exception {
        when(usersService.getUsersWithOffsetAndInstitutionNameJdbc(eq(0), eq(10)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/users/offset")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Internal server error")));
    }

    // ===== searchUsersByAccount (search2) 測試 =====
    @Test
    void testSearchUsersByAccount_EmptyAccount() throws Exception {
        mockMvc.perform(get("/users/search2")
                .param("account", "")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("查詢帳號不能為空")));
    }

    @Test
    void testSearchUsersByAccount_InvalidOffset() throws Exception {
        mockMvc.perform(get("/users/search2")
                .param("account", "test")
                .param("offset", "-1")
                .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchUsersByAccount_Exception() throws Exception {
        when(usersService.searchUsersByAccountWithOffset(anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/users/search2")
                .param("account", "test")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isInternalServerError());
    }

    // ===== searchCitizenUsersByAccount (SEARCH3) 測試 =====
    @Test
    void testSearchCitizenUsersByAccount_InvalidOffset() throws Exception {
        mockMvc.perform(get("/users/SEARCH3")
                .param("account", "test")
                .param("offset", "-1")
                .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchCitizenUsersByAccount_Exception() throws Exception {
        when(usersService.searchCitizenUsersByAccountWithOffset(anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/users/SEARCH3")
                .param("account", "test")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isInternalServerError());
    }

    // ===== searchUsers 測試 =====
    @Test
    void testSearchUsers_WithSearchTerm() throws Exception {
        when(usersService.searchUsersWithOffset(eq("test"), eq(0), eq(10))).thenReturn(Arrays.asList());
        when(usersService.getSearchCount(eq("test"))).thenReturn(0L);

        mockMvc.perform(get("/users/search")
                .param("searchTerm", "test")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchTerm", is("test")));
    }

    @Test
    void testSearchUsers_EmptySearchTerm() throws Exception {
        when(usersService.getUsersWithOffsetAndInstitutionNameJdbc(eq(0), eq(10))).thenReturn(Arrays.asList());
        when(usersService.getTotalCount()).thenReturn(0L);

        mockMvc.perform(get("/users/search")
                .param("searchTerm", "")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchUsers_NoSearchTerm() throws Exception {
        when(usersService.getUsersWithOffsetAndInstitutionNameJdbc(eq(0), eq(10))).thenReturn(Arrays.asList());
        when(usersService.getTotalCount()).thenReturn(0L);

        mockMvc.perform(get("/users/search")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchUsers_InvalidOffset() throws Exception {
        mockMvc.perform(get("/users/search")
                .param("offset", "-1")
                .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchUsers_Exception() throws Exception {
        when(usersService.getUsersWithOffsetAndInstitutionNameJdbc(anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/users/search")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isInternalServerError());
    }

    // ===== checkAccountExists 測試 =====
    @Test
    void testCheckAccountExists_True() throws Exception {
        when(usersService.isAccountExists("existingAccount")).thenReturn(true);

        mockMvc.perform(get("/users/check-account/{account}", "existingAccount"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckAccountExists_False() throws Exception {
        when(usersService.isAccountExists("newAccount")).thenReturn(false);

        mockMvc.perform(get("/users/check-account/{account}", "newAccount"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testCheckAccountExists_EmptyAccount() throws Exception {
        mockMvc.perform(get("/users/check-account/{account}", " "))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("false"));
    }

    @Test
    void testCheckAccountExists_Exception() throws Exception {
        when(usersService.isAccountExists("test")).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/users/check-account/{account}", "test"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("false"));
    }

    // ===== checkEmailExists 測試 =====
    @Test
    void testCheckEmailExists_True() throws Exception {
        when(usersService.isEmailExists("existing@email.com")).thenReturn(true);

        mockMvc.perform(get("/users/check-email/{email}", "existing@email.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckEmailExists_False() throws Exception {
        when(usersService.isEmailExists("new@email.com")).thenReturn(false);

        mockMvc.perform(get("/users/check-email/{email}", "new@email.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testCheckEmailExists_EmptyEmail() throws Exception {
        mockMvc.perform(get("/users/check-email/{email}", " "))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("false"));
    }

    @Test
    void testCheckEmailExists_Exception() throws Exception {
        when(usersService.isEmailExists("test@email.com")).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/users/check-email/{email}", "test@email.com"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("false"));
    }

    // ===== getUserFamilyInfo 測試 =====
    @Test
    void testGetUserFamilyInfo_Success() throws Exception {
        testUser.setFamilyInfoID(UUID.randomUUID());
        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(parentInfoService.getByFamilyInfoID(any())).thenReturn(Arrays.asList());
        when(childInfoService.getByFamilyInfoID(any())).thenReturn(Arrays.asList());

        mockMvc.perform(get("/users/users-familyInfo/{userID}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userID").exists());
    }

    @Test
    void testGetUserFamilyInfo_NotFound() throws Exception {
        when(usersService.getUserById(testUserId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/users-familyInfo/{userID}", testUserId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUserFamilyInfo_NoFamilyInfo() throws Exception {
        testUser.setFamilyInfoID(null);
        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/users/users-familyInfo/{userID}", testUserId))
                .andExpect(status().isOk());

        verify(parentInfoService, never()).getByFamilyInfoID(any());
        verify(childInfoService, never()).getByFamilyInfoID(any());
    }

    // ===== createOrUpdateUserJdbc (/new-member) 測試 =====
    @Test
    void testCreateOrUpdateUserJdbc_Success() throws Exception {
        Users newUser = new Users();
        newUser.setAccount("newuser");
        newUser.setPassword("password123");
        newUser.setName("新使用者");
        newUser.setEmail("new@example.com");

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(usersService.isAccountExists("newuser")).thenReturn(false);
        when(usersService.isEmailExists("new@example.com")).thenReturn(false);
        when(usersService.saveUsingJdbc(any(Users.class))).thenReturn(testUser);

        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.message", is("註冊成功")));

        verify(passwordEncoder).encode("password123");
        verify(usersService).saveUsingJdbc(any(Users.class));
    }

    @Test
    void testCreateOrUpdateUserJdbc_NullBody() throws Exception {
        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrUpdateUserJdbc_MissingAccount() throws Exception {
        Users newUser = new Users();
        newUser.setPassword("password123");

        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("Missing required fields")))
                .andExpect(jsonPath("$.missingFields[0]", is("account")));
    }

    @Test
    void testCreateOrUpdateUserJdbc_MissingPassword() throws Exception {
        Users newUser = new Users();
        newUser.setAccount("newuser");
        newUser.setEmail("new@example.com");

        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.missingFields[0]", is("password")));
    }

    @Test
    void testCreateOrUpdateUserJdbc_EmptyAccount() throws Exception {
        Users newUser = new Users();
        newUser.setAccount("  ");
        newUser.setPassword("password123");

        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.missingFields[0]", is("account")));
    }

    @Test
    void testCreateOrUpdateUserJdbc_AccountExists() throws Exception {
        Users newUser = new Users();
        newUser.setAccount("existinguser");
        newUser.setPassword("password123");

        when(usersService.isAccountExists("existinguser")).thenReturn(true);

        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("Account already exists")));
    }

    @Test
    void testCreateOrUpdateUserJdbc_EmailExists() throws Exception {
        Users newUser = new Users();
        newUser.setAccount("newuser");
        newUser.setPassword("password123");
        newUser.setEmail("existing@example.com");

        when(usersService.isAccountExists("newuser")).thenReturn(false);
        when(usersService.isEmailExists("existing@example.com")).thenReturn(true);

        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("Email already exists")));
    }

    @Test
    void testCreateOrUpdateUserJdbc_WithAllFields() throws Exception {
        Users newUser = new Users();
        newUser.setAccount("  newuser  ");
        newUser.setPassword("  password123  ");
        newUser.setName("  新使用者  ");
        newUser.setNationalID("  A123456789  ");
        newUser.setMailingAddress("  台北市  ");
        newUser.setEmail("  new@example.com  ");
        newUser.setPhoneNumber("  0912345678  ");

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(usersService.isAccountExists("newuser")).thenReturn(false);
        when(usersService.isEmailExists("new@example.com")).thenReturn(false);
        when(usersService.saveUsingJdbc(any(Users.class))).thenReturn(testUser);

        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(usersService).saveUsingJdbc(any(Users.class));
    }

    @Test
    void testCreateOrUpdateUserJdbc_UpdateExistingUser() throws Exception {
        Users existingUser = new Users();
        existingUser.setUserID(testUserId); // 有 userID 表示更新
        existingUser.setAccount("existinguser");
        existingUser.setPassword("  newpassword  ");

        when(usersService.saveUsingJdbc(any(Users.class))).thenReturn(testUser);

        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(existingUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void testCreateOrUpdateUserJdbc_WithDefaultValues() throws Exception {
        Users newUser = new Users();
        newUser.setAccount("newuser");
        newUser.setPassword("password123");
        // accountStatus 和 permissionType 為 null，應設定預設值

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(usersService.isAccountExists("newuser")).thenReturn(false);
        when(usersService.saveUsingJdbc(any(Users.class))).thenReturn(testUser);

        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testCreateOrUpdateUserJdbc_EmptyEmail() throws Exception {
        Users newUser = new Users();
        newUser.setAccount("newuser");
        newUser.setPassword("password123");
        newUser.setEmail(""); // 空字串不檢查

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(usersService.isAccountExists("newuser")).thenReturn(false);
        when(usersService.saveUsingJdbc(any(Users.class))).thenReturn(testUser);

        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(usersService, never()).isEmailExists(anyString());
    }

    @Test
    void testCreateOrUpdateUserJdbc_Exception() throws Exception {
        Users newUser = new Users();
        newUser.setAccount("newuser");
        newUser.setPassword("password123");

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(usersService.isAccountExists("newuser")).thenReturn(false);
        when(usersService.saveUsingJdbc(any(Users.class))).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(500)));
    }

    // ===== updateUserProfile 額外分支測試 =====
    @Test
    void testUpdateUserProfile_AllFieldsNull() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("name", null);
        request.put("email", null);
        request.put("phoneNumber", null);
        request.put("mailingAddress", null);

        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(usersService.updateUserProfile(eq(testUserId), any(), any(), any(), any())).thenReturn(1);

        mockMvc.perform(put("/users/{id}/profile", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateUserProfile_EmptyStrings() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("name", "  ");
        request.put("email", "  ");

        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(usersService.updateUserProfile(eq(testUserId), any(), any(), any(), any())).thenReturn(1);

        mockMvc.perform(put("/users/{id}/profile", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateUserProfile_PartialFields() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("name", "新名字");
        request.put("phoneNumber", "0987654321");

        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(usersService.updateUserProfile(eq(testUserId), eq("新名字"), eq(null), eq("0987654321"), eq(null)))
                .thenReturn(1);

        mockMvc.perform(put("/users/{id}/profile", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    // ===== searchUsersByAccount (search2) 額外分支測試 =====
    @Test
    void testSearchUsersByAccount2_NullAccount() throws Exception {
        mockMvc.perform(get("/users/search2")
                .param("account", "")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("查詢帳號不能為空")));
    }

    @Test
    void testSearchUsersByAccount2_WhitespaceAccount() throws Exception {
        mockMvc.perform(get("/users/search2")
                .param("account", "   ")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("查詢帳號不能為空")));
    }

    @Test
    void testSearchUsersByAccount2_LargeSize() throws Exception {
        when(usersService.searchUsersByAccountWithOffset(eq("test"), eq(0), eq(100)))
                .thenReturn(Arrays.asList());
        when(usersService.getSearchTotalCount(eq("test"))).thenReturn(0L);

        mockMvc.perform(get("/users/search2")
                .param("account", "test")
                .param("offset", "0")
                .param("size", "200"))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchUsersByAccount2_InvalidSize() throws Exception {
        mockMvc.perform(get("/users/search2")
                .param("account", "test")
                .param("offset", "0")
                .param("size", "-1"))
                .andExpect(status().isBadRequest());
    }

    // ===== searchCitizenUsersByAccount (SEARCH3) 額外分支測試 =====
    @Test
    void testSearchCitizenUsersByAccount_NullAccount() throws Exception {
        when(usersService.searchCitizenUsersByAccountWithOffset(eq(""), eq(0), eq(10)))
                .thenReturn(Arrays.asList());
        when(usersService.getSearchCitizenTotalCount(eq(""))).thenReturn(0L);

        mockMvc.perform(get("/users/SEARCH3")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchCitizenUsersByAccount_LargeSize() throws Exception {
        when(usersService.searchCitizenUsersByAccountWithOffset(eq("test"), eq(0), eq(100)))
                .thenReturn(Arrays.asList());
        when(usersService.getSearchCitizenTotalCount(eq("test"))).thenReturn(0L);

        mockMvc.perform(get("/users/SEARCH3")
                .param("account", "test")
                .param("offset", "0")
                .param("size", "150"))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchCitizenUsersByAccount_InvalidSize() throws Exception {
        mockMvc.perform(get("/users/SEARCH3")
                .param("account", "test")
                .param("offset", "0")
                .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    // ===== searchUsers 額外分支測試 =====
    @Test
    void testSearchUsers_LargeSize() throws Exception {
        when(usersService.getUsersWithOffsetAndInstitutionNameJdbc(eq(0), eq(100)))
                .thenReturn(Arrays.asList());
        when(usersService.getTotalCount()).thenReturn(0L);

        mockMvc.perform(get("/users/search")
                .param("offset", "0")
                .param("size", "200"))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchUsers_InvalidSize() throws Exception {
        mockMvc.perform(get("/users/search")
                .param("offset", "0")
                .param("size", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchUsers_WhitespaceSearchTerm() throws Exception {
        when(usersService.getUsersWithOffsetAndInstitutionNameJdbc(eq(0), eq(10)))
                .thenReturn(Arrays.asList());
        when(usersService.getTotalCount()).thenReturn(0L);

        mockMvc.perform(get("/users/search")
                .param("searchTerm", "   ")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    // ===== updateAccountStatus 額外分支測試 =====
    @Test
    void testUpdateAccountStatus_NumberValue() throws Exception {
        when(usersService.updateAccountStatus(eq(testUserId), eq(1))).thenReturn(testUser);

        mockMvc.perform(put("/users/{id}/status", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountStatus\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testUpdateAccountStatus_Value3() throws Exception {
        mockMvc.perform(put("/users/{id}/status", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountStatus\": 3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("accountStatus must be 1 or 2")));
    }

    // ============================================================
    // 提升分支覆蓋率的額外測試 - 針對未覆蓋分支
    // ============================================================

    @Test
    void testCreateOrUpdateUserJdbc_NullUser() throws Exception {
        // 測試 user 為 null 的情況 (L472)
        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrUpdateUserJdbc_PasswordOnlyWhitespace() throws Exception {
        // 測試 password 只有空白的情況 (L484)
        Users user = new Users();
        user.setAccount("testaccount");
        user.setPassword("   ");

        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Missing required fields")))
                .andExpect(jsonPath("$.missingFields[0]", is("password")));
    }

    @Test
    void testCreateOrUpdateUserJdbc_DefaultAccountStatus() throws Exception {
        // 測試預設 accountStatus 為 null 時設定為 1 (L548)
        Users user = new Users();
        user.setAccount("newuser123");
        user.setPassword("password123");
        user.setAccountStatus(null);

        Users savedUser = new Users();
        savedUser.setUserID(UUID.randomUUID());
        savedUser.setAccount("newuser123");
        savedUser.setPassword("encodedPassword");
        savedUser.setAccountStatus((byte) 1);

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(usersService.isAccountExists("newuser123")).thenReturn(false);
        when(usersService.saveUsingJdbc(any(Users.class))).thenReturn(savedUser);

        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(usersService).saveUsingJdbc(argThat(u -> u.getAccountStatus() == 1));
    }

    @Test
    void testCreateOrUpdateUserJdbc_DefaultPermissionType() throws Exception {
        // 測試預設 permissionType 為 null 時設定為 1 (L551)
        Users user = new Users();
        user.setAccount("newuser456");
        user.setPassword("password456");
        user.setPermissionType(null);

        Users savedUser = new Users();
        savedUser.setUserID(UUID.randomUUID());
        savedUser.setAccount("newuser456");
        savedUser.setPassword("encodedPassword");
        savedUser.setPermissionType((byte) 1);

        when(passwordEncoder.encode("password456")).thenReturn("encodedPassword");
        when(usersService.isAccountExists("newuser456")).thenReturn(false);
        when(usersService.saveUsingJdbc(any(Users.class))).thenReturn(savedUser);

        mockMvc.perform(post("/users/new-member")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(usersService).saveUsingJdbc(argThat(u -> u.getPermissionType() == 1));
    }

    @Test
    void testSearchUsersByAccount_OnlyWhitespace() throws Exception {
        // 測試 account 只有空白字元的情況 (L193)
        mockMvc.perform(get("/users/search2")
                .param("account", "   ")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("查詢帳號不能為空")));
    }

    @Test
    void testSearchUsersByAccount_HasNextTrue() throws Exception {
        // 測試 hasNext 為 true 的情況 (L215)
        UserSummaryDTO user1 = new UserSummaryDTO();
        user1.setUserID(UUID.randomUUID());
        user1.setAccount("user1");
        user1.setInstitutionName(null);
        user1.setPermissionType((byte) 1);
        user1.setAccountStatus((byte) 1);

        UserSummaryDTO user2 = new UserSummaryDTO();
        user2.setUserID(UUID.randomUUID());
        user2.setAccount("user2");
        user2.setInstitutionName(null);
        user2.setPermissionType((byte) 1);
        user2.setAccountStatus((byte) 1);

        List<UserSummaryDTO> users = Arrays.asList(user1, user2);

        when(usersService.searchUsersByAccountWithOffset("test", 0, 2)).thenReturn(users);
        when(usersService.getSearchTotalCount("test")).thenReturn(10L);

        mockMvc.perform(get("/users/search2")
                .param("account", "test")
                .param("offset", "0")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext", is(true)))
                .andExpect(jsonPath("$.totalElements", is(10)));
    }

    @Test
    void testSearchCitizenUsersByAccount_AccountParameterNull() throws Exception {
        // 測試 account 參數為 null 的情況 (L269)
        UserSummaryDTO citizen1 = new UserSummaryDTO();
        citizen1.setUserID(UUID.randomUUID());
        citizen1.setAccount("citizen1");
        citizen1.setInstitutionName(null);
        citizen1.setPermissionType((byte) 3);
        citizen1.setAccountStatus((byte) 1);

        List<UserSummaryDTO> users = Arrays.asList(citizen1);

        when(usersService.searchCitizenUsersByAccountWithOffset("", 0, 10)).thenReturn(users);
        when(usersService.getSearchCitizenTotalCount("")).thenReturn(1L);

        mockMvc.perform(get("/users/SEARCH3")
                .param("offset", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void testSearchCitizenUsersByAccount_HasNextTrue() throws Exception {
        // 測試 hasNext 為 true 的情況 (L280)
        UserSummaryDTO citizen1 = new UserSummaryDTO();
        citizen1.setUserID(UUID.randomUUID());
        citizen1.setAccount("citizen1");
        citizen1.setInstitutionName(null);
        citizen1.setPermissionType((byte) 3);
        citizen1.setAccountStatus((byte) 1);

        UserSummaryDTO citizen2 = new UserSummaryDTO();
        citizen2.setUserID(UUID.randomUUID());
        citizen2.setAccount("citizen2");
        citizen2.setInstitutionName(null);
        citizen2.setPermissionType((byte) 3);
        citizen2.setAccountStatus((byte) 1);

        List<UserSummaryDTO> users = Arrays.asList(citizen1, citizen2);

        when(usersService.searchCitizenUsersByAccountWithOffset("test", 0, 2)).thenReturn(users);
        when(usersService.getSearchCitizenTotalCount("test")).thenReturn(5L);

        mockMvc.perform(get("/users/SEARCH3")
                .param("account", "test")
                .param("offset", "0")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext", is(true)));
    }

    @Test
    void testUpdateAccountStatus_NonStringNonNumber() throws Exception {
        // 測試 accountStatus 既非 Number 也非 String 類型 (L344)
        mockMvc.perform(put("/users/{id}/status", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountStatus\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("accountStatus must be 1 or 2")));
    }

    @Test
    void testUpdateUserProfile_NameIsNull() throws Exception {
        // 測試 name 為 null 的情況 (L404)
        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(usersService.updateUserProfile(eq(testUserId), isNull(), eq("new@email.com"), isNull(), isNull()))
                .thenReturn(1);
        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));

        Map<String, Object> request = new HashMap<>();
        request.put("name", null);
        request.put("email", "new@email.com");

        mockMvc.perform(put("/users/{id}/profile", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(usersService).updateUserProfile(eq(testUserId), isNull(), eq("new@email.com"), isNull(), isNull());
    }

    @Test
    void testUpdateUserProfile_MailingAddressNotNull() throws Exception {
        // 測試 mailingAddress 不為 null 的情況 (L413-414)
        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(usersService.updateUserProfile(eq(testUserId), eq("新名字"), isNull(), isNull(), eq("新地址123號")))
                .thenReturn(1);
        when(usersService.getUserById(testUserId)).thenReturn(Optional.of(testUser));

        Map<String, String> request = Map.of("name", "新名字", "mailingAddress", "新地址123號");

        mockMvc.perform(put("/users/{id}/profile", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(usersService).updateUserProfile(eq(testUserId), eq("新名字"), isNull(), isNull(), eq("新地址123號"));
    }

    @Test
    void testSearchUsers_HasNextTrue() throws Exception {
        // 測試 hasNext 為 true 的情況 (L765)
        UserSummaryDTO user1 = new UserSummaryDTO();
        user1.setUserID(UUID.randomUUID());
        user1.setAccount("user1");
        user1.setInstitutionName("機構A");
        user1.setPermissionType((byte) 2);
        user1.setAccountStatus((byte) 1);

        UserSummaryDTO user2 = new UserSummaryDTO();
        user2.setUserID(UUID.randomUUID());
        user2.setAccount("user2");
        user2.setInstitutionName("機構B");
        user2.setPermissionType((byte) 2);
        user2.setAccountStatus((byte) 1);

        List<UserSummaryDTO> users = Arrays.asList(user1, user2);

        when(usersService.searchUsersWithOffset("search", 0, 2)).thenReturn(users);
        when(usersService.getSearchCount("search")).thenReturn(8L);

        mockMvc.perform(get("/users/search")
                .param("searchTerm", "search")
                .param("offset", "0")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext", is(true)))
                .andExpect(jsonPath("$.totalElements", is(8)));
    }

    @Test
    void testGetUsersByOffsetJdbc_HasNextTrue() throws Exception {
        // 測試 hasNext 為 true 的情況 (L147)
        UserSummaryDTO user1 = new UserSummaryDTO();
        user1.setUserID(UUID.randomUUID());
        user1.setAccount("user1");
        user1.setInstitutionName("機構A");
        user1.setPermissionType((byte) 1);
        user1.setAccountStatus((byte) 1);

        UserSummaryDTO user2 = new UserSummaryDTO();
        user2.setUserID(UUID.randomUUID());
        user2.setAccount("user2");
        user2.setInstitutionName("機構B");
        user2.setPermissionType((byte) 2);
        user2.setAccountStatus((byte) 1);

        UserSummaryDTO user3 = new UserSummaryDTO();
        user3.setUserID(UUID.randomUUID());
        user3.setAccount("user3");
        user3.setInstitutionName("機構C");
        user3.setPermissionType((byte) 3);
        user3.setAccountStatus((byte) 1);

        List<UserSummaryDTO> users = Arrays.asList(user1, user2, user3);

        when(usersService.getUsersWithOffsetAndInstitutionNameJdbc(0, 3)).thenReturn(users);
        when(usersService.getTotalCount()).thenReturn(20L);

        mockMvc.perform(get("/users/offset")
                .param("offset", "0")
                .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext", is(true)))
                .andExpect(jsonPath("$.totalElements", is(20)));
    }

    @Test
    void testCheckAccountExists_OnlyWhitespace() throws Exception {
        // 測試 account 只有空白字元的情況 (L789)
        mockMvc.perform(get("/users/check-account/{account}", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("false"));
    }

    @Test
    void testCheckEmailExists_OnlyWhitespace() throws Exception {
        // 測試 email 只有空白字元的情況 (L811)
        mockMvc.perform(get("/users/check-email/{email}", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("false"));
    }
}

