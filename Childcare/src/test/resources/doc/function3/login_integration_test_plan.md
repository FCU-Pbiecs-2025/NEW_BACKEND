# 登入功能 (含 reCAPTCHA) 整合測試規劃

## 1. 測試目標
驗證 `AuthController` 的登入介面 (`/api/auth/login`) 能正確處理外部請求，並與 `RecaptchaService` 及 `AuthService` 正確互動。
同時驗證密碼重設相關 API (`forgot-password`, `verify-reset-token`, `reset-password`) 的參數檢查與流程控制。

## 2. 測試策略
採用 **單元測試 (Unit Testing)**，使用 `MockMvcBuilders.standaloneSetup` 建構測試環境。
Mock `RecaptchaService`、`AuthService` 與 `PasswordResetService` 以隔離外部依賴與資料庫操作。

## 3. 測試範圍
*   **Controller**: `Group4.Childcare.Controller.AuthController`
*   **Service (Mock)**:
    *   `Group4.Childcare.Service.RecaptchaService`
    *   `Group4.Childcare.Service.AuthService`
    *   `Group4.Childcare.Service.PasswordResetService`

## 4. 測試案例 (Test Cases)

### 4.1 登入 (Login)
| ID | 測試情境 | 輸入資料 | Mock 行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-L-01** | **登入成功** | Account: "validUser"<br>Pass: "validPass"<br>Token: "valid" | Recaptcha: true<br>Auth: Success | HTTP 200 OK<br>回傳 User JSON |
| **TC-L-02** | **機器人驗證失敗** | Account: "validUser"<br>Pass: "validPass"<br>Token: "invalid" | Recaptcha: false | HTTP 400 Bad Request<br>訊息: "機器人驗證失敗..." |
| **TC-L-03** | **帳號或密碼錯誤** | Account: "wrongUser"<br>Pass: "wrongPass" | Recaptcha: true<br>Auth: Error (Unauthorized) | HTTP 401 Unauthorized<br>訊息: "帳號或密碼錯誤" |
| **TC-L-04** | **帳號被停用** | Account: "disabledUser"<br>Pass: "validPass" | Recaptcha: true<br>Auth: Error (Disabled) | HTTP 401 Unauthorized<br>訊息: "帳號未啟用..." |

### 4.2 忘記密碼 (Forgot Password)
| ID | 測試情境 | 輸入資料 | Mock 行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-FP-01** | **Token 為 Null** | Email: "test@example.com"<br>Token: null | (跳過 Recaptcha 驗證)<br>呼叫 requestReset | HTTP 200 OK<br>Success: true |
| **TC-FP-04** | **Token 為空字串** | Email: "test@example.com"<br>Token: "" | (跳過 Recaptcha 驗證)<br>呼叫 requestReset | HTTP 200 OK<br>Success: true |
| **TC-FP-02** | **Service 異常 (有訊息)** | Email: "error@example.com" | requestReset 拋出 RuntimeException("DB Error") | HTTP 500<br>Error: "DB Error" |
| **TC-FP-05** | **Service 異常 (無訊息)** | Email: "error@example.com" | requestReset 拋出 RuntimeException() | HTTP 500<br>Error: "RuntimeException" |
| **TC-FP-03** | **Email 為空** | Email: "" | (不呼叫 Service) | HTTP 400 Bad Request |

### 4.3 驗證重設 Token (Verify Reset Token)
| ID | 測試情境 | 輸入資料 | Mock 行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-VR-01** | **Email 為空字串** | Email: "", Token: "valid" | (不呼叫 Service) | HTTP 400 Bad Request |
| **TC-VR-05** | **Email 為 Null** | Email: null, Token: "valid" | (不呼叫 Service) | HTTP 400 Bad Request |
| **TC-VR-02** | **Token 為空字串** | Email: "test", Token: "" | (不呼叫 Service) | HTTP 400 Bad Request |
| **TC-VR-06** | **Token 為 Null** | Email: "test", Token: null | (不呼叫 Service) | HTTP 400 Bad Request |
| **TC-VR-03** | **驗證成功** | Email: "test", Token: "valid" | verifyToken 回傳 true | HTTP 200 OK<br>Success: true |
| **TC-VR-04** | **驗證失敗** | Email: "test", Token: "invalid" | verifyToken 回傳 false | HTTP 200 OK<br>Success: false |

### 4.4 重設密碼 (Reset Password)
| ID | 測試情境 | 輸入資料 | Mock 行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-RP-01** | **NewPassword 為 Null** | Email: "test", Token: "valid", NewPass: null | (不呼叫 Service) | HTTP 400 Bad Request |
| **TC-RP-04** | **Email 為 Null** | Email: null, Token: "valid", NewPass: "123" | (不呼叫 Service) | HTTP 400 Bad Request |
| **TC-RP-06** | **Email 為空字串** | Email: "", Token: "valid", NewPass: "123" | (不呼叫 Service) | HTTP 400 Bad Request |
| **TC-RP-05** | **Token 為 Null** | Email: "test", Token: null, NewPass: "123" | (不呼叫 Service) | HTTP 400 Bad Request |
| **TC-RP-07** | **Token 為空字串** | Email: "test", Token: "", NewPass: "123" | (不呼叫 Service) | HTTP 400 Bad Request |
| **TC-RP-02** | **重設成功** | Email: "test", Token: "valid", NewPass: "123" | resetPassword 回傳 true | HTTP 200 OK<br>Success: true |
| **TC-RP-03** | **重設失敗** | Email: "test", Token: "invalid", NewPass: "123" | resetPassword 回傳 false | HTTP 200 OK<br>Success: false |

## 5. 實作建議
```java
@ExtendWith(MockitoExtension.class)
public class AuthControllerIntegrationTest {

    private MockMvc mockMvc;

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;
    
    // ... 其他 Mocks

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    // ... 測試方法實作
}
```
