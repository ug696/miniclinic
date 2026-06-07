# 作業五：期末專案整合部署

> **對應課程單元：** Ch09-E — Git、Docker 與 Render 雲端部署
> **截止時間：** 下次上課前（請於上課前上傳至教學平台）
> **繳交方式：** 在教學平台提交**兩個網址**——GitHub repository URL 與 Render 部署 URL；**不需要上傳 zip 檔**

---

## 作業說明

這是本學期的**期末整合作業**。你的任務是把前四週開發的 MiniClinic，透過 Git 版本控制、Docker 容器化，部署到 Render 雲端平台，讓任何人都能透過公開網址訪問。

除了部署之外，本作業還要求你**實作兩個新功能**——每個功能都以 AI 輔助完成，並誠實記錄 AI 使用過程。這是課程中「與 AI 協作開發」理念的最終實踐。

請在第四週的 `miniclinic` 專案**繼續開發**，不要建立新專案。

---

## 學習目標

完成本作業後，你應該能夠：

- 說明 Git 的工作區、暫存區、本地 repo、遠端 repo 四個層次，以及 `add`、`commit`、`push` 各自在哪個層次發生
- 正確設定 `.gitignore`，知道 `target/`、`.db` 等為什麼不應推上去
- 撰寫有意義的 commit message，並依課程規範加上 `[AI-USED]` 或 `[NO-AI]` 標記
- 說明 `[AI-USED]` commit 三欄詳述（問AI什麼／AI給什麼建議／我怎麼修改）的目的
- 說明 Spring Profiles 的作用，以及 `dev` Profile 用 SQLite、`prod` Profile 用 PostgreSQL 的設定方式
- 說明 Dockerfile 的 multi-stage build：第一階段用 Maven 編譯、第二階段只帶 JAR 執行
- 在 Render 上建立 Web Service 並正確設定四個環境變數（`SPRING_PROFILES_ACTIVE`、`DATABASE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`）
- 說明為什麼免費方案的 Render 服務會「冬眠」，以及如何喚醒
- 使用 Spring Data JPA 的 `count()` 查詢與分組計算，設計並實作統計摘要 API

---

## 作業要求

### Part A：基本部署要求（必做）

以下項目是部署的前提條件，全部完成才算達到及格門檻：

- [ ] 專案根目錄有正確的 `.gitignore`，已排除 `target/`、`*.db`、`.idea/` 等
- [ ] 專案根目錄有 `Dockerfile`（multi-stage build，使用 `maven:3.9-eclipse-temurin-17-alpine` 編譯）
- [ ] GitHub 上有公開的 `miniclinic` repository
- [ ] 本週所有修改都有對應的 commit（**總計至少 5 個 commit**）
- [ ] 所有 commit message 以 `[NO-AI]` 或 `[AI-USED]` 開頭
- [ ] Spring Profiles 正確設定：dev Profile 使用 SQLite，prod Profile 使用 PostgreSQL
- [ ] 成功部署到 Render，服務公開可訪問
- [ ] README.md 包含：專案介紹、線上 Demo 網址、技術棧、本機執行步驟、預設帳密

---

### Part B：新功能實作（AI 輔助，必做）

本作業要求你實作**兩個新功能**，每個功能都需要留下至少一筆 `[AI-USED]` commit（詳見下方格式說明）。

#### 功能一：看診完成按鈕

**目標**：在醫師 Dashboard 的「今日掛號」清單中，為每筆狀態為 `BOOKED` 的掛號，除了原有的「取消」按鈕之外，新增一個**「看診完成」按鈕**。

按鈕行為：
- 點擊後呼叫已有的 `PUT /api/appointments/{id}/status` API，傳送 `{"status": "COMPLETED"}`
- 收到成功回應後，重新整理頁面（`location.reload()`）

實作要點：
- 後端 API 已在 Week 4 實作完成，本功能只需修改 Thymeleaf 模板（`dashboard.html`）
- 「完成」按鈕只在 `BOOKED` 狀態下顯示，`COMPLETED` 與 `CANCELLED` 不顯示

AI 輔助提示：
> - 「Thymeleaf 如何在表格的同一列中，依照不同狀態分別顯示不同的按鈕？」
> - 「Spring Boot dashboard 頁面如何用 JavaScript fetch 呼叫 PUT API，成功後重新整理頁面？」

---

#### 功能二：統計摘要端點 `GET /api/stats`

**目標**：新增一支統計摘要端點，**不需要登入即可呼叫**，回傳整個系統的資料摘要，供外部驗收工具（AI agent）查核。

**API 規格**：

```
GET /api/stats
不需要認證（不受 Interceptor 保護）
Content-Type: application/json
```

**回傳 JSON 範例**：

```json
{
  "totalDoctors": 5,
  "totalPatients": 3,
  "totalAppointments": 6,
  "byStatus": {
    "BOOKED": 4,
    "COMPLETED": 1,
    "CANCELLED": 1
  }
}
```

**欄位規格**：

| 欄位 | 型別 | 說明 |
|---|---|---|
| `totalDoctors` | int | doctor 表總筆數 |
| `totalPatients` | int | patient 表總筆數 |
| `totalAppointments` | int | appointment 表總筆數 |
| `byStatus.BOOKED` | int | 狀態為 BOOKED 的掛號數 |
| `byStatus.COMPLETED` | int | 狀態為 COMPLETED 的掛號數 |
| `byStatus.CANCELLED` | int | 狀態為 CANCELLED 的掛號數 |

> **提示**：`byStatus.COMPLETED ≥ 1` 是自動驗收的判斷條件之一。也就是說，你必須真正點過「看診完成」按鈕，才能讓這個欄位有值——兩個功能互相連動。

**實作要點**：
- 在 `AppointmentRepository` 或新建的 `StatsRepository` 中，用 `@Query` 或 Spring Data 的 `countBy...` 方法查詢各狀態筆數
- 建立一個簡單的 `StatsController`（或在現有 Controller 中新增方法），路由為 `GET /api/stats`
- 回傳的 Java 物件可以用一個簡單的 Map 或新建一個 `StatsResponse` 類別

AI 輔助提示：
> - 「Spring Data JPA 如何用 `count()` 方法查詢資料表總筆數？」
> - 「Spring Data JPA 如何依照 enum 欄位分組計算數量？」
> - 「Spring Boot REST Controller 如何回傳含巢狀物件的 JSON？」

---

### Part C：Git 提交與文件規範（必做）

#### Commit 規範

- 每筆 commit message 必須以 `[NO-AI]` 或 `[AI-USED]` 開頭
- 整個 repository 中，`[AI-USED]` commit **至少 2 筆**（對應功能一與功能二）

#### `[AI-USED]` commit 三欄詳述格式

每筆 `[AI-USED]` commit 的 message body，必須包含以下三欄：

```
[AI-USED] 新增看診完成按鈕

問AI：Thymeleaf 如何在表格依狀態顯示不同按鈕？
AI建議：使用 th:if="${appt.status == 'BOOKED'}" 控制按鈕顯示，用 fetch 呼叫 PUT API
我的修改：採用 AI 建議的方式，但將成功回呼改為 location.reload() 而非局部更新
```

三欄的目的是讓你誠實反思：你問了什麼？AI 給了什麼？你最終做了什麼決定？**複製 AI 輸出而不加任何修改或說明，不符合規範**。

#### `[NO-AI]` commit 範例

```
[NO-AI] 初始化 Git repository 並推上 GitHub

建立 .gitignore，排除 target/、miniclinic.db、.idea/
```

---

## 繳交方式

不需要上傳 zip 檔。在教學平台提交**兩個網址**：

1. **GitHub repository URL**（格式：`https://github.com/你的帳號/miniclinic`）
2. **Render 部署 URL**（格式：`https://miniclinic-你的帳號.onrender.com`）

Repository 在繳交期間必須為 **Public**（評分系統需要存取）；成績公布後可自行到 GitHub → Settings → Change visibility 改回 Private。

---

## 自動化驗收說明

本作業的大部分評分項目由 **AI agent 自動驗收**，減少人工閱卷。驗收工具收到你的 Render URL 與 GitHub URL 後，依照以下清單逐一執行：

### HTTP 端點測試（T 序列）

| # | 測試項目 | 請求 | 通過條件 |
|---|---|---|---|
| T01 | 服務存活 | `GET /api/health` | HTTP 200，body 含 `"status":"ok"` |
| T02 | 醫師清單 | `GET /api/doctors` | HTTP 200，JSON 陣列長度 ≥ 5 |
| T03 | 醫師清單欄位 | `GET /api/doctors` | 每筆含 `doctorId`、`name`、`department`、`specialty` |
| T04 | 未登入保護 | `GET /dashboard` | HTTP 302 重導到登入頁，或 HTTP 200 但 body 含登入表單 |
| T05 | 統計端點存在 | `GET /api/stats` | HTTP 200 |
| T06 | 統計端點格式 | `GET /api/stats` | JSON 含 `totalDoctors`、`totalPatients`、`totalAppointments`、`byStatus` |
| T07 | 統計基準值 | `GET /api/stats` | `totalDoctors` ≥ 5，`totalPatients` ≥ 3，`totalAppointments` ≥ 3，`byStatus.BOOKED` ≥ 1，`byStatus.COMPLETED` ≥ 1，`byStatus.CANCELLED` ≥ 1 |

### GitHub 結構測試（G 序列）

| # | 測試項目 | 通過條件 |
|---|---|---|
| G01 | Repo 可存取 | GitHub URL 公開，可正常存取 |
| G02 | Commit 數量 | `git log --oneline` 輸出 ≥ 5 筆 |
| G03 | Commit 格式 | 所有 commit message 以 `[NO-AI]` 或 `[AI-USED]` 開頭 |
| G04 | AI-USED commit 數量 | 至少 **2** 筆 `[AI-USED]` commit |
| G05 | Dockerfile 存在 | repo 根目錄有 `Dockerfile` |
| G06 | .gitignore 存在 | repo 根目錄有 `.gitignore`，且包含 `target/` |

> **T07 聯動說明**：`byStatus.COMPLETED ≥ 1` 代表你必須真正點過「看診完成」按鈕並讓資料寫入 PostgreSQL。僅實作按鈕 UI 但沒有實際操作，這一項仍會 fail。

---

## 評分標準

| 項目 | 配分 | 驗收方式 |
|---|---|---|
| 基礎 API 測試通過（T01–T04） | 30% | AI agent 自動 |
| `/api/stats` 新功能實作（T05–T07） | 25% | AI agent 自動 |
| GitHub 結構測試通過（G01–G06） | 25% | AI agent 自動 |
| `[AI-USED]` commit 三欄詳述品質 | 10% | AI agent 初篩（LLM 判斷內容是否誠實切題）+ 人工抽查 |
| README 完整度 | 10% | AI agent 初篩（檢查關鍵 heading）+ 人工抽查 |

**只要有繳交作業，最低保底 40 分**（對應服務存活 T01 通過 + repo 可存取 G01 通過的基本情況）。

> **關於 `[AI-USED]` 三欄詳述的評分**：AI 初篩會判斷「問AI的問題與提交的功能是否相關」「三欄是否都有實質內容」「是否像在如實描述開發過程」。把 AI 輸出整段貼上而不加任何描述，或三欄僅有一行字，都可能在抽查時被扣分。

---

## 常見問題

**Q：按下「看診完成」按鈕後，頁面沒有任何反應？**

先打開瀏覽器的開發者工具（F12），看 Console 是否有 JavaScript 錯誤，或 Network 分頁的 PUT 請求是否回了非 200 的狀態碼。常見原因：
- URL 中的 `apptId` 取錯欄位（應從 `th:data-id="${appt.apptId}"` 取）
- Session 已過期（重新登入再試）
- `PUT /api/appointments/{id}/status` 的 `/status` 路徑拼錯

**Q：`GET /api/stats` 回傳 401 或 302？**

代表這支 API 被 Interceptor 攔截了。確認 `WebConfig.java` 的 `addPathPatterns()` 中，`/api/stats` 沒有被保護路徑涵蓋。這支 API 不需要登入，因此不應加入受保護路徑清單。

**Q：`byStatus.COMPLETED` 在 Render 上是 0，本機是 1？**

Render 的 PostgreSQL 是獨立的資料庫，初始資料由 `data-prod.sql` 的 `INSERT ... ON CONFLICT DO NOTHING` 填入，初始值三筆掛號狀態都是 `BOOKED`。你需要登入線上服務，實際點一次「看診完成」，才會有 `COMPLETED` 資料進入 PostgreSQL。

**Q：`byStatus` 的 JSON 結構怎麼做？**

最簡單的方式是用 `Map<String, Object>` 組合：

```java
Map<String, Object> byStatus = new LinkedHashMap<>();
byStatus.put("BOOKED", bookedCount);
byStatus.put("COMPLETED", completedCount);
byStatus.put("CANCELLED", cancelledCount);

Map<String, Object> result = new LinkedHashMap<>();
result.put("totalDoctors", totalDoctors);
result.put("totalPatients", totalPatients);
result.put("totalAppointments", totalAppointments);
result.put("byStatus", byStatus);

return ResponseEntity.ok(result);
```

也可以定義一個 `StatsResponse` 類別（含 `ByStatus` 內部類別）讓 Jackson 自動序列化。

**Q：Render 部署成功，但 `GET /api/stats` 的 `totalDoctors` 是 0？**

可能是 `data-prod.sql` 未執行，或 INSERT 語句有錯誤。確認：
1. `application-prod.properties` 有 `spring.sql.init.mode=always`（或在 Render 環境變數設定）
2. `data-prod.sql` 的 INSERT 使用 `ON CONFLICT (doctor_id) DO NOTHING` 語法（不是 `INSERT OR IGNORE`）

**Q：`[AI-USED]` commit 可以事後補嗎？**

Commit 的時間點是有記錄的。建議在每次用 AI 輔助完成一個小段落後，立即 commit——不要等功能全部完成才補一筆大 commit。事後補的 commit 會讓 AI 初篩難以判斷 commit 對應的實際修改內容，可能影響評分。

**Q：GitHub repo 可以先 Private，繳交前再改 Public 嗎？**

可以，只要**繳交截止前**確保為 Public 即可。改為 Public 的方法：GitHub repo 頁面 → Settings → Danger Zone → Change visibility → Make public。

---

## 本學期回顧

恭喜你完成了整個 MiniClinic 的開發與部署。回顧五週的歷程：

| 週次 | 里程碑 |
|---|---|
| Week 1 | Spring Boot 骨架，`/api/health` 第一個 API |
| Week 2 | Thymeleaf 頁面，醫師清單，掛號表單 |
| Week 3 | SQLite + JPA，資料真正持久化 |
| Week 4 | BCrypt 密碼、Session 登入、Interceptor 保護 |
| Week 5 | Git、Docker、Render 部署，AI 輔助新功能，線上可訪問 |

你在課程中實踐的**每次用 AI、每次記錄、每次部署**，都是業界真實開發流程的縮影。
