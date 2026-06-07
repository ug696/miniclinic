# Ch09-A｜Web 開發總覽與 Spring Boot 入門

> **Web Development Overview and Spring Boot Fundamentals**
>
> 對應週次：第一週｜建議講解時間：135 分鐘

---

## 本週學習目標

完成本週課程後，你應該能夠：

1. 說明 HTTP 請求與回應的基本運作模型
2. 在 VS Code 中安裝好 Java 開發環境與 Gemini Code Assist
3. 使用 Spring Initializr 建立一個全新的 Spring Boot 專案
4. 理解 Spring Boot 專案的基本目錄結構
5. 撰寫並執行你的第一個 REST API 端點
6. 完成 GitHub 與 Render 帳號註冊，為後續部署做準備

---

## 1. 全章導覽 — 我們要做出什麼？

這五週，我們要一起從零打造一套 **MiniClinic 社區診所門診掛號系統**。

### 系統情境

想像一家小型社區診所，提供五個科別的門診服務：

- 家醫科
- 內科
- 復健科
- 小兒科
- 身心科

系統要能做的事：

- 顯示各科別醫師的資訊
- 病患線上預約掛號
- 醫師登入後查看自己當日的掛號名單
- 醫師能將掛號狀態標記為「已完成」或「取消」
- 管理員能查看掛號統計

### 五週的演進路徑

| 週次 | 主題 | 本週結束時的成品狀態 |
|---|---|---|
| **第一週（Ch09-A）** | Spring Boot 入門 | 專案建好、能跑、有一個回傳 JSON 的端點 |
| **第二週（Ch09-B）** | Controller + Thymeleaf | 有醫師清單頁面、能依科別篩選 |
| **第三週（Ch09-C）** | SQLite + JPA | 資料從資料庫讀取、完整 CRUD |
| **第四週（Ch09-D）** | Session + 登入 + 安全 | 醫師能登入、看自己的 dashboard |
| **第五週（Ch09-E）** | Git + GitHub + Render | 系統部署到雲端，全世界都連得到 |

### 重要：資料倫理聲明

本課程所有的病患資料、醫師資料、掛號資料**皆為虛構教學用資料**：

- 病歷號使用 `TEST00001`、`TEST00002` 這種一眼可辨識的教學格式
- 病患姓名使用「測試病患甲」、「王小明」等明顯虛構的名稱
- 醫師姓名、科別、專長皆為虛構，不對應任何真實診所

在真實的醫療系統開發中，病歷資料受到《個人資料保護法》保護，處理、儲存、傳輸都有嚴格規範。**本課程不會使用任何真實的病歷、病患、醫師資料**。我們會在第四週（Ch09-D）詳細討論醫療資料保護的相關議題。

---

## 2. Web 是怎麼運作的？

在我們開始寫程式之前，先花一點時間理解「當你在瀏覽器輸入一個網址、按下 Enter 後，發生了什麼事？」

### HTTP 的 Request / Response 模型

整個 Web 的運作建立在一個非常簡單的模型上：

```
┌──────────────┐                            ┌──────────────┐
│              │ ─── HTTP Request ─────▶   │              │
│   瀏覽器     │                            │  網頁伺服器   │
│  (Client)    │                            │   (Server)   │
│              │ ◀──── HTTP Response ───── │              │
└──────────────┘                            └──────────────┘
```

1. **瀏覽器發出請求（Request）**：告訴伺服器「我要看 `/doctors` 這個頁面」
2. **伺服器處理並回應（Response）**：把 HTML 內容（或其他資料）傳回給瀏覽器
3. **瀏覽器渲染**：把收到的 HTML 畫成你看到的網頁

這個模型很重要的特性是：**每次請求都是獨立的**，伺服器不會記得你上次做了什麼（除非我們用 Session/Cookie 另外處理，這是第四週的主題）。

### HTTP Request 長什麼樣子？

一個完整的 HTTP Request 大致長這樣：

```
GET /api/doctors HTTP/1.1
Host: miniclinic.example.com
Accept: application/json
User-Agent: Mozilla/5.0 ...

(空行)
(如果是 POST，這裡會放 body 資料)
```

關鍵元素：

- **方法（Method）**：`GET`、`POST`、`PUT`、`DELETE` 等
- **路徑（Path）**：`/api/doctors`
- **標頭（Headers）**：傳遞附加資訊
- **主體（Body）**：POST / PUT 時用來傳資料

### 常用的 HTTP Method

| 方法 | 用途 | 語意 | 是否會改變伺服器狀態 |
|---|---|---|---|
| `GET` | 取得資料 | 「給我看」 | 否（只讀） |
| `POST` | 新增資料 | 「幫我建立」 | 是 |
| `PUT` | 更新資料（整筆取代） | 「幫我改」 | 是 |
| `DELETE` | 刪除資料 | 「幫我刪」 | 是 |

這四個動詞對應到資料操作的 CRUD（Create、Read、Update、Delete），後續章節會大量使用。

### HTTP 狀態碼（Status Code）

伺服器回應時會附上一個三位數的狀態碼，告訴瀏覽器「這次請求的結果如何」：

| 範圍 | 意義 | 常見例子 |
|---|---|---|
| `2xx` | 成功 | `200 OK`、`201 Created` |
| `3xx` | 重導向 | `301 Moved Permanently`、`302 Found` |
| `4xx` | 用戶端錯誤 | `400 Bad Request`、`401 Unauthorized`、`404 Not Found` |
| `5xx` | 伺服器錯誤 | `500 Internal Server Error` |

在 MiniClinic 中，你會常看到：

- `200 OK`：成功取得資料
- `201 Created`：成功新增一筆資料
- `400 Bad Request`：使用者輸入格式錯誤
- `401 Unauthorized`：尚未登入卻想存取需要權限的資源
- `404 Not Found`：要找的資料不存在
- `500 Internal Server Error`：程式噴了例外

### 前端與後端的分工

在一個網頁應用中，工作分成兩邊：

**前端（Frontend）——瀏覽器做的事**：

- 渲染 HTML、CSS 顯示畫面
- 執行 JavaScript 處理互動
- 發送 HTTP Request 給後端

**後端（Backend）——伺服器做的事**：

- 接收 HTTP Request
- 處理商業邏輯
- 存取資料庫
- 產生 Response（HTML 或 JSON）

**本課程的重點是後端開發**：用 Java + Spring Boot 寫出一個能接收 Request、處理邏輯、回傳 Response 的伺服器。

---

## 3. 為什麼選 Spring Boot？

### 傳統 Java Web 開發的痛點

在 Spring Boot 出現之前，用 Java 開發 Web 應用要做很多繁瑣的事：

1. 自己下載、安裝 Tomcat 伺服器
2. 設定一堆 XML 組態檔（`web.xml`、`applicationContext.xml` 等）
3. 把程式打包成 `.war` 檔，手動部署到 Tomcat
4. 修改程式後要重新打包、重新部署才看得到結果
5. 不同元件（資料庫、安全、驗證）都要自己整合

這些繁瑣步驟會吃掉大量時間，而且出錯機率很高。

### Spring Boot 的核心價值

**Spring Boot 的口號：Convention over Configuration（慣例優於組態）**

它做了三件關鍵的事，讓 Java Web 開發大幅簡化：

1. **內嵌伺服器**：Tomcat 內建在應用程式中，你不用另外安裝、不用另外部署
2. **自動配置**：只要加入依賴（例如加入 `spring-boot-starter-web`），就自動幫你配好所有基本設定
3. **Starter 依賴組合包**：一次加一組相關的依賴，不用自己拼湊版本

結果是什麼？**一行指令就能啟動一個 Web 伺服器**：

```bash
java -jar miniclinic.jar
```

### Spring Boot 在業界的地位

在台灣和全球的後端開發市場，Spring Boot 是 Java 生態系中**最主流的 Web 框架**，大多數企業的 Java 後端職缺都要求熟悉 Spring Boot。學會 Spring Boot，意味著你具備了實際進入產業的基本能力。

### Spring Boot 與 Tomcat / Servlet 的關係（給好奇的你）

你可能聽過 Tomcat、Servlet、JSP 這些名詞。它們和 Spring Boot 是什麼關係？

簡單說：**Spring Boot 底層其實還是用 Tomcat 跑 Servlet**，只是幫你全部包好了：

```
你寫的 Controller
    ↓
Spring MVC 框架
    ↓
Servlet API
    ↓
內嵌 Tomcat 伺服器
```

所以學 Spring Boot 不是「跳過」Servlet，而是**站在 Servlet 的基礎上，用更高層的抽象來寫程式**。未來如果要看底層 Servlet 的程式碼，你也看得懂基本概念。

---

## 4. 開發環境準備

這一節我會在課堂上帶大家一起做。請跟著操作，有問題當場舉手問。

### 4.1 安裝 JDK 17

本課程使用 **Java 17**（LTS 長期支援版本）。如果你還沒安裝，推薦以下其中一個發行版：

- **Eclipse Temurin**（OpenJDK）：<https://adoptium.net/> Download link: https://adoptium.net/zh-CN/temurin/releases?version=17&os=any&arch=any


安裝完成後，打開命令列確認：

```bash
java -version
```

應該看到類似：

```
openjdk version "17.0.x" ...
```

### 4.2 安裝 VS Code 與必備擴充

如果還沒安裝 VS Code，從官網下載：<https://code.visualstudio.com/>

接著到擴充商店（Ctrl+Shift+X）安裝以下擴充：

| 擴充名稱 | 用途 | 必要性 |
|---|---|---|
| Extension Pack for Java | Java 開發核心套件（Microsoft 出品） | 必裝 |
| Spring Boot Extension Pack | Spring Boot 專用支援 | 必裝 |
| SQLite Viewer | 查看 SQLite 資料庫內容 | 第三週會用 |
| REST Client | 測試 REST API | 第二週會用 |
| GitLens | Git 輔助工具 | 第五週會用 |

### 4.3 啟用 Gemini Code Assist

本課程鼓勵大家使用 AI 助手輔助開發。我們使用 Google 的 Gemini Code Assist（目前對學生免費）。

**安裝與啟用步驟**：

1. 到 VS Code 擴充商店搜尋「Gemini Code Assist」並安裝
2. 安裝後，左側邊欄會出現 Gemini 圖示
3. 點開後登入你的 Google 帳號（建議使用你的學校信箱）
4. 同意使用條款後，即可開始使用

### AI 助手使用政策（重要）

本課程對 AI 助手的使用規範如下：

- **允許使用** Gemini Code Assist（或其他如 GitHub Copilot、ChatGPT、Claude）輔助開發
- **看得懂才用**：不要貼 AI 給你的程式碼而完全不理解（見本章最後的「如何有效使用 AI」）
- **批判性採納**：AI 可能給出錯誤、過時或不符合本課程風格的建議
- **誠實揭露**：第五週（Ch09-E）部署到 GitHub 時，會要求以規範格式在 Git commit message 中記錄 AI 使用歷程

**前四週繳交 zip 檔**，不使用 Git，也不要求 commit 記錄。但強烈建議你養成習慣，**用自己的方式記下跟 AI 的每次對話**（記事本、Markdown 筆記都可以），第五週整理期末專案時會非常有幫助。

簡要預告：Ch09-E 的 commit 規範會要求你寫清楚以下三件事：

- 你問 AI 什麼
- AI 建議什麼
- 你為什麼採納（或為什麼修改）

這三欄資訊就是本課程最核心的「AI 使用透明度」要求。

### 4.4 註冊必要帳號（課堂上完成）

請在這堂課當場完成以下註冊，第五週部署時才不會卡關：

1. **GitHub 帳號**：<https://github.com/signup>
   - 建議使用學校信箱（未來申請教育方案會用到）
   - 帳號名稱建議簡潔、專業（面試時可能會看到）
2. **Render 帳號**：<https://render.com/>
   - 點選「Sign up with GitHub」可直接用 GitHub 帳號登入
   - 暫時不需要做任何設定，第五週才會用到

---

## 5. 建立第一個 Spring Boot 專案

終於要開始寫程式了！

### 5.1 使用 Spring Initializr

Spring Initializr 是官方的專案產生器。有兩種方式使用：

**方法一：網頁版**（推薦第一次使用）

1. 開啟 <https://start.spring.io/>
2. 按照以下設定填寫：

   | 欄位 | 值 |
   |---|---|
   | Project | Maven |
   | Language | Java |
   | Spring Boot | 3.x.x（最新穩定版） |
   | Group | `tw.edu.fju` |
   | Artifact | `miniclinic` |
   | Name | `miniclinic` |
   | Description | `MiniClinic - 社區診所掛號系統` |
   | Package name | `tw.edu.fju.miniclinic` |
   | Packaging | Jar |
   | Java | 17 |

3. 點選「ADD DEPENDENCIES」加入以下依賴：
   - Spring Web
   - Thymeleaf
   - Spring Boot DevTools

備註: Thymeleaf 是 HTML 模板引擎（第二週才會用到）

4. 按「GENERATE」下載壓縮檔，解壓縮到你常用的專案資料夾

**方法二：VS Code 內建**（熟練後推薦）

1. 按 `Ctrl+Shift+P` 開啟命令面板
2. 輸入「Spring Initializr: Create a Maven Project」
3. 依照提示填入專案資訊（同上表）

### 5.2 用 VS Code 開啟專案

1. VS Code 選單：File → Open Folder
2. 選擇剛才解壓縮的 `miniclinic` 資料夾
3. 首次開啟時，VS Code 右下角會跳出提示訊息，點選「Import Maven Projects」
4. 等待右下角的進度條跑完（Java 伸展套件會自動下載依賴）

### 5.3 專案目錄結構

解壓縮後你會看到這樣的結構：

```
miniclinic/
├── .mvn/                              # Maven Wrapper（不用管）
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── tw/edu/fju/miniclinic/
│   │   │       └── MiniclinicApplication.java   ← 程式進入點
│   │   └── resources/
│   │       ├── static/                # 靜態資源（CSS、JS、圖片）
│   │       ├── templates/             # Thymeleaf 模板（第二週會用）
│   │       └── application.properties # 設定檔
│   └── test/                          # 單元測試（本課程不會用到）
├── .gitignore                         # Git 忽略清單（已預先設定好）
├── mvnw, mvnw.cmd                     # Maven Wrapper 執行檔
├── pom.xml                            # Maven 專案描述檔
└── README.md                          # 專案說明文件
```

重點資料夾說明：

- **`src/main/java/`**：你寫的 Java 程式碼放這裡
- **`src/main/resources/application.properties`**：專案設定檔
- **`pom.xml`**：列出所有依賴函式庫

### 5.4 認識 pom.xml

打開 `pom.xml` 看一下，找到 `<dependencies>` 區塊，你會看到類似：

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
</dependencies>
```

每個 `<dependency>` 代表一個引入的函式庫。`spring-boot-starter-*` 系列是 Spring Boot 提供的「組合包」，一加入就自動帶進一堆相關依賴。

---

## 6. 認識 Application 進入點

打開 `src/main/java/tw/edu/fju/miniclinic/MiniclinicApplication.java`：

```java
package tw.edu.fju.miniclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MiniclinicApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniclinicApplication.class, args);
    }
}
```

這短短幾行做了很多事，逐一理解：

### `@SpringBootApplication` 註解

這是一個「複合註解」，等同於同時加上：

- `@Configuration`：告訴 Spring 這是一個配置類別
- `@EnableAutoConfiguration`：啟動 Spring Boot 的自動配置機制
- `@ComponentScan`：掃描同 package 下所有的 Spring 元件

實務上你**只需要記得「每個 Spring Boot 專案要有一個被 `@SpringBootApplication` 標註的類別」**，其他細節日後再深入。

### `main()` 方法

```java
public static void main(String[] args) {
    SpringApplication.run(MiniclinicApplication.class, args);
}
```

這就是一般 Java 程式的進入點。`SpringApplication.run()` 會：

1. 啟動 Spring 容器
2. 掃描所有 `@Component`、`@Controller`、`@Service` 等元件
3. 啟動內嵌的 Tomcat 伺服器（預設 port 8080）
4. 你的程式就開始對外服務了

### 第一次執行程式

在 VS Code 中有兩種方式執行：

**方法一：點選 `main()` 上方的 Run 按鈕**

VS Code 會自動偵測 `main()` 方法並在上方顯示「Run | Debug」小按鈕，點選「Run」。

**方法二：按 F5**

使用 VS Code 的 debugger 執行，可以設中斷點。

**應該看到的輸出**：

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.x.x)

... (一堆啟動訊息)

Tomcat started on port 8080 (http) with context path ''
Started MiniclinicApplication in 2.5 seconds
```

看到 **「Tomcat started on port 8080」** 和 **「Started MiniclinicApplication」** 就代表啟動成功了。

### 打開瀏覽器測試

在瀏覽器輸入：<http://localhost:8080>

你應該會看到一個「Whitelabel Error Page」的錯誤頁面。**別擔心，這是正常的！**

原因是我們還沒寫任何 Controller，伺服器不知道要回什麼。Spring Boot 看到 `/` 路徑找不到對應的 handler，就顯示這個預設的錯誤頁。

這其實是個好消息——代表伺服器正在運作，只是沒內容。接下來我們就來加入第一個端點。

---

## 7. 寫你的第一個 REST API

### 7.1 什麼是 REST API？

先給一個最精簡的定義：

> **REST API 是一組遵循 HTTP 慣例的端點，用來讓程式與程式之間交換資料（通常是 JSON 格式）。**

和「給人看的網頁」的差別：

| 比較項 | 給人看的網頁 | REST API |
|---|---|---|
| 回傳內容 | HTML | JSON（通常） |
| 使用者 | 人類透過瀏覽器 | 另一個程式 |
| 範例 | `/doctors` 顯示醫師清單頁 | `/api/doctors` 回傳醫師資料 JSON |

在 MiniClinic 中，我們會同時提供「網頁」和「API」兩種介面：

- 網頁：給人掛號用
- API：給未來可能的手機 App、或其他系統使用（也方便 AI Agent 自動評分）

### 7.2 建立第一個 Controller

在 `src/main/java/tw/edu/fju/miniclinic/` 下建立一個新的 package `controller`，然後在裡面建立 `HealthController.java`：

```java
package tw.edu.fju.miniclinic.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "ok",
            "service", "miniclinic"
        );
    }
}
```

### 7.3 這段程式做了什麼？

**逐行拆解**：

- `@RestController`：告訴 Spring 這個類別是用來處理 HTTP 請求的，而且**回傳值會自動轉成 JSON**
- `@GetMapping("/api/health")`：當使用者用 GET 方法訪問 `/api/health` 時，執行下面這個方法
- `Map<String, String>`：回傳一個 Map，Spring 會自動把它轉成 JSON

### 7.4 重新啟動與測試

**重新啟動伺服器**：

如果你剛才的程式還在跑，先停止它（紅色方塊按鈕），再重新執行。

> 💡 **提示**：因為我們加了 `spring-boot-devtools` 依賴，其實大部分時候修改程式後**不需要手動重啟**。儲存檔案後 DevTools 會自動熱重載。但新增 Controller 的第一次，建議手動重啟確認。

**瀏覽器測試**：

開啟 <http://localhost:8080/api/health>

應該看到：

```json
{"status":"ok","service":"miniclinic"}
```

🎉 恭喜！你完成了第一個 REST API。

### 7.5 用 REST Client 擴充測試（第二週實作）

瀏覽器網址列只能測試 GET 請求。未來 POST、PUT、DELETE 會需要更強大的工具。我們可以使用 VS Code 的 REST Client 擴充來測試。具體在第二週時，會介紹到如何建立和利 `api-tests.http` 檔案。

---

## 8. application.properties 基本設定

打開 `src/main/resources/application.properties`，這個檔案初始是空的。我們加入一些基本設定：

```properties
# 應用程式名稱
spring.application.name=miniclinic

# 伺服器 port（預設 8080，如果 8080 被佔用可以改）
server.port=8080

# 日誌等級（INFO 是預設，DEBUG 會顯示更多細節）
logging.level.tw.edu.fju.miniclinic=DEBUG
```

修改後 DevTools 會自動重啟伺服器。

### 如果 port 8080 被佔用

有時候 8080 port 已經被其他程式佔用（例如你電腦裝了 Tomcat），啟動時會報錯：

```
Port 8080 was already in use
```

解決方法：把 `server.port=8080` 改成其他數字（例如 `8081`、`9000`），並記得以後連線時 URL 也要改。

---

## 9. 作業一：建立專案與第一個 API

### 作業說明

本次作業是整個 Ch09 五週專案的**起點**。請務必妥善保管你的專案資料夾，後續四週都會在這個基礎上繼續開發。

### Part A：基本要求（必做）

- [ ] 完成 JDK 17、VS Code、所有指定擴充的安裝
- [ ] 使用 Spring Initializr 建立名為 `miniclinic` 的專案
- [ ] 專案能正常啟動（看到「Started MiniclinicApplication」訊息）
- [ ] 實作 `GET /api/health` 端點，回傳 JSON `{"status":"ok","service":"miniclinic"}`
- [ ] 在瀏覽器或 REST Client 中成功測試 `/api/health`

### Part B：進階要求（必做）

- [ ] 另外實作 `GET /api/about` 端點，回傳包含以下欄位的 JSON：
  - `student_id`：你的學號
  - `student_name`：你的姓名
  - `project`：`"MiniClinic"`
  - `version`：`"0.1.0"`
  - `chapter`：`"Ch09-A"`

### 繳交方式

第一週的繳交**不需要 GitHub**（第五週才會教），直接將整個專案資料夾壓縮為 zip 檔：

- 檔名：`學號_姓名_Ch09A.zip`
- 上傳至教學平台
- **必須先刪除 `target/` 資料夾**（這是編譯產物，會讓檔案很大）

### 評分重點

1. 兩個 API 端點都能正常回應正確格式
2. 專案結構符合 Spring Boot 標準
3. 程式碼整潔，沒有多餘的檔案

---

## 10. 常見問題排查

### Q1：VS Code 開啟專案後一直出現錯誤

**症狀**：紅色波浪線、找不到符號。

**解決**：

1. 確認右下角 Java 版本顯示 17
2. 按 `Ctrl+Shift+P` → 輸入「Java: Clean Java Language Server Workspace」→ 選擇「Restart and delete」
3. 等待重新載入（可能需要 1–2 分鐘）

### Q2：執行時找不到 `main` 方法

**症狀**：右上角沒有「Run | Debug」按鈕。

**解決**：確認你開啟的是**專案根目錄**（有 `pom.xml` 的那層），不是上一層或下一層。

### Q3：瀏覽器訪問 `/api/health` 得到 404

**症狀**：回應「Whitelabel Error Page: 404」。

**解決**：

1. 檢查 `@GetMapping` 的路徑拼字是否正確
2. 檢查 `HealthController` 是否放在 `tw.edu.fju.miniclinic` 或其子 package 下（Spring 只會掃描主類別所在 package 的後代）
3. 檢查是否有 `@RestController` 註解
4. 重新啟動伺服器

### Q4：Gemini Code Assist 建議的程式碼我看不懂

**解決**：這就是 AI 使用紀錄作業的意義所在。請：

1. 先不要採用你看不懂的程式碼
2. 問 AI：「請逐行解釋這段程式碼」
3. 理解後再決定是否採用
4. 在 commit message 中誠實記錄這段對話

**看不懂就不要貼，是基本原則**。

---

## 延伸閱讀（非必讀）

以下內容不在課堂上講解，但建議有餘力的同學閱讀：

### 11. HTTP/2 與 HTTPS

現代網站已經普遍使用 HTTP/2（效能更好、支援多工）和 HTTPS（加密傳輸）。Spring Boot 可以透過設定啟用兩者，但本課程為了簡化，都用 HTTP/1.1 的明文通訊。正式部署後 Render 會自動幫我們加上 HTTPS。

### 12. Maven 依賴機制的運作原理

`pom.xml` 中宣告的依賴，Maven 會從中央倉庫（Maven Central）下載 `.jar` 檔案到本地快取（通常在 `~/.m2/repository/`）。Spring Boot Starter 的機制讓一個 Starter 相依於一整組函式庫，版本也由 Spring Boot 父 POM 統一管理，避免版本衝突。

### 13. 為什麼 Spring Boot 選擇內嵌 Tomcat？

傳統上 Java Web 應用需要部署到一個外部的 Tomcat / GlassFish / WildFly 伺服器。Spring Boot 顛倒這個關係——把伺服器內嵌到應用裡。好處包括：

- 每個應用有自己獨立的伺服器設定，不互相干擾
- 部署只需要一個 `java -jar` 指令
- 適合 microservice 架構與容器化部署（Docker）
- 開發與正式環境的伺服器版本一致

### 14. 為什麼不直接寫 Servlet？

你可能會想：「既然 Spring Boot 底層也是 Servlet，為什麼不直接寫 Servlet 就好？」

技術上可以，但 Spring Boot 提供的是**更高層的抽象**：

- `@RestController` 比 `HttpServlet` 簡潔太多
- 自動 JSON 序列化（不用手寫 `response.getWriter().write(...)`)
- 依賴注入（`@Autowired`）、資料庫抽象（Spring Data）等生態系
- 業界幾乎都用 Spring Boot，Servlet 直寫已很少見

---

## 下週預告：Ch09-B 將學到

- Spring MVC 架構
- `@Controller` vs `@RestController` 的差別
- Thymeleaf 模板語法
- 建立醫師清單頁面與 API
- 表單處理（GET 與 POST）
- REST 設計的基本慣例

下週開始，MiniClinic 就會有真正的頁面可以看了！

---

*本講義由教師自行編寫，所有程式碼範例均為原創，供課堂教學使用。*
