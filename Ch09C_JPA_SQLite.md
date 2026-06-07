# Ch09-C｜SQLite、Spring Data JPA 與資料持久化

> **SQLite, Spring Data JPA, and Data Persistence**
>
> 對應週次：第三週｜建議講解時間：135 分鐘
>
> 注意：本週內容最多、節奏較緊。建議上課前先瀏覽本講義的架構，掌握主軸。

---

## 本週學習目標

完成本週課程後，你應該能夠：

1. 說明為什麼需要資料庫（相較於 Excel 或檔案）
2. 撰寫基本的 SQL 查詢（`SELECT`、`INSERT`、`UPDATE`、`DELETE`、`WHERE`）
3. 說明 SQLite 的特性並在 Spring Boot 專案中設定連線
4. 使用 JPA 註解（`@Entity`、`@Id`、`@ManyToOne`）把 Java 類別對應到資料表
5. 利用 `JpaRepository` 實作 CRUD，不寫一行 SQL
6. 把 MiniClinic 的資料真正存進資料庫，重啟後還在

---

## 1. 上週回顧與本週的關鍵轉變

### 上週做了什麼

你在 Ch09-B 完成了：

- 首頁、醫師清單頁、單一醫師詳細頁
- `/api/doctors`、`/api/doctors/{id}`、`/api/departments` 系列 REST API
- 掛號表單的 GET/POST 處理

但有一個**重大缺陷**：所有資料都是**硬編碼在 `DoctorRepository` 裡**。

這意味著：

- 每次啟動，資料都一樣
- 透過 POST 新增的掛號，重啟就消失
- 無法真正「新增」、「修改」、「刪除」資料
- 這根本不是一個能運作的系統

### 本週的關鍵轉變

本週，我們要讓 MiniClinic 的資料**真正存起來**：

```
之前（Ch09-B）                      本週（Ch09-C）
硬編碼的 List<Doctor>    ──→       SQLite 資料庫中的 doctor 資料表
                                    所有資料真正儲存在檔案
                                    程式重啟後資料還在
                                    新增、修改、刪除都會真的生效
```

### 關鍵設計：Ch09-B 的鋪陳

還記得 Ch09-B 的 `DoctorRepository` 嗎？

```java
public List<Doctor> findAll() { ... }
public Optional<Doctor> findById(String doctorId) { ... }
public List<Doctor> findByDepartment(String department) { ... }
```

這些方法的命名**刻意和 JPA 的標準一致**。本週我們要做的事情是：**把這個手寫的類別換成 Spring Data JPA 自動產生的 Repository**，而 Controller 幾乎完全不用動。

這個設計的好處：你會親身體會「介面」與「實作」分離的威力——換資料來源不用改使用者。

---

## 2. 為什麼需要資料庫？

### 如果用 Excel 儲存呢？

你可能想：「醫師資料不多，為什麼不用 Excel？」

| 比較項 | Excel | 資料庫 |
|---|---|---|
| 儲存容量 | 約 100 萬列 | 數十億筆以上 |
| 多人同時修改 | 容易衝突、覆蓋 | 支援並發、鎖定機制 |
| 快速查詢 | 資料多時很慢 | 有索引，查詢極快 |
| 資料完整性 | 容易打錯、缺漏 | 可設定約束（不得為空、必須唯一等）|
| 跨表查詢 | 困難、需要公式 | JOIN 輕鬆完成 |
| 程式化存取 | 需要另外寫 | 程式直接操作 |

### 資料庫的幾個核心概念

- **資料表（Table）**：像 Excel 的工作表，儲存某種資料
- **欄位（Column）**：像 Excel 的直行，定義資料的屬性
- **記錄（Row / Record）**：像 Excel 的一列，代表一筆資料
- **主鍵（Primary Key, PK）**：每筆記錄的唯一識別碼（例如病歷號）
- **外鍵（Foreign Key, FK）**：指向另一張表的主鍵，建立表與表的關聯

### MiniClinic 的三張表

本週結束時，MiniClinic 會有三張實體資料表：

```
┌───────────────────┐       ┌──────────────────────┐       ┌───────────────────┐
│  patient          │       │  appointment         │       │  doctor           │
│  ───────────────  │       │  ──────────────────  │       │  ───────────────  │
│  chart_no (PK)    │◄──────┤  appt_id (PK)        ├──────►│  doctor_id (PK)   │
│  name             │  FK   │  chart_no (FK)       │  FK   │  name             │
│  gender           │       │  doctor_id (FK)      │       │  department       │
│  birth_date       │       │  appt_date           │       │  specialty        │
│  phone            │       │  time_slot           │       │  password_hash    │
│                   │       │  status              │       │  (Ch09-D 用)      │
└───────────────────┘       └──────────────────────┘       └───────────────────┘
```

- `appointment` 表用兩個外鍵，記錄「哪位病患預約了哪位醫師」
- 這種關係叫做「**多對一**」：一筆掛號對應一位病患、一位醫師

---

## 3. SQL 基礎快速複習

### SQL 是什麼

**SQL（Structured Query Language，結構化查詢語言）**是和資料庫溝通的標準語言。四個最常用的操作：

| 動作 | SQL 關鍵字 | 說明 |
|---|---|---|
| 查詢 | `SELECT` | 從資料表讀取資料 |
| 新增 | `INSERT` | 新增一筆資料 |
| 修改 | `UPDATE` | 修改既有資料 |
| 刪除 | `DELETE` | 刪除資料 |

### 3.1 CREATE TABLE：建立資料表

```sql
CREATE TABLE doctor (
    doctor_id   VARCHAR(10)  PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    department  VARCHAR(20)  NOT NULL,
    specialty   VARCHAR(100)
);
```

- `PRIMARY KEY`：主鍵約束（不可重複、不可為空）
- `NOT NULL`：此欄位不能是空值
- `VARCHAR(n)`：字串型別，最多 n 個字元

### 3.2 INSERT：新增資料

```sql
INSERT INTO doctor (doctor_id, name, department, specialty)
VALUES ('D001', '陳志明醫師', '家醫科', '一般內科、慢性病管理');
```

一次插入多筆：

```sql
INSERT INTO doctor (doctor_id, name, department, specialty) VALUES
('D001', '陳志明醫師', '家醫科', '一般內科、慢性病管理'),
('D002', '林佩君醫師', '內科', '心臟血管、高血壓');
```

### 3.3 SELECT：查詢

**查全部**：

```sql
SELECT * FROM doctor;
```

**只看特定欄位**：

```sql
SELECT name, department FROM doctor;
```

**加條件（WHERE）**：

```sql
SELECT * FROM doctor WHERE department = '內科';
SELECT * FROM doctor WHERE doctor_id = 'D001';
```

**排序（ORDER BY）**：

```sql
SELECT * FROM doctor ORDER BY name;
SELECT * FROM doctor ORDER BY doctor_id DESC;
```

**限制筆數（LIMIT）**：

```sql
SELECT * FROM doctor LIMIT 3;
```

### 3.4 UPDATE：修改資料

```sql
UPDATE doctor
SET specialty = '心臟血管、高血壓、糖尿病'
WHERE doctor_id = 'D002';
```

⚠️ **別忘了 `WHERE`**：如果省略，會**把整張表所有資料都改掉**。

### 3.5 DELETE：刪除資料

```sql
DELETE FROM doctor WHERE doctor_id = 'D005';
```

⚠️ **別忘了 `WHERE`**：如果省略，會**把整張表清空**。

### 3.6 JOIN：跨表查詢（點到為止）

以後寫 Dashboard 時可能會用到：

```sql
SELECT a.appt_date, p.name AS patient_name, d.name AS doctor_name
FROM appointment a
JOIN patient p ON a.chart_no = p.chart_no
JOIN doctor  d ON a.doctor_id = d.doctor_id
WHERE a.appt_date = '2026-05-01';
```

不過本課程會把 JOIN 交給 JPA 幫我們處理，你不用自己寫。這裡只是讓你知道「資料庫可以跨表查詢」。

### 本課程的 SQL 使用原則

你可能會鬆一口氣：**本課程不要求你手寫大量 SQL**。原因是：

1. JPA 會自動產生 SQL
2. 方法命名查詢（見後面）能涵蓋 90% 的情境
3. 你只需要**看得懂** SQL，debug 時能讀懂 log 即可

但 SQL 的基本概念（SELECT / INSERT / UPDATE / DELETE / WHERE / 主鍵 / 外鍵）**必須掌握**，這是後端工程師的基本功。

---

## 4. SQLite 簡介

### SQLite 的三大特色

**1. 單一檔案，零配置**

不需要安裝資料庫伺服器、不需要啟動服務、不需要帳號密碼。整個資料庫就是一個 `.db` 檔案。

```
miniclinic/
└── miniclinic.db        ← 所有資料都在這個檔案裡
```

**2. 嵌入式**

SQLite 不是獨立的程式，而是一個**函式庫**。你的 Spring Boot 應用直接讀寫這個檔案。

**3. 標準 SQL**

SQLite 支援絕大多數標準 SQL 語法。你學的 SQL 在 PostgreSQL、MySQL 上也能用（只有細節差異，Ch09-E 會說明）。

### 真實世界的 SQLite 應用

你可能以為 SQLite 是「小玩具」，其實它的應用範圍極廣：

- iPhone 與 Android：每個 App 的本地資料庫都是 SQLite
- Firefox、Chrome：瀏覽器的歷史紀錄、書籤、cookie 都是 SQLite
- WhatsApp、Line：聊天紀錄儲存
- 所有你用過的軟體裡，**大約一半背後都有 SQLite**

### 為什麼本課程用 SQLite？

- 零安裝成本：學生回家立刻能開發
- 資料檔能跟著程式碼走：`git clone` 後就能看到預設資料
- 與 Java 的 JDBC 相容：換成 PostgreSQL 不用改程式碼邏輯
- **最適合教學與開發環境**

Ch09-E 會帶到：部署到雲端時，把 SQLite 換成 PostgreSQL。

---

## 5. 在 Spring Boot 中設定 SQLite

### 5.1 加入依賴

打開 `pom.xml`，在 `<dependencies>` 區塊加入兩個依賴：

```xml
<!-- SQLite JDBC Driver -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.0.0</version>
</dependency>

<!-- Hibernate 對 SQLite 的方言支援 -->
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-community-dialects</artifactId>
</dependency>

<!-- Spring Data JPA（主角） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

儲存後 VS Code 會提示 Maven 重新載入，按下 Reload。

### 5.2 設定 application.properties

在 `src/main/resources/application.properties` 加入：

```properties
# 確保 data.sql 以 UTF-8 讀取（Windows 預設編碼為 CP950，會造成中文亂碼）
spring.sql.init.encoding=UTF-8

# SQLite 資料庫檔案位置（專案根目錄）
spring.datasource.url=jdbc:sqlite:miniclinic.db
spring.datasource.driver-class-name=org.sqlite.JDBC

# Hibernate 使用 SQLite 的方言
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect

# 啟動時自動建立/更新資料表結構
spring.jpa.hibernate.ddl-auto=update

# 顯示執行的 SQL（開發時很有用）
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### 5.3 重要設定說明

**`spring.jpa.hibernate.ddl-auto`** 的選項：

| 值 | 作用 | 適用情境 |
|---|---|---|
| `none` | 什麼都不做 | 正式環境 |
| `validate` | 檢查資料表是否符合 Entity | 正式環境 |
| `update` | 資料表不存在就建立，缺欄位就加上 | **開發環境（我們用這個）** |
| `create` | 每次啟動先刪掉，再重建 | 測試 |
| `create-drop` | 啟動時建立，關閉時刪除 | 測試 |

### 5.4 SQLite Viewer 擴充

VS Code 裝了「SQLite Viewer」擴充（Ch09-A 已經裝）後：

- 專案產生 `miniclinic.db` 後，點選該檔案會開啟圖形介面
- 可以看到所有資料表、欄位、資料內容
- 方便除錯時直接看資料狀態

---

## 6. JPA 核心觀念：從類別到資料表

### JPA 是什麼

**JPA（Java Persistence API，Java 持久化 API）**是一套 Java 的規範，讓你可以**用物件導向的方式操作資料庫**，不用自己寫 SQL。

### ORM 觀念：物件↔資料表

JPA 屬於 **ORM（Object-Relational Mapping，物件關聯對映）**技術。核心想法：

| Java 世界 | 資料庫世界 |
|---|---|
| 類別（Class） | 資料表（Table） |
| 欄位（Field） | 欄位（Column） |
| 物件（Object） | 一筆資料（Row） |
| 集合（List） | 查詢結果（ResultSet） |

JPA 負責在這兩個世界之間**自動翻譯**。你寫 Java，它自動產生 SQL。

### Hibernate 是什麼

**Hibernate** 是 JPA 規範**最流行的實作**。Spring Data JPA 預設使用 Hibernate。

你不用直接接觸 Hibernate，透過 Spring 的抽象層操作就好。

---

## 7. 建立 Doctor Entity

### 7.1 修改 Doctor 類別

打開 `Doctor.java`，加上 JPA 註解：

```java
package tw.edu.fju.miniclinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    // JPA 需要無參數的建構子
    public Doctor() {}

    // 原本的建構子保留，讓 data.sql 或手動建立物件時方便
    public Doctor(String doctorId, String name, String department, String specialty) {
        this.doctorId = doctorId;
        this.name = name;
        this.department = department;
        this.specialty = specialty;
    }

    // Getters & Setters（省略）
}
```

### 7.2 逐行拆解

**`@Entity`**
- 告訴 JPA：「這個類別對應到一張資料表」
- 沒有它，JPA 會忽略這個類別

**`@Table(name = "doctor")`**
- 指定對應的資料表名稱
- 不寫也可以，預設會用類別名小寫（`doctor`）

**`@Id`**
- 標記此欄位為**主鍵**
- 每個 Entity 必須有且只有一個主鍵

**`@Column(...)`**
- 指定欄位的詳細屬性
- `name`：資料表中的欄位名
- `length`：字串長度上限
- `nullable`：是否可為空
- 不寫 `@Column` 也可以，會用預設值

### 7.3 為什麼需要無參建構子？

```java
public Doctor() {}  // JPA 要求必須有
```

JPA 從資料庫撈出一筆資料時，會先用 `new Doctor()` 建立空物件，再用 setter 把欄位值填進去。所以**不管你需不需要，一定要提供一個無參建構子**。

---

## 8. 建立 Patient Entity（講義自學）

前一週的作業檢核點要同學依照 `Doctor` 的模式建立 `Patient.java`，現在需要修改成以下內容：

```java
package tw.edu.fju.miniclinic.model;

import jakarta.persistence.*;
import java.sql.Types;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "patient")
public class Patient {

    @Id
    @Column(name = "chart_no", length = 20)
    private String chartNo;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "gender", length = 10)
    private String gender;

    @JdbcTypeCode(Types.VARCHAR)
    @Convert(converter = LocalDateConverter.class)
    @Column(name = "birth_date", columnDefinition = "TEXT")
    private LocalDate birthDate;

    @Column(name = "phone", length = 20)
    private String phone;

    public Patient() {}

    public Patient(String chartNo, String name, String gender,
            LocalDate birthDate, String phone) {
        this.chartNo = chartNo;
        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;
        this.phone = phone;
    }

    public String getChartNo() { return chartNo; }
    public void setChartNo(String chartNo) { this.chartNo = chartNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}

```

注意：
- `LocalDate` 是 Java 8+ 的日期型別，對應 SQL 的 `DATE`
- `chartNo` 格式仍是 `TEST00001` 這種虛構教學格式
- `birthDate` 的三個額外標記是 SQLite 相容性補丁，說明見 **8.1 節**

---

## 8.1 SQLite 的日期型別補丁：LocalDateConverter

### 為什麼需要這個？

SQLite 沒有原生的 `DATE` 型別——它把日期存成純文字（`TEXT`）。Hibernate 6（Spring Boot 3.x 使用）在 SQLite 這個組合下，無法自動把 `LocalDate` 存進去；若不處理，啟動時會報錯：

```
No Dialect mapping for JDBC type: 91
```

解法是寫一個 `AttributeConverter`，手動告訴 Hibernate 怎麼轉換：

```
Java 端                資料庫端（SQLite TEXT 欄位）
LocalDate(2026-05-01)  →  "2026-05-01"
"2026-05-01"           →  LocalDate(2026-05-01)
```

### LocalDateConverter.java

在 `model/` 下新增 `LocalDateConverter.java`：

```java
package tw.edu.fju.miniclinic.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;

@Converter
public class LocalDateConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate date) {
        return date == null ? null : date.toString();  // LocalDate → "2026-05-01"
    }

    @Override
    public LocalDate convertToEntityAttribute(String s) {
        return s == null ? null : LocalDate.parse(s);  // "2026-05-01" → LocalDate
    }
}
```

- `AttributeConverter<X, Y>`：X 是 Java 端型別，Y 是資料庫端型別
- `convertToDatabaseColumn`：存入時呼叫（Java → DB）
- `convertToEntityAttribute`：讀出時呼叫（DB → Java）

### 搭配使用的三個標記

每個 `LocalDate` 欄位都要加上這三行：

```java
@JdbcTypeCode(Types.VARCHAR)                       // 告訴 Hibernate：這欄當 VARCHAR 處理
@Convert(converter = LocalDateConverter.class)     // 使用上面的 Converter 轉換
@Column(name = "birth_date", columnDefinition = "TEXT")  // 建表時明確指定 TEXT 型別
private LocalDate birthDate;
```

三個標記缺一可能出問題，這是 SQLite + Hibernate 6 的已知相容性問題。

### 換成 PostgreSQL 後

PostgreSQL 有原生的 `DATE` 型別，Hibernate 能自動處理。Ch09-E 遷移到 PostgreSQL 後，這三個標記和 `LocalDateConverter.java` **全部都可以刪掉**，欄位只需要寫：

```java
@Column(name = "birth_date")
private LocalDate birthDate;
```

這就是為什麼開發環境用 SQLite 有時會多一些「補丁」程式碼。

---

## 9. 建立 Appointment Entity 與 @ManyToOne 關聯

這是本週最核心的新觀念：**如何表達資料表之間的關聯**。

### 9.1 情境與設計

一筆掛號：

- 指向**一位病患**（透過 `chart_no` 外鍵）
- 指向**一位醫師**（透過 `doctor_id` 外鍵）
- 記錄日期、時段、狀態

### 9.2 Appointment Entity

新增一個 `Appointment.java`，加上 JPA 註解：

```java
package tw.edu.fju.miniclinic.model;

import jakarta.persistence.*;
import java.sql.Types;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appt_id")
    private Long apptId;

    // 多個 Appointment 對應一個 Patient
    @ManyToOne
    @JoinColumn(name = "chart_no", nullable = false)
    private Patient patient;

    // 多個 Appointment 對應一個 Doctor
    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @JdbcTypeCode(Types.VARCHAR)
    @Convert(converter = LocalDateConverter.class)
    @Column(name = "appt_date", nullable = false, columnDefinition = "TEXT")
    private LocalDate apptDate;

    @Column(name = "time_slot", length = 20, nullable = false)
    private String timeSlot;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "BOOKED";

    public Appointment() {}

    public Long getApptId() { return apptId; }
    public void setApptId(Long apptId) { this.apptId = apptId; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public LocalDate getApptDate() { return apptDate; }
    public void setApptDate(LocalDate apptDate) { this.apptDate = apptDate; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

```

### 9.3 三個新註解

**`@GeneratedValue(strategy = GenerationType.IDENTITY)`**
- 主鍵由**資料庫自動產生**（流水號 1, 2, 3...）
- 你不需要自己給 `apptId` 值，`save()` 時 JPA 會自動填
- 適用於「系統產生的 ID」，不是業務編號（如病歷號）

**`@ManyToOne`**
- 表達「**多對一**」的關係
- 「多筆 Appointment」對應「一位 Doctor」
- JPA 會自動處理 JOIN 查詢

**`@JoinColumn(name = "chart_no")`**
- 指定外鍵欄位的名稱
- 在 `appointment` 資料表中，會有一個叫 `chart_no` 的欄位，指向 `patient.chart_no`

### 9.4 為什麼不做雙向關聯？

有經驗的讀者會問：「為什麼 `Patient` 裡面不加個 `List<Appointment>` 欄位，做成雙向關聯？」

雙向關聯（`@OneToMany`）會帶來以下複雜度：

- 要處理 `cascade`（刪除病患時，要不要刪掉他的掛號？）
- 要處理 `fetch` 策略（每次讀病患就要撈所有掛號嗎？會卡很慢）
- JSON 序列化會出現**無限遞迴**（Patient → Appointments → Patient → ...）

**本課程只用單向 `@ManyToOne`**。要查某病患的掛號時，用 `appointmentRepo.findByPatient(patient)` 即可，功能不受影響。

### 9.5 產生的資料表長什麼樣子？

啟動後 JPA 會自動產生：

```sql
CREATE TABLE appointment (
    appt_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    chart_no    VARCHAR(20) NOT NULL,
    doctor_id   VARCHAR(10) NOT NULL,
    appt_date   DATE NOT NULL,
    time_slot   VARCHAR(20) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    FOREIGN KEY (chart_no)  REFERENCES patient(chart_no),
    FOREIGN KEY (doctor_id) REFERENCES doctor(doctor_id)
);
```

用 SQLite Viewer 打開 `miniclinic.db` 檔案，就能看到這張表的結構。

---

## 10. Spring Data JPA：最神奇的部分

現在來到本週的**殺手級功能**。

### 10.1 原本的 DoctorRepository

Ch09-B 你寫的 `DoctorRepository`：

```java
@Component
public class DoctorRepository {
    // 手寫 List，自己實作 findAll、findById、findByDepartment...
}
```

### 10.2 改用 Spring Data JPA

**刪掉**原本的 `DoctorRepository`，改成這樣：

```java
package tw.edu.fju.miniclinic.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, String> {

    // 不用寫實作，Spring 會依命名規則自動產生
    List<Doctor> findByDepartment(String department);

    // 命名規則查詢只能查欄位值，無法表達 DISTINCT 聚合
    // 要取所有不重複的科別清單，必須用 @Query 寫 JPQL
    @Query("SELECT DISTINCT d.department FROM Doctor d ORDER BY d.department")
    List<String> findAllDepartments();
}
```

**就這樣。** 幾行非空白的程式碼。

> **補充說明：何時需要 `@Query`？**
>
> 方法命名查詢（`findByXxx`）能自動派生的條件是：**依欄位值篩選或排序**。但「取所有不重複的科別名稱」不是在篩選某個欄位值，而是在做 **DISTINCT 聚合**，命名規則無法表達這種語意。
>
> 因此 `findAllDepartments()` 必須用 `@Query` 手寫 JPQL：
>
> ```java
> @Query("SELECT DISTINCT d.department FROM Doctor d ORDER BY d.department")
> List<String> findAllDepartments();
> ```
>
> JPQL（Java Persistence Query Language）語法與 SQL 類似，但操作的是 **Java 類別與欄位名**（`Doctor`、`d.department`），而非資料表名或欄位名。Hibernate 會把它翻譯成對應資料庫的 SQL。
>
> `/api/departments` 端點會呼叫這個方法——少了它，啟動時就會因找不到 `findAllDepartments()` 而編譯失敗。

### 10.3 發生了什麼事？

**`JpaRepository<Doctor, String>`** 提供了 N 個開箱即用的方法：

| 方法 | 功能 |
|---|---|
| `findAll()` | 查全部 |
| `findById(id)` | 依主鍵查詢（回傳 `Optional`）|
| `save(entity)` | 新增或更新 |
| `deleteById(id)` | 依主鍵刪除 |
| `count()` | 計算總筆數 |
| `existsById(id)` | 檢查是否存在 |

還有一個**魔法功能——方法命名查詢（Query Derivation）**：

```java
List<Doctor> findByDepartment(String department);
// ↑ Spring 看到這個方法名，自動產生 SQL：
//   SELECT * FROM doctor WHERE department = ?
```

### 10.4 方法命名查詢的規則

| 方法名 | 產生的 SQL |
|---|---|
| `findByName(String name)` | `WHERE name = ?` |
| `findByNameAndDepartment(...)` | `WHERE name = ? AND department = ?` |
| `findByDepartmentOrderByName()` | `ORDER BY name` |
| `countByDepartment(...)` | `SELECT COUNT(*) WHERE department = ?` |
| `findByApptDateBetween(from, to)` | `WHERE appt_date BETWEEN ? AND ?` |
| `existsByDoctorId(id)` | `SELECT COUNT > 0` |

只要方法名符合規則，Spring 自動產生實作。**你不用寫任何 SQL**。

### 10.5 為什麼不需要 @Component？

你會注意到 `DoctorRepository` 是個 `interface`（介面），沒有 `@Component` 註解，Controller 還是能 `@Autowired`。

因為 Spring Data JPA 在啟動時會**掃描所有繼承 `JpaRepository` 的介面**，自動產生實作類別並註冊為 Bean。**你不用管它怎麼辦到的，知道能用就好**。

---

## 11. 改寫 PatientRepository 與 AppointmentRepository

同樣地，建立：

PatientRepository.java
```java
package tw.edu.fju.miniclinic.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, String> {

    List<Patient> findByName(String name);
}
```

AppointmentRepository.java
```java
package tw.edu.fju.miniclinic.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByApptDate(LocalDate apptDate);
    List<Appointment> findByDoctor(Doctor doctor);
    List<Appointment> findByPatient(Patient patient);
    long countByApptDateBetween(LocalDate from, LocalDate to);
}

```

注意：

- `JpaRepository<Patient, String>`：第二個參數是主鍵型別，`Patient` 的主鍵是 `String`
- `JpaRepository<Appointment, Long>`：`Appointment` 的主鍵是自動產生的 `Long`

---

## 12. Controller 該怎麼改？

**幾乎不用改。**

回想 Ch09-B 的 `DoctorApiController`：

```java
@RestController
public class DoctorApiController {

    @Autowired
    private DoctorRepository doctorRepo;

    @GetMapping("/api/doctors")
    public List<Doctor> getDoctors(@RequestParam(required = false) String department) {
        if (department == null || department.isBlank()) {
            return doctorRepo.findAll();
        }
        return doctorRepo.findByDepartment(department);
    }

    // ... 其他方法
}
```

這段程式碼**一個字都不用改**。因為：

- 方法名相同：`findAll()`、`findByDepartment(...)`
- 回傳型別相同：`List<Doctor>`
- Spring 會自動注入新的 JPA Repository 實作

這就是上週「預先設計一致命名」的威力——換資料來源不影響使用端。

---

## 13. 初始化資料：data.sql

每次啟動，我們希望資料庫裡已經有基本資料，方便開發和測試。

### 13.1 在 resources 下建立 data.sql

`src/main/resources/data.sql`：

```sql
-- 初始醫師資料（5 位）
INSERT OR IGNORE INTO doctor (doctor_id, name, department, specialty) VALUES
    ('D001', '陳志明醫師', '家醫科', '一般內科、慢性病管理'),
    ('D002', '林佩君醫師', '內科',   '心臟血管、高血壓'),
    ('D003', '王建華醫師', '復健科', '運動傷害、脊椎復健'),
    ('D004', '李美玲醫師', '小兒科', '兒童感冒、疫苗接種'),
    ('D005', '張雅筑醫師', '身心科', '焦慮、失眠、情緒調適');

-- 初始病患資料（3 位虛構病患）
INSERT OR IGNORE INTO patient (chart_no, name, gender, birth_date, phone) VALUES
    ('TEST00001', '測試病患甲', '男', '1985-03-15', '0912-345-678'),
    ('TEST00002', '王小明',     '男', '1990-07-22', '0923-456-789'),
    ('TEST00003', '李小華',     '女', '1988-11-30', '0934-567-890');

-- 初始掛號資料（appt_id 必須明確指定，INSERT OR IGNORE 才能防止重複，見 13.4 說明）
INSERT OR IGNORE INTO appointment (appt_id, chart_no, doctor_id, appt_date, time_slot, status) VALUES
    (1, 'TEST00001', 'D001', '2026-05-01', 'AM', 'BOOKED'),
    (2, 'TEST00002', 'D002', '2026-05-01', 'AM', 'BOOKED'),
    (3, 'TEST00003', 'D003', '2026-05-02', 'PM', 'BOOKED');
```

### 13.2 設定：讓 data.sql 執行

在 `application.properties` 加入：

```properties
# 先讓 JPA 建立資料表結構，再執行 data.sql
spring.jpa.defer-datasource-initialization=true

# 每次啟動都執行 data.sql（開發時方便；正式環境要設為 never）
spring.sql.init.mode=always
```

### 13.3 資料庫檔案處理

**第一次啟動**，Spring Boot 會：

1. 檢查 `miniclinic.db` 是否存在 → 不存在就建立空檔
2. JPA 讀取 Entity 定義，建立所有資料表
3. 執行 `data.sql` 插入初始資料

**之後啟動**：

- 資料表已存在，JPA 只檢查結構是否正確
- `data.sql` 每次都會再執行一次 → **可能導致主鍵重複錯誤**

### 13.4 避免重複插入

最簡單的解法：改用 `INSERT OR IGNORE`（SQLite 專屬語法）。

但 `doctor` 和 `appointment` 的處理方式**不同**，必須分開說明。

**`doctor` 和 `patient`：直接加 `INSERT OR IGNORE` 即可**

```sql
INSERT OR IGNORE INTO doctor (doctor_id, name, ...) VALUES (...);
```

`INSERT OR IGNORE` 靠**主鍵衝突**來判斷「這筆已存在，跳過」。`doctor_id`（如 `'D001'`）和 `chart_no`（如 `'TEST00001'`）是你自己指定的字串，第二次執行時衝突 → 被 IGNORE，正確。

**`appointment`：必須明確指定 `appt_id`**

`appointment` 的主鍵 `appt_id` 是**資料庫自動產生**的流水號。若不指定，每次 INSERT 都拿到新 ID，永遠不衝突，`INSERT OR IGNORE` 形同虛設 → 每次啟動都多出 3 筆重複記錄。

解法是在 seed 資料裡明確給定 `appt_id`：

```sql
INSERT OR IGNORE INTO appointment (appt_id, chart_no, doctor_id, appt_date, time_slot, status) VALUES
    (1, 'TEST00001', 'D001', '2026-05-01', 'AM', 'BOOKED'),
    (2, 'TEST00002', 'D002', '2026-05-01', 'AM', 'BOOKED'),
    (3, 'TEST00003', 'D003', '2026-05-02', 'PM', 'BOOKED');
```

這樣第二次啟動時，`appt_id = 1` 已存在 → 衝突 → IGNORE，不重複插入。之後透過表單新增的掛號，資料庫會從 `4` 開始自動遞增，不受影響。

或者開發時的偷懶做法：**直接刪掉 `miniclinic.db`**，下次啟動就會重新建立一次乾淨的資料。

---

## 14. 實際演練：Doctor CRUD 完整流程（課堂示範）

透過 `DoctorApiController.java` 來進行醫生資料的創建、修改與刪除

### 14.1 新增 Doctor API

```java
@PostMapping("/api/doctors")
public ResponseEntity<Doctor> createDoctor(@RequestBody Doctor doctor) {
    Doctor saved = doctorRepo.save(doctor);
    return ResponseEntity.status(201).body(saved);
}
```

**新觀念：`@RequestBody`**

- 把 HTTP 請求的 JSON body 反序列化成 `Doctor` 物件
- 和 `@ModelAttribute`（接收表單）不同，這接收的是 JSON

### 14.2 更新 Doctor API

```java
@PutMapping("/api/doctors/{doctorId}")
public ResponseEntity<Doctor> updateDoctor(
        @PathVariable String doctorId,
        @RequestBody Doctor updated) {

    return doctorRepo.findById(doctorId)
        .map(existing -> {
            existing.setName(updated.getName());
            existing.setDepartment(updated.getDepartment());
            existing.setSpecialty(updated.getSpecialty());
            return ResponseEntity.ok(doctorRepo.save(existing));
        })
        .orElse(ResponseEntity.notFound().build());
}
```

### 14.3 刪除 Doctor API

```java
@DeleteMapping("/api/doctors/{doctorId}")
public ResponseEntity<Void> deleteDoctor(@PathVariable String doctorId) {
    if (!doctorRepo.existsById(doctorId)) {
        return ResponseEntity.notFound().build();
    }
    doctorRepo.deleteById(doctorId);
    return ResponseEntity.noContent().build();  // 204 No Content
}
```

### 14.4 用 REST Client 測試

更新 `api-tests.http`：

```
### 新增醫師
POST http://localhost:8080/api/doctors
Content-Type: application/json

{
    "doctorId": "D006",
    "name": "吳大明醫師",
    "department": "家醫科",
    "specialty": "健康檢查、疫苗"
}

###

### 更新醫師
PUT http://localhost:8080/api/doctors/D006
Content-Type: application/json

{
    "name": "吳大明醫師",
    "department": "家醫科",
    "specialty": "健康檢查、疫苗、成人預防保健"
}

###

### 刪除醫師
DELETE http://localhost:8080/api/doctors/D006
```

測試完後用 SQLite Viewer 打開 `miniclinic.db`，會看到 `doctor` 表的變化。

---

## 15. 掛號 POST：從 Ch09-B 到 Ch09-C

Ch09-B 的掛號表單只是「假裝」處理——Controller 收到表單資料後，只是把欄位值回顯給使用者，沒有任何資料真的被儲存。本節要讓掛號表單真正運作：填完送出 → 存入資料庫 → 顯示掛號編號。

### 15.1 Ch09-B 的問題在哪裡

Ch09-B 的 POST handler 大概長這樣：

```java
// Ch09-B（不完整版）
@PostMapping("/appointment/new")
public String submitAppointment(@ModelAttribute AppointmentForm form, Model model) {
    model.addAttribute("doctorId", form.getDoctorId());
    model.addAttribute("apptDate", form.getApptDate());
    // ... 只是把表單資料丟給模板顯示
    return "appointment-result";
}
```

問題在於：
- 表單接收的是 `doctorId`（字串），不是 `Doctor` 物件
- 沒有查詢資料庫、沒有建立 `Appointment`、沒有呼叫 `save()`
- 頁面雖然顯示「成功」，但什麼都沒存

### 15.2 為什麼需要 AppointmentForm？

掛號表單送來的資料：

```
chartNo  = "TEST00001"    ← 字串
doctorId = "D001"         ← 字串
apptDate = "2026-05-01"   ← 字串（HTML input type="date" 送的是字串）
timeSlot = "AM"           ← 字串
```

但 `Appointment` Entity 需要的是：

```java
private Patient patient;   ← Patient 物件
private Doctor  doctor;    ← Doctor 物件
private LocalDate apptDate; ← LocalDate（不是字串）
```

**`AppointmentForm`** 就是用來橋接這兩個世界的 DTO（Data Transfer Object）——它接住表單的原始字串，Controller 再把字串轉換為物件。

建立 `model/AppointmentForm.java`
```java
package tw.edu.fju.miniclinic.model;

public class AppointmentForm {
    private String chartNo;       // 病歷號
    private String doctorId;      // 掛號的醫師
    private String apptDate;      // 日期
    private String timeSlot;      // 時段

    public AppointmentForm() {}

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

注意：`AppointmentForm` **不是** Entity，不需要 `@Entity` 或任何 JPA 註解。它只是一個普通的 Java 類別，負責接收表單資料。

### 15.3 完整的 POST 處理流程

完善 `AppointmentController.java` 程式碼：

```java
@Autowired
private DoctorRepository doctorRepo;

@Autowired
private PatientRepository patientRepo;

@Autowired
private AppointmentRepository appointmentRepo;

@GetMapping("/appointment/new")
public String newAppointmentForm(Model model) {
	model.addAttribute("form", new AppointmentForm());
	model.addAttribute("doctors", doctorRepo.findAll());
	return "appointment-new";
}

@PostMapping("/appointment/new")
public String submitAppointment(
        @ModelAttribute AppointmentForm form,
        Model model) {

    // 步驟 1：用表單的字串 ID，從資料庫查出真正的物件
    Patient patient = patientRepo.findById(form.getChartNo()).orElse(null);
    Doctor  doctor  = doctorRepo.findById(form.getDoctorId()).orElse(null);

    // 步驟 2：驗證——找不到就回表單顯示錯誤
    if (patient == null || doctor == null) {
        model.addAttribute("error", "查無此病歷號或醫師，請確認後重試");
        model.addAttribute("form", form);
        model.addAttribute("doctors", doctorRepo.findAll());
        return "appointment-new";   // ← 回到表單頁，不是跳轉
    }

    // 步驟 3：建立 Appointment Entity，設定關聯物件
    Appointment appt = new Appointment();
    appt.setPatient(patient);
    appt.setDoctor(doctor);
    appt.setApptDate(LocalDate.parse(form.getApptDate()));  // 字串 → LocalDate
    appt.setTimeSlot(form.getTimeSlot());
    appt.setStatus("BOOKED");

    // 步驟 4：存入資料庫，JPA 自動填入 apptId
    Appointment saved = appointmentRepo.save(appt);

    // 步驟 5：把儲存後的物件交給結果頁面
    model.addAttribute("appointment", saved);
    return "appointment-result";
}

@PostMapping("/api/appointments")
public ResponseEntity<Appointment> createAppointment(
		@RequestBody Map<String, String> request) {

	// 從 request 取出資料
	String chartNo = request.get("chartNo");
	String doctorId = request.get("doctorId");
	LocalDate apptDate = LocalDate.parse(request.get("apptDate"));
	String timeSlot = request.get("timeSlot");

	// 查詢關聯的 Patient 與 Doctor
	Patient patient = patientRepo.findById(chartNo).orElse(null);
	Doctor doctor = doctorRepo.findById(doctorId).orElse(null);

	if (patient == null || doctor == null) {
		return ResponseEntity.badRequest().build();
	}

	// 建立 Appointment 物件
	Appointment appt = new Appointment();
	appt.setPatient(patient);
	appt.setDoctor(doctor);
	appt.setApptDate(apptDate);
	appt.setTimeSlot(timeSlot);
	appt.setStatus("BOOKED");

	Appointment saved = appointmentRepo.save(appt);
	return ResponseEntity.status(201).body(saved);
}
```

** @PostMapping("/appointment/new") 逐步拆解：**

**步驟 1：查詢資料庫取得關聯物件**

表單只送來 `"TEST00001"` 這個字串，但 `Appointment` 需要整個 `Patient` 物件（因為外鍵指向的是完整記錄）。所以必須先查詢：

```java
Patient patient = patientRepo.findById(form.getChartNo()).orElse(null);
```

`findById()` 回傳 `Optional<Patient>`；`.orElse(null)` 在找不到時給 `null`，讓後面的驗證步驟能夠判斷。

**步驟 2：驗證輸入**

如果病歷號不存在或醫師不存在，不應該繼續。此時**回到表單頁面（不是 redirect）**並帶上錯誤訊息，讓使用者看到問題在哪裡。注意 `doctors` 清單也要重新放進 model，否則下拉選單會消失。

**步驟 3：轉換並建立 Entity**

這裡有一個關鍵動作：

```java
appt.setApptDate(LocalDate.parse(form.getApptDate()));
```

HTML 的 `<input type="date">` 送出的值是字串格式 `"2026-05-01"`；`LocalDate.parse()` 把它轉換成 Java 的日期物件。`@ModelAttribute` 不會自動做這個轉換，所以需要手動處理。

**步驟 4：`save()` 回傳儲存後的物件**

```java
Appointment saved = appointmentRepo.save(appt);
```

`save()` 不只是存入資料庫，它還**回傳一個帶有資料庫自動產生值的新物件**。此時 `saved.getApptId()` 才有值（例如 `1`、`2`、`3`...）——送進去的 `appt` 的 `apptId` 還是 `null`。

**步驟 5：把 `saved` 傳給模板**

注意一定要用 `saved`，不是 `appt`。只有 `saved` 的 `apptId` 有填值。

### 15.4 掛號表單模板（appointment-new.html）

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
    </style>
</head>
<body>
    <nav>
        <a th:href="@{/}">首頁</a>
        <a th:href="@{/doctors}">醫師清單</a>
        <a th:href="@{/appointment/new}">線上掛號</a>
        <a th:href="@{/patients}">病歷資料</a>
    </nav>

    <h1>線上掛號</h1>

    <div th:if="${error}" class="error" th:text="${error}"></div>

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

**要注意的地方：**

- 醫師下拉選單：`th:value="${doc.doctorId}"` 送的是 ID 字串；`th:text` 顯示的是人看的名稱。送到 Controller 的只有 `doctorId` 字串，不是整個 `Doctor` 物件。
- `th:field="*{chartNo}"` 等同於 `name="chartNo"` + `id="chartNo"` + `value="${form.chartNo}"`，是 Thymeleaf 的縮寫語法。

GET handler 負責把空白的 `form` 和 `doctors` 清單傳給模板：

```java
@GetMapping("/appointment/new")
public String newAppointmentForm(Model model) {
    model.addAttribute("form", new AppointmentForm());
    model.addAttribute("doctors", doctorRepo.findAll());
    return "appointment-new";
}
```

### 15.5 掛號結果頁（appointment-result.html	）

儲存成功後，結果頁可以顯示資料庫中的真實資料：

```html
<!DOCTYPE html>
<html lang="zh-Hant" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>掛號完成 - MiniClinic</title>
    <style>
        body { font-family: "Microsoft JhengHei", sans-serif; max-width: 560px; margin: 40px auto; padding: 0 20px; }
        h1 { color: #0F2443; }
        nav { background: #0F2443; padding: 12px; border-radius: 8px; margin-bottom: 24px; }
        nav a { color: white; margin-right: 20px; text-decoration: none; font-weight: bold; }
        nav a:hover { color: #5EEAD4; }
        .success { background: #D1FAE5; color: #065F46; padding: 16px; border-radius: 4px; margin: 20px 0; }
        .summary { background: #F8FAFC; padding: 20px; border-left: 5px solid #14B8A6; }
        dt { font-weight: bold; color: #0F2443; margin-top: 8px; }
        dd { margin-left: 0; margin-bottom: 4px; }
    </style>
</head>
<body>
    <nav>
        <a th:href="@{/}">首頁</a>
        <a th:href="@{/doctors}">醫師清單</a>
        <a th:href="@{/appointment/new}">線上掛號</a>
        <a th:href="@{/appointments}">掛號記錄</a>
    </nav>

    <h1>掛號成功！</h1>

    <div class="success">
        您的掛號已成功寫入資料庫（掛號編號：<span th:text="${appointment.apptId}"></span>）
    </div>

    <div class="summary">
        <h2>掛號摘要</h2>
        <dl>
            <dt>掛號編號</dt> <dd th:text="${appointment.apptId}">1</dd>
            <dt>病歷號</dt>   <dd th:text="${appointment.patient.chartNo}">TEST00001</dd>
            <dt>病患姓名</dt> <dd th:text="${appointment.patient.name}">測試病患甲</dd>
            <dt>醫師</dt>    <dd th:text="${appointment.doctor.name}">陳志明醫師</dd>
            <dt>科別</dt>    <dd th:text="${appointment.doctor.department}">家醫科</dd>
            <dt>日期</dt>    <dd th:text="${appointment.apptDate}">2026-05-01</dd>
            <dt>時段</dt>    <dd th:text="${appointment.timeSlot}">AM</dd>
            <dt>狀態</dt>    <dd th:text="${appointment.status}">BOOKED</dd>
        </dl>
    </div>

    <p><a th:href="@{/appointments}">查看所有掛號記錄</a> | <a th:href="@{/}">← 回首頁</a></p>
</body>
</html>
```

因為 `appointment.patient` 和 `appointment.doctor` 是完整物件（透過 `@ManyToOne` 載入），可以直接用 `appointment.patient.name`、`appointment.doctor.department` 等屬性。這就是 JPA 關聯的便利之處——不用再手動查詢。

### 15.6 整個流程串起來

```
使用者填表單（chartNo, doctorId, apptDate, timeSlot）
         ↓
POST /appointment/new
         ↓
Controller 收到 AppointmentForm（四個字串欄位）
         ↓
 ┌──────────────────────────────┐
 │ patientRepo.findById(chartNo) │  → 查 patient 資料表
 │ doctorRepo.findById(doctorId) │  → 查 doctor 資料表
 └──────────────────────────────┘
         ↓ 找不到？
      回表單 + 錯誤訊息
         ↓ 找到？
建立 Appointment Entity（設定 patient、doctor、apptDate、timeSlot）
         ↓
appointmentRepo.save(appt)   → INSERT INTO appointment (...)
         ↓
回傳帶有 apptId 的 saved 物件
         ↓
appointment-result.html 顯示掛號編號與摘要
```

### 15.7 驗證：重啟後資料還在嗎？

這正是 Ch09-C 的核心目標。你可以用以下方式驗證：

1. 填表單送出掛號，記下掛號編號（例如 `apptId = 4`）
2. 停止 Spring Boot，重新啟動
3. 連到 `/appointments`，確認那筆掛號還在，編號還是 `4`

這和 Ch09-B 的根本差異：Ch09-B 重啟後什麼都沒了；Ch09-C 的資料寫進了 `miniclinic.db`，重啟不影響。

### 15.8 查看所有掛號記錄

掛號存入資料庫之後，還需要一個頁面讓人看到所有掛號。這個功能非常簡單——用 `AppointmentRepository.findAll()` 撈出全部，傳給模板顯示即可。

**Controller（AppointmentPageController.java）**

```java
package tw.edu.fju.miniclinic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import tw.edu.fju.miniclinic.model.AppointmentRepository;

@Controller
public class AppointmentPageController {

    @Autowired
    private AppointmentRepository appointmentRepo;

    @GetMapping("/appointments")
    public String listAppointments(Model model) {
        model.addAttribute("appointments", appointmentRepo.findAll());
        return "appointments";
    }
}

```

這個 Controller 只做一件事：查出所有掛號，放進 `model`，交給 `appointments.html` 渲染。和掛號表單的 Controller 分開，是因為兩者職責不同——掛號新增是寫入操作，查看清單是讀取操作，分開放比較清楚。

**模板（appointments.html）**

```html
<!DOCTYPE html>
<html lang="zh-Hant" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>掛號記錄 - MiniClinic</title>
    <style>
        body { font-family: "Microsoft JhengHei", sans-serif; max-width: 900px; margin: 40px auto; padding: 0 20px; }
        h1 { color: #0F2443; border-bottom: 3px solid #14B8A6; padding-bottom: 8px; }
        nav { background: #0F2443; padding: 12px; border-radius: 8px; margin-bottom: 24px; }
        nav a { color: white; margin-right: 20px; text-decoration: none; font-weight: bold; }
        nav a:hover { color: #5EEAD4; }
        table { width: 100%; border-collapse: collapse; }
        th { background: #0F2443; color: white; padding: 10px; text-align: left; }
        td { padding: 10px; border-bottom: 1px solid #E2E8F0; }
        tr:hover td { background: #F0FDFA; }
        .status-BOOKED { color: #0369A1; font-weight: bold; }
        .status-COMPLETED { color: #15803D; font-weight: bold; }
        .status-CANCELLED { color: #9F1239; font-weight: bold; }
        .empty { text-align: center; color: #94A3B8; padding: 40px; }
    </style>
</head>
<body>
    <nav>
        <a th:href="@{/}">首頁</a>
        <a th:href="@{/doctors}">醫師清單</a>
        <a th:href="@{/appointment/new}">線上掛號</a>
        <a th:href="@{/appointments}">掛號記錄</a>
    </nav>

    <h1>掛號記錄</h1>

    <p>共 <strong th:text="${#lists.size(appointments)}">0</strong> 筆掛號</p>

    <table th:if="${not #lists.isEmpty(appointments)}">
        <thead>
            <tr>
                <th>#</th>
                <th>病歷號</th>
                <th>病患姓名</th>
                <th>醫師</th>
                <th>科別</th>
                <th>日期</th>
                <th>時段</th>
                <th>狀態</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="appt : ${appointments}">
                <td th:text="${appt.apptId}">1</td>
                <td th:text="${appt.patient.chartNo}">TEST00001</td>
                <td th:text="${appt.patient.name}">測試病患甲</td>
                <td th:text="${appt.doctor.name}">陳志明醫師</td>
                <td th:text="${appt.doctor.department}">家醫科</td>
                <td th:text="${appt.apptDate}">2026-05-01</td>
                <td th:text="${appt.timeSlot}">AM</td>
                <td>
                    <span th:class="'status-' + ${appt.status}"
                          th:text="${appt.status}">BOOKED</span>
                </td>
            </tr>
        </tbody>
    </table>

    <p class="empty" th:if="${#lists.isEmpty(appointments)}">目前尚無掛號記錄</p>

    <p style="margin-top: 20px;">
        <a th:href="@{/appointment/new}">+ 新增掛號</a> | <a th:href="@{/}">← 回首頁</a>
    </p>
</body>
</html>
```

幾個值得注意的地方：

**`th:each="appt : ${appointments}"`**
Thymeleaf 的迴圈語法，對應 Java 的 `for (Appointment appt : appointments)`。每一個 `<tr>` 就是一筆掛號記錄。

**`appt.patient.chartNo`、`appt.doctor.name`**
因為 `Appointment` Entity 裡有 `@ManyToOne` 關聯，`appt.patient` 是完整的 `Patient` 物件，可以直接存取它的屬性。這是 JPA 關聯的核心優勢——不用自己再去查一次 `patient` 資料表。

**`th:if` 控制空白頁**
`th:if="${not #lists.isEmpty(appointments)}"` 讓表格只在有資料時顯示；沒有資料時顯示「目前尚無掛號記錄」的提示。兩者用 `th:if` 互斥控制，不需要在 Controller 裡做判斷。

**狀態顯示的技巧**
```html
<span th:class="'status-' + ${appt.status}" th:text="${appt.status}">
```
這行把狀態值（`BOOKED`、`COMPLETED`、`CANCELLED`）直接拼進 CSS class 名稱，搭配 CSS 就能讓不同狀態顯示不同顏色，不需要用 `th:if` 一一判斷。

---

## 16. REST 設計的進一步討論

從 Ch09-B 的 REST 基本慣例，延伸到更細的設計原則。

### 16.1 狀態碼選擇

| 情境 | 正確狀態碼 | 常見錯誤 |
|---|---|---|
| 查詢成功 | 200 OK | (沒問題) |
| 新增成功 | 201 Created | 回傳 200 也不算錯，但不夠精準 |
| 刪除成功（無回應內容）| 204 No Content | (沒問題) |
| 找不到資源 | 404 Not Found | 回傳 200 + 錯誤訊息（壞做法）|
| 輸入格式錯誤 | 400 Bad Request | 回傳 500 |
| 未登入 | 401 Unauthorized | (Ch09-D 會碰到) |
| 已登入但無權限 | 403 Forbidden | (Ch09-D 會碰到) |
| 伺服器 Bug | 500 Internal Server Error | (不該發生)|

### 16.2 查詢參數 vs 路徑變數的選擇

| 判斷標準 | 用路徑變數 | 用查詢參數 |
|---|---|---|
| 這個值決定**哪一筆資源** | ✓ | |
| 這個值是**篩選條件** | | ✓ |
| 這個值**可有可無** | | ✓ |
| 這個值是**分頁 / 排序** | | ✓ |

範例：

```
✓ /api/doctors/D001                         (路徑變數)
✓ /api/doctors?department=內科               (查詢參數)
✓ /api/appointments?date=2026-05-01&page=0   (查詢參數)
✗ /api/doctors/department/內科                (應該用查詢參數)
```

### 16.3 巢狀資源

當資源有階層關係時：

```
/api/doctors/D001/appointments    ← D001 醫師的所有掛號
/api/patients/TEST00001/appointments  ← TEST00001 的所有掛號
```

這種設計很直覺。本週暫時不需要實作，Ch09-D 的 dashboard 會用到。

---

## 17. 作業三：讓 MiniClinic 的資料真的存起來

### 作業說明

本週延續 Ch09-B 的專案。請在現有的 `miniclinic` 專案繼續開發。

### Part A：基本要求（必做）

- [ ] `pom.xml` 加入 `sqlite-jdbc`、`hibernate-community-dialects`、`spring-boot-starter-data-jpa`
- [ ] `application.properties` 正確設定 SQLite 連線與 JPA 設定
- [ ] **Doctor Entity**：加入 JPA 註解，成為可持久化的類別
- [ ] **Patient Entity**：建立新類別含 JPA 註解
- [ ] **Appointment Entity**：建立新類別含 `@ManyToOne` 關聯
- [ ] `DoctorRepository` 改為 `extends JpaRepository<Doctor, String>`
- [ ] `PatientRepository` 與 `AppointmentRepository` 同樣繼承 `JpaRepository`
- [ ] `data.sql` 含至少 5 位醫師、3 位病患、3 筆掛號
- [ ] 原本所有 `/doctors`、`/api/doctors` 系列頁面與 API 能正常運作（資料從資料庫讀出）
- [ ] 實作 `POST /api/doctors`、`PUT /api/doctors/{id}`、`DELETE /api/doctors/{id}`
- [ ] 掛號表單送出後真的把資料存入資料庫
- [ ] 實作 `GET /api/patients`、`GET /api/patients/{chartNo}`、`GET /api/appointments`
- [ ] `api-tests.http` 含所有新增 API 的測試

### Part B：進階要求（必做）

實作**掛號統計頁面**：

- [ ] 新增頁面 `GET /stats`：顯示基本統計資訊
  - 醫師總數、病患總數、掛號總數
  - 依科別分組，列出每科的掛號數
- [ ] 新增 API `GET /api/appointments/count`：回傳總掛號數
- [ ] 新增 API `GET /api/appointments?date=YYYY-MM-DD`：依日期篩選掛號
- [ ] 新增 API `GET /api/appointments?doctorId=D001`：依醫師篩選掛號

**提示**：`AppointmentRepository` 可以加入這類方法：

```java
List<Appointment> findByApptDate(LocalDate apptDate);
List<Appointment> findByDoctor(Doctor doctor);
long count();  // JpaRepository 內建
```

### 繳交方式

- 壓縮整個 `miniclinic` 專案為 zip 檔
- 檔名：`學號_姓名_Ch09C.zip`
- **記得刪除 `target/` 資料夾與 `miniclinic.db` 檔案**（讓助教測試時能看到乾淨的初始狀態）
- 上傳至教學平台

### 評分重點

1. 所有資料操作都真的對資料庫生效
2. 三個 Entity 的關聯正確（Appointment 的 `@ManyToOne`）
3. `data.sql` 的初始資料合理、涵蓋五個科別
4. REST API 回應的狀態碼符合情境
5. `api-tests.http` 涵蓋所有 API
6. Git commit message 符合規範

---

## 18. 常見問題排查

### Q1：啟動失敗，錯誤訊息提到 `No bean of type 'DoctorRepository'`

**原因**：Spring 掃描不到你的 Repository 介面。

**排查**：
1. `DoctorRepository` 是否在 `tw.edu.fju.miniclinic` 或其子 package 下？
2. 是否有 `extends JpaRepository<Doctor, String>`？
3. 重新啟動時有先 Maven reload 嗎？

### Q2：`data.sql` 第二次啟動時報錯「UNIQUE constraint failed」

**原因**：`data.sql` 每次啟動都會執行，第二次插入 `D001` 時主鍵重複。

**解法**（任選其一）：
- 把 `data.sql` 的 `INSERT` 改成 `INSERT OR IGNORE`
- 或每次測試前刪掉 `miniclinic.db`
- 或把 `spring.sql.init.mode` 改成 `never`，只手動執行一次

### Q3：SQLite Viewer 打開 `.db` 檔案看不到資料表

**原因**：應用還沒啟動過，`.db` 是空的。

**解法**：先啟動一次應用程式，讓 JPA 建立資料表、`data.sql` 插入資料。然後在 SQLite Viewer 中按重新整理。

### Q4：`@ManyToOne` 欄位 JSON 輸出很怪，包含整個 Patient 物件

**症狀**：
```json
{
    "apptId": 1,
    "patient": {
        "chartNo": "TEST00001",
        "name": "測試病患甲",
        ...(整個 Patient 物件)
    },
    "doctor": {
        ...(整個 Doctor 物件)
    }
}
```

**說明**：這其實是正常的，JPA 會把關聯物件一起序列化。如果你覺得 JSON 太肥，Ch09-D 會教「DTO」的觀念來處理。本週先接受這個現狀。

### Q5：方法命名查詢不動作（例如 `findByDepartmentName`）

**原因**：Spring 找不到 `Doctor` 類別中叫 `departmentName` 的欄位。

**排查**：方法名的欄位部分**必須和 Entity 的欄位名一致**（駝峰式）。`findByDepartment` 對應 `department` 欄位，不是 `departmentName`。

### Q6：啟動時 Hibernate 產生 SQL 有 `rowid` 相關錯誤

**原因**：SQLite 對於 `@GeneratedValue(strategy = IDENTITY)` 有特殊處理。

**解法**：確認 `application.properties` 有設定：
```properties
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
```

版本不對也會有問題，確認 `hibernate-community-dialects` 的版本和 Spring Boot 3.x 相容。

---

## 延伸閱讀（非必讀）

### 19. @Query 自訂查詢

當方法命名查詢表達不出來你要的邏輯時，可以用 `@Query` 寫 JPQL 或原生 SQL：

```java
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a WHERE a.apptDate = :date AND a.doctor.department = :dept")
    List<Appointment> findByDateAndDepartment(
        @Param("date") LocalDate date,
        @Param("dept") String department
    );

    @Query(value = "SELECT department, COUNT(*) FROM appointment a " +
                   "JOIN doctor d ON a.doctor_id = d.doctor_id " +
                   "GROUP BY department", nativeQuery = true)
    List<Object[]> countByDepartment();
}
```

### 20. Transaction 管理

資料庫操作通常需要**交易（Transaction）**來保證一致性：

```java
@Service
public class AppointmentService {

    @Transactional
    public void transferAppointment(Long apptId, String newDoctorId) {
        Appointment appt = appointmentRepo.findById(apptId).orElseThrow();
        Doctor newDoctor = doctorRepo.findById(newDoctorId).orElseThrow();

        appt.setDoctor(newDoctor);
        appointmentRepo.save(appt);

        // 如果這段中間出例外，Spring 會 rollback 整筆交易
    }
}
```

Spring 的 `@Transactional` 會自動處理 begin / commit / rollback。本課程的情境單純，沒有多步驟的複雜操作，所以暫時不用深入。

### 21. 生產級資料庫版本管理（Flyway / Liquibase）

`ddl-auto=update` 和 `data.sql` 是開發環境的快速做法。正式環境會用：

- **Flyway**：SQL migration 檔案版本控管
- **Liquibase**：XML/YAML/SQL migration

它們能確保團隊所有環境的 schema 狀態一致。超出本課程範圍，但你在業界會碰到。

### 22. SQLite vs PostgreSQL 的差異預告

Ch09-E 部署到雲端時，會換成 PostgreSQL。一些關鍵差異：

| 項目 | SQLite | PostgreSQL |
|---|---|---|
| 資料型別 | 寬鬆（TEXT 存什麼都行） | 嚴格（必須符合宣告）|
| 自動遞增 | `AUTOINCREMENT` | `SERIAL` / `BIGSERIAL` |
| 日期時間 | 存成字串 | 原生支援 |
| 並發 | 寫入序列化 | 真正的並發寫入 |
| 大寫敏感 | 預設不敏感 | 預設敏感 |

**JPA 的美好之處在於**：這些差異由 `Dialect` 物件處理，你的 Java 程式碼幾乎不用動。

---

## 下週預告：Ch09-D 將學到

- HTTP 為何是無狀態、Cookie 與 Session 的差別
- `HttpSession` 的使用
- **手寫醫師登入流程**（不用 Spring Security，從原理搞懂）
- Interceptor 保護需登入的路由
- 密碼雜湊（BCrypt）
- Bean Validation（`@NotBlank`、`@Pattern`、`@Valid`）
- **SQL Injection 攻擊與防範**（本週埋下的伏筆）
- XSS、CSRF 基本概念
- 醫療資料倫理的深入討論

---

*本講義由教師自行編寫，所有程式碼範例均為原創，供課堂教學使用。*
