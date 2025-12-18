package Group4.Childcare.Controller;

import Group4.Childcare.Model.Users;
import Group4.Childcare.Repository.UserJdbcRepository;
import Group4.Childcare.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/Login")
@CrossOrigin
public class SimpleLoginController {

    @Autowired
    private UserJdbcRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/Verify")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        Map<String, Object> result = new HashMap<>();

        String account = loginRequest.get("account");
        String password = loginRequest.get("password");

        // 檢查輸入是否為空
        if (account == null || account.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "帳號不能為空");
            return ResponseEntity.badRequest().body(result);
        }

        if (password == null || password.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "密碼不能為空");
            return ResponseEntity.badRequest().body(result);
        }

        // 根據帳號查詢使用者
        Optional<Users> userOptional = userRepository.findByAccount(account);

        if (userOptional.isEmpty()) {
            result.put("success", false);
            result.put("message", "帳號不存在");
            return ResponseEntity.notFound().build();
        }

        Users user = userOptional.get();

        // 使用 BCrypt 比對密碼
        if (!passwordEncoder.matches(password, user.getPassword())) {
            result.put("success", false);
            result.put("message", "密碼錯誤");
            return ResponseEntity.badRequest().body(result);
        }

        // 檢查帳號狀態，AccountStatus: 1=啟用, 2=停用
        if (user.getAccountStatus() == null || user.getAccountStatus() != 1) {
            result.put("success", false);
            result.put("message", "帳號未啟用或已被停用");
            return ResponseEntity.badRequest().body(result);
        }
        // 禁止後台帳號由前台一般使用者頁面登入
        if (user.getPermissionType() == 1 || user.getPermissionType() == 2) {
            result.put("success", false);
            result.put("message", "請由後台頁面登入");
            return ResponseEntity.badRequest().body(result);
        }


        // 登入成功
        // ✅ 生成 JWT token
        String token = jwtUtil.generateToken(user);
        System.out.println("✅ 用戶登入成功，已生成 JWT token: " + token.substring(0, 50) + "...");

        Map<String, Object> userInfo = new HashMap<>();
        // Original (existing) PascalCase keys
        userInfo.put("UserID", user.getUserID() != null ? user.getUserID().toString() : null);
        userInfo.put("PermissionType", user.getPermissionType());
        userInfo.put("Name", user.getName());
        userInfo.put("Email", user.getEmail());
        userInfo.put("PhoneNumber", user.getPhoneNumber());
        userInfo.put("Account", user.getAccount());

        // Also include normalized lowercase keys for frontend consumption
        userInfo.put("userId", user.getUserID() != null ? user.getUserID().toString() : null);
        userInfo.put("permissionType", user.getPermissionType());
        userInfo.put("name", user.getName());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhoneNumber());
        userInfo.put("account", user.getAccount());
        userInfo.put("FamilyInfoID", user.getFamilyInfoID());
        userInfo.put("InstitutionID", user.getInstitutionID());

        result.put("success", true);
        result.put("message", "登入成功");
        result.put("token", token);  // ✅ 添加 token 到回應
        result.put("user", userInfo);

        return ResponseEntity.ok(result);
    }
    @PostMapping("/Verify2")
    public ResponseEntity<Map<String, Object>> adminlogin(@RequestBody Map<String, String> loginRequest) {
        Map<String, Object> result = new HashMap<>();

        String account = loginRequest.get("account");
        String password = loginRequest.get("password");

        // 檢查輸入是否為空
        if (account == null || account.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "帳號不能為空");
            return ResponseEntity.badRequest().body(result);
        }

        if (password == null || password.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "密碼不能為空");
            return ResponseEntity.badRequest().body(result);
        }

        // 根據帳號查詢使用者
        Optional<Users> userOptional = userRepository.findByAccount(account);

        if (userOptional.isEmpty()) {
            result.put("success", false);
            result.put("message", "帳號不存在");
            return ResponseEntity.notFound().build();
        }

        Users user = userOptional.get();

        // 使用 BCrypt 比對密碼
        if (!passwordEncoder.matches(password, user.getPassword())) {
            result.put("success", false);
            result.put("message", "密碼錯誤");
            return ResponseEntity.badRequest().body(result);
        }

        // 檢查帳號狀態，AccountStatus: 1=啟用, 2=停用
        if (user.getAccountStatus() == null || user.getAccountStatus() != 1) {
            result.put("success", false);
            result.put("message", "帳號未啟用或已被停用");
            return ResponseEntity.badRequest().body(result);
        }
        // 禁止後台帳號由前台一般使用者頁面登入
        if (user.getPermissionType() == 3) {
            result.put("success", false);
            result.put("message", "非管理員權限無法登入");
            return ResponseEntity.badRequest().body(result);
        }


        // 登入成功
        // ✅ 生成 JWT token
        String token = jwtUtil.generateToken(user);
        System.out.println("✅ 管理員登入成功，已生成 JWT token: " + token.substring(0, 50) + "...");

        Map<String, Object> userInfo = new HashMap<>();
        // Original (existing) PascalCase keys
        userInfo.put("UserID", user.getUserID() != null ? user.getUserID().toString() : null);
        userInfo.put("PermissionType", user.getPermissionType());
        userInfo.put("Name", user.getName());
        userInfo.put("Email", user.getEmail());
        userInfo.put("PhoneNumber", user.getPhoneNumber());
        userInfo.put("Account", user.getAccount());

        // Also include normalized lowercase keys for frontend consumption
        userInfo.put("userId", user.getUserID() != null ? user.getUserID().toString() : null);
        userInfo.put("permissionType", user.getPermissionType());
        userInfo.put("name", user.getName());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhoneNumber());
        userInfo.put("account", user.getAccount());
        userInfo.put("FamilyInfoID", user.getFamilyInfoID());
        userInfo.put("InstitutionID", user.getInstitutionID());

        result.put("success", true);
        result.put("message", "登入成功");
        result.put("token", token);  // ✅ 添加 token 到回應
        result.put("user", userInfo);

        return ResponseEntity.ok(result);
    }
}
