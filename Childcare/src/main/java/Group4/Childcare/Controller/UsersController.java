package Group4.Childcare.Controller;

import Group4.Childcare.Model.Users;
import Group4.Childcare.Service.UsersService;
import Group4.Childcare.DTO.UserSummaryDTO;
import Group4.Childcare.DTO.UserFamilyInfoDTO;
import Group4.Childcare.Service.ChildInfoService;
import Group4.Childcare.Service.ParentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UsersController {
  @Autowired
  private UsersService usersService;

  @Autowired
  private ChildInfoService childInfoService;

  @Autowired
  private ParentInfoService parentInfoService;

  @PostMapping
  public ResponseEntity<Users> createUser(@RequestBody Users user) {
    return ResponseEntity.ok(usersService.createUser(user));
  }

  /**
    * GET  /users/{id}
   * 範例資料為
   * {
   *     "userID": "86c23732-ce0d-4ec7-93d5-048faee27d4b",
   *     "account": "inst001",
   *     "password": "$2a$10$xYzHashedPasswordExample2234567890",
   *     "accountStatus": 1,
   *     "permissionType": 2,
   *     "name": "王小明",
   *     "gender": true,
   *     "phoneNumber": "0923456789",
   *     "mailingAddress": "台北市中正區重慶南路一段100號",
   *     "email": "wang@institution.com",
   *     "birthDate": "1985-03-20",
   *     "familyInfoID": "6659e1bc-a2ea-4bd2-854f-4141ba6ad924",
   *     "institutionID": "e09f1689-17a4-46f7-ae95-160a368147af",
   *     "nationalID": "B234567890"
   * }
   * 依使用者ID取得使用者資料
   * @param id 使用者ID
   *@return 使用者資料
   */

  @GetMapping("/{id}")
  public ResponseEntity<Users> getUserById(@PathVariable UUID id) {
    Optional<Users> user = usersService.getUserById(id);
    return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping
  public List<Users> getAllUsers() {
    return usersService.getAllUsers();
  }


  /**
   * GET  /users/offset
   * 範例資料:
   * {
   *     "offset": 0,
   *     "size": 10,
   *     "totalPages": 1,
   *     "hasNext": false,
   *     "content": [
   *         {
   *             "userID": "86c23732-ce0d-4ec7-93d5-048faee27d4b",
   *             "account": "inst001",
   *             "institutionName": "小天使托嬰中心",
   *             "permissionType": 2,
   *             "accountStatus": 1
   *         },
   *         {
   *             "userID": "c6948f8d-de54-40ba-9bb8-18dec3880c5b",
   *             "account": "parent002",
   *             "institutionName": null,
   *             "permissionType": 3,
   *             "accountStatus": 1
   *         },
   *         {
   *             "userID": "e2960508-6922-4b58-a8ce-6f9f94579b41",
   *             "account": "admin001",
   *             "institutionName": null,
   *             "permissionType": 1,
   *             "accountStatus": 1
   *         },
   *         {
   *             "userID": "b8b2c453-9604-4b2e-b435-c097d141d5c2",
   *             "account": "parent001",
   *             "institutionName": null,
   *             "permissionType": 3,
   *             "accountStatus": 1
   *         }
   *     ],
   *     "totalElements": 4
   * }
  *後台帳號管理頁面使用
  *以 offset 分頁方式取得使用者列表
  *
   * @param offset 起始位置
   * @param size   頁面大小
   * @return 分頁使用者列表及分頁資訊
   * permissionType=3 為一般使用者，permissionType=1 為管理員,permissionType=2 為機構人員
  **/
  @GetMapping("/offset")
  public ResponseEntity<Map<String, Object>> getUsersByOffsetJdbc(
          @RequestParam(defaultValue = "0") int offset,
          @RequestParam(defaultValue = "10") int size) {
    try {
      if (offset < 0 || size <= 0) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid offset/size"));
      }

      final int MAX_SIZE = 100;
      if (size > MAX_SIZE) size = MAX_SIZE;

      List<UserSummaryDTO> users = usersService.getUsersWithOffsetAndInstitutionNameJdbc(offset, size);
      long totalCount = usersService.getTotalCount();

      Map<String, Object> response = new HashMap<>();
      response.put("content", users);
      response.put("offset", offset);
      response.put("size", size);
      response.put("totalElements", totalCount);
      response.put("totalPages", (int) Math.ceil((double) totalCount / size));
      response.put("hasNext", offset + size < totalCount);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      System.err.println("Error in getUsersByOffsetJdbc: " + e.getMessage());
      e.printStackTrace();

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", "Internal server error");
      errorResponse.put("message", e.getMessage());
      return ResponseEntity.status(500).body(errorResponse);
    }
  }

  /**
   * PUT /users/{id}
   * body 範例資料:
   * {
   *     "userID": "86c23732-ce0d-4ec7-93d5-048faee27d4b",
   *     "account": "inst001",
   *     "password": "$2a$10$xYzHashedPasswordExample2234567890",
   *     "accountStatus": 1,
   *     "permissionType": 1,
   *     "name": "王小明",
   *     "gender": true,
   *     "phoneNumber": "0923456789",
   *     "mailingAddress": "台北市中正區重慶南路一段100號",
   *     "email": "wang@institution.com",
   *     "birthDate": "1985-03-20",
   *     "familyInfoID": "6659e1bc-a2ea-4bd2-854f-4141ba6ad924",
   *     "institutionID": "e09f1689-17a4-46f7-ae95-160a368147af",
   *     "nationalID": "B234567890"
   * }
   * 更新使用者資料
   * @param id 使用者ID
   * @param user 使用者資料
   * @return 更新後的使用者資料
   */
  @PutMapping("/{id}")
  public ResponseEntity<Users> updateUser(@PathVariable UUID id, @RequestBody Users user) {
    return ResponseEntity.ok(usersService.updateUser(id, user));
  }

  /**
   * PUT /users/{id}/status
   * 更新使用者帳號狀態（僅 accountStatus 欄位）
   * body 範例: { "accountStatus": 0 }
   */
  @PutMapping("/{id}/status")
  public ResponseEntity<Map<String, Object>> updateAccountStatus(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
    Map<String, Object> result = new HashMap<>();
    try {
      if (!request.containsKey("accountStatus")) {
        result.put("success", false);
        result.put("message", "Missing accountStatus in request body");
        return ResponseEntity.badRequest().body(result);
      }

      Object raw = request.get("accountStatus");
      Integer statusInt = null;
      if (raw instanceof Number) {
        statusInt = ((Number) raw).intValue();
      } else if (raw instanceof String) {
        try { statusInt = Integer.parseInt((String) raw); } catch (NumberFormatException e) { statusInt = null; }
      }

      if (statusInt == null || (statusInt != 1 && statusInt != 2)) {
        result.put("success", false);
        result.put("message", "accountStatus must be 1 or 2");
        return ResponseEntity.badRequest().body(result);
      }

      Users updated = usersService.updateAccountStatus(id, statusInt);

      result.put("success", true);
      result.put("message", "Account status updated");
      result.put("user", updated);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      System.err.println("Error in updateAccountStatus: " + e.getMessage());
      e.printStackTrace();
      result.put("success", false);
      result.put("message", "Failed to update account status");
      return ResponseEntity.status(500).body(result);
    }
  }

  /**
   * PUT /users/{id}/profile
   * 更新使用者基本資料（僅更新姓名、信箱、電話、地址）- 使用 JDBC
   * body 範例資料:
   * {
   *     "name": "王小明",
   *     "email": "wang@example.com",
   *     "phoneNumber": "0923456789",
   *     "mailingAddress": "台北市中正區重慶南路一段100號"
   * }
   * @param id 使用者ID
   * @param request 包含要更新的欄位
   * @return 更新後的使用者資料
   */
  @PutMapping("/{id}/profile")
  public ResponseEntity<Map<String, Object>> updateUserProfile(
          @PathVariable UUID id,
          @RequestBody Map<String, String> request) {
    Map<String, Object> result = new HashMap<>();

    try {
      // 檢查使用者是否存在
      Optional<Users> userOpt = usersService.getUserById(id);
      if (userOpt.isEmpty()) {
        result.put("success", false);
        result.put("message", "使用者不存在");
        return ResponseEntity.notFound().build();
      }

      // 準備要更新的欄位（只更新有提供的欄位）
      String name = request.containsKey("name") && request.get("name") != null
          ? request.get("name").trim() : null;
      String email = request.containsKey("email") && request.get("email") != null
          ? request.get("email").trim() : null;
      String phoneNumber = request.containsKey("phoneNumber") && request.get("phoneNumber") != null
          ? request.get("phoneNumber").trim() : null;
      String mailingAddress = request.containsKey("mailingAddress") && request.get("mailingAddress") != null
          ? request.get("mailingAddress").trim() : null;

      // 使用 JDBC 方式更新
      int rowsUpdated = usersService.updateUserProfile(id, name, email, phoneNumber, mailingAddress);

      if (rowsUpdated > 0) {
        // 重新查詢更新後的使用者資料
        Users updatedUser = usersService.getUserById(id).orElse(null);

        result.put("success", true);
        result.put("message", "資料更新成功");
        result.put("user", updatedUser);
        result.put("rowsUpdated", rowsUpdated);
        return ResponseEntity.ok(result);
      } else {
        result.put("success", false);
        result.put("message", "沒有欄位被更新");
        return ResponseEntity.ok(result);
      }

    } catch (Exception e) {
      System.err.println("Error in updateUserProfile: " + e.getMessage());
      e.printStackTrace();
      result.put("success", false);
      result.put("message", "資料更新失敗，請稍後再試");
      return ResponseEntity.status(500).body(result);
    }
  }
  /**
   * POST /users/new-member
   * 註冊新使用者
   * body 範例資料:
   * {
   *"account": "inst001",
   *"password": "$2a$10$xYzHashedPasswordExample2234567890",
   *"accountStatus": 1,
   *"permissionType": 1,
   *"name": "王小明",
   *"gender": true,
   *"phoneNumber": "0923456789",
   *"mailingAddress": "台北市中正區重慶南路一段100號",
   *"email": "wang@institution.com",
   *"birthDate": "1985-03-20",
   *"familyInfoID": "6659e1bc-a2ea-4bd2-854f-4141ba6ad924",
   *"institutionID": "e09f1689-17a4-46f7-ae95-160a368147af",
   *"nationalID": "B234567890"
   }
   *     @param user 使用者資料
   *     @return 註冊結果
   */
  @PostMapping("/new-member")
  public ResponseEntity<Map<String, Object>> createOrUpdateUserJdbc(@RequestBody Users user) {
    Map<String, Object> result = new HashMap<>();
    try {
      // 驗證 request body 是否為空
      if (user == null) {
        result.put("success", false);
        result.put("code", 400);
        result.put("error", "Request body is empty");
        return ResponseEntity.badRequest().body(result);
      }

      // 必要欄位驗證（僅 account 和 password 為必填）
      List<String> missingFields = new ArrayList<>();
      if (user.getAccount() == null || user.getAccount().trim().isEmpty()) {
        missingFields.add("account");
      }
      if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
        missingFields.add("password");
      }

      if (!missingFields.isEmpty()) {
        result.put("success", false);
        result.put("code", 400);
        result.put("error", "Missing required fields");
        result.put("missingFields", missingFields);
        return ResponseEntity.badRequest().body(result);
      }

      // 清理基本字串欄位（trim）
      user.setAccount(user.getAccount().trim());
      user.setPassword(user.getPassword().trim());

      if (user.getName() != null) {
        user.setName(user.getName().trim());
      }
      if (user.getNationalID() != null) {
        user.setNationalID(user.getNationalID().trim());
      }
      if (user.getMailingAddress() != null) {
        user.setMailingAddress(user.getMailingAddress().trim());
      }
      if (user.getEmail() != null) {
        user.setEmail(user.getEmail().trim());
      }
      if (user.getPhoneNumber() != null) {
        user.setPhoneNumber(user.getPhoneNumber().trim());
      }

      // 設定預設值
      if (user.getAccountStatus() == null) {
        user.setAccountStatus((byte) 1);
      }
      if (user.getPermissionType() == null) {
        user.setPermissionType((byte) 1);
      }

      // 注意：userID 不在此設置，讓 Repository 層在 insert 時自動生成

      Users saved = usersService.saveUsingJdbc(user);

      result.put("success", true);
      result.put("code", 200);
      result.put("message", "註冊成功");
      result.put("user", saved);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      System.err.println("Error in createOrUpdateUserJdbc: " + e.getMessage());
      e.printStackTrace();
      result.put("success", false);
      result.put("code", 500);
      result.put("error", "Failed to create/update user");
      result.put("message", e.getMessage());
      return ResponseEntity.status(500).body(result);
    }
  }

  /**
   * GET /users-familyIfo/{userID}
   * 根據使用者 ID 取得使用者資料、家庭資訊、父母資訊和子女資訊
   *
   * 範例資料:
   * {
   *     "userID": "86c23732-ce0d-4ec7-93d5-048faee27d4b",
   *     "accountStatus": 1,
   *     "permissionType": 3,
   *     "name": "王小明",
   *     "gender": true,
   *     "phoneNumber": "0923456789",
   *     "mailingAddress": "台北市中正區重慶南路一段100號",
   *     "email": "wang@institution.com",
   *     "birthDate": "1985-03-20",
   *     "familyInfoID": "6659e1bc-a2ea-4bd2-854f-4141ba6ad924",
   *     "institutionID": "e09f1689-17a4-46f7-ae95-160a368147af",
   *     "nationalID": "B234567890",
   *     "Parents": [...],
   *     "Children": [...]
   * }
   *
   * @param userID 使用者 ID
   * @return UserFamilyInfoDTO 包含使用者資料及其家庭成員資訊
   */
  @GetMapping("/users-familyInfo/{userID}")
  public ResponseEntity<UserFamilyInfoDTO> getUserFamilyInfo(@PathVariable UUID userID) {
    Optional<Users> userOpt = usersService.getUserById(userID);
    if (userOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Users user = userOpt.get();
    UserFamilyInfoDTO dto = new UserFamilyInfoDTO();

    // 映射用戶基本資訊
    dto.setUserID(user.getUserID());
    dto.setAccountStatus(user.getAccountStatus());
    dto.setPermissionType(user.getPermissionType());
    dto.setName(user.getName());
    dto.setGender(user.getGender());
    dto.setPhoneNumber(user.getPhoneNumber());
    dto.setMailingAddress(user.getMailingAddress());
    dto.setEmail(user.getEmail());
    dto.setBirthDate(user.getBirthDate());
    dto.setFamilyInfoID(user.getFamilyInfoID());
    dto.setInstitutionID(user.getInstitutionID());
    dto.setNationalID(user.getNationalID());

    // 根據 familyInfoID 查找父母和子女資訊
    if (user.getFamilyInfoID() != null) {
      dto.setParents(parentInfoService.getByFamilyInfoID(user.getFamilyInfoID()));
      dto.setChildren(childInfoService.getByFamilyInfoID(user.getFamilyInfoID()));
    }

    return ResponseEntity.ok(dto);
  }

  /**
   * PUT /users/{id}/password
   * 修改使用者密碼
   * body 範例資料:
   * {
   *     "newPassword": "newPassword123"
   * }
   * @param id 使用者ID
   * @param request 包含新密碼的請求
   * @return 修改結果
   */
  @PutMapping("/{id}/password")
  public ResponseEntity<Map<String, Object>> changePassword(@PathVariable UUID id, @RequestBody Map<String, String> request) {
    Map<String, Object> result = new HashMap<>();

    try {
      String newPassword = request.get("newPassword");

      // 驗證新密碼不為空
      if (newPassword == null || newPassword.trim().isEmpty()) {
        result.put("success", false);
        result.put("message", "新密碼不能為空");
        return ResponseEntity.badRequest().body(result);
      }

      // 驗證密碼長度
      if (newPassword.length() < 6) {
        result.put("success", false);
        result.put("message", "密碼長度至少為 6 個字元");
        return ResponseEntity.badRequest().body(result);
      }

      // 檢查使用者是否存在
      Optional<Users> userOpt = usersService.getUserById(id);
      if (userOpt.isEmpty()) {
        result.put("success", false);
        result.put("message", "使用者不存在");
        return ResponseEntity.notFound().build();
      }

      Users user = userOpt.get();

      // 檢查使用者權限是否為 admin (permissionType = 2)
      if (user.getPermissionType() == null || user.getPermissionType() != 2) {
        result.put("success", false);
        result.put("message", "只有管理員可以修改密碼");
        return ResponseEntity.status(403).body(result);
      }

      // 更新密碼 (這裡使用明文儲存，實際環境建議使用雜湊)
      user.setPassword(newPassword.trim());
      Users updatedUser = usersService.updateUser(id, user);

      result.put("success", true);
      result.put("message", "密碼修改成功");
      return ResponseEntity.ok(result);

    } catch (Exception e) {
      System.err.println("Error in changePassword: " + e.getMessage());
      e.printStackTrace();
      result.put("success", false);
      result.put("message", "密碼修改失敗，請稍後再試");
      return ResponseEntity.status(500).body(result);
    }
  }

}
