# JWT 抽籤權限問題修復報告

## 問題描述
當使用最高權限角色（SUPER_ADMIN，permissionType=1）登入並執行 conductLottery 動作時，JWT 驗證失敗，返回 403 Forbidden 錯誤。

## 根本原因
在 `WaitlistController.java` 的 `conductLottery` 方法中，使用了 `@PreAuthorize("hasRole('ADMIN')")` 權限檢查。

根據 `JwtUtil.java` 的角色生成邏輯：
- permissionType = 1 → `ROLE_SUPER_ADMIN`
- permissionType = 2 → `ROLE_ADMIN`
- 其他 → `ROLE_USER`

因此，當使用最高權限（permissionType=1）登入時，JWT token 中的角色是 `ROLE_SUPER_ADMIN`，但控制器只允許 `ROLE_ADMIN`，導致權限驗證失敗。

## 解決方案
將 `WaitlistController.java` 第 46 行的權限註解從：
```java
@PreAuthorize("hasRole('ADMIN')")
```

修改為：
```java
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
```

這樣就允許 SUPER_ADMIN 和 ADMIN 兩種角色都可以執行抽籤功能。

## 修改檔案
- `src/main/java/Group4/Childcare/Controller/WaitlistController.java` (第 46 行)

## 部署步驟
1. 儲存修改後的檔案
2. 重新編譯專案（如果使用 IDE 的自動編譯功能，會自動編譯）
3. 重新啟動 Spring Boot 應用程式
4. 使用 SUPER_ADMIN 角色重新測試抽籤功能

## 驗證
修改完成後，使用 permissionType=1 (SUPER_ADMIN) 或 permissionType=2 (ADMIN) 的帳號登入，應該都能成功執行 POST /api/waitlist/lottery 請求。

## 注意事項
- SecurityConfig.java 已經正確配置了 `/waitlist/lottery` 端點需要身份驗證
- JwtFilter 正確地從 JWT token 中提取角色並設置到 Spring Security Context 中
- 這個修改不影響其他功能，只是擴展了抽籤功能的權限範圍

## 日期
2025-12-18

