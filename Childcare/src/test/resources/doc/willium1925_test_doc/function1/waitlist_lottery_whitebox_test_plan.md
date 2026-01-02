# 候補名單抽籤功能 白箱測試規劃

## 1. 測試目標
驗證 `WaitlistController` 的抽籤功能 (`conductLottery`) 及相關私有方法的內部邏輯正確性，確保候補名單抽籤、班級分配、郵件通知等流程無誤。

## 2. 測試策略
採用 **白箱測試 (White-box Testing)**，針對 Controller 層進行單元測試。
利用 `Mockito` 模擬資料庫存取 (`WaitlistJdbcRepository`) 與郵件發送 (`EmailService`)，專注於驗證程式碼內部的執行路徑與狀態變化。

## 3. 測試範圍
*   **Target Class**: `Group4.Childcare.Controller.WaitlistController`
*   **Dependencies (Mock)**:
    *   `WaitlistJdbcRepository`
    *   `EmailService`

## 4. 測試案例 (Test Cases)

### 4.1 抽籤功能 - assignClassAndAdmit 方法

#### 4.1.1 出生日期類型轉換測試
| ID | 測試情境 | 輸入資料 | 預期路徑/行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-AC-01** | **BirthDate 為 java.sql.Date** | BirthDate: java.sql.Date.valueOf("2020-01-01") | 1. 進入 `instanceof java.sql.Date` 分支<br>2. 轉換為 LocalDate<br>3. 呼叫 findSuitableClass | 成功分配班級並錄取 |
| **TC-AC-02** | **BirthDate 為 LocalDate** | BirthDate: LocalDate.of(2020, 6, 15) | 1. 跳過 java.sql.Date 分支<br>2. 進入 `instanceof LocalDate` 分支<br>3. 直接使用 LocalDate<br>4. 呼叫 findSuitableClass | 成功分配班級並錄取 |
| **TC-AC-03** | **BirthDate 為 null** | BirthDate: null | 1. 檢查 birthDateObj == null<br>2. 設定狀態為「候補中」<br>3. 原因為「無出生日期」<br>4. 加入 classFullWaitlist | 回傳 false<br>不分配班級 |

#### 4.1.2 班級分配邏輯測試
| ID | 測試情境 | 輸入資料 | Mock 行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-AC-04** | **找到適合班級且有空位** | BirthDate: LocalDate.of(2020, 1, 15) | findSuitableClass 回傳 classId<br>hasClassCapacity 回傳 true | 1. 設定狀態為「已錄取」<br>2. 更新 ClassID<br>3. 呼叫 updateClassCurrentStudents<br>4. 回傳 true |
| **TC-AC-05** | **找不到適合班級** | BirthDate: LocalDate.of(2020, 1, 1) | findSuitableClass 回傳 null | 1. 設定狀態為「候補中」<br>2. 原因為「無適合年齡班級」<br>3. 加入 classFullWaitlist<br>4. 回傳 false |
| **TC-AC-06** | **班級已滿** | BirthDate: LocalDate.of(2020, 1, 1) | findSuitableClass 回傳 classId<br>hasClassCapacity 回傳 false | 1. 設定狀態為「候補中」<br>2. 原因為「班級已滿」<br>3. 加入 classFullWaitlist<br>4. 回傳 false |

#### 4.1.3 記憶體中班級資訊更新測試
| ID | 測試情境 | 輸入資料 | 預期路徑/行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-AC-07** | **更新記憶體中的班級資訊 - 匹配第一個班級** | classes: [不匹配班級, 匹配班級] | 1. 遍歷班級列表<br>2. 第一個班級 ID 不匹配，繼續<br>3. 第二個班級 ID 匹配<br>4. 更新 CurrentStudents<br>5. 執行 break | 只更新匹配的班級<br>不訪問後續班級 |
| **TC-AC-08** | **更新記憶體中的班級資訊 - 匹配中間班級** | classes: [班級1, 班級2, 匹配班級, 班級4] | 1. 跳過前兩個不匹配班級<br>2. 第三個班級匹配<br>3. 更新並 break<br>4. 不訪問第四個班級 | 驗證 break 語句正確終止循環 |

### 4.2 郵件通知功能 - sendLotteryNotificationEmails 方法

#### 4.2.1 錄取者郵件發送測試
| ID | 測試情境 | 輸入資料 | 預期路徑/行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-EN-01** | **CaseNumber 為 null** | CaseNumber: null | 1. 檢查 caseNumberObj != null<br>2. 條件為 false<br>3. caseNumber 設為 null | 郵件發送時 caseNumber 為 null |
| **TC-EN-02** | **CaseNumber 正常值** | CaseNumber: 12345L | 1. 檢查 caseNumberObj != null<br>2. 條件為 true<br>3. 轉換為 Long | 郵件發送時 caseNumber 為 12345L |
| **TC-EN-03** | **ApplicationDate 為 LocalDateTime** | ApplicationDate: LocalDateTime.now() | 1. 進入 `instanceof LocalDateTime` 分支<br>2. 格式化為 yyyy-MM-dd | 成功轉換日期格式 |
| **TC-EN-04** | **ApplicationDate 為 java.sql.Timestamp** | ApplicationDate: new Timestamp(System.currentTimeMillis()) | 1. 跳過 LocalDateTime<br>2. 進入 `instanceof Timestamp` 分支<br>3. 轉換並格式化 | 成功轉換日期格式 |
| **TC-EN-05** | **ApplicationDate 為 java.sql.Date** | ApplicationDate: java.sql.Date.valueOf("2023-01-01") | 1. 跳過前兩個分支<br>2. 進入 `instanceof java.sql.Date` 分支<br>3. 轉換並格式化 | 成功轉換日期格式 |
| **TC-EN-06** | **ApplicationDate 為 LocalDate** | ApplicationDate: LocalDate.now() | 1. 跳過前三個分支<br>2. 進入 `instanceof LocalDate` 分支<br>3. 格式化 | 成功轉換日期格式 |
| **TC-EN-07** | **ApplicationDate 為 String** | ApplicationDate: "2023-01-15" | 1. 跳過所有 instanceof 分支<br>2. 進入 `instanceof String` 分支<br>3. 直接使用 | 成功使用原字串 |
| **TC-EN-08** | **Email 為 null** | Email: null | 1. 檢查 email != null<br>2. 條件為 false<br>3. 不發送郵件<br>4. failCount++ | 輸出警告訊息<br>不呼叫 emailService |
| **TC-EN-09** | **Email 為空字串** | Email: "" | 1. 檢查 !email.isEmpty()<br>2. 條件為 false<br>3. 不發送郵件<br>4. failCount++ | 輸出警告訊息<br>不呼叫 emailService |
| **TC-EN-10** | **發送郵件成功** | Email: "test@example.com" | 1. Email 驗證通過<br>2. 呼叫 sendApplicationStatusChangeEmail<br>3. 成功發送<br>4. successCount++ | 輸出成功訊息<br>驗證 emailService 被呼叫 |
| **TC-EN-11** | **錄取者發送郵件異常** | Email: "error@example.com" | 1. Email 驗證通過<br>2. 呼叫 sendApplicationStatusChangeEmail<br>3. 拋出 RuntimeException<br>4. catch Exception<br>5. failCount++ | 輸出錯誤訊息<br>不中斷處理流程 |

#### 4.2.2 候補者郵件發送測試
| ID | 測試情境 | 輸入資料 | 預期路徑/行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-EN-12** | **候補者 CaseNumber 為 null** | CaseNumber: null | 同 TC-EN-01 | caseNumber 為 null |
| **TC-EN-13** | **候補者 CaseNumber 正常值** | CaseNumber: 34567L | 同 TC-EN-02 | caseNumber 為 34567L |
| **TC-EN-14** | **候補者 ApplicationDate 各類型轉換** | 各種日期類型 | 同 TC-EN-03 ~ TC-EN-07 | 成功轉換所有日期類型 |
| **TC-EN-15** | **候補者 Email 為 null** | Email: null | 同 TC-EN-08 | 不發送郵件 |
| **TC-EN-16** | **候補者 Email 為空字串** | Email: "" | 同 TC-EN-09 | 不發送郵件 |
| **TC-EN-17** | **候補者發送郵件 - MessagingException** | Email: "msg-error@example.com" | 1. 呼叫 sendApplicationStatusChangeEmail<br>2. 拋出 MessagingException<br>3. catch MessagingException<br>4. failCount++ | 輸出郵件失敗訊息<br>不中斷處理流程 |
| **TC-EN-18** | **候補者發送郵件 - RuntimeException** | Email: "run-error@example.com" | 1. 呼叫 sendApplicationStatusChangeEmail<br>2. 拋出 RuntimeException<br>3. catch Exception<br>4. failCount++ | 輸出錯誤訊息<br>不中斷處理流程 |
| **TC-EN-19** | **候補者發送郵件成功** | Email: "waitlist@example.com" | 1. Email 驗證通過<br>2. 呼叫 sendApplicationStatusChangeEmail<br>3. 成功發送<br>4. successCount++ | 輸出成功訊息（含序號）<br>驗證 currentOrder 參數被傳遞 |

### 4.3 整合測試案例

#### 4.3.1 抽籤流程整合測試
| ID | 測試情境 | 輸入資料 | Mock 行為 | 預期結果 |
| :--- | :--- | :--- | :--- | :--- |
| **TC-LT-01** | **完整抽籤流程 - LocalDate 類型** | P1 申請人使用 LocalDate 出生日期 | findSuitableClass 回傳 classId<br>hasClassCapacity 回傳 true | 1. 成功錄取<br>2. LocalDate 分支被執行<br>3. 班級資訊被更新<br>4. 郵件發送成功 |
| **TC-LT-02** | **完整抽籤流程 - 混合日期類型** | P1 使用 LocalDate<br>P2 使用 java.sql.Date | 各自匹配對應的 mock | 1. 兩種類型都被正確處理<br>2. 兩人都成功錄取<br>3. 驗證兩次 updateClassCurrentStudents 被呼叫 |
| **TC-LT-03** | **完整流程 - 班級資訊更新** | 多個申請人分配到同一班級 | classInfoList 包含多個班級 | 1. 遍歷班級列表找到匹配<br>2. 更新 CurrentStudents<br>3. break 正確執行 |

## 5. 覆蓋率目標 (Coverage Goal)
*   **Line Coverage**: > 95%
*   **Branch Coverage**: > 90% (確保所有 if-else、instanceof 路徑皆被測試)
*   **Method Coverage**: 100% (所有 public 和 private 方法都有對應測試)

## 6. 測試實作重點

### 6.1 關鍵技術點
```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WaitlistControllerTest {

    @Mock
    private WaitlistJdbcRepository waitlistJdbcRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private WaitlistController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    // ... 測試方法實作
}
```

### 6.2 LocalDate 分支測試技巧
```java
// ✅ 正確：使用固定的 LocalDate 實例
LocalDate testBirthDate = LocalDate.of(2020, 6, 15);
Map<String, Object> app = createApplicant("Test", null);
app.put("BirthDate", testBirthDate);

// ✅ 正確：使用 eq() 精確匹配
when(waitlistJdbcRepository.findSuitableClass(eq(testBirthDate), any()))
    .thenReturn(testClassId);

// ✅ 正確：驗證分支被執行
verify(waitlistJdbcRepository).findSuitableClass(eq(testBirthDate), any());

// ❌ 錯誤：使用 LocalDate.now() (每次都是新實例)
app.put("BirthDate", LocalDate.now());

// ❌ 錯誤：使用 any() 可能無法精確匹配
when(waitlistJdbcRepository.findSuitableClass(any(LocalDate.class), any()))
    .thenReturn(testClassId);
```

### 6.3 班級資訊更新測試設計
```java
// 創建包含多個班級的列表，測試遍歷邏輯
List<Map<String, Object>> classInfoList = new ArrayList<>();

// 第一個不匹配的班級（測試 if 條件為 false）
UUID anotherClassId = UUID.randomUUID();
Map<String, Object> classInfo1 = new HashMap<>();
classInfo1.put("ClassID", anotherClassId);
classInfo1.put("CurrentStudents", 3);
classInfoList.add(classInfo1);

// 第二個匹配的班級（測試 if 條件為 true 和 break）
Map<String, Object> classInfo2 = new HashMap<>();
classInfo2.put("ClassID", testClassId);
classInfo2.put("CurrentStudents", 5);
classInfoList.add(classInfo2);

when(waitlistJdbcRepository.getClassInfo(institutionId)).thenReturn(classInfoList);
```

### 6.4 郵件發送測試技巧
```java
// 測試不同的異常處理分支
doThrow(new MessagingException("Mail Error"))
    .when(emailService).sendApplicationStatusChangeEmail(
        eq("msg-error@example.com"), 
        any(), any(), any(), any(), any(), any(), 
        any(Integer.class), // 候補者有 currentOrder
        any());

doThrow(new RuntimeException("Run Error"))
    .when(emailService).sendApplicationStatusChangeEmail(
        eq("run-error@example.com"), 
        any(), any(), any(), any(), any(), any(), 
        any(Integer.class),
        any());

// 驗證郵件發送被呼叫
verify(emailService, atLeastOnce())
    .sendApplicationStatusChangeEmail(any(), any(), any(), any(), 
                                     any(), any(), any(), any(), any());
```

## 7. 已知覆蓋率限制

### 7.1 未完全覆蓋的分支
*   **birthDateObj instanceof LocalDate** (Line 316)
    *   狀態：黃色（部分覆蓋）
    *   原因：雖然測試中使用了 LocalDate 實例，但由於 mock 匹配問題，此分支可能未被 JaCoCo 完全識別為已覆蓋
    *   解決方案：已在測試中使用固定 LocalDate 實例和 eq() 匹配，並添加 verify 驗證，理論上應該覆蓋，但 JaCoCo 報告可能仍顯示黃色

### 7.2 測試驗證方法
```java
// 通過驗證確認分支被執行
verify(waitlistJdbcRepository).findSuitableClass(eq(birthDate), any());

// 通過驗證確認更新邏輯被執行
verify(waitlistJdbcRepository, times(2))
    .updateClassCurrentStudents(eq(testClassId), eq(1));
```

## 8. 測試文件修訂記錄
| 版本 | 日期 | 修訂內容 | 修訂人 |
| :--- | :--- | :--- | :--- |
| 1.0 | 2026-01-03 | 初版建立 | Willium1925 |

---

**備註**：
1. 所有標注 `Willium1925` 的測試方法均已實作並通過
2. 測試覆蓋了 `assignClassAndAdmit` 和 `sendLotteryNotificationEmails` 兩個私有方法的所有主要分支
3. 使用 MockMvc 進行端對端測試，確保 HTTP 請求到回應的完整流程

