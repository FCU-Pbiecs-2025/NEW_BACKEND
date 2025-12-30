package Group4.Childcare.service;

import Group4.Childcare.Model.FamilyInfo;
import Group4.Childcare.Model.Users;
import Group4.Childcare.Repository.FamilyInfoJdbcRepository;
import Group4.Childcare.Repository.UserJdbcRepository;
import Group4.Childcare.Service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthService 單元測試
 * 
 * 測試範圍：
 * 1. login() - 登入功能各分支
 * - 帳號不存在
 * - 帳號狀態為 null
 * - 帳號已停用
 * - 密碼錯誤
 * - 登入成功（含各種 UserID/InstitutionID/FamilyInfoID 組合）
 * 2. register() - 註冊功能各分支
 * - 帳號已存在
 * - 註冊成功（男性）
 * - 註冊成功（女性）
 * - 註冊成功（含生日）
 * - 註冊成功（不含生日）
 * - 註冊失敗（例外）
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserJdbcRepository userRepository;

    @Mock
    private FamilyInfoJdbcRepository familyInfoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Users testUser;
    private UUID testUserId;
    private UUID testInstitutionId;
    private UUID testFamilyInfoId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testInstitutionId = UUID.randomUUID();
        testFamilyInfoId = UUID.randomUUID();

        testUser = new Users();
        testUser.setUserID(testUserId);
        testUser.setAccount("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPhoneNumber("0912345678");
        testUser.setAccountStatus((byte) 1);
        testUser.setPermissionType((byte) 1);
        testUser.setInstitutionID(testInstitutionId);
        testUser.setFamilyInfoID(testFamilyInfoId);
    }

    // ========== login() 測試 ==========

    @Test
    void testLogin_AccountNotFound() {
        when(userRepository.findByAccount("nonexistent")).thenReturn(Optional.empty());

        Map<String, Object> result = authService.login("nonexistent", "password");

        assertFalse((Boolean) result.get("success"));
        assertEquals("帳號不存在", result.get("message"));
    }

    @Test
    void testLogin_AccountStatusNull() {
        testUser.setAccountStatus(null);
        when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(testUser));

        Map<String, Object> result = authService.login("testuser", "password");

        assertFalse((Boolean) result.get("success"));
        assertEquals("帳號未啟用或已被停用", result.get("message"));
    }

    @Test
    void testLogin_AccountDisabled() {
        testUser.setAccountStatus((byte) 2); // 停用
        when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(testUser));

        Map<String, Object> result = authService.login("testuser", "password");

        assertFalse((Boolean) result.get("success"));
        assertEquals("帳號未啟用或已被停用", result.get("message"));
    }

    @Test
    void testLogin_WrongPassword() {
        when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        Map<String, Object> result = authService.login("testuser", "wrongpassword");

        assertFalse((Boolean) result.get("success"));
        assertEquals("密碼錯誤", result.get("message"));
    }

    @Test
    void testLogin_Success() {
        when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correctpassword", "encodedPassword")).thenReturn(true);

        Map<String, Object> result = authService.login("testuser", "correctpassword");

        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("user"));

        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) result.get("user");
        assertEquals(testUserId.toString(), userInfo.get("UserID"));
        assertEquals("testuser", userInfo.get("account"));
        assertEquals("Test User", userInfo.get("Name"));
        assertEquals("test@example.com", userInfo.get("Email"));
        assertEquals(testInstitutionId.toString(), userInfo.get("InstitutionID"));
        assertEquals(testFamilyInfoId.toString(), userInfo.get("FamilyInfoID"));
    }

    @Test
    void testLogin_Success_WithNullUserID() {
        testUser.setUserID(null);
        when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correctpassword", "encodedPassword")).thenReturn(true);

        Map<String, Object> result = authService.login("testuser", "correctpassword");

        assertTrue((Boolean) result.get("success"));
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) result.get("user");
        assertNull(userInfo.get("UserID"));
    }

    @Test
    void testLogin_Success_WithNullInstitutionID() {
        testUser.setInstitutionID(null);
        when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correctpassword", "encodedPassword")).thenReturn(true);

        Map<String, Object> result = authService.login("testuser", "correctpassword");

        assertTrue((Boolean) result.get("success"));
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) result.get("user");
        assertNull(userInfo.get("InstitutionID"));
        assertNull(userInfo.get("institutionID"));
    }

    @Test
    void testLogin_Success_WithNullFamilyInfoID() {
        testUser.setFamilyInfoID(null);
        when(userRepository.findByAccount("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correctpassword", "encodedPassword")).thenReturn(true);

        Map<String, Object> result = authService.login("testuser", "correctpassword");

        assertTrue((Boolean) result.get("success"));
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) result.get("user");
        assertNull(userInfo.get("FamilyInfoID"));
        assertNull(userInfo.get("familyInfoID"));
    }

    // ========== register() 測試 ==========

    @Test
    void testRegister_AccountAlreadyExists() {
        Map<String, String> request = createRegisterRequest("existinguser", "password", "Test", "男", "0912345678",
                "address", "test@example.com", "1990-01-01");
        when(userRepository.findByAccount("existinguser")).thenReturn(Optional.of(testUser));

        Map<String, Object> result = authService.register(request);

        assertFalse((Boolean) result.get("success"));
        assertEquals("帳號已存在", result.get("message"));
    }

    @Test
    void testRegister_Success_Male() {
        Map<String, String> request = createRegisterRequest("newuser", "password", "Test User", "男", "0912345678",
                "address", "test@example.com", "1990-01-01");
        FamilyInfo savedFamilyInfo = new FamilyInfo();
        savedFamilyInfo.setFamilyInfoID(testFamilyInfoId);

        when(userRepository.findByAccount("newuser")).thenReturn(Optional.empty());
        when(familyInfoRepository.save(any(FamilyInfo.class))).thenReturn(savedFamilyInfo);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = authService.register(request);

        assertTrue((Boolean) result.get("success"));
        assertEquals("註冊成功", result.get("message"));
        assertNotNull(result.get("userId"));
    }

    @Test
    void testRegister_Success_Female() {
        Map<String, String> request = createRegisterRequest("newuser", "password", "Test User", "女", "0912345678",
                "address", "test@example.com", "1990-01-01");
        FamilyInfo savedFamilyInfo = new FamilyInfo();
        savedFamilyInfo.setFamilyInfoID(testFamilyInfoId);

        when(userRepository.findByAccount("newuser")).thenReturn(Optional.empty());
        when(familyInfoRepository.save(any(FamilyInfo.class))).thenReturn(savedFamilyInfo);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = authService.register(request);

        assertTrue((Boolean) result.get("success"));
        assertEquals("註冊成功", result.get("message"));
    }

    @Test
    void testRegister_Success_MaleEnglish() {
        Map<String, String> request = createRegisterRequest("newuser", "password", "Test User", "male", "0912345678",
                "address", "test@example.com", "1990-01-01");
        FamilyInfo savedFamilyInfo = new FamilyInfo();
        savedFamilyInfo.setFamilyInfoID(testFamilyInfoId);

        when(userRepository.findByAccount("newuser")).thenReturn(Optional.empty());
        when(familyInfoRepository.save(any(FamilyInfo.class))).thenReturn(savedFamilyInfo);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = authService.register(request);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testRegister_Success_WithoutBirthday() {
        Map<String, String> request = createRegisterRequest("newuser", "password", "Test User", "男", "0912345678",
                "address", "test@example.com", null);
        FamilyInfo savedFamilyInfo = new FamilyInfo();
        savedFamilyInfo.setFamilyInfoID(testFamilyInfoId);

        when(userRepository.findByAccount("newuser")).thenReturn(Optional.empty());
        when(familyInfoRepository.save(any(FamilyInfo.class))).thenReturn(savedFamilyInfo);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = authService.register(request);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testRegister_Success_WithEmptyBirthday() {
        Map<String, String> request = createRegisterRequest("newuser", "password", "Test User", "男", "0912345678",
                "address", "test@example.com", "");
        FamilyInfo savedFamilyInfo = new FamilyInfo();
        savedFamilyInfo.setFamilyInfoID(testFamilyInfoId);

        when(userRepository.findByAccount("newuser")).thenReturn(Optional.empty());
        when(familyInfoRepository.save(any(FamilyInfo.class))).thenReturn(savedFamilyInfo);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = authService.register(request);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testRegister_Failure_Exception() {
        Map<String, String> request = createRegisterRequest("newuser", "password", "Test User", "男", "0912345678",
                "address", "test@example.com", "1990-01-01");

        when(userRepository.findByAccount("newuser")).thenReturn(Optional.empty());
        when(familyInfoRepository.save(any(FamilyInfo.class))).thenThrow(new RuntimeException("Database error"));

        Map<String, Object> result = authService.register(request);

        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("message").toString().contains("註冊失敗"));
    }

    // ========== 輔助方法 ==========

    private Map<String, String> createRegisterRequest(String account, String password, String name,
            String gender, String phone, String address,
            String email, String birthday) {
        Map<String, String> request = new HashMap<>();
        request.put("account", account);
        request.put("password", password);
        request.put("name", name);
        request.put("gender", gender);
        request.put("phone", phone);
        request.put("address", address);
        request.put("email", email);
        request.put("birthday", birthday);
        return request;
    }
}
