# 密碼重設功能 白箱測試規劃

## 1. 測試目標
驗證 `PasswordResetService` 的內部邏輯正確性，確保 Token 生成、雜湊驗證、過期檢查及郵件發送流程無誤。

## 2. 測試策略
採用 **白箱測試 (White-box Testing)**，針對 Service 層進行單元測試。
利用 `Mockito` 模擬資料庫存取 (`Repository`) 與郵件發送 (`JavaMailSender`)，專注於驗證程式碼內部的執行路徑與狀態變化。

## 3. 測試範圍
*   **Target Class**: `Group4.Childcare.Service.PasswordResetService`
*   **Dependencies (Mock)**:
    *   `PasswordResetTokenRepository`
    *   `UserJdbcRepository`
    *   `JavaMailSender`
    *   `PasswordEncoder`

## 4. 測試案例 (Test Cases)

### 4.1 請求重設密碼 (requestReset)
| ID | 測試情境 | 輸入資料 | 預期路徑/行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-PR-01** | **Email 存在** | Email: "exist@example.com" | 1. 查詢 User 成功<br>2. 呼叫 `invalidateAllTokensByUserID`<br>3. 生成 Token 並 Hash<br>4. 儲存 Token 到 DB<br>5. 呼叫 `mailSender.send` | 回傳成功 Result<br>驗證 Repository `save` 被呼叫<br>驗證 `mailSender` 被呼叫 |
| **TC-PR-02** | **Email 不存在** | Email: "nonexist@example.com" | 1. 查詢 User 失敗 (Empty)<br>2. **不** 執行後續 Token 生成與寄信 | 回傳成功 Result (安全考量不報錯)<br>驗證 Repository `save` **未** 被呼叫<br>驗證 `mailSender` **未** 被呼叫 |
| **TC-PR-08** | **清除舊 Token 失敗** | Email: "exist@example.com" | 1. 查詢 User 成功<br>2. `invalidateAllTokensByUserID` 拋出異常<br>3. 繼續執行後續流程 | 回傳成功 Result<br>驗證 Token 仍被儲存 |
| **TC-PR-09** | **未預期錯誤** | Email: "error@example.com" | 1. `findByEmail` 拋出 RuntimeException | 拋出 RuntimeException (包裝過的錯誤訊息) |
| **TC-PR-10** | **寄信失敗** | Email: "exist@example.com" | 1. ...<br>2. `mailSender.send` 拋出 MailException<br>3. 流程繼續 | 回傳成功 Result<br>驗證 Token 已儲存 |
| **TC-PR-15** | **User Name 為 Null** | Email: "nullname@example.com" | 1. User 物件 Name 為 null<br>2. 寄信時使用預設稱謂 | 回傳成功 Result<br>驗證 `mailSender` 被呼叫 (覆蓋三元運算子分支) |

### 4.2 驗證 Token (verifyToken)
| ID | 測試情境 | 輸入資料 | 預期路徑/行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-PR-03** | **Token 有效** | Email: "user@example.com"<br>Token: "valid-raw-token" | 1. 查詢 User 成功<br>2. 計算 Hash<br>3. 查詢 DB 找到對應且未過期 Token<br>4. 比對 UserID | 回傳 `true` |
| **TC-PR-04** | **Token 過期** | Email: "user@example.com"<br>Token: "expired-raw-token" | 1. ...<br>2. ...<br>3. 查詢 DB 發現 Token `ExpiresAt` < `Now` | 回傳 `false` (透過 Repository 過濾或邏輯判斷) |
| **TC-PR-05** | **Token 已失效 (Invalidated)** | Email: "user@example.com"<br>Token: "used-raw-token" | 1. ...<br>2. ...<br>3. 查詢 DB 發現 Token `Invalidated` = `true` | 回傳 `false` |
| **TC-PR-06** | **Token 雜湊不符** | Email: "user@example.com"<br>Token: "wrong-token" | 1. ...<br>2. 計算 Hash<br>3. 查詢 DB 找不到對應 Hash 的 Token | 回傳 `false` |
| **TC-PR-11** | **UserID 不匹配** | Email: "user@example.com"<br>Token: "other-user-token" | 1. ...<br>2. ...<br>3. Token 存在但 UserID 與 Email 對應的 UserID 不同 | 回傳 `false` |

### 4.3 重設密碼 (resetPassword)
| ID | 測試情境 | 輸入資料 | 預期路徑/行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-PR-07** | **重設成功** | Token: "valid-token"<br>NewPass: "newPass123" | 1. 驗證 Token 有效<br>2. 查詢 User<br>3. 呼叫 `passwordEncoder.encode`<br>4. 更新 User 密碼<br>5. 將 Token 設為失效 (Invalidated=true) | 回傳 `true`<br>驗證 UserRepo `save` 被呼叫<br>驗證 TokenRepo `save` 被呼叫 (更新狀態) |
| **TC-PR-12** | **重設失敗 (驗證不過)** | Email: "user@example.com"<br>Token: "invalid"<br>NewPass: "123" | 1. `verifyToken` 回傳 false<br>2. 不執行重設 | 回傳 `false`<br>驗證 UserRepo `save` 未被呼叫 |
| **TC-PR-13** | **重設失敗 (User不存在)** | Token: "valid-token"<br>NewPass: "123" | 1. Token 有效<br>2. `findById` 找不到 User | 回傳 `false`<br>驗證 UserRepo `save` 未被呼叫 |
| **TC-PR-14** | **重設成功 (含Email)** | Email: "user@example.com"<br>Token: "valid"<br>NewPass: "123" | 1. `verifyToken` 回傳 true<br>2. 呼叫 `resetPasswordInternal`<br>3. 執行重設流程 | 回傳 `true`<br>驗證 UserRepo `save` 被呼叫 |

## 5. 覆蓋率目標 (Coverage Goal)
*   **Line Coverage**: > 90%
*   **Branch Coverage**: > 85% (確保所有 if-else 路徑皆被測試)

<br>

# 密碼重設功能 白箱測試規劃 2 (使用者修改密碼)

## 1. 測試目標
驗證 `UsersController` 中的 `changePassword` API，確保已登入使用者修改密碼的邏輯正確，包括參數驗證、權限檢查與密碼更新。

## 2. 測試策略
採用 **單元測試 (Unit Testing)**，使用 `MockMvc` 模擬 HTTP 請求，並 Mock `UsersService` 與 `PasswordEncoder`。

## 3. 測試範圍
*   **Target Class**: `Group4.Childcare.Controller.UsersController`
*   **Dependencies (Mock)**:
    *   `UsersService`
    *   `PasswordEncoder`

## 4. 測試案例 (Test Cases)

### 4.1 修改密碼 (changePassword)
| ID | 測試情境 | 輸入資料 | Mock 行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-CP-01** | **修改成功** | UserID: "valid"<br>NewPass: "newPass123" | User存在<br>Permission=2 (Admin)<br>Update成功 | HTTP 200 OK<br>Success: true |
| **TC-CP-02** | **新密碼為空白** | NewPass: " " | (不呼叫 Service) | HTTP 400 Bad Request<br>Msg: "新密碼不能為空" |
| **TC-CP-07** | **新密碼為 Null** | NewPass: null | (不呼叫 Service) | HTTP 400 Bad Request<br>Msg: "新密碼不能為空" |
| **TC-CP-03** | **新密碼長度不足** | NewPass: "12345" | (不呼叫 Service) | HTTP 400 Bad Request<br>Msg: "密碼長度至少為 6 個字元" |
| **TC-CP-04** | **使用者不存在** | UserID: "invalid" | getUserById 回傳 Empty | HTTP 404 Not Found<br>Msg: "使用者不存在" |
| **TC-CP-05** | **權限不足** | UserID: "valid"<br>Permission=1 | getUserById 回傳 User (Perm=1) | HTTP 403 Forbidden<br>Msg: "只有管理員可以修改密碼" |
| **TC-CP-08** | **權限為 Null** | UserID: "valid"<br>Permission=null | getUserById 回傳 User (Perm=null) | HTTP 403 Forbidden<br>Msg: "只有管理員可以修改密碼" |
| **TC-CP-06** | **Service 異常** | UserID: "valid" | getUserById 拋出 RuntimeException | HTTP 500 Internal Server Error<br>Msg: "密碼修改失敗..." |

## 5. 覆蓋率目標
*   **Branch Coverage**: 100% (包含所有參數驗證與權限檢查的 if-else 分支)
