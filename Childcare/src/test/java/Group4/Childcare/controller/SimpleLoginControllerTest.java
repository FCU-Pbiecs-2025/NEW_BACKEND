package Group4.Childcare.controller;

import Group4.Childcare.Controller.SimpleLoginController;
import Group4.Childcare.Model.Users;
import Group4.Childcare.Repository.UserJdbcRepository;
import Group4.Childcare.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SimpleLoginController 單元測試
 * 測試登入功能（一般用戶和管理員）
 */
@ExtendWith(MockitoExtension.class)
class SimpleLoginControllerTest {

    @Mock
    private UserJdbcRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private SimpleLoginController controller;

    private Users testUser;
    private Users adminUser;

    @BeforeEach
    void setUp() {
        testUser = new Users();
        testUser.setUserID(UUID.randomUUID());
        testUser.setAccount("testuser");
        testUser.setPassword("$2a$10$hashedPassword"); // BCrypt hashed
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPhoneNumber("0912345678");
        testUser.setPermissionType((byte) 3); // 一般用戶
        testUser.setAccountStatus((byte) 1); // 啟用

        adminUser = new Users();
        adminUser.setUserID(UUID.randomUUID());
        adminUser.setAccount("admin");
        adminUser.setPassword("$2a$10$hashedAdminPassword");
        adminUser.setName("Admin User");
        adminUser.setEmail("admin@example.com");
        adminUser.setPhoneNumber("0987654321");
        adminUser.setPermissionType((byte) 1); // 管理員
        adminUser.setAccountStatus((byte) 1); // 啟用
    }

    // ==================== login() Tests ====================

    @Test
    void testLogin_Success() {
        // Given
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "testuser");
        loginRequest.put("password", "plainPassword");

        when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("plainPassword", testUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(testUser)).thenReturn("mock.jwt.token");

        // When
        ResponseEntity<Map<String, Object>> response = controller.login(loginRequest);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals("登入成功", body.get("message"));
        assertEquals("mock.jwt.token", body.get("token"));
        assertNotNull(body.get("user"));

        verify(userRepository, times(1)).findByAccount("testuser");
        verify(passwordEncoder, times(1)).matches("plainPassword", testUser.getPassword());
        verify(jwtUtil, times(1)).generateToken(testUser);
    }

    @Test
    void testLogin_EmptyAccount() {
        // Given
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "");
        loginRequest.put("password", "password");

        // When
        ResponseEntity<Map<String, Object>> response = controller.login(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("帳號不能為空", body.get("message"));

        verify(userRepository, never()).findByAccount(anyString());
    }

    @Test
    void testLogin_EmptyPassword() {
        // Given
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "testuser");
        loginRequest.put("password", "");

        // When
        ResponseEntity<Map<String, Object>> response = controller.login(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("密碼不能為空", body.get("message"));
    }

    @Test
    void testLogin_AccountNotFound() {
        // Given
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "nonexistent");
        loginRequest.put("password", "password");

        when(userRepository.findByAccount("nonexistent")).thenReturn(Optional.empty());

        // When
        ResponseEntity<Map<String, Object>> response = controller.login(loginRequest);

        // Then
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testLogin_WrongPassword() {
        // Given
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "testuser");
        loginRequest.put("password", "wrongPassword");

        when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);

        // When
        ResponseEntity<Map<String, Object>> response = controller.login(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("密碼錯誤", body.get("message"));
    }

    @Test
    void testLogin_AccountDisabled() {
        // Given
        testUser.setAccountStatus((byte) 2); // 停用
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "testuser");
        loginRequest.put("password", "plainPassword");

        when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("plainPassword", testUser.getPassword())).thenReturn(true);

        // When
        ResponseEntity<Map<String, Object>> response = controller.login(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("帳號未啟用或已被停用", body.get("message"));
    }

    @Test
    void testLogin_AdminAccountForbidden() {
        // Given - 管理員不能從一般登入頁面登入
        testUser.setPermissionType((byte) 1); // 管理員權限
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "testuser");
        loginRequest.put("password", "plainPassword");

        when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("plainPassword", testUser.getPassword())).thenReturn(true);

        // When
        ResponseEntity<Map<String, Object>> response = controller.login(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("請由後台頁面登入", body.get("message"));
    }

    // ==================== adminlogin() Tests ====================

    @Test
    void testAdminLogin_Success() {
        // Given
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "admin");
        loginRequest.put("password", "adminPassword");

        when(userRepository.findByAccount("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("adminPassword", adminUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(adminUser)).thenReturn("admin.jwt.token");

        // When
        ResponseEntity<Map<String, Object>> response = controller.adminlogin(loginRequest);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals("登入成功", body.get("message"));
        assertEquals("admin.jwt.token", body.get("token"));

        verify(jwtUtil, times(1)).generateToken(adminUser);
    }

    @Test
    void testAdminLogin_NonAdminForbidden() {
        // Given - 一般用戶不能從管理員登入頁面登入
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "testuser");
        loginRequest.put("password", "plainPassword");

        when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("plainPassword", testUser.getPassword())).thenReturn(true);

        // When
        ResponseEntity<Map<String, Object>> response = controller.adminlogin(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("非管理員權限無法登入", body.get("message"));
    }

    @Test
    void testAdminLogin_EmptyAccount() {
        // Given
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "");
        loginRequest.put("password", "password");

        // When
        ResponseEntity<Map<String, Object>> response = controller.adminlogin(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("帳號不能為空", body.get("message"));
    }

    @Test
    void testAdminLogin_WrongPassword() {
        // Given
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "admin");
        loginRequest.put("password", "wrongPassword");

        when(userRepository.findByAccount("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("wrongPassword", adminUser.getPassword())).thenReturn(false);

        // When
        ResponseEntity<Map<String, Object>> response = controller.adminlogin(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("密碼錯誤", body.get("message"));
    }

    // ==================== 新增測試用例以提升分支覆蓋率 ====================

    @Test
    void testLogin_NullAccount() {
        // Given - account 為 null
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", null);
        loginRequest.put("password", "password");

        // When
        ResponseEntity<Map<String, Object>> response = controller.login(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("帳號不能為空", body.get("message"));
    }

    @Test
    void testLogin_NullPassword() {
        // Given - password 為 null
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "testuser");
        loginRequest.put("password", null);

        // When
        ResponseEntity<Map<String, Object>> response = controller.login(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("密碼不能為空", body.get("message"));
    }

    @Test
    void testLogin_AccountStatusNull() {
        // Given - accountStatus 為 null
        testUser.setAccountStatus(null);
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "testuser");
        loginRequest.put("password", "plainPassword");

        when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("plainPassword", testUser.getPassword())).thenReturn(true);

        // When
        ResponseEntity<Map<String, Object>> response = controller.login(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("帳號未啟用或已被停用", body.get("message"));
    }

    @Test
    void testLogin_PermissionType2Forbidden() {
        // Given - permissionType=2 (機構人員) 不能從一般登入頁面登入
        testUser.setPermissionType((byte) 2);
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "testuser");
        loginRequest.put("password", "plainPassword");

        when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("plainPassword", testUser.getPassword())).thenReturn(true);

        // When
        ResponseEntity<Map<String, Object>> response = controller.login(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("請由後台頁面登入", body.get("message"));
    }

    @Test
    void testAdminLogin_AccountNotFound() {
        // Given
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "nonexistent");
        loginRequest.put("password", "password");

        when(userRepository.findByAccount("nonexistent")).thenReturn(Optional.empty());

        // When
        ResponseEntity<Map<String, Object>> response = controller.adminlogin(loginRequest);

        // Then
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testAdminLogin_AccountDisabled() {
        // Given - 管理員帳號已停用
        adminUser.setAccountStatus((byte) 2);
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "admin");
        loginRequest.put("password", "adminPassword");

        when(userRepository.findByAccount("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("adminPassword", adminUser.getPassword())).thenReturn(true);

        // When
        ResponseEntity<Map<String, Object>> response = controller.adminlogin(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("帳號未啟用或已被停用", body.get("message"));
    }

    @Test
    void testAdminLogin_EmptyPassword() {
        // Given
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "admin");
        loginRequest.put("password", "");

        // When
        ResponseEntity<Map<String, Object>> response = controller.adminlogin(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("密碼不能為空", body.get("message"));
    }

    @Test
    void testAdminLogin_AccountStatusNull() {
        // Given - 管理員 accountStatus 為 null
        adminUser.setAccountStatus(null);
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "admin");
        loginRequest.put("password", "adminPassword");

        when(userRepository.findByAccount("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("adminPassword", adminUser.getPassword())).thenReturn(true);

        // When
        ResponseEntity<Map<String, Object>> response = controller.adminlogin(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("帳號未啟用或已被停用", body.get("message"));
    }

    @Test
    void testAdminLogin_PermissionType2Allowed() {
        // Given - permissionType=2 (機構人員) 可以從管理員登入頁面登入
        adminUser.setPermissionType((byte) 2);
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "admin");
        loginRequest.put("password", "adminPassword");

        when(userRepository.findByAccount("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("adminPassword", adminUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(adminUser)).thenReturn("institution.jwt.token");

        // When
        ResponseEntity<Map<String, Object>> response = controller.adminlogin(loginRequest);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(true, body.get("success"));
        assertEquals("登入成功", body.get("message"));
    }

    @Test
    void testAdminLogin_NullPassword() {
        // Given
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "admin");
        loginRequest.put("password", null);

        // When
        ResponseEntity<Map<String, Object>> response = controller.adminlogin(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("密碼不能為空", body.get("message"));
    }

    @Test
    void testLogin_WhitespaceOnlyAccount() {
        // Given - 帳號只有空格
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "   ");
        loginRequest.put("password", "password");

        // When
        ResponseEntity<Map<String, Object>> response = controller.login(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("帳號不能為空", body.get("message"));
    }

    @Test
    void testLogin_WhitespaceOnlyPassword() {
        // Given - 密碼只有空格
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("account", "testuser");
        loginRequest.put("password", "   ");

        // When
        ResponseEntity<Map<String, Object>> response = controller.login(loginRequest);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("密碼不能為空", body.get("message"));
    }
}
