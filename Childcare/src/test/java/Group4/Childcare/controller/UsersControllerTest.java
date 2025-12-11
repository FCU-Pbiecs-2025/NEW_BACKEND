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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
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
}

