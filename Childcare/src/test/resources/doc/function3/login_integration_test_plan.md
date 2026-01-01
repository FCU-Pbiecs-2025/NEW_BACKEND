# 登入功能 (含 reCAPTCHA) 整合測試規劃

## 1. 測試目標
驗證 `AuthController` 的登入介面 (`/api/auth/login`) 能正確處理外部請求，並與 `RecaptchaService` 及 `AuthService` 正確互動。

## 2. 測試策略
採用 **整合測試 (Integration Testing)**，使用 `MockMvc` 模擬 HTTP 請求。
由於 `RecaptchaService` 會呼叫 Google 外部 API，測試環境中必須予以 **Mock (模擬)**，以避免網路依賴與不穩定的測試結果。

## 3. 測試範圍
*   **Controller**: `Group4.Childcare.Controller.AuthController`
*   **Service (Mock)**: `Group4.Childcare.Service.RecaptchaService`
*   **Service (Real/Mock)**: `Group4.Childcare.Service.AuthService` (視是否要連同 DB 一起測，建議 Mock 以專注於 Controller 邏輯，或使用 H2 DB 進行完整整合)

## 4. 測試案例 (Test Cases)

| ID | 測試情境 | 輸入資料 | 前置條件 (Mock 行為) | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-L-01** | **登入成功** | Account: "validUser"<br>Password: "validPass"<br>Token: "valid-token" | `recaptchaService.verify(...)` 回傳 `true`<br>`authService.login(...)` 回傳成功 Map | HTTP 200 OK<br>回傳包含 User 資訊的 JSON |
| **TC-L-02** | **機器人驗證失敗** | Account: "validUser"<br>Password: "validPass"<br>Token: "invalid-token" | `recaptchaService.verify(...)` 回傳 `false` | HTTP 400 Bad Request<br>訊息: "機器人驗證失敗，請重試！" |
| **TC-L-03** | **帳號或密碼錯誤** | Account: "wrongUser"<br>Password: "wrongPass"<br>Token: "valid-token" | `recaptchaService.verify(...)` 回傳 `true`<br>`authService.login(...)` 回傳失敗 Map | HTTP 401 Unauthorized<br>回傳錯誤訊息 |
| **TC-L-04** | **帳號被停用** | Account: "disabledUser"<br>Password: "validPass"<br>Token: "valid-token" | `recaptchaService.verify(...)` 回傳 `true`<br>`authService.login(...)` 回傳 "帳號未啟用" | HTTP 401 Unauthorized<br>訊息: "帳號未啟用或已被停用" |

## 5. 實作建議
```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecaptchaService recaptchaService;

    @MockBean
    private AuthService authService;

    // ... 測試方法實作
}
```
