# 測試後程式碼品質與修改建議

## 1. 總結
在完成 `AuthController` 與 `PasswordResetService` 的高覆蓋率測試後，我們發現了幾個可以改進的點。本文件旨在整理這些發現，分為 **潛在錯誤**、**程式碼品質建議** 與 **建構設定建議** 三個部分，以提升系統的穩健性與可維護性。

---

## 2. 潛在錯誤與邏輯缺陷

### 2.1 硬編碼的權限檢查 (Hard-coded Permission Check)
- **問題點**: `UsersController.changePassword` 方法中，權限檢查寫死為 `user.getPermissionType() != 2`。
- **位置**: `UsersController.java`, `changePassword()`
- **風險**:
    - **缺乏彈性**: 如果未來新增另一種管理員角色 (例如 `SUPER_ADMIN`，`permissionType = 3`)，他們將無法使用此功能，因為權限被寫死為 `2`。
    - **可讀性差**: 數字 `2` 的意義不明確，需要看資料庫或註解才能理解其代表「管理員」。
- **修改建議**:
    - **使用 Enum**: 建立一個 `PermissionType` 的 Enum (例如 `ADMIN`, `USER`)，讓判斷式變成 `user.getPermissionType() != PermissionType.ADMIN`，更具可讀性。
    - **角色驗證**: 更好的做法是使用 Spring Security 的角色驗證機制，例如在方法上加上 `@PreAuthorize("hasRole('ADMIN')")`，將權限判斷交給框架處理。

---

## 3. 程式碼品質與可維護性建議

### 3.1 過於複雜的參數驗證 `if` 條件
- **問題點**: `AuthController` 中的多個方法 (如 `verifyResetToken`, `resetPassword`) 使用了由多個 `||` 組成的長條件式來檢查 `null` 與 `isBlank`。
- **位置**: `AuthController.java`
- **風險**:
    - **測試困難**: 正如我們所見，為了達到 100% 分支覆蓋率，需要撰寫大量測試案例來覆蓋每一個 `OR` 條件。
    - **不易閱讀**: 一長串的 `if` 條件降低了程式碼的可讀性。
- **修改建議**:
    - **使用 Bean Validation**: 在 DTO (如 `ResetPasswordRequest`) 的欄位上加上 `@NotNull`、`@NotBlank`、`@Email` 等註解，並在 Controller 的方法參數前加上 `@Valid`。這樣 Spring 會自動進行驗證，讓 Controller 程式碼更乾淨。
    ```java
    // DTO
    public class ResetPasswordRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String token;
        // ...
    }

    // Controller
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        // ... 不再需要手動 if 判斷
    }
    ```

### 3.2 開發階段的殘留程式碼
- **問題點**: `PasswordResetService.verifyToken` 方法中，有一個空的 `try-catch` 區塊，其目的是在開發時印出錯誤，但對正式功能沒有幫助。
- **位置**: `PasswordResetService.java`, `verifyToken()`
- **風險**:
    - **程式碼雜訊**: 增加了無謂的程式碼，讓邏輯顯得混亂。
    - **覆蓋率干擾**: 成為 JaCoCo 報告中的一個紅色缺口，且難以透過正常測試覆蓋。
- **修改建議**:
    - **直接移除**: 安全地移除該 `try-catch` 區塊。

### 3.3 不可測試的異常捕捉
- **問題點**: `PasswordResetService.hashToken` 方法中，捕捉了 `NoSuchAlgorithmException`。
- **位置**: `PasswordResetService.java`, `hashToken()`
- **風險**:
    - **無法覆蓋**: 在標準 Java 環境中，SHA-256 演算法必定存在，因此這個 `catch` 區塊幾乎不可能被執行到，成為永久的覆蓋率缺口。
- **修改建議**:
    - **接受現狀**: 這是為了程式碼穩健性而寫的防禦性程式碼，可以接受它無法被覆蓋。
    - **(可選) JaCoCo 過濾**: 如果團隊追求 100% 覆蓋率，可以在 `pom.xml` 的 JaCoCo 設定中，配置排除規則來忽略這類無法測試的 `catch` 區塊。

---

## 4. 建構與設定建議

### 4.1 `pom.xml` 的 JDK 與 Spring Boot 版本配置
- **問題點**:
    1.  `--enable-preview` 標籤與 JDK 23 的組合在您的開發環境中導致編譯錯誤。
    2.  `@MockBean` 在 Spring Boot 3.4+ 中已被棄用 (`deprecated`)。
- **風險**:
    - **環境不一致**: 可能導致團隊成員之間或 CI/CD 環境中出現非預期的編譯失敗。
    - **技術債**: 繼續使用被棄用的 API，會在未來升級時產生額外的工作。
- **修改建議**:
    - **移除 `enable-preview`**: 除非專案真的用到了 JDK 的預覽功能，否則應從 `maven-compiler-plugin` 的設定中移除 `<enablePreview>true</enablePreview>`。
    - **升級 Maven 插件**: 將 `maven-compiler-plugin` 和 `maven-surefire-plugin` 升級到與 JDK 23 和 Spring Boot 3.x 更匹配的新版本。
    - **逐步汰換 `@MockBean`**: 在新的測試中，開始使用 Spring Boot 3.4 推薦的 `@MockitoBean`，並逐步替換舊測試中的 `@MockBean`。
