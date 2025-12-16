# 候補名單與抽籤系統完整指南

## 📋 目錄
1. [核心概念](#核心概念)
2. [抽籤流程完整說明](#抽籤流程完整說明)
3. [班級分配邏輯](#班級分配邏輯)
4. [候補排序規則](#候補排序規則)
5. [API 使用說明](#api-使用說明)
6. [完整範例](#完整範例)

---

## 核心概念

### 1. 法定名額 vs 實際名額 vs 可抽籤名額

| 項目 | 說明 | 計算方式 |
|------|------|---------|
| **法定名額** | 政府規定的序位比例 | 總容量 × 比例 |
| **實際名額** | 機構內已錄取人數 | 統計資料庫中已錄取人數 |
| **可抽籤名額** | 本次抽籤可錄取人數 | 法定名額 - 實際名額 |

### 2. 序位比例規定

依據政府規定：
- **第一序位（優先收托）**: 總容量 × 20%
- **第二序位（次優先收托）**: 總容量 × 10%
- **第三序位（一般家庭）**: 總容量 × 70%

### 3. 年齡班級模型

| 班級 | 年齡範圍 | 容量 | 說明 |
|------|---------|------|------|
| 嬰兒班 | 0-1歲 | 由機構決定 | MinAgeDescription=0, MaxAgeDescription=1 |
| 小班 | 1-2歲 | 由機構決定 | MinAgeDescription=1, MaxAgeDescription=2 |
| 中班 | 2-3歲 | 由機構決定 | MinAgeDescription=2, MaxAgeDescription=3 |

---

## 抽籤流程完整說明

### 前置步驟：資料準備

#### 步驟 1: 計算法定名額
```javascript
總容量 = 100 人（所有班級 Capacity 總和）
就讀中 = 70 人（所有班級 CurrentStudents 總和）
剩餘空位 = 30 人

第一序位法定名額 = 100 × 20% = 20 人
第二序位法定名額 = 100 × 10% = 10 人
第三序位法定名額 = 100 × 70% = 70 人
```

#### 步驟 2: 統計機構內已錄取各序位人數

**SQL 查詢**：
```sql
SELECT 
    a.IdentityType AS 序位,
    COUNT(*) AS 已錄取人數
FROM application_participants ap
LEFT JOIN applications a ON ap.ApplicationID = a.ApplicationID
WHERE a.InstitutionID = @InstitutionID
  AND ap.Status = '已錄取'
  AND ap.ParticipantType = 0
GROUP BY a.IdentityType;
```

**統計結果（假設）**：
```
第一序位已錄取: 18 人
第二序位已錄取: 8 人
第三序位已錄取: 44 人
總已錄取: 70 人 ✅
```

#### 步驟 3: 計算可抽籤名額

```
第一序位可抽籤名額 = 20 - 18 = 2 人 ✅
第二序位可抽籤名額 = 10 - 8 = 2 人 ✅
第三序位可抽籤名額 = 70 - 44 = 26 人 ✅
總可抽籤名額 = 2 + 2 + 26 = 30 人 = 剩餘空位 ✅
```

#### 步驟 4: 檢查是否可以抽籤

```javascript
if (總可抽籤名額 <= 0) {
    return "各序位名額已滿，無法進行抽籤";
}
```

---

### 三階段抽籤流程

#### 階段 1️⃣: 第一序位抽籤

```
輸入資料：
- 法定名額: 20 人
- 已錄取: 18 人
- 可抽籤名額: 2 人
- 申請人數: 25 人

處理流程：
1. 檢查是否已滿額
   if (可抽籤名額 <= 0) {
       所有申請人 → 併入第二序位池
   }

2. 隨機洗牌 25 人

3. 抽籤決定正取
   if (申請人數 <= 可抽籤名額) {
       全部正取
   } else {
       抽出前 2 人正取
       剩餘 23 人 → 併入第二序位池
   }

輸出結果：
- 第一序位正取: 2 人
- 併入第二序位池: 23 人
```

#### 階段 2️⃣: 第二序位抽籤

```
輸入資料：
- 法定名額: 10 人
- 已錄取: 8 人
- 本序位可抽籤名額: 2 人
- 第一序位剩餘名額: 2 - 2 = 0 人
- 總可抽籤名額: 2 + 0 = 2 人
- 申請人數: 15 (原第二序位) + 23 (第一序位落選) = 38 人

處理流程：
1. 合併申請人池（含第一序位落選者）

2. 檢查是否已滿額
   if (總可抽籤名額 <= 0) {
       所有申請人 → 併入第三序位池
   }

3. 隨機洗牌 38 人

4. 抽籤決定正取
   if (申請人數 <= 總可抽籤名額) {
       全部正取
   } else {
       抽出前 2 人正取
       剩餘 36 人 → 併入第三序位池
   }

輸出結果：
- 第二序位正取: 2 人
- 併入第三序位池: 36 人
```

#### 階段 3️⃣: 第三序位抽籤

```
輸入資料：
- 法定名額: 70 人
- 已錄取: 44 人
- 本序位可抽籤名額: 26 人
- 前兩序位剩餘名額: 0 + 0 = 0 人
- 總可抽籤名額: 26 + 0 = 26 人
- 申請人數: 80 (原第三序位) + 36 (前兩序位落選) = 116 人

處理流程：
1. 合併申請人池（含前兩序位落選者）

2. 檢查是否已滿額
   if (總可抽籤名額 <= 0) {
       所有申請人 → 全部成為備取
   }

3. 隨機洗牌 116 人

4. 抽籤決定正取與備取
   if (申請人數 <= 總可抽籤名額) {
       全部正取
   } else {
       抽出前 26 人正取
       剩餘 90 人成為備取
   }

輸出結果：
- 第三序位正取: 26 人
- 備取: 90 人
```

---

## 班級分配邏輯

抽籤完成後，需要依照年齡將正取者分配到適合的班級。

### 分配流程

```
依序位順序處理：第一序位正取 → 第二序位正取 → 第三序位正取

for 每個正取者 (依 LotteryOrder 排序):
    1. 取得幼童出生日期
    
    2. 計算年齡（月齡）
       年齡(月) = (今天 - 出生日期) 的月數
    
    3. 尋找適合年齡的班級
       for 每個班級:
           if (年齡 >= MinAge && 年齡 < MaxAge && CurrentStudents < Capacity):
               找到適合班級
               break
    
    4. 分配結果
       if (找到適合班級 && 班級有空位):
           ✅ 正式錄取
           Status = "已錄取（第X序位正取）"
           ClassID = 班級UUID
           更新 CurrentStudents + 1
       else:
           ❌ 無法錄取
           Status = "錄取候補中（第X序位正取-班級已滿）" 或
                   "錄取候補中（第X序位正取-無適合年齡班級）"
           ClassID = NULL
           加入 classFullWaitlist
```

### 班級容量動態更新

```javascript
// 記憶體中的班級資訊也要同步更新
for (每次成功錄取):
    更新資料庫: UPDATE classes SET CurrentStudents = CurrentStudents + 1
    更新記憶體: classInfo.CurrentStudents += 1
    
// 確保後續檢查都是最新狀態
```

### 範例

假設 30 人正取（2 + 2 + 26），班級分配結果：

```
第一序位 2 人:
  - 王小明 (1歲3個月) → 小班 ✅
  - 李小華 (0歲8個月) → 嬰兒班 ✅

第二序位 2 人:
  - 張三 (2歲1個月) → 中班 ✅
  - 李四 (1歲9個月) → 小班 ✅

第三序位 26 人:
  - 前 20 人成功分配到各班級 ✅
  - 後 6 人因班級已滿無法錄取 ❌

最終結果：
- 實際錄取: 24 人 (2 + 2 + 20)
- 班級已滿候補: 6 人
- 未抽中備取: 90 人
- 總候補: 96 人
```

---

## 候補排序規則

### 排序原則

只有**候補中的申請人**需要 `CurrentOrder`，已錄取者不需要。

### 排序依據：抽籤順序 (LotteryOrder)

所有候補者（包含班級已滿和未抽中）都依照**抽籤時的順序**排列。

### 排序步驟

```javascript
// 1. 記錄抽籤順序
let lotteryOrder = 1;

for (第一序位正取者) {
    applicant.LotteryOrder = lotteryOrder++;
}
for (第二序位正取者) {
    applicant.LotteryOrder = lotteryOrder++;
}
for (第三序位正取者) {
    applicant.LotteryOrder = lotteryOrder++;
}
for (第三序位未抽中者) {
    applicant.LotteryOrder = lotteryOrder++;
}

// 2. 合併所有候補者
allWaitlist = classFullWaitlist + 未抽中備取者

// 3. 依 LotteryOrder 排序
allWaitlist.sort((a, b) => a.LotteryOrder - b.LotteryOrder)

// 4. 分配 CurrentOrder
let currentOrder = 1;
for (候補者 in allWaitlist) {
    候補者.CurrentOrder = currentOrder++;
}

// 5. 已錄取者不需要 CurrentOrder
for (已錄取者 in acceptedList) {
    已錄取者.CurrentOrder = null;
}
```

### 排序範例

```
LotteryOrder 記錄（抽籤順序）:
1-2:    第一序位正取（已錄取）
3-4:    第二序位正取（已錄取）
5-30:   第三序位正取（24人已錄取，6人班級已滿）
31-120: 第三序位未抽中（90人備取）

CurrentOrder 分配（候補順序）:
第一序位正取 2人 → CurrentOrder = NULL（已錄取）
第二序位正取 2人 → CurrentOrder = NULL（已錄取）
第三序位正取 20人 → CurrentOrder = NULL（已錄取）
班級已滿 6人 (LotteryOrder 25-30) → CurrentOrder = 1-6
未抽中 90人 (LotteryOrder 31-120) → CurrentOrder = 7-96
```

---

## API 使用說明

### 1. 執行抽籤

**端點**: `POST /waitlist/lottery`

**請求**:
```json
{
  "institutionId": "your-institution-uuid",
  "isLotteryPeriod": true
}
```

**回應**:
```json
{
  "success": true,
  "message": "抽籤完成。總容量=100，就讀中=70，剩餘空位=30。法定名額：第一序位=20（已錄取18，本次可錄取2），第二序位=10（已錄取8，本次可錄取2），第三序位=70（已錄取44，本次可錄取26）。本次實際錄取=24",
  "totalProcessed": 120,
  "firstPriorityAccepted": 2,
  "secondPriorityAccepted": 2,
  "thirdPriorityAccepted": 20,
  "waitlisted": 96,
  "acceptedList": [
    {
      "ApplicationID": "uuid",
      "NationalID": "A123456789",
      "Name": "王小明",
      "Status": "已錄取（第一序位正取）",
      "ClassID": "class-uuid",
      "CurrentOrder": null
    }
  ],
  "waitlistList": [
    {
      "ApplicationID": "uuid",
      "NationalID": "B234567890",
      "Name": "李小華",
      "Status": "錄取候補中（第三序位正取-班級已滿）",
      "ClassID": null,
      "CurrentOrder": 1
    }
  ]
}
```

### 2. 查詢候補名單

**端點**: `GET /waitlist/by-institution`

**請求**:
```
GET /waitlist/by-institution?institutionId=xxx&name=王
```

**回應**:
```json
[
  {
    "ApplicationID": "uuid",
    "Name": "王小明",
    "BirthDate": "2022-05-15",
    "Age": "2歲6個月",
    "IdentityType": 1,
    "CurrentOrder": 5,
    "Status": "錄取候補中"
  }
]
```

### 3. 查詢統計資訊

**端點**: `GET /waitlist/statistics`

**請求**:
```
GET /waitlist/statistics?institutionId=xxx
```

**回應**:
```json
{
  "totalCapacity": 100,
  "firstPriorityCount": 25,
  "secondPriorityCount": 15,
  "thirdPriorityCount": 80,
  "firstPriorityQuota": 20,
  "secondPriorityQuota": 10,
  "classInfo": [
    {
      "ClassID": "uuid",
      "ClassName": "小班",
      "Capacity": 20,
      "CurrentStudents": 15,
      "MinAgeDescription": 12,
      "MaxAgeDescription": 24
    }
  ]
}
```

### 4. 重置抽籤（每年 7/31 執行）

**端點**: `POST /waitlist/reset-lottery`

**請求**:
```
POST /waitlist/reset-lottery?institutionId=xxx
```

**回應**:
```json
{
  "success": true,
  "message": "已重置所有候補順位"
}
```

---

## 完整範例

### 情境：某機構要進行年度抽籤

#### 初始狀態
```
機構名稱: 快樂托育中心
總容量: 100 人
  - 嬰兒班 (0-1歲): 30 人
  - 小班 (1-2歲): 40 人
  - 中班 (2-3歲): 30 人

目前就讀中: 70 人
  - 嬰兒班: 25 人
  - 小班: 30 人
  - 中班: 15 人

剩餘空位: 30 人

已錄取各序位人數:
  - 第一序位: 18 人
  - 第二序位: 8 人
  - 第三序位: 44 人

本次申請人數:
  - 第一序位: 25 人
  - 第二序位: 15 人
  - 第三序位: 80 人
```

#### 執行抽籤

```bash
# 1. 執行 7/31 重置
POST /waitlist/reset-lottery?institutionId=xxx

# 2. 執行抽籤
POST /waitlist/lottery
{
  "institutionId": "xxx",
  "isLotteryPeriod": true
}
```

#### 抽籤過程

**第一序位**:
- 法定名額: 20 人，已錄取: 18 人，可抽籤: 2 人
- 申請 25 人 → 抽 2 人正取，23 人落選

**第二序位**:
- 法定名額: 10 人，已錄取: 8 人，可抽籤: 2 人
- 申請 15 + 23 = 38 人 → 抽 2 人正取，36 人落選

**第三序位**:
- 法定名額: 70 人，已錄取: 44 人，可抽籤: 26 人
- 申請 80 + 36 = 116 人 → 抽 26 人正取，90 人備取

#### 班級分配

```
第一序位 2 人正取:
  - 王小明 (1歲2個月) → 小班 ✅
  - 李小華 (0歲9個月) → 嬰兒班 ✅

第二序位 2 人正取:
  - 張三 (2歲3個月) → 中班 ✅
  - 李四 (1歲8個月) → 小班 ✅

第三序位 26 人正取:
  - 嬰兒班剩餘 5 個位子 → 錄取 5 人 ✅
  - 小班剩餘 10 個位子 → 錄取 8 人 ✅（2人已被第一、二序位佔用）
  - 中班剩餘 15 個位子 → 錄取 12 人 ✅（1人已被第二序位佔用）
  - 無法分配 1 人（無適合年齡班級）❌
```

#### 最終結果

```
本次實際錄取: 26 人
  - 第一序位: 2 人
  - 第二序位: 2 人
  - 第三序位: 22 人

候補名單: 94 人
  - 班級已滿/無適合班級: 4 人 (CurrentOrder 1-4)
  - 未抽中備取: 90 人 (CurrentOrder 5-94)

機構總錄取人數: 70 + 26 = 96 人
機構剩餘空位: 100 - 96 = 4 人
```

---

## 核心原則總結

1. ✅ **法定名額固定**：基於總容量 × 比例 (20%, 10%, 70%)
2. ✅ **可抽籤名額動態**：法定名額 - 機構內已錄取人數
3. ✅ **三階段抽籤**：先決定正取資格，落選者遞補下一序位
4. ✅ **班級分配檢核**：正取者需通過年齡、班級容量檢查
5. ✅ **實際錄取人數**：由班級剩餘空位自然控制
6. ✅ **候補排序**：依抽籤順序，班級已滿者自然排在前面
7. ✅ **已錄取者**：不需要 CurrentOrder

---

## 資料庫更新欄位

### application_participants 表

| 欄位 | 已錄取者 | 候補者 | 說明 |
|------|---------|--------|------|
| Status | "已錄取（第X序位正取）" | "錄取候補中（...）" | 詳細狀態 |
| CurrentOrder | NULL | 1, 2, 3... | 候補順序 |
| ClassID | 有值 | NULL | 分配的班級 |
| ReviewDate | 有值 | 有值 | 錄取/審核時間 |

---

## 常見問題 FAQ

### Q1: 為什麼要統計機構內已錄取人數？
**A**: 因為機構可能在不同時期錄取過學生，必須扣除這些已錄取人數，才能確保各序位比例符合政府規定。

### Q2: 如果某序位已滿額會怎樣？
**A**: 該序位的所有申請人會全部併入下一序位抽籤池，與下一序位申請人一起抽籤。

### Q3: 班級已滿者為什麼排在候補最前面？
**A**: 因為他們的抽籤順序（LotteryOrder）較前，依照抽籤順序排列時自然會排在前面。

### Q4: 可以重複執行抽籤嗎？
**A**: 建議加上防護機制，例如記錄抽籤執行日期，同一天不可重複執行。

### Q5: 如何確認抽籤結果正確？
**A**: 驗證以下項目：
- 總可抽籤名額 = 剩餘空位 ✅
- 實際錄取人數 ≤ 剩餘空位 ✅
- 各序位錄取人數 ≤ 可抽籤名額 ✅

---

**文件版本**: 1.0  
**最後更新**: 2025-01-25  
**維護者**: Childcare 開發團隊

