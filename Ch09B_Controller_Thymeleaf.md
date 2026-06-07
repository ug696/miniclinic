# Ch09-B｜Controller、Thymeleaf 模板與 REST API 設計

> **Controllers, Thymeleaf Templates, and REST API Design**
>
> 對應週次：第二週｜建議講解時間：135 分鐘

---

## 本週學習目標

完成本週課程後，你應該能夠：

1. 區分 `@Controller` 與 `@RestController` 的差別與使用時機
2. 使用 Thymeleaf 語法在 HTML 模板中嵌入動態資料
3. 透過 `@PathVariable` 與 `@RequestParam` 接收 URL 的變數
4. 設計並實作一組符合 RESTful 慣例的 API 端點
5. 使用 REST Client 擴充測試 API 回應
6. 完成 MiniClinic 的醫師清單頁面與 `/api/doctors` 系列 API

---

## 1. 上週回顧與本週規劃

### 上週做了什麼

你在 Ch09-A 完成了：

- 使用 Spring Initializr 建立 `miniclinic` 專案
- 寫了第一個端點 `GET /api/health`
- 看到瀏覽器回傳 JSON：`{"status":"ok","service":"miniclinic"}`

但現階段 MiniClinic 只有一個健康檢查端點，**還沒有任何真正的功能**。

### 本週要做什麼

本週結束時，MiniClinic 會變成：

- 有一個 **首頁**，顯示診所簡介與導覽列
- 有一個 **醫師清單頁面**（`/doctors`），顯示五位虛擬醫師資訊
- 可以 **依科別篩選**（`/doctors?department=內科`）
- 有 **一組對應的 REST API**（`/api/doctors`、`/api/doctors/{id}`、`/api/departments`）
- 有一個 **掛號表單頁面**（`/appointment/new`），練習 POST 表單處理

注意：**本週的資料都是硬編碼（寫死在程式中）**，還沒有資料庫。下週（Ch09-C）才會把資料真正存入資料庫。

### 重要思維：先有資料，再談功能

很多同學一開始寫程式會急著寫 Controller，但 Controller 是「處理請求」的層，它背後需要有資料才有意義。所以本週我們會**先設計好資料類別**（`Doctor`、`Department`），再寫 Controller。

---

## 2. Spring MVC 架構

### MVC 是什麼？

MVC 是一種將程式碼分層的設計模式，三個字母分別代表：

- **M (Model)**：資料與商業邏輯
- **V (View)**：畫面呈現
- **C (Controller)**：處理請求、協調 M 與 V

在 Spring Boot 中，這三者的對應：

```
使用者瀏覽器
     │
     ▼
[Controller]  ← 接收 HTTP 請求，決定要回什麼
     │
     ├─ 取資料 ──→ [Model]  (我們下週會接資料庫)
     │
     └─ 渲染畫面 → [View]   (Thymeleaf 模板)
            │
            ▼
        回傳 HTML / JSON
```

### 舉個具體例子

使用者訪問 `/doctors`：

1. **Controller** 接收請求
2. **Controller** 準備資料（五位醫師的 `List<Doctor>`）
3. **Controller** 把資料塞進 `Model`
4. **Controller** 指定用 `doctors.html` 這個 View 來渲染
5. **Thymeleaf** 把 Model 裡的資料填入 HTML 模板
6. 完整的 HTML 回傳給瀏覽器

---

## 3. `@Controller` vs `@RestController`

這是本週最關鍵的觀念之一，一定要搞清楚。

### 兩者的核心差別

| 比較項 | `@Controller` | `@RestController` |
|---|---|---|
| 回傳內容 | HTML 頁面（透過模板渲染） | JSON / XML 資料 |
| 回傳值處理 | 方法回傳的字串 = View 的名字 | 方法回傳的物件 = 自動序列化為 JSON |
| 使用者 | 給人類透過瀏覽器看的 | 給程式（JavaScript、手機 App、其他服務）呼叫 |
| 對應範例 | `/doctors` 頁面 | `/api/doctors` JSON |

### 為什麼 MiniClinic 兩種都用？

我們採用「**同一筆資料、兩種呈現方式**」的設計：

- **網頁介面** (`@Controller`)：給病患、醫師在瀏覽器看的
- **API 介面** (`@RestController`)：給未來可能的手機 App、其他系統、或自動化測試用

這也是**現代網站的常見設計**。例如 Facebook、YouTube 同時有網頁版和手機 App，背後的 API 是共用的。

在 MiniClinic 的 Ch09-E，我們會用這些 API 讓 AI Agent 自動評分你的作業——**沒有 API，就沒辦法自動驗證功能**。

### 實際程式碼對比

**@Controller 範例：**

```java
@Controller
public class DoctorPageController {

    @GetMapping("/doctors")
    public String listDoctors(Model model) {
        List<Doctor> doctors = getDoctorData();  // 取得資料
        model.addAttribute("doctors", doctors);  // 放進 Model
        return "doctors";  // 對應 templates/doctors.html
    }
}
```

**@RestController 範例：**

```java
@RestController
public class DoctorApiController {

    @GetMapping("/api/doctors")
    public List<Doctor> getDoctors() {
        return getDoctorData();  // 自動轉成 JSON
    }
}
```

看出差別了嗎？

- `@Controller` 的方法回傳字串 `"doctors"`，這是**模板名稱**
- `@RestController` 的方法回傳 `List<Doctor>`，這就是**要序列化成 JSON 的資料**

---

## 4. 建立資料類別

在寫 Controller 前，先建立資料類別。這叫做 **POJO**（Plain Old Java Object），就是一個純粹用來裝資料的類別。

### 建立 `Doctor` 類別

在 `src/main/java/tw/edu/fju/miniclinic/` 下建立一個新 package `model`，然後建立 `Doctor.java`：

```java
package tw.edu.fju.miniclinic.model;

public class Doctor {
    private String doctorId;      // 醫師編號
    private String name;          // 姓名
    private String department;    // 科別
    private String specialty;     // 專長

    public Doctor(String doctorId, String name, String department, String specialty) {
        this.doctorId = doctorId;
        this.name = name;
        this.department = department;
        this.specialty = specialty;
    }

    // Getters（Spring 會透過這些方法讀取欄位）
    public String getDoctorId() { return doctorId; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public String getSpecialty() { return specialty; }

    // Setters（之後會用到）
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public void setName(String name) { this.name = name; }
    public void setDepartment(String department) { this.department = department; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
}
```

**關於 Getter/Setter 的重要提醒**：

Spring（以及 Thymeleaf、JSON 序列化）都依賴這些方法來讀寫物件的欄位。即使你覺得很多餘，**一定要有 getter**。否則 Thymeleaf 渲染時會找不到資料，JSON 輸出也會是空物件 `{}`。

> **小技巧**：VS Code 的 Java 擴充可以自動產生 getter / setter。在欄位上按 `Ctrl+.`（或 `Cmd+.`），選擇 "Generate Getters and Setters"。

### 建立資料提供者（暫時代替資料庫）

我們本週還不接資料庫，所以建立一個暫時的資料提供者。在 `model` package 下建立 `DoctorRepository.java`：

```java
package tw.edu.fju.miniclinic.model;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class DoctorRepository {

    // 虛構的醫師資料
    private static final List<Doctor> DOCTORS = Arrays.asList(
        new Doctor("D001", "陳志明醫師", "家醫科", "一般內科、慢性病管理"),
        new Doctor("D002", "林佩君醫師", "內科",   "心臟血管、高血壓"),
        new Doctor("D003", "王建華醫師", "復健科", "運動傷害、脊椎復健"),
        new Doctor("D004", "李美玲醫師", "小兒科", "兒童感冒、疫苗接種"),
        new Doctor("D005", "張雅筑醫師", "身心科", "焦慮、失眠、情緒調適")
    );

    public List<Doctor> findAll() {
        return DOCTORS;
    }

    public Optional<Doctor> findById(String doctorId) {
        return DOCTORS.stream()
            .filter(d -> d.getDoctorId().equals(doctorId))
            .findFirst();
    }

    public List<Doctor> findByDepartment(String department) {
        return DOCTORS.stream()
            .filter(d -> d.getDepartment().equals(department))
            .toList();
    }

    public List<String> findAllDepartments() {
        return DOCTORS.stream()
            .map(Doctor::getDepartment)
            .distinct()
            .toList();
    }
}
```

**幾個重要觀念**：

- `@Component`：告訴 Spring「這個類別要交給你管理」，之後 Controller 才能直接拿到它
- `Optional<T>`：避免回傳 `null`，用更安全的包裝。在 Java 中，Optional<T> 是一個容器物件，用來代表一個值「可能存在」也「可能不存在（為 null）」。
- `stream().filter()...`：Java 8 引入的 Stream API，之後會在 Ch11 講解

---

## 5. 寫你的第一個網頁 Controller

### 安裝 Thymeleaf 依賴

如果在上一週剛開始用 Spring Initializr 建立專案時，沒有先在 ADD DEPENDENCIES 步驟裡加入 Thymeleaf 。那麼這週就需要先在 pom.xml 裡新增 Thymeleaf 依賴。

開啟在根目錄底下的 pom.xml，在 `<dependencies> ... </dependencies>` 標籤裡面，加入一組 `<dependency> ... </dependency>` 內含 thymeleaf 依賴，具體語法如下：

```java
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

### 建立 HomeController

建立新 package `controller`（如果還沒有的話），然後建立 `HomeController.java`：

```java
package tw.edu.fju.miniclinic.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home";  // 對應 templates/home.html
    }
}
```

### 建立第一個 Thymeleaf 模板

在 `src/main/resources/templates/` 下建立 `home.html`：

```html
<!DOCTYPE html>
<html lang="zh-Hant" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>MiniClinic 社區診所</title>
    <style>
        body { font-family: "Microsoft JhengHei", sans-serif; max-width: 960px; margin: 40px auto; padding: 0 20px; }
        h1 { color: #0F2443; border-bottom: 3px solid #14B8A6; padding-bottom: 8px; }
        nav { background: #0F2443; padding: 12px; border-radius: 8px; }
        nav a { color: white; margin-right: 20px; text-decoration: none; font-weight: bold; }
        nav a:hover { color: #5EEAD4; }
    </style>
</head>
<body>
    <h1>MiniClinic 社區診所</h1>
    <nav>
        <a th:href="@{/}">首頁</a>
        <a th:href="@{/doctors}">醫師清單</a>
        <a th:href="@{/appointment/new}">線上掛號</a>
    </nav>
    <section>
        <p>歡迎蒞臨 MiniClinic。我們提供下列科別的門診服務：</p>
        <ul>
            <li>家醫科</li>
            <li>內科</li>
            <li>復健科</li>
            <li>小兒科</li>
            <li>身心科</li>
        </ul>
    </section>
</body>
</html>
```

### 測試

啟動應用程式，打開瀏覽器訪問 <http://localhost:8080/>，應該看到一個有導覽列的首頁。

### 為什麼 HTML 要寫 `xmlns:th="http://www.thymeleaf.org"`？

這告訴瀏覽器和 IDE：「我這個 HTML 有使用 Thymeleaf 的特殊屬性（`th:xxx`）」。少了這行，`th:href`、`th:text` 等語法就無法運作，頁面會變成純靜態 HTML。

### 補充說明
nav bar 目前只有 3 個連結（首頁 / 醫師清單 / 線上掛號），未來有新的功能加入時，可以擴充。例如：第四個連結 `<a href="#">[預留]</a>`，在 Week 4 登入功能完成後會接上。

---

## 6. Thymeleaf 核心語法

Thymeleaf 是一套「**漸進式增強的模板引擎**」——它的特色是**模板檔案本身就是合法的 HTML**，可以直接在瀏覽器打開來看，也可以由伺服器動態渲染。這和傳統的 JSP（滿篇 `<% %>` 語法）大不相同。

以下是本課程會用到的四個核心語法。

### 6.1 `th:text` — 替換文字內容

```html
<h1 th:text="${title}">預設標題</h1>
```

說明：
- `${title}` 是從 Model 取變數 `title` 的值
- 伺服器渲染後會用 Model 的值**替換**掉原本的「預設標題」
- 如果直接打開檔案瀏覽（未經伺服器），會看到「預設標題」
- 如果用伺服器渲染且 Model 的 `title = "MiniClinic"`，會看到「MiniClinic」

### 6.2 `th:each` — 迴圈

```html
<ul>
    <li th:each="doctor : ${doctors}">
        <span th:text="${doctor.name}">醫師姓名</span>
        -
        <span th:text="${doctor.department}">科別</span>
    </li>
</ul>
```

說明：
- `doctor : ${doctors}` 類似 Java 的 `for (Doctor doctor : doctors)`
- 對 `doctors` 清單中的每一個 `doctor`，產生一個 `<li>`
- `${doctor.name}` 實際上是呼叫 `doctor.getName()`

### 6.3 `th:if` / `th:unless` — 條件判斷

```html
<p th:if="${doctors.size() > 0}">共 <span th:text="${doctors.size()}"></span> 位醫師</p>
<p th:unless="${doctors.size() > 0}">目前沒有醫師資料</p>
```

說明：
- `th:if` 條件成立才顯示
- `th:unless` 條件**不**成立才顯示
- 兩者互斥，可搭配使用

### 6.4 `th:href` / `@{...}` — URL 處理

```html
<a th:href="@{/doctors/{id}(id=${doctor.doctorId})}">查看詳細</a>
```

說明：
- `@{...}` 是 Thymeleaf 的 URL 語法
- 會自動處理 context path、參數等
- 上例渲染成：`<a href="/doctors/D001">查看詳細</a>`

**常見的 URL 寫法**：

| Thymeleaf 寫法 | 渲染結果 |
|---|---|
| `@{/doctors}` | `/doctors` |
| `@{/doctors/{id}(id=${doctor.doctorId})}` | `/doctors/D001` |
| `@{/doctors(department=${selectedDept})}` | `/doctors?department=內科` |

---

## 7. 醫師清單頁面

現在把所學串起來，做出醫師清單頁面。

### 7.1 Controller

在 `HomeController.java` 新增一個方法（或建立 `DoctorPageController.java`）：

```java
package tw.edu.fju.miniclinic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import tw.edu.fju.miniclinic.model.Doctor;
import tw.edu.fju.miniclinic.model.DoctorRepository;

import java.util.List;

@Controller
public class DoctorPageController {

    @Autowired
    private DoctorRepository doctorRepo;

    @GetMapping("/doctors")
    public String listDoctors(
            @RequestParam(required = false) String department,
            Model model) {

        List<Doctor> doctors;
        if (department == null || department.isBlank()) {
            doctors = doctorRepo.findAll();
        } else {
            doctors = doctorRepo.findByDepartment(department);
        }

        model.addAttribute("doctors", doctors);
        model.addAttribute("departments", doctorRepo.findAllDepartments());
        model.addAttribute("selectedDept", department);

        return "doctors";
    }
}
```

### 7.2 兩個重要註解

**`@Autowired`**：

- 告訴 Spring：「請把 `DoctorRepository` 的實例注入這個欄位」
- 這叫做 **依賴注入（Dependency Injection, DI）**，是 Spring 的核心功能
- 因為 `DoctorRepository` 有 `@Component`，Spring 知道怎麼產生實例

**`@RequestParam`**：

- 從 URL 查詢字串取值（即 `?department=內科` 中的 `department`）
- `required = false` 表示這個參數可有可無
- 沒帶參數時，`department` 會是 `null`

### 7.3 View 模板

在 `templates/` 下建立 `doctors.html`：

```html
<!DOCTYPE html>
<html lang="zh-Hant" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>醫師清單 - MiniClinic</title>
    <style>
        body { font-family: "Microsoft JhengHei", sans-serif; max-width: 960px; margin: 40px auto; padding: 0 20px; }
        h1 { color: #0F2443; border-bottom: 3px solid #14B8A6; padding-bottom: 8px; }
        .filter { margin: 20px 0; }
        .filter a { display: inline-block; padding: 6px 14px; margin: 4px; background: #E2E8F0; color: #0F2443; text-decoration: none; border-radius: 4px; }
        .filter a.active { background: #14B8A6; color: white; }
        table { width: 100%; border-collapse: collapse; margin-top: 16px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #E2E8F0; }
        th { background: #0F2443; color: white; }
        tr:hover { background: #F8FAFC; }
    </style>
</head>
<body>
    <h1>醫師清單</h1>

    <div class="filter">
        <strong>科別篩選：</strong>
        <a th:href="@{/doctors}"
           th:classappend="${selectedDept == null} ? 'active'">全部</a>
        <a th:each="dept : ${departments}"
           th:href="@{/doctors(department=${dept})}"
           th:text="${dept}"
           th:classappend="${dept == selectedDept} ? 'active'">科別</a>
    </div>

    <p th:if="${doctors.isEmpty()}">目前沒有符合條件的醫師。</p>

    <table th:unless="${doctors.isEmpty()}">
        <thead>
            <tr>
                <th>編號</th>
                <th>姓名</th>
                <th>科別</th>
                <th>專長</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="doctor : ${doctors}">
                <td th:text="${doctor.doctorId}">D001</td>
                <td th:text="${doctor.name}">陳志明醫師</td>
                <td th:text="${doctor.department}">家醫科</td>
                <td th:text="${doctor.specialty}">一般內科</td>
            </tr>
        </tbody>
    </table>
</body>
</html>
```

### 7.4 新增導覽列（可選，但建議）

`th:classappend` 是一個新語法：**附加** CSS class（而不是取代）。

- 當 `selectedDept == null` 時，「全部」這個連結會加上 `active` class
- 當 `dept == selectedDept` 時，對應的科別連結會加上 `active` class

### 7.5 測試

啟動後訪問：

- <http://localhost:8080/doctors> — 看到全部五位醫師
- <http://localhost:8080/doctors?department=內科> — 只看到內科醫師
- <http://localhost:8080/doctors?department=不存在> — 顯示「沒有符合條件的醫師」

點選上方的科別連結，應該可以平順切換篩選條件。

---

## 8. 單一醫師頁面：`@PathVariable`

**查詢參數** 適合做篩選（`?department=內科`）；**路徑變數** 適合代表「**這是哪一筆資源**」（`/doctors/D001`）。

### 什麼是路徑變數？

```
/doctors/D001   ← D001 是路徑變數
         ^^^^
```

對應的 Controller 方法：

```java
@GetMapping("/doctors/{doctorId}")
public String doctorDetail(@PathVariable String doctorId, Model model) {
    Optional<Doctor> doctor = doctorRepo.findById(doctorId);

    if (doctor.isEmpty()) {
        return "redirect:/doctors";  // 找不到就回清單頁
    }

    model.addAttribute("doctor", doctor.get());
    return "doctor-detail";
}
```

**重點**：

- URL 中的 `{doctorId}` 是**佔位符**
- `@PathVariable` 把它的實際值（例如 `D001`）注入方法參數
- 參數名稱要和 `{...}` 中的名稱一致（或用 `@PathVariable("doctorId")` 明確指定）
- 補充說明：找不到就回清單頁這種做法比較不完善，更好的做法是做一個專屬的「找不到醫師」頁面來顯示（建立 `doctor-not-found.html` 模板），使用者體驗會更好，且方便未來顯示客製化錯誤訊息

### 對應的模板 `doctor-detail.html`

```html
<!DOCTYPE html>
<html lang="zh-Hant" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="${doctor.name} + ' - MiniClinic'">醫師詳細</title>
    <style>
        body { font-family: "Microsoft JhengHei", sans-serif; max-width: 720px; margin: 40px auto; padding: 0 20px; }
        h1 { color: #0F2443; }
        .card { background: #F8FAFC; padding: 24px; border-left: 5px solid #14B8A6; border-radius: 4px; }
        dt { font-weight: bold; color: #0F2443; margin-top: 12px; }
        dd { margin-left: 0; margin-bottom: 8px; }
        .back { display: inline-block; margin-top: 20px; color: #14B8A6; }
    </style>
</head>
<body>
    <h1 th:text="${doctor.name}">醫師姓名</h1>
    <div class="card">
        <dl>
            <dt>醫師編號</dt><dd th:text="${doctor.doctorId}">D001</dd>
            <dt>科別</dt>    <dd th:text="${doctor.department}">家醫科</dd>
            <dt>專長</dt>    <dd th:text="${doctor.specialty}">一般內科</dd>
        </dl>
    </div>
    <a th:href="@{/doctors}" class="back">← 返回清單</a>
</body>
</html>
```

### 讓清單頁連結到詳細頁

修改 `doctors.html` 的表格，讓姓名可點：

```html
<td>
    <a th:href="@{/doctors/{id}(id=${doctor.doctorId})}"
       th:text="${doctor.name}">醫師姓名</a>
</td>
```

### 測試

點醫師姓名應該能進入詳細頁，看到該醫師的完整資訊，並能返回清單。

---

## 9. REST API 設計基本慣例

前面都在做「給人看的網頁」（`@Controller`），現在來做「給程式看的 API」（`@RestController`）。

### RESTful 命名慣例

REST 設計有一些約定俗成的慣例：

**1. 使用名詞複數代表資源集合**

| ✓ 好 | ✗ 不建議 |
|---|---|
| `/api/doctors` | `/api/getDoctorList` |
| `/api/appointments` | `/api/queryAppointments` |

**2. HTTP 動詞對應 CRUD，而不是把動作塞進 URL**

| 操作 | ✓ RESTful | ✗ 不建議 |
|---|---|---|
| 列出所有醫師 | `GET /api/doctors` | `GET /api/listDoctors` |
| 取得單一醫師 | `GET /api/doctors/D001` | `GET /api/getDoctor?id=D001` |
| 新增醫師 | `POST /api/doctors` | `POST /api/createDoctor` |
| 更新醫師 | `PUT /api/doctors/D001` | `POST /api/updateDoctor` |
| 刪除醫師 | `DELETE /api/doctors/D001` | `GET /api/deleteDoctor?id=D001` |

**3. 路徑變數代表「哪一筆資源」，查詢參數代表「篩選條件」**

```
/api/doctors/D001               ← D001 是特定醫師（路徑變數）
/api/doctors?department=內科    ← department 是篩選條件（查詢參數）
```

**4. 有階層關係用巢狀路徑**

```
/api/doctors/D001/appointments  ← D001 醫師的所有掛號（Ch09-D 會用到）
```

### REST 不是宗教

這些是**慣例**不是**法律**。實務上會有各種變形。但如果你的 API 能遵守這些慣例，**其他開發者看一眼就知道怎麼用**，這就是 RESTful 設計的價值。

---

## 10. 實作 REST API

### 10.1 `/api/doctors` 與篩選

建立 `DoctorApiController.java`：

```java
package tw.edu.fju.miniclinic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tw.edu.fju.miniclinic.model.Doctor;
import tw.edu.fju.miniclinic.model.DoctorRepository;

import java.util.List;
import java.util.Optional;

@RestController
public class DoctorApiController {

    @Autowired
    private DoctorRepository doctorRepo;

    @GetMapping("/api/doctors")
    public List<Doctor> getDoctors(
            @RequestParam(required = false) String department) {
        if (department == null || department.isBlank()) {
            return doctorRepo.findAll();
        }
        return doctorRepo.findByDepartment(department);
    }

    @GetMapping("/api/doctors/{doctorId}")
    public ResponseEntity<Doctor> getDoctor(@PathVariable String doctorId) {
        Optional<Doctor> doctor = doctorRepo.findById(doctorId);
        return doctor
            .map(d -> ResponseEntity.ok(d))       // 有 → 200 OK + 資料
            .orElse(ResponseEntity.notFound().build());  // 沒有 → 404
    }

    @GetMapping("/api/departments")
    public List<String> getDepartments() {
        return doctorRepo.findAllDepartments();
    }
}
```

### 10.2 認識 `ResponseEntity`

`ResponseEntity<T>` 讓你可以同時控制 **HTTP Status Code** 和 **Response Body**。

```java
// 只回資料，狀態碼自動是 200
return doctor;

// 手動控制
return ResponseEntity.ok(doctor);                // 200 + body
return ResponseEntity.notFound().build();         // 404 無 body
return ResponseEntity.badRequest().body(errors);  // 400 + body
return ResponseEntity.status(201).body(doctor);   // 201 + body
```

**什麼時候用 `ResponseEntity`？**

- 需要根據邏輯回傳**不同狀態碼**時（例如找不到 → 404）
- 一般的成功查詢，直接回物件就夠（Spring 自動 200 OK）

### 10.3 測試 API

在瀏覽器測試 GET：

- <http://localhost:8080/api/doctors> — 回傳 JSON 陣列，5 筆
- <http://localhost:8080/api/doctors?department=內科> — 回傳 1 筆
- <http://localhost:8080/api/doctors/D001> — 回傳單筆物件
- <http://localhost:8080/api/doctors/D999> — 回傳 404
- <http://localhost:8080/api/departments> — 回傳字串陣列

---

## 11. 使用 REST Client 擴充測試

瀏覽器只能測 GET。更完整的 API 測試（POST、PUT、DELETE）需要 REST Client 擴充（VS Code裡安裝 Extension）。

### 建立測試檔案

在專案根目錄建立 `api-tests.http`：

```
### API端點健康檢查
GET http://localhost:8080/api/health
Accept: application/json

###

### about 端點檢查 (上週作業檢核點)
GET http://localhost:8080/api/about
Accept: application/json

###

### 所有醫師
GET http://localhost:8080/api/doctors
Accept: application/json

###

### 依科別篩選（內科）
GET http://localhost:8080/api/doctors?department=內科
Accept: application/json

###

### 依科別篩選（家醫科）
GET http://localhost:8080/api/doctors?department=家醫科
Accept: application/json

###

### 單一醫師（D001）
GET http://localhost:8080/api/doctors/D001
Accept: application/json

###

### 單一醫師（D005）
GET http://localhost:8080/api/doctors/D005
Accept: application/json

###

### 不存在的醫師（應回 404）
GET http://localhost:8080/api/doctors/D999
Accept: application/json

###

### 所有科別
GET http://localhost:8080/api/departments
Accept: application/json
```

### 使用方式

1. VS Code 裡安裝 Extension： `REST Client`
2. VS Code 打開 `api-tests.http`
3. 每個 `###` 區塊上方會顯示「Send Request」
4. 點選即可執行，右側會開啟結果視窗
5. 結果視窗會顯示：
   - Status Code（例如 200 OK）
   - Response Headers
   - Response Body（格式化後的 JSON）
   - 執行時間

### 為什麼不用 Postman？

Postman 是很好的工具，但 REST Client 的優勢是：

- 測試請求**跟專案程式碼一起存在 Git**（Postman 集合要另外 export/匯入）
- 純文字格式，可以**直接 diff** 看改了什麼
- 不用離開 VS Code

對課堂學習與作業繳交來說，REST Client 更方便。

---

## 12. POST 表單處理

現在做最後一個功能：掛號表單。這涉及到 POST 請求和表單資料綁定。

### 12.1 建立掛號資料類別

在 `model` package 下建立 `AppointmentForm.java`：

```java
package tw.edu.fju.miniclinic.model;

public class AppointmentForm {
    private String chartNo;       // 病歷號
    private String doctorId;      // 掛號的醫師
    private String apptDate;      // 日期
    private String timeSlot;      // 時段

    // 建構子（無參，表單綁定需要）
    public AppointmentForm() {}

    // Getters & Setters
    public String getChartNo() { return chartNo; }
    public void setChartNo(String chartNo) { this.chartNo = chartNo; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getApptDate() { return apptDate; }
    public void setApptDate(String apptDate) { this.apptDate = apptDate; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
}
```

**注意**：本週不真正存入資料庫，只是練習表單處理流程。

### 12.2 Controller

建立 `AppointmentController.java`：

```java
package tw.edu.fju.miniclinic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import tw.edu.fju.miniclinic.model.AppointmentForm;
import tw.edu.fju.miniclinic.model.DoctorRepository;

@Controller
public class AppointmentController {

    @Autowired
    private DoctorRepository doctorRepo;

    // GET：顯示表單
    @GetMapping("/appointment/new")
    public String newAppointmentForm(Model model) {
        model.addAttribute("form", new AppointmentForm());
        model.addAttribute("doctors", doctorRepo.findAll());
        return "appointment-new";
    }

    // POST：接收表單
    @PostMapping("/appointment/new")
    public String submitAppointment(
            @ModelAttribute AppointmentForm form,
            Model model) {

        // 本週還沒資料庫，只把收到的資料放進 Model，秀給使用者看
        model.addAttribute("form", form);
        model.addAttribute("doctor",
            doctorRepo.findById(form.getDoctorId()).orElse(null));

        return "appointment-result";
    }
}
```

### 12.3 關鍵觀念：為什麼 GET 和 POST 能同一個路徑？

```
GET  /appointment/new  → 顯示表單
POST /appointment/new  → 提交表單
```

這是很常見的模式：

- **使用者打開頁面** → 瀏覽器發 GET → 看到空白表單
- **使用者填完按送出** → 瀏覽器發 POST（表單的預設送出方式就是 POST）→ 看到結果

兩個不同的 Method 即使路徑相同，對應不同的 Controller 方法。

### 12.4 `@ModelAttribute` 做什麼？

`@ModelAttribute AppointmentForm form`：

- 告訴 Spring：「把表單的所有欄位，自動塞進一個 `AppointmentForm` 物件」
- Spring 會根據 `name` 屬性，呼叫對應的 setter
  - 表單 `<input name="chartNo">` → 呼叫 `form.setChartNo(...)`
- 如果欄位型別不符會試著轉換

這就是**表單資料綁定（Form Binding）**，Spring MVC 最方便的功能之一。

### 12.5 表單模板 `appointment-new.html`

```html
<!DOCTYPE html>
<html lang="zh-Hant" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>線上掛號 - MiniClinic</title>
    <style>
        body { font-family: "Microsoft JhengHei", sans-serif; max-width: 560px; margin: 40px auto; padding: 0 20px; }
        h1 { color: #0F2443; border-bottom: 3px solid #14B8A6; padding-bottom: 8px; }
        .field { margin: 16px 0; }
        label { display: block; font-weight: bold; color: #0F2443; margin-bottom: 6px; }
        input, select { width: 100%; padding: 8px; border: 1px solid #E2E8F0; border-radius: 4px; font-size: 14px; }
        button { background: #14B8A6; color: white; padding: 10px 24px; border: none; border-radius: 4px; font-size: 15px; cursor: pointer; }
        button:hover { background: #0D9488; }
    </style>
</head>
<body>
    <h1>線上掛號</h1>

    <form th:action="@{/appointment/new}" th:object="${form}" method="post">

        <div class="field">
            <label>病歷號</label>
            <input type="text" th:field="*{chartNo}" placeholder="TEST00001">
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
        </div>

        <div class="field">
            <label>日期</label>
            <input type="date" th:field="*{apptDate}">
        </div>

        <div class="field">
            <label>時段</label>
            <select th:field="*{timeSlot}">
                <option value="">-- 請選擇 --</option>
                <option value="AM">上午門診</option>
                <option value="PM">下午門診</option>
                <option value="EVENING">夜間門診</option>
            </select>
        </div>

        <button type="submit">送出掛號</button>
    </form>
</body>
</html>
```

### 12.6 結果模板 `appointment-result.html`

```html
<!DOCTYPE html>
<html lang="zh-Hant" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>掛號完成 - MiniClinic</title>
    <style>
        body { font-family: "Microsoft JhengHei", sans-serif; max-width: 560px; margin: 40px auto; padding: 0 20px; }
        h1 { color: #0F2443; }
        .success { background: #D1FAE5; color: #065F46; padding: 16px; border-radius: 4px; margin: 20px 0; }
        .summary { background: #F8FAFC; padding: 20px; border-left: 5px solid #14B8A6; }
        dt { font-weight: bold; color: #0F2443; margin-top: 8px; }
    </style>
</head>
<body>
    <h1>掛號提交完成</h1>

    <div class="success">
        您的掛號已收到（注意：本週尚未寫入資料庫，下週我們才會真的儲存）
    </div>

    <div class="summary">
        <h2>掛號摘要</h2>
        <dl>
            <dt>病歷號</dt>   <dd th:text="${form.chartNo}">TEST00001</dd>
            <dt>醫師</dt>    <dd th:text="${doctor != null ? doctor.name : '(未選擇)'}"></dd>
            <dt>科別</dt>    <dd th:text="${doctor != null ? doctor.department : '-'}"></dd>
            <dt>日期</dt>    <dd th:text="${form.apptDate}">2026-05-01</dd>
            <dt>時段</dt>    <dd th:text="${form.timeSlot}">AM</dd>
        </dl>
    </div>

    <p><a th:href="@{/}">← 回首頁</a></p>
</body>
</html>
```

### 12.7 `th:object` 和 `th:field` 的搭配

```html
<form th:object="${form}" method="post">
    <input th:field="*{chartNo}">
</form>
```

這對搭配非常強大：

- `th:object="${form}"`：告訴 Thymeleaf「整個表單對應到 Model 的 `form` 物件」
- `th:field="*{chartNo}"`：自動產生 `id`、`name`、`value` 三個屬性

上面會渲染成：

```html
<input id="chartNo" name="chartNo" value="">
```

提交表單時，`name="chartNo"` 會對應到 `AppointmentForm.setChartNo(...)`，完成自動綁定。

---

## 13. 作業二：完成醫師頁面與 REST API

### 作業說明

本週延續 Ch09-A 的專案。請在現有的 `miniclinic` 專案**繼續開發**，不要建立新專案。作業要求的完整細節，請見 TronClass 上作業「社區診所平台專案製作 檢核點2」的說明。

### Part A：基本要求（必做）

- [ ] 建立 `Doctor` 類別，含 `doctorId`、`name`、`department`、`specialty` 四個欄位與對應的 getter/setter
- [ ] 建立 `DoctorRepository` 類別，含至少 5 位虛構醫師資料（可直接使用講義提供的版本）
- [ ] 實作首頁 `GET /` 與 `home.html`（含導覽列）
- [ ] 實作醫師清單頁 `GET /doctors`，可依科別篩選（`?department=...`）
- [ ] 實作單一醫師詳細頁 `GET /doctors/{doctorId}`
- [ ] 實作 REST API：
  - `GET /api/doctors`（可篩選）
  - `GET /api/doctors/{doctorId}`（找不到回 404）
  - `GET /api/departments`
- [ ] 在專案根目錄建立 `api-tests.http`，至少包含對上述 API 的測試

### Part B：進階要求（必做）

- [ ] 實作線上掛號表單頁 `GET /appointment/new`
- [ ] 實作表單處理 `POST /appointment/new`，顯示掛號摘要頁
- [ ] **自行加碼**新增 `GET /patients` 頁面：顯示三筆虛構病患資料
  - 至少欄位：`chartNo`、`name`、`gender`、`phone`
  - 需要建立 `Patient` 類別與 `PatientRepository`
  - 提供對應的 `GET /api/patients` API
  - 設計 patients.html：參考 doctors.html，以表格顯示病患資訊，並在首頁的導覽列裡加入一個病歷資料連結。

### 繳交方式

- 將整個 `miniclinic` 專案資料夾壓縮為 zip 檔
- 檔名：`學號_姓名_Ch09B.zip`
- 上傳至教學平台
- 記得先刪除 `target/` 資料夾

### 評分重點

1. 網頁與 API 都能正常運作
2. 程式碼結構符合 MVC 分層（controller、model 各自獨立）
3. 至少 5 位醫師、涵蓋課堂上指定的五個科別
4. Thymeleaf 語法使用正確
5. REST API 命名符合慣例

---

## 14. 常見問題排查

### Q1：訪問 `/doctors` 出現 Whitelabel Error Page

**可能原因**：

1. `HomeController` 或 `DoctorPageController` 沒有 `@Controller` 註解
2. 方法的 `@GetMapping` 路徑拼錯
3. `templates/doctors.html` 檔案不存在或檔名拼錯
4. Controller 類別不在 `tw.edu.fju.miniclinic` 或其子 package 下（Spring 找不到）

**排查步驟**：

1. 查看終端機啟動訊息，確認 port 與錯誤訊息
2. 確認檔案路徑：`src/main/resources/templates/doctors.html`
3. 重新啟動伺服器

### Q2：頁面顯示但資料是空的

**可能原因**：

1. `DoctorRepository` 沒有 `@Component` 註解
2. `Doctor` 類別沒有 getter
3. Thymeleaf 變數名稱拼錯（例如 `${doctor.name}` 寫成 `${doctor.Name}`）

**排查步驟**：

1. Model 裡的變數名要和 Thymeleaf 的 `${...}` 一致
2. `${doctor.name}` 會呼叫 `getName()`，首字母是否正確？

### Q3：POST 表單提交後資料是 null

**可能原因**：

1. `AppointmentForm` 沒有無參建構子（Spring 需要呼叫 `new AppointmentForm()`）
2. `AppointmentForm` 沒有對應的 setter
3. HTML 的 `name` 屬性拼錯（例如 `chart_no` 寫成 `chartno`）

**排查步驟**：

1. 檢查 `AppointmentForm` 有 `public AppointmentForm() {}` 空建構子
2. 檢查所有 setter 方法存在
3. `th:field="*{chartNo}"` 中的名稱要和 setter 一致

### Q4：瀏覽器直接看 JSON 很亂

**解決方法**：

- Chrome / Edge 安裝「JSON Formatter」擴充
- 或用 REST Client 測試，結果會自動格式化

---

## 延伸閱讀（非必讀）

### 15. Thymeleaf 更多語法

本課程只教四個核心語法，Thymeleaf 還有很多進階功能：

- **Fragments**：像函式一樣的可重用模板片段（`th:insert`、`th:replace`）
- **Layout Dialect**：定義全站共用的 layout 範本
- **條件表達式**：`th:text="${age > 18 ? '成年' : '未成年'}"`
- **工具物件**：`#numbers`、`#dates`、`#strings` 等

如果你要做更複雜的頁面，官方文件值得一讀：<https://www.thymeleaf.org/documentation.html>

### 16. 依賴注入的進階寫法

本課程用 `@Autowired` 欄位注入，簡單但不是最佳實踐。業界推薦的是**建構子注入**：

```java
@Controller
public class DoctorPageController {
    private final DoctorRepository doctorRepo;

    // 建構子注入（Spring 4.3 之後可省略 @Autowired）
    public DoctorPageController(DoctorRepository doctorRepo) {
        this.doctorRepo = doctorRepo;
    }
    // ...
}
```

優點：
- 欄位可以是 `final`（不可變）
- 測試時更容易替換
- Spring 官方推薦

### 17. DTO 設計

本課程直接把 `Doctor` entity 回傳給前端，但在正式專案中，通常會多一層 **DTO（Data Transfer Object）**：

```java
// Entity（對應資料庫）
public class Doctor {
    private String passwordHash;  // 不該給前端看
    // ...
}

// DTO（給前端看的資料）
public class DoctorDTO {
    private String doctorId;
    private String name;
    // 沒有敏感欄位
}
```

Ch09-D 講安全時會再提到為什麼這很重要。

### 18. 為什麼不用前端框架？

你可能會好奇：現代網站不是都用 React / Vue / Angular 嗎？

確實是的。但本課程用 **Thymeleaf（伺服器端渲染）** 有幾個理由：

1. **學習門檻低**：不用學一整套 JavaScript 生態系
2. **AI Agent 好評分**：伺服器端渲染的 HTML 可以被 `web_fetch` 讀到，React 單頁應用讀到的是空殼
3. **全 Java 技術棧**：專注在後端思維
4. **保留 API 入口**：我們同時做 REST API，未來想改前端框架，API 都已經在了

這也是很多台灣傳統企業 / 公部門系統仍在用的做法。

---

## 下週預告：Ch09-C 將學到

- SQL 基礎（`SELECT`、`INSERT`、`UPDATE`、`DELETE`、`WHERE`）
- SQLite 特性與 Spring Boot 整合
- JPA 核心註解：`@Entity`、`@Id`、`@ManyToOne`
- `JpaRepository` 的開箱功能
- 讓 MiniClinic 的資料**真的存到檔案**、重啟後還在
- 建立三張表：`patient`、`doctor`、`appointment`
- SQL Injection 概念（為 Ch09-D 鋪陳）

下週開始，MiniClinic 就是一個有真正資料庫的系統了。

---

*本講義由教師自行編寫，所有程式碼範例均為原創，供課堂教學使用。*
