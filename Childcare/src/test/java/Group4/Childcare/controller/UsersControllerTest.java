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
}
