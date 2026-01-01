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

### 4.2 驗證 Token (verifyToken)
| ID | 測試情境 | 輸入資料 | 預期路徑/行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-PR-03** | **Token 有效** | Email: "user@example.com"<br>Token: "valid-raw-token" | 1. 查詢 User 成功<br>2. 計算 Hash<br>3. 查詢 DB 找到對應且未過期 Token<br>4. 比對 UserID | 回傳 `true` |
| **TC-PR-04** | **Token 過期** | Email: "user@example.com"<br>Token: "expired-raw-token" | 1. ...<br>2. ...<br>3. 查詢 DB 發現 Token `ExpiresAt` < `Now` | 回傳 `false` (透過 Repository 過濾或邏輯判斷) |
| **TC-PR-05** | **Token 已失效 (Invalidated)** | Email: "user@example.com"<br>Token: "used-raw-token" | 1. ...<br>2. ...<br>3. 查詢 DB 發現 Token `Invalidated` = `true` | 回傳 `false` |
| **TC-PR-06** | **Token 雜湊不符** | Email: "user@example.com"<br>Token: "wrong-token" | 1. ...<br>2. 計算 Hash<br>3. 查詢 DB 找不到對應 Hash 的 Token | 回傳 `false` |

### 4.3 重設密碼 (resetPassword)
| ID | 測試情境 | 輸入資料 | 預期路徑/行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-PR-07** | **重設成功** | Token: "valid-token"<br>NewPass: "newPass123" | 1. 驗證 Token 有效<br>2. 查詢 User<br>3. 呼叫 `passwordEncoder.encode`<br>4. 更新 User 密碼<br>5. 將 Token 設為失效 (Invalidated=true) | 回傳 `true`<br>驗證 UserRepo `save` 被呼叫<br>驗證 TokenRepo `save` 被呼叫 (更新狀態) |

## 5. 覆蓋率目標 (Coverage Goal)
*   **Line Coverage**: > 90%
*   **Branch Coverage**: > 85% (確保所有 if-else 路徑皆被測試)
