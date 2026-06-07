# Ch09-E｜Git、GitHub 與 Render 雲端部署

> **Git, GitHub, and Cloud Deployment with Render**
>
> 對應週次：第五週（期末整合週）｜建議講解時間：135 分鐘

---

## 本週學習目標

完成本週課程後，你應該能夠：

1. 使用 Git 基本指令（`add`、`commit`、`push`、`pull`、`status`、`log`）管理程式碼
2. 撰寫符合本課程規範的 commit message，含 AI 使用紀錄
3. 設定 `.gitignore`，避免把敏感檔案推上 GitHub
4. 撰寫專業的 README.md，說明專案如何執行
5. 使用 Spring Profiles 切換 dev（本機 SQLite）/ prod（雲端 PostgreSQL）
6. 把 MiniClinic 部署到 Render，讓全世界都能訪問

---

## 1. 本週的特殊定位：期末整合

這是 Ch09 模組的最後一週，也是整個學期的收尾。

### 四週的累積

| 週次 | 你完成了什麼 |
|---|---|
| Ch09-A | Spring Boot 專案骨架、第一個 REST API |
| Ch09-B | Controller、Thymeleaf、完整 API 設計 |
| Ch09-C | SQLite、JPA、資料真正存起來 |
| Ch09-D | 登入、驗證、安全、醫療資料倫理 |
| **Ch09-E** | **Git、GitHub、雲端部署 → MiniClinic 上線** |

本週結束時，你會擁有：

- 一個**真實線上運作**的 Web 應用
- 一個**公開的 GitHub repository**
- 一份**專業的 README**
- **完整的 commit history**，符合課程規範

這些可以直接放進你的**履歷或求職作品集**。

### 本週的授課方式

和前四週不同，本週大部分時間會是**實機示範**：

- 老師在大螢幕上從 `git init` 開始，一路示範到 Render 部署完成
- 學生跟著做，有問題立刻提出
- 講義提供步驟記錄，回家可對照複習

程式碼展示會很少，**概念與操作步驟是主角**。

---

## 2. 為什麼需要版本控制？

### 你可能已經做過的事

- 「final.java」、「final_v2.java」、「final_final.java」、「really_final.java」
- 把專案資料夾複製好幾份：「miniclinic_backup_0415」、「miniclinic_backup_0416」
- 改壞了想回到昨天的版本，但不確定昨天改了什麼
- 和同學合作，用 email 互寄 zip 檔

這些都是**土法煉鋼的版本控制**，很容易出錯。

### Git 做了什麼

Git 自動幫你紀錄每一次的變更：

```
12:30  新增 Doctor.java
13:45  新增 DoctorRepository.java、修改 pom.xml
14:20  修正 SQL Injection 漏洞（改用 PreparedStatement）
15:00  新增登入功能
```

你可以：

- **看到每次的改動**
- **跳回任何一個過去的版本**
- **與別人合作而不互相覆蓋**
- **把程式碼同步到雲端**

### 為什麼求職時 GitHub 很重要？

面試官看你的 GitHub 能快速判斷：

- 你的程式碼**寫作習慣**
- 你對 **Git 工具**的熟悉程度
- 你的**溝通能力**（commit message 寫得好不好）
- 你是否**認真做完一個專案**

一份好的 GitHub profile **比一份履歷更有說服力**。

---

## 3. Git 基本操作

### 3.1 初始化

```bash
cd miniclinic
git init
```

### 3.2 基本工作流程

```
編輯檔案 → git add → git commit → git push
  ↓          ↓           ↓            ↓
修改內容   加入暫存區   建立版本     推到 GitHub
```

### 3.3 最常用的六個指令

```bash
# 查看目前狀態
git status

# 把變更加入暫存區
git add .                    # 加入所有變更
git add Doctor.java          # 只加入特定檔案

# 建立版本（commit）
git commit -m "[Ch09-A][NO-AI] 建立 Doctor 類別"

# 推到遠端 GitHub
git push

# 從 GitHub 拉下最新的
git pull

# 查看歷史紀錄
git log --oneline
```

### 3.4 VS Code 的 Git 整合

VS Code 左側邊欄有 **Source Control** 圖示（分支符號）：

- 點選後顯示所有變更檔案
- 可以視覺化檢視 diff（左右對照）
- 可以在介面上直接 commit、push
- **建議初學者同時用 VS Code GUI 和 terminal**，熟悉後就習慣了

### 3.5 提交歷史查看

```bash
git log --oneline --graph --all
```

會看到類似：

```
* a3c5f21 [Ch09-D][AI-USED] 加入登入 Interceptor
* 8d2b1e9 [Ch09-D][NO-AI] 修正 BCrypt 雜湊產生
* 4f7a3d2 [Ch09-C][AI-USED] 實作 Appointment Entity 關聯
* 2e9c8b5 [Ch09-C][NO-AI] 修正 data.sql 編碼問題
...
```

---

## 4. .gitignore：不該推上去的檔案

### 4.1 為什麼需要 .gitignore？

有些檔案**不該**進入版本控制：

- **編譯產物**：`target/`（每次 build 會重新產生）
- **IDE 設定**：`.vscode/settings.json`、`.idea/`（每個人設定不同）
- **敏感資料**：`.env`（API key、資料庫密碼等真正的秘密）
- **資料庫檔案**：`miniclinic.db`（有測試資料、會變動）
- **作業系統檔**：`.DS_Store`、`Thumbs.db`

### 4.2 MiniClinic 的 .gitignore

在專案根目錄建立 `.gitignore`：

```gitignore
# Compiled output
target/
*.class

# IDE
.idea/
.vscode/
*.iml

# OS
.DS_Store
Thumbs.db

# Logs
*.log

# Database
*.db
*.db-journal

# Sensitive config
.env
```

> ⚠️ **重要：不要把 `mvnw` 加進 `.gitignore`**
>
> `mvnw`、`mvnw.cmd` 和 `.mvn/` 這三個檔案**必須推上 GitHub**。
> Render 部署時，Docker image 的建置過程會在容器內執行 `./mvnw clean package`，如果這些檔案被排除，Dockerfile 的 build 就會失敗。
>
> **為什麼 `application-prod.properties` 不在這裡？**
>
> `application-prod.properties` 只包含 `${DATABASE_URL}` 等**環境變數占位符**，不含實際密碼，可以安全地推上 GitHub。真正的敏感資訊是 Render 環境變數設定頁裡的值（例如實際的資料庫連線字串），而非這份屬性檔。詳見 §9.2 說明。

### 4.3 敏感資訊的紅線

**絕對不能推上 GitHub**：

- 資料庫連線密碼
- API key（OpenAI、Google、AWS...）
- 雲端服務的 service account
- 任何「如果外洩會很麻煩」的東西

**實際案例**：有人不小心把 AWS key 推上 GitHub，半小時後就有機器人掃到，用來挖礦，**一小時累積 2 萬美金的雲端帳單**。

### 4.4 如果不小心 commit 了敏感資料怎麼辦？

光刪除再 commit **不夠**——過去的 commit 裡還看得到。必須：

1. 立刻**作廢該 key / 密碼**
2. 用 `git filter-repo` 清除歷史
3. 強制推送（`git push --force`）

但**最好的方法是一開始就別推上去**。

---

## 5. GitHub：把本機連到雲端

### 5.1 建立 GitHub repository

1. 前往 <https://github.com/new>
2. Repository name：`miniclinic`
3. Choose visibility: 選擇 **Public**（作業要求公開）
4. **不要**勾選 "Add a README file"（保持選項 Off ，等下會從本機推上去）
5. 點 **Create repository**

### 5.2 把本機連到遠端

GitHub 會顯示的指令：

```bash
# 初始化
git init

# 告訴 Git：這個本機 repo 要連到這個遠端
git remote add origin https://github.com/你的帳號/miniclinic.git

# 改 main 分支名稱
git branch -M main

# 將變更加入暫存區
git add .

# 建立版本（commit）
git commit -m "[NO-AI] 初始提交：Spring Boot MiniClinic 專案骨架"
  
# 第一次推送（-u 建立追蹤關係）
git push -u origin main
```

### 5.3 之後的例行操作

```bash
git add .
git commit -m "[AI-USED/NO-AI] 簡短說明變更之處"
git push
```

---

## 6. Commit Message 規範（本週新規範）

### 6.0 為什麼到第五週才規範？

你在 Ch09-A 應該注意到：本課程要求在使用 AI 時**誠實記錄**。但前四週繳交 zip 檔，**沒有用到 Git**——所以沒有 commit 這個載體來記錄。

本週開始，MiniClinic 要推上 GitHub。這是第一次真正用到 Git commit message 的時機，也是本課程對 AI 使用記錄的**正式要求**。

**本節要回答兩個問題**：

- 什麼樣的 commit message 是好的？
- 怎麼用 commit message 誠實記錄 AI 使用？

### 6.1 為什麼要規範 commit message？

- **可追溯**：半年後你自己回頭看，知道每次改了什麼
- **可溝通**：合作夥伴或面試官能理解你的開發過程
- **誠實面對 AI**：課程要求**揭露** AI 使用情形，這是學術誠信

### 6.2 標題格式

```
[AI 標籤] 簡短描述

範例：
[NO-AI] 加入 PostgreSQL JDBC 依賴到 pom.xml
[AI-USED] 處理 Render 的 DATABASE_URL 格式轉換
```

**AI 標籤**（兩種擇一，必填）：

- `[NO-AI]`：完全沒有使用 AI（自己寫 / 參考講義 / Google 搜尋）
- `[AI-USED]`：有向 AI 詢問或採納 AI 建議

### 6.3 詳細內容（有使用 AI 時必寫）

當你用 `[AI-USED]` 時，**commit message 的 body 必須包含三個區塊**：

```
[AI-USED] 設定 Spring Profiles 切換 dev 與 prod

問AI: 「Spring Boot 如何根據環境變數自動切換 application-dev
      和 application-prod 兩份設定？」
AI建議: 設定 spring.profiles.active，或用命令列參數
       --spring.profiles.active=prod 覆寫
我的修改: 本機 application.properties 預設 dev；
        Render 環境變數 SPRING_PROFILES_ACTIVE=prod 覆寫
```

**三個必備欄位**：

1. **問 AI**：你實際問了什麼（可以縮寫，抓到精髓即可）
2. **AI 建議**：AI 給了什麼建議或程式碼
3. **我的修改**：你自己做了什麼調整、或為什麼照用

### 6.4 完整範例

以下範例都是本週會做的事情：

#### 範例 A：完全沒用 AI

```
[NO-AI] 加入 PostgreSQL JDBC 依賴到 pom.xml

對照 Ch09-E 講義第 9 節完成。
```

#### 範例 B：用 AI 但做了修改

```
[AI-USED] 實作 dev / prod 兩個 Profile 的設定分離

問AI: 「Spring Boot 如何根據環境變數自動切換 application-dev
      和 application-prod 兩份設定？」
AI建議:
  - 設定 spring.profiles.active
  - 或用命令列 --spring.profiles.active=prod
  - 也可以用 @Profile 註解分環境載入 bean
我的修改:
  - 本機 application.properties 預設 spring.profiles.active=dev
  - Render 透過環境變數 SPRING_PROFILES_ACTIVE=prod 覆寫
  - 沒有用 @Profile（課程沒教，維持簡單）
```

#### 範例 C：直接採納 AI 建議

```
[AI-USED] 修正 Render 的 DATABASE_URL 格式

問AI: 「Render 給的 postgres://user:pass@host/db 格式，
      Spring Boot 報錯 No suitable driver found」
AI建議: Spring Boot 需要 jdbc:postgresql:// 開頭的格式，
       並把 user/password 另外設為環境變數
我的修改: 直接採納，確實是 URL schema 前綴的差異
```

### 6.5 評分重點

- 本週至少要有 **5 個 commit**（不接受一次性全部 commit）
- 全部 commit 都符合格式
- `[AI-USED]` 的 commit 必須有三欄詳述
- 刻意把所有 commit 都標 `[NO-AI]`（明顯不合理）會被助教特別檢查

### 6.6 AI 使用的誠實原則

本課程**鼓勵你用 AI**，但要求你**誠實記錄**。原因：

- **保護你自己**：未來回頭看得出哪些部分是 AI 寫的（可能需要驗證）
- **訓練批判思維**：寫下「我的修改」迫使你思考「為什麼要改」
- **職場預備**：業界越來越要求 AI 使用透明度
- **避免學術倫理問題**：刻意隱瞞才是問題，使用本身沒問題

---

## 7. README.md：專案的門面

### 7.1 README 的重要性

當別人進入你的 GitHub repo，第一眼看到的就是 README。

**面試官會看的**：

- 專案是做什麼的？
- 用了什麼技術？
- 如何在自己的電腦跑起來？
- 有線上 demo 嗎？

### 7.2 本課程要求的 README 結構

````markdown
# MiniClinic 社區診所掛號系統

一個以 Spring Boot 實作的社區診所掛號系統，支援醫師登入、病患掛號、
掛號狀態管理等功能。

## 線上 Demo

https://miniclinic-你的帳號.onrender.com

## 技術棧

- Java 17
- Spring Boot 3.x
- Spring Data JPA
- Thymeleaf
- SQLite（開發）/ PostgreSQL（部署）
- BCrypt（密碼雜湊）

## 功能清單

- 醫師登入 / 登出
- 醫師個人 Dashboard
- 病患資料管理（CRUD）
- 線上掛號功能
- 掛號狀態變更（booked / completed / cancelled）
- RESTful API（支援第三方整合）

## 本機執行

```bash
git clone https://github.com/你的帳號/miniclinic.git
cd miniclinic
./mvnw spring-boot:run
```

開啟瀏覽器訪問 http://localhost:8080

預設醫師帳密：

- D001 / pass1234
- D002 / pass1234
- （其他醫師密碼均為 pass1234）

## 資料初始化

第一次啟動時，`data.sql` 會自動插入：
- 5 位虛構醫師
- 3 位虛構病患（TEST00001, TEST00002, TEST00003）
- 3 筆示範掛號

## 專案結構

```
src/
├── main/
│   ├── java/tw/edu/fju/miniclinic/
│   │   ├── controller/     # HTTP 請求處理
│   │   ├── model/          # Entity 與 Repository
│   │   ├── interceptor/    # 登入驗證
│   │   └── config/         # Spring 配置
│   └── resources/
│       ├── templates/      # Thymeleaf 模板
│       ├── static/         # CSS、JS
│       └── application.properties
```

## 作者

2026 年 Java 程式設計課程作業

## 聲明

所有病患資料均為虛構，僅供教學使用。
````

### 7.3 可以有，但非必要的章節

- **API 文件**（如果時間允許，列出所有端點）
- **螢幕截圖**（Dashboard、登入頁等）
- **已知問題**（誠實列出 bug 也是一種專業）
- **授權**（MIT、Apache 2.0 等）

### 7.4 實作：建立並推送 README.md

#### 步驟 1：在專案根目錄建立 README.md

在 VS Code 的 Explorer 面板，對專案根目錄按右鍵 → **New File** → 輸入 `README.md`。

或在 terminal 裡：

```bash
# 在 miniclinic/ 根目錄執行
code README.md
```

#### 步驟 2：撰寫內容

參考 §7.2 的結構，把以下幾個佔位符換成你自己的資訊：

- `你的帳號` → 你的 GitHub 帳號名稱
- `miniclinic-你的帳號.onrender.com` → 等部署完成後再填入 Render 給的網址

其他內容（技術棧、功能清單、本機執行步驟）可以直接照 §7.2 的範例複製貼上，再依你實際的情況微調。

> 💡 Render 部署網址在 §11 完成後才會知道，可以先空著，部署成功後再回來補上、commit 一次。

#### 步驟 3：加入版本控制並推送

```bash
# 把 README.md 加入暫存區
git add README.md

# 建立 commit（這是一個不需要 AI 的動作）
git commit -m "[NO-AI] 新增 README.md，說明專案功能與本機執行步驟"

# 推到 GitHub
git push
```

#### 步驟 4：確認 GitHub 上正確顯示

推送後，前往你的 GitHub repository 頁面（`https://github.com/你的帳號/miniclinic`），確認：

- 頁面下方自動顯示 README 內容
- 標題、技術棧、本機執行步驟都正確呈現

Markdown 的標題（`#`）、粗體（`**`）、程式碼區塊（` ``` `）都會正確渲染——這就是為什麼我們用 `.md` 格式而不是純文字。

---

## 8. 從 SQLite 到 PostgreSQL

### 8.1 為什麼雲端不用 SQLite？

本機用 SQLite 很方便，但 Render 等雲端平台**大多不適合 SQLite**：

| 問題 | 說明 |
|---|---|
| **檔案系統不持久** | Render 每次重新部署，檔案會被清空 |
| **並發限制** | SQLite 寫入是序列化的，多人使用會慢 |
| **無法外部存取** | 無法從其他服務連線 |
| **備份困難** | 雲端服務沒有標準化的 SQLite 備份機制 |

### 8.2 PostgreSQL 的優勢

- **真正的客戶端/伺服器架構**
- **支援高並發寫入**
- **Render 提供免費方案**（夠你做作業）
- **業界標準**（求職加分）

### 8.3 好消息：JPA 替你處理大部分差異

因為你整個 Ch09-C 都用 JPA 的抽象介面，**換資料庫幾乎不用改程式碼**。

需要改的只有：

1. `pom.xml` 加 PostgreSQL 的 JDBC driver
2. `application.properties` 的 datasource URL
3. `data.sql` 的語法細節（如果有用到 SQLite 方言）

### 8.4 SQLite vs PostgreSQL 常見差異

| 項目 | SQLite | PostgreSQL |
|---|---|---|
| 自動遞增 | `INTEGER PRIMARY KEY AUTOINCREMENT` | `SERIAL` / `BIGSERIAL` |
| 日期型別 | 存成 TEXT | 原生 `DATE` / `TIMESTAMP` |
| 字串型別 | `VARCHAR` 其實存為 TEXT | 嚴格型別檢查 |
| 大小寫 | 預設不區分 | 預設區分 |

不用擔心記住——JPA 的 `@GeneratedValue(strategy = IDENTITY)` 兩邊都能用。

---

## 9. Spring Profiles：dev 與 prod 分離

### 9.1 什麼是 Profiles？

Spring Profiles 讓你有**多套設定**，啟動時擇一使用。

```
application.properties              ← 共通設定
application-dev.properties          ← 本機開發用
application-prod.properties         ← 正式環境用
```

啟動時透過環境變數切換：

```bash
SPRING_PROFILES_ACTIVE=prod java -jar miniclinic.jar
```

### 9.2 我們的配置設計

**application.properties（共通）**：

```properties
# 不論 dev 或 prod 都要的設定
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.sql.init.mode=always
spring.jpa.defer-datasource-initialization=true
server.port=${PORT:8080}
```

`${PORT:8080}` 意思是：優先用環境變數 `PORT`，沒有則用 8080（Render 會設定這個變數）。

**application-dev.properties（本機 SQLite）**：

```properties
spring.datasource.url=jdbc:sqlite:miniclinic.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
```

**application-prod.properties（雲端 PostgreSQL）**：

```properties
spring.datasource.url=${DATABASE_URL}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.sql.init.data-locations=classpath:data-prod.sql
```

`${DATABASE_URL}` 從環境變數讀，由 Render 自動注入。

`spring.sql.init.data-locations` 讓 prod 環境使用獨立的 `data-prod.sql`，而非預設的 `data.sql`——原因見下方說明。

> **關於版本控制**：`application-prod.properties` 只包含環境變數占位符，不含任何實際密碼，可以安全地推上 GitHub。真正需要保密的是 Render 環境變數設定頁裡的值。`.gitignore` 裡**不需要**也**不應該**把這份檔案排除——排除後 Render 讀不到 prod 設定，啟動必然失敗。

**為什麼 prod 需要獨立的 `data-prod.sql`？**

`data.sql` 使用 SQLite 專有的 `INSERT OR IGNORE` 語法，PostgreSQL 不支援。同時，PostgreSQL 的 auto-increment 是靠**序列（sequence）**產生 ID：`data.sql` 手動插入 `appt_id` 1/2/3 時，序列不知道這件事，仍然從 1 開始計數，第一筆新增的掛號就會撞主鍵報錯。SQLite 沒有這個問題（它的 AUTOINCREMENT 看的是表裡最大的 ID），所以本機不會出錯，部署後才會發現。

因此 prod 使用獨立的 `data-prod.sql`，解決這兩個差異：

| | `data.sql`（本機 SQLite）| `data-prod.sql`（prod PostgreSQL）|
|---|---|---|
| 防重複語法 | `INSERT OR IGNORE` | `ON CONFLICT (...) DO NOTHING` |
| 序列重設 | 不需要 | 需要（末尾加一行，如下面第11節的步驟0）|


### 9.3 pom.xml 加入 PostgreSQL

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

SQLite 依賴保留，兩個同時存在沒問題。

### 9.4 預設 Profile

在 `application.properties` 加：

```properties
# 本機啟動預設 dev 模式
spring.profiles.active=dev
```

這樣本機啟動預設是 dev 模式（SQLite）。雲端則透過環境變數強制用 prod。

### 9.5 提交並推送 Profile 設定

完成 §9.1–9.4 的設定後，把這批相關檔案推上 GitHub：

```bash
git add pom.xml \
        src/main/resources/application.properties \
        src/main/resources/application-dev.properties \
        src/main/resources/application-prod.properties
git commit -m "[NO-AI] 設定 Spring Profiles：dev（SQLite）/ prod（PostgreSQL）分離，加入 PostgreSQL 依賴"
git push
```

這裡老師示範使用 VS Code 內建的 git 操作，在 Source Control 分頁裡。

---

## 10. Render 平台介紹

### 10.1 什麼是 Render？

Render 是一個**現代化的雲端部署平台**，特色：

- **免費方案**夠做作業
- **Git 整合**：push 就自動部署
- **免費 PostgreSQL**（90 天，足夠學期使用）
- **支援多語言**：Node、Python、Go、Rust，以及透過 Docker 的任何語言（包含 Java）
- **UI 簡潔**，新手友好

### 10.2 免費方案的限制

- 沒訪問時會**進入休眠**（30 秒後回應第一次請求）
- Web service 每月 750 小時（夠 24/7 運作）
- 資料庫 90 天後需要重建（或升級付費版）

這些限制對教學專案**完全沒影響**。

### 10.3 其他類似平台

- **Railway**：類似 Render，UI 更精緻（但免費額度更少）
- **Fly.io**：更進階，全球部署
- **Heroku**：老牌，但免費方案已取消
- **AWS / GCP / Azure**：企業級，設定複雜

本課程選 Render 因為**學習曲線最平緩**。

---

## 11. Render 部署完整流程（課堂現場示範）

### 步驟 0：部署前準備——更新 data-prod.sql

在開始設定 Render 之前，先把 `data.sql` 的語法寫成 `data-prod.sql`。

**為什麼要改？**

Week 3 / Week 4 用的 `INSERT OR IGNORE` 是 SQLite 專有語法，PostgreSQL 不認識，啟動時會直接報錯。改成 `ON CONFLICT (...) DO NOTHING` 兩邊都可以執行（SQLite 3.24+ 與 PostgreSQL 皆支援）。

**修改方式**：把三張表的 INSERT 都改成以下格式：

```sql
-- 改前（SQLite 專有，PostgreSQL 會報錯）
INSERT OR IGNORE INTO doctor (...) VALUES (...);

-- 改後（SQLite 3.24+ 與 PostgreSQL 皆可）
INSERT INTO doctor (...) VALUES (...)
ON CONFLICT (doctor_id) DO NOTHING;
```

另外還要新增序列重設的語法，在最後加入。這行語法是 PostgreSQL 專有的，所以必須放在 prod 專屬的檔案裡。原本的 `data.sql` 保持不變，本機開發繼續正常運作：

```sql
-- 重設 appointment 序列，讓下一筆 ID 從 max+1 開始，避免主鍵衝突
SELECT setval(pg_get_serial_sequence('appointment', 'appt_id'),
              COALESCE((SELECT MAX(appt_id) FROM appointment), 0) + 1, false);
```

以下是 data-prod.sql 完整內容：
```sql
-- 初始醫師資料（5 位）帶入 pass1234 的 BCrypt 雜湊
INSERT INTO doctor (doctor_id, name, department, specialty, password_hash) VALUES
    ('D001', '陳志明醫師', '家醫科', '一般內科、慢性病管理','$2a$10$XhyEgd4qh5TXJa7NkMg3gOqsJxATykAyJERH7ZqTD7eEPVlcmgewm'),
    ('D002', '林佩君醫師', '內科',   '心臟血管、高血壓', '$2a$10$/x/fVm66HZJWeeYZRUbPp..gS9Czgs3a27RjYQPs75obpRoUWU9ZC'),
    ('D003', '王建華醫師', '復健科', '運動傷害、脊椎復健', '$2a$10$4fZBPZq1NJmqW5MUgOUsqukV6OiTJutAKR/WbiFiQ6PRTjFbNsMFy'),
    ('D004', '李美玲醫師', '小兒科', '兒童感冒、疫苗接種',  '$2a$10$ZlsUgEo2MOm0RYxwcP55qukrjipEXYNKyyRfdIKkOEv7RpuXEPhxK'),
    ('D005', '張雅筑醫師', '身心科', '焦慮、失眠、情緒調適', '$2a$10$XsgY9Cmk7PqJ2pve2k4xwuTnV/hakC6LOGJqicQyjH.wDiM7PQhWa')
ON CONFLICT (doctor_id) DO NOTHING;

-- 初始病患資料（3 位虛構病患）
INSERT INTO patient (chart_no, name, gender, birth_date, phone) VALUES
    ('TEST00001', '測試病患甲', '男', '1985-03-15', '0912-345-678'),
    ('TEST00002', '王小明',     '男', '1990-07-22', '0923-456-789'),
    ('TEST00003', '李小華',     '女', '1988-11-30', '0934-567-890')
ON CONFLICT (chart_no) DO NOTHING;

-- 初始掛號資料（appt_id 必須明確指定，ON CONFLICT 才能防止重複）
INSERT INTO appointment (appt_id, chart_no, doctor_id, appt_date, time_slot, status) VALUES
    (1, 'TEST00001', 'D001', '2026-05-01', 'AM', 'BOOKED'),
    (2, 'TEST00002', 'D002', '2026-05-01', 'AM', 'BOOKED'),
    (3, 'TEST00003', 'D003', '2026-05-02', 'PM', 'BOOKED')
ON CONFLICT (appt_id) DO NOTHING;

-- 重設 appointment 序列，讓下一筆新增的掛號 ID 從 max+1 開始，避免主鍵衝突
SELECT setval(pg_get_serial_sequence('appointment', 'appt_id'),
              COALESCE((SELECT MAX(appt_id) FROM appointment), 0) + 1, false);
```

> **為什麼要這樣做？** `data.sql` 手動插入了 `appt_id` 1/2/3，但 PostgreSQL 的序列不知道這件事，仍從 1 開始計數。不重設的話，第一筆新增的掛號就會與初始資料撞主鍵，系統報錯。詳見 §9.2 說明。

建立完成後，一起提交：

```bash
git add src/main/resources/data-prod.sql src/main/resources/application-prod.properties
git commit -m "[NO-AI] 部署前準備：新增 data-prod.sql（PostgreSQL 語法＋序列重設），prod 設定指向新檔"
git push
```

> `data.sql` 保持原本的 `INSERT OR IGNORE` 語法不動，本機開發不受影響。


### 步驟 1：註冊 Render 帳號

1. 前往 <https://render.com>
2. 點選 **Get Started**
3. **使用 GitHub 帳號登入**（直接打通後續授權）

### 步驟 2：建立 PostgreSQL 資料庫

1. Dashboard → **New +** → **Postgres**
2. Name：`miniclinic-db`
3. Region：選離台灣近的（例如 Singapore）
4. Plan：**Free**
5. 點 **Create Database**

稍等一下（三分鐘）。建立後，頁面會顯示：

- **Internal Database URL**（內部連線字串）
- **External Database URL**（外部連線字串）

**記下 Internal URL**，下一步會用到。

### 步驟 3：建立 Dockerfile 並推上 GitHub

Render 的 Language 下拉選單沒有 Java，Java 專案透過 **Docker** 部署。在設定 Render 之前，先在專案根目錄（`miniclinic/` 下，與 `pom.xml` 同層）建立 `Dockerfile`：

```dockerfile
# 第一階段：用 Maven 編譯出 JAR
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 第二階段：只帶著 JAR 執行，image 較小
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/miniclinic-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

這是 **multi-stage build**：第一階段用含 Maven 的官方 image 編譯，第二階段只用輕量 JRE 執行，最終 image 較小、部署較快。

> 這裡用 `mvn`（Maven 官方 image 內建），而非 `./mvnw`——Alpine image 沒有 `curl`，`mvnw` 嘗試下載 Maven 時會失敗。

建立後提交並推送：

```bash
git add Dockerfile
git commit -m "[NO-AI] 新增 Dockerfile，供 Render 部署使用"
git push
```

### 步驟 4：建立 Web Service

1. Dashboard → **New +** → **Web Service**
2. **Source Code** → Git Provider → 選擇你的 GitHub 上剛剛建立的 repo：`miniclinic`
3. 填寫：
   - **Name**：`miniclinic-你的帳號`
   - **Language**：**Docker**
   - **Branch**：`main`
   - **Region**：和資料庫同一區
4. **Plan**：Free

> Build Command 和 Start Command 欄位留空——這兩件事已由 Dockerfile 處理。

### 步驟 5：設定環境變數

在 **Environment Variables** 區塊，共需設定 **4 個**環境變數。

**① 先拆解 Render 給的 Internal Database URL**

URL 格式長這樣：

```
postgresql://帳號:密碼@主機位址(:埠號)/資料庫名稱
```

以真實範例說明：

```
postgresql://miniclinic_db_o3us_user:rKYH1Bo96XZ9GaehYLmevxZePGbUO3GG@dpg-d88hk5cm0tmc7386jiqg-a/miniclinic_db_o3us
```

從這串 URL 拆出三個值：

| 要填的環境變數 | 從哪裡取 | 範例值 |
|---|---|---|
| `SPRING_DATASOURCE_USERNAME` | `://` 之後、`:` 之前的帳號 | `miniclinic_db_o3us_user` |
| `SPRING_DATASOURCE_PASSWORD` | 帳號後 `:` 到 `@` 之間的密碼 | `rKYH1Bo96XZ9GaehYLmevxZePGbUO3GG` |
| `DATABASE_URL` | 去掉 `帳號:密碼@`，把 `postgresql://` 換成 `jdbc:postgresql://` | `jdbc:postgresql://dpg-d88hk5cm0tmc7386jiqg-a/miniclinic_db_o3us` |

**② 填入 Render 的 Environment Variables**

| Key | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | `jdbc:postgresql://主機位址/資料庫名稱`（見上方說明）|
| `SPRING_DATASOURCE_USERNAME` | 從 URL 取出的帳號 |
| `SPRING_DATASOURCE_PASSWORD` | 從 URL 取出的密碼 |

### 步驟 6：點 Depoly Web Service

Render 開始：

1. clone 你的 GitHub repo
2. 建置 Docker image（含 Maven 編譯，第一次約 8-12 分鐘）
3. 啟動容器

整個過程比純 Maven build 稍長，因為要下載 Docker base image 並執行兩階段 build。右側即時顯示 log，可以看到 Maven 下載依賴的過程。

### 步驟 7：訪問線上網站

成功後，頁面上會顯示網址：

```
https://miniclinic-你的帳號.onrender.com
```

**你的 MiniClinic 已經上線了！**

### 步驟 8：自動部署

之後每次 `git push` 到 GitHub main 分支，Render **自動重新部署**。

---

## 12. 常見部署錯誤排查

### Q1：Build 失敗，找不到 `./mvnw`

**原因**：`.gitignore` 把 `mvnw` 排除了。

**解法**：從 `.gitignore` 拿掉 `mvnw`，重新推一次：

```bash
git rm --cached mvnw  # 從 git 追蹤移除（但不刪本機）
# 編輯 .gitignore 移除那行
git add mvnw .gitignore
git commit -m "[Ch09-E][NO-AI] 修正 .gitignore，讓 mvnw 被追蹤"
git push
```

### Q2：啟動時報錯「Driver org.postgresql.Driver not found」

**原因**：忘了在 `pom.xml` 加 PostgreSQL 依賴。

**解法**：加入依賴、push、等 Render 重新 build。

### Q3：啟動成功但訪問時 500 錯誤

**原因**：`DATABASE_URL` 格式不對，或 profile 沒切到 prod。

**排查**：
1. Render 的 Environment 頁面確認 `SPRING_PROFILES_ACTIVE=prod`
2. 確認 `DATABASE_URL` 是 `jdbc:postgresql://...` 格式
3. 看 Render 的 Log 視窗找錯誤訊息

### Q4：每次訪問要等 30 秒

**這是正常的**。Render 免費方案沒流量會休眠，第一個訪客會等「喚醒時間」。如果在意，設個 UptimeRobot（<https://uptimerobot.com>）每 5 分鐘 ping 一次即可。

### Q5：data.sql 執行失敗，PostgreSQL 不支援某語法

**常見差異**：
- SQLite 的 `INSERT OR IGNORE` → PostgreSQL 不支援，需改寫
- 日期字串 `'2026-05-01'` 兩邊都可，但複雜格式要檢查

**解法**：將 `INSERT OR IGNORE` 改為 `ON CONFLICT ... DO NOTHING` 語法，SQLite 3.24+ 與 PostgreSQL 皆支援：

```sql
-- 原本（SQLite 專有語法，PostgreSQL 不支援）
INSERT OR IGNORE INTO doctor (doctor_id, name, department, specialty, password_hash)
VALUES ('D001', '陳志明醫師', '家醫科', '一般內科、慢性病管理', '$2a$10$...');

-- 修改後（SQLite 3.24+ 與 PostgreSQL 皆可執行）
INSERT INTO doctor (doctor_id, name, department, specialty, password_hash)
VALUES ('D001', '陳志明醫師', '家醫科', '一般內科、慢性病管理', '$2a$10$...')
ON CONFLICT (doctor_id) DO NOTHING;
```

`appointment` 資料表（主鍵為 `appt_id`）同樣改法：

```sql
INSERT INTO appointment (appt_id, chart_no, doctor_id, appt_date, time_slot, status)
VALUES (1, 'TEST00001', 'D001', '2026-05-01', 'AM', 'BOOKED')
ON CONFLICT (appt_id) DO NOTHING;
```

規則是：`INSERT OR IGNORE INTO table VALUES (...)` → `INSERT INTO table VALUES (...) ON CONFLICT (主鍵欄) DO NOTHING`。全檔替換後，SQLite 與 PostgreSQL 皆可啟動。

### Q6：我把密碼不小心推上去了

**立刻行動**：
1. **改密碼**（舊的當作已洩漏）
2. 用 `git filter-repo` 清除歷史
3. 強制推送
4. 檢查是否要把整個 repo 刪掉重建

---

## 13. 作業五：期末專案

本週作業的完整說明請參閱獨立文件：

> **[HW5_Ch09E_Final_Project.md](HW5_Ch09E_Final_Project.md)**

作業要求學生除了整合部署之外，還需實作**兩個新功能**（看診完成按鈕、統計摘要端點），並以 AI 輔助完成，誠實記錄使用過程。評分大部分由 AI agent 自動驗收。

**繳交方式摘要**：不需要 zip 檔，在教學平台提交兩個網址——GitHub repository URL 與 Render 部署 URL。

---

## 14. 常見問題排查

### Q1：git push 時要輸入帳號密碼

GitHub 從 2021 年起**不再接受密碼推送**。你需要用：

- **Personal Access Token（PAT）** 代替密碼
- **SSH key**（更方便，一次設定永遠不用再輸入）

講義附錄提供 SSH key 的設定步驟。

### Q2：Render 一直停留在 "Building..."

通常是 Maven 下載依賴較慢（第一次 build 約 5-8 分鐘）。如果超過 15 分鐘，看 log 找原因。

### Q3：我用 Windows，Linux 指令不能用

本課程的 `./mvnw` 在 Windows 是 `mvnw.cmd`。Render 是 Linux 環境，不受影響。

### Q4：可以把期末專案 fork 到其他 GitHub 帳號再繳交嗎？

不可以。作業繳交的 repo 必須是你自己的，而且有完整的 commit history 顯示整學期的開發軌跡。

### Q5：畢業後 Render 免費方案還能用嗎？

可以，只要你按時「喚醒」它（訪問或 ping 一次），就能一直用。資料庫 90 天後要注意備份。

---

## 延伸閱讀（非必讀）

### 15. Git 進階操作

本課程只教了最基本的操作。業界常用的還有：

- **branch（分支）**：同時開發多個功能
- **merge / rebase**：合併分支
- **pull request（PR）**：團隊協作的標準流程
- **tag**：標記版本（`v1.0.0`）
- **stash**：暫存未完成的改動

在寒假或明年更進階的課程會碰到。

### 16. CI/CD 快速概念

**CI（Continuous Integration，持續整合）**：每次 push 自動跑測試
**CD（Continuous Deployment，持續部署）**：測試通過自動部署

Render 的自動部署其實就是**簡易版 CD**。業界會用 GitHub Actions 加上測試、程式碼品質檢查、Docker build 等流程。

### 17. Docker 容器化概念補充

現代雲端部署的標準是 **Docker 容器**。把應用連同它的執行環境打包成一個「image」，可以在任何支援 Docker 的平台運行（AWS、GCP、Kubernetes）。

本課程的 Render 部署就是透過 Docker 進行——你在 §11 步驟 3 建立的 Dockerfile 就是實例。

**Multi-stage Build 概念**：

本課程的 Dockerfile 分兩個階段：

| 階段 | Base image | 目的 |
|---|---|---|
| Build（第一階段） | `maven:3.9-eclipse-temurin-17-alpine` | 內建 Maven，執行 `mvn clean package` 產生 JAR |
| Run（第二階段） | `eclipse-temurin:17-jre-alpine` | 只有 JRE，複製 JAR 進來執行 |

這樣做的好處是：最終部署的 image 只含 JRE（約 80MB），不帶 JDK 和 Maven（合計約 500MB），部署更快、攻擊面更小。

**為什麼業界這樣做？**

傳統做法是「在本機 build 好 JAR，把 JAR 和 JRE 一起打包」。Multi-stage build 的好處是整個 CI/CD 流程不依賴開發者的本機環境——把程式碼推上 GitHub，雲端自己從頭 build，保證可重現性。

### 18. 監控與日誌

正式環境的應用需要**監控**：

- **Log 聚合**：ELK stack、Datadog
- **錯誤追蹤**：Sentry
- **效能監控**：New Relic、Prometheus + Grafana
- **使用者行為**：Google Analytics、Mixpanel

超出本課程範圍，但你會在業界碰到。

### 19. 自訂網域

Render 支援綁自訂網域（例如 `miniclinic.chihuah.tw`），只要：

1. 你擁有這個網域（Gandi、Namecheap 都可買）
2. 設定 DNS CNAME 指向 Render
3. 在 Render 設定頁加入自訂網域

這是讓你的作品看起來更「正式」的方法。

---

## 課程總回顧

### 你在 Ch09 完成了什麼？

**技術面**：

- Spring Boot 專案架構
- REST API 設計（GET、POST、PUT、DELETE）
- Thymeleaf 動態網頁
- JPA + 資料庫操作
- Session 登入、Interceptor 保護
- 表單驗證（Bean Validation）
- 密碼雜湊（BCrypt）
- 雲端部署（Git + GitHub + Render）

**非技術面**：

- 軟體工程實務：版本控制、README、commit 習慣
- 安全思維：SQL Injection、XSS、CSRF 防範
- 醫療資料倫理：個資法、資料最小化
- AI 協作能力：誠實揭露、批判性審查
- 文件撰寫：README、API 文件

### 你現在能做什麼？

- 獨立開發一個**中小型 Web 應用**
- 把作品**部署到雲端**，給別人試用
- 有一個能放進履歷的**作品集**
- 理解**業界工作流程**（Git、雲端、安全）

這些技能放到業界**初階工程師職缺**已綽綽有餘。

### 下一步

如果想繼續精進，推薦方向：

- **前端框架**：React / Vue，強化 UI 能力
- **Spring Security**：正式的認證 / 授權框架
- **Docker + Kubernetes**：雲端原生架構
- **單元測試**：JUnit 5、Mockito
- **資料庫進階**：SQL 優化、索引、交易

**最重要的**：持續為你的 GitHub 加新專案。每一個 commit 都是你成長的證明。

---

*本講義由教師自行編寫，所有程式碼範例均為原創，供課堂教學使用。*

*Ch09 系列到此告一段落。感謝同學整個學期的投入。*
