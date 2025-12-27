# Exchange Platform (卡片交換平台) - 軟體工程專案成果

## 📖 專案簡介 (Project Overview)
**Exchange Platform** 是一個專為「偶像明星小卡」設計的以物易物交換平台。本專案旨在解決現有社群交易分散、缺乏保障的問題，提供一個透明、可追蹤且**嚴格無金流**的交換環境。系統強調交易流程的可追蹤性（Traceability）與狀態一致性（Consistency）。

*   **核心理念**：以物易物 (Barter only)、無金流介入、完整的物流追蹤與狀態管理。
*   **關鍵功能**：第三方登入 (Google OAuth2)、小卡刊登管理、交換提案鎖定機制 (Row-level locking)、即時聊天協商、雙向物流追蹤 (7-11/面交)。

---

## 👥 團隊成員 (Group 8)
**指導教授**：許智誠 教授

| 學號 | 姓名 | 專案角色 (Role) | email |
| :--- | :--- | :--- | :--- |
| **114423051** | **張謦麒** | **測試/開發負責人** | 114423051@cc.ncu.edu.tw |
| **114423037** | **蕭筑云** | **專案經理 (PM)** | 114423037@cc.ncu.edu.tw |
| **114423020** | **陳欣妤** | **測試工程師 (QA)** | 114423020@cc.ncu.edu.tw |
| **114423068** | **廖承偉** | **測試架構師 (TA)** | 114423068@cc.ncu.edu.tw |

---

## 📚 課程內容應用與文件索引 (Course Concepts & Documentation)

本專案嚴格遵循軟體工程生命週期 (SDLC) 與課程教學之標準流程，產出以下七份標準化文件：

### 1. 專案管理 (Project Management)
*   **A. 軟體專案管理規劃文件 (SPMP)**
    *   **課程應用**：
        *   **WBS 與時程規劃**：應用 **Chapter 23** 的專案規劃技巧，定義里程碑與任務。
        *   **風險管理 (Risk Management)**：識別 R1-R5 風險（如外部 API 故障），並制定緩解計畫。
    *   **重點**：確認採用 MVP (Minimum Viable Product) 模式，鎖定 UC01-UC08 為核心範圍。

### 2. 需求工程 (Requirements Engineering)
*   **B. 軟體需求分析文件 (SRS)**
    *   **課程應用**：
        *   **需求獲取 (Elicitation)**：定義功能性與非功能性需求 (NFR)，如「登入 < 3秒」的效能指標。
        *   **使用案例 (Use Cases)**：應用 **Chapter 7** 與 **Chapter 8**，繪製 Use Case Diagram 與撰寫 Expanded Use Cases (主要/替代流程)。
        *   **需求追溯 (RTM)**：建立需求追溯矩陣，確保每個需求都有對應的測試案例。

### 3. 系統分析與設計 (System Analysis & Design)
*   **C. 軟體設計規格書文件 (SADD)**
    *   **課程應用**：
        *   **架構模式**：採用 **Layered Architecture (分層架構)** 與 **BCE (Boundary-Control-Entity)** 模式，將系統劃分為 Controller (Boundary)、Service (Control)、Repository (Entity)。
        *   **設計模式 (Design Patterns)**：
            *   **Controller Pattern**：應用於 API 層接收請求。
            *   **Factory/Bridge**：應用於不同支付/物流方式的擴充設計。
            *   **State Machine**：設計 Listing 與 Proposal 的狀態流轉 (Pending -> Accepted -> Completed)。
*   **D. 物件設計文件 (ODD)**
    *   **課程應用**：
        *   **物件互動 (Interaction Modeling)**：應用 **Chapter 9**，將 Use Case 轉化為 Sequence Diagram，定義物件間的訊息傳遞。
        *   **資料建模**：設計 Entity Class Diagram 與資料庫 Schema，確保資料一致性。

### 4. 測試與品質保證 (Testing & SQA)
*   **E. 測試計畫書 (Test Plan)**
    *   **課程應用**：
        *   **測試層級**：規劃單元測試 (Unit)、整合測試 (Integration) 與系統測試 (System)。
        *   **測試方法**：應用 **Chapter 20** 的黑箱測試（等價劃分、邊界值）與白箱測試（路徑覆蓋）概念。
*   **F. 測試結果報告 (Test Summary Report)**
    *   **課程應用**：
        *   **覆蓋率指標**：達成 **Web Testing** (Chap 20.5) 的頁面覆蓋率 (Page Coverage) 與連結覆蓋率 (Link Coverage) 100%。
        *   **非功能性測試**：執行壓力測試 (Stress) 與恢復測試 (Recovery)，驗證系統在高負載下的穩定性。
        *   **發布決策**：基於測試結果做出 "Conditional Go" 的發布建議。

### 5. AI 賦能軟體工程 (AI in SE)
*   **G. AI 應用文件**
    *   **課程應用**：
        *   **AI 輔助開發**：呼應課程 **"AI 時代軟體工程師的存活方式"**，利用 GitHub Copilot 協助生成 SADD/ODD 草稿、解決 Thymeleaf 前端渲染錯誤，以及撰寫單元測試代碼。

---

## 🛠️ 技術架構 (Tech Stack)

本專案採用 **Java Spring Boot** 生態系，實踐高內聚低耦合的物件導向設計。

| 類別 | 技術/工具 | 架構位置 (Layer) |
| :--- | :--- | :--- |
| **Backend** | Java 17, Spring Boot 3.5.7 | Control / Service Layer |
| **Frontend** | Thymeleaf (SSR), HTML/CSS | Boundary / Presentation Layer |
| **Database** | MySQL 8.0, Spring Data JPA | Entity / Persistence Layer |
| **Security** | Spring Security + Google OAuth2 | Security Cross-cutting concern |
| **Testing** | JUnit 5, MockMvc | Testing / QA |
| **Tools** | Maven, Git, Python (Crawler) | Configuration & Integration |

---

## 🚀 專案亮點與成果 (Highlights)

1.  **完整的 BCE 架構實作**：
    *   嚴格遵守 Boundary (Controller) -> Control (Service) -> Entity (Repository) 的呼叫依賴，無循環依賴，符合 SADD 設計規範。
2.  **高品質的測試覆蓋**：
    *   **總測試案例**：101 個，通過率 97%。
    *   **效能表現**：API 響應時間 P95 < 100ms，遠優於預期的 2s 目標。
    *   **Web 架構測試**：100% 頁面與連結覆蓋，無孤兒頁面。
3.  **複雜業務邏輯處理**：
    *   成功實作 **Row-level Locking** 機制，解決「多卡換一卡」的併發衝突問題。
    *   設計 **ShipmentEvent** 事件流模型，完整追蹤雙向物流狀態。
4.  **AI 協作開發流程**：
    *   從需求分析到程式碼修復，全程利用 AI 提升效率，體現了課程對於「AI 時代軟體工程」的實踐要求。

---

## 🔧 環境建置與執行指南 (Setup & Installation)

本節提供完整的步驟說明，協助評審者在全新環境下建置並執行專案。

### 📋 前置需求 (Prerequisites)

請確保您的開發環境已安裝以下工具：

| 軟體 | 版本要求 | 下載連結 |
| :--- | :--- | :--- |
| **Java JDK** | 17 或以上 | [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) / [OpenJDK](https://adoptium.net/) |
| **Maven** | 3.6+ | [Apache Maven](https://maven.apache.org/download.cgi) |
| **MySQL** | 8.0+ | [MySQL Community Server](https://dev.mysql.com/downloads/mysql/) |
| **Git** | 最新版 | [Git](https://git-scm.com/downloads) |
| **Python** | 3.7+ (選用，用於物流追蹤工具) | [Python](https://www.python.org/downloads/) |

**驗證安裝**：
```bash
# 確認 Java 版本
java -version

# 確認 Maven 版本
mvn -version

# 確認 MySQL 服務運行中
mysql --version
```

---

### 📥 步驟 1：下載專案程式碼

```bash
# clone專案儲存庫
git clone https://github.com/chi157/Exchange-Platform.git

# 進入專案目錄
cd Exchange-Platform/exchange-web-app
```

---

### 🗄️ 步驟 2：設定資料庫

#### 2.1 建立 MySQL 資料庫

登入 MySQL，執行以下指令建立資料庫：

```sql
CREATE DATABASE exchange_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 2.2 配置資料庫連線

編輯 `src/main/resources/application.yml`，修改資料庫連線參數：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/exchange_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Taipei&allowPublicKeyRetrieval=true
    username: root          # ⚠️ 替換為您的 MySQL 使用者名稱
    password: your_password # ⚠️ 替換為您的 MySQL 密碼
```

> **提示**：資料庫結構會在首次啟動時自動建立（透過 `ddl-auto: update`）。

---

### 🔐 步驟 3：設定 Google OAuth2（第三方登入）

#### 3.1 建立 Google Cloud 專案

1. 前往 [Google Cloud Console](https://console.cloud.google.com/)
2. 建立新專案（專案名稱：Exchange Platform）
3. 啟用「OAuth 同意畫面」：
   - 選擇「外部」
   - 填寫必填欄位（應用程式名稱、支援電子郵件）
   - 範圍選擇：`email`, `profile`, `openid`

#### 3.2 建立 OAuth 2.0 用戶端 ID

1. 進入「憑證」頁面 → 「建立憑證」 → 「OAuth 用戶端 ID」
2. 應用程式類型：「網路應用程式」
3. 已授權的重新導向 URI：
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
4. 建立後，複製 **Client ID** 和 **Client Secret**

#### 3.3 配置 OAuth2 憑證

在 `application.yml` 中替換以下內容：

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID        # ⚠️ 替換為您的 Client ID
            client-secret: YOUR_GOOGLE_CLIENT_SECRET # ⚠️ 替換為您的 Client Secret
            scope: openid,email,profile
```

> **詳細設定指南**：請參閱 [GOOGLE_OAUTH_SETUP.md](GOOGLE_OAUTH_SETUP.md)

---

### 📧 步驟 4：設定電子郵件服務（選用）

若需測試電子郵件通知功能（如驗證碼、交易通知），請配置 Gmail SMTP：

#### 4.1 啟用 Gmail 應用程式密碼

1. 前往 Google 帳戶 → 「安全性」 → 「兩步驟驗證」
2. 啟用兩步驟驗證後，點選「應用程式密碼」
3. 產生新的應用程式密碼（選擇「郵件」和「其他裝置」）

#### 4.2 修改郵件配置

在 `application.yml` 中替換：

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your_email@gmail.com     # ⚠️ 替換為您的 Gmail 地址
    password: your_app_password        # ⚠️ 替換為應用程式密碼
    from-email: your_email@gmail.com   # ⚠️ 替換為寄件者信箱
```

> **注意**：若不需測試郵件功能，可跳過此步驟，系統仍可正常運行其他功能。且Google因安全性限制請使用有網域名稱之伺服器或localhost。

---

### 🚀 步驟 5：編譯與執行專案

#### 5.1 安裝專案依賴

```bash
# 使用 Maven 下載並安裝所有依賴套件
mvn clean install
```

#### 5.2 啟動應用程式

```bash
# 方式 1：使用 Spring Boot Maven 插件
mvn spring-boot:run

# 方式 2：執行編譯後的 JAR 檔案
java -jar target/exchange-web-app-0.0.1-SNAPSHOT.jar
```

#### 5.3 驗證啟動成功

當您看到以下訊息時，表示啟動成功：

```
Started ExchangeWebAppApplication in X.XXX seconds
Tomcat started on port 8080
```

**訪問應用程式**：
- 主頁：[http://localhost:8080](http://localhost:8080)
- 登入頁面：[http://localhost:8080/login](http://localhost:8080/login)

---

### 🧪 步驟 6：執行測試

#### 6.1 執行單元測試與整合測試

```bash
# 執行所有測試（共 101 個測試案例）
mvn test

# 查看測試報告
# 報告位置：target/surefire-reports/index.html
```

#### 6.2 測試覆蓋率分析

```bash
# 產生測試覆蓋率報告（需安裝 JaCoCo 插件）
mvn clean verify

# 報告位置：target/site/jacoco/index.html
```

#### 6.3 預期測試結果

- **總測試數**：101
- **通過率**：≥ 97%
- **失敗測試**：≤ 3（已知問題，不影響核心功能）

---

### 🐍 步驟 7：設定 Python 物流追蹤工具（選用）

若需測試 7-11 物流追蹤功能：

```bash
# 進入工具目錄
cd utils/etracking

# 安裝 Python 依賴
pip install -r ../../requirements.txt

# 執行追蹤測試
python etracking.py
```

---

### 📋 快速檢查清單 (Checklist)

在正式開始使用前，請確認以下項目：

- [ ] Java 17+ 已安裝並配置 `JAVA_HOME`
- [ ] Maven 已安裝並可執行 `mvn -version`
- [ ] MySQL 服務已啟動，資料庫 `exchange_db` 已建立
- [ ] `application.yml` 中的資料庫帳密已更新
- [ ] Google OAuth2 Client ID 和 Secret 已設定
- [ ] 應用程式成功啟動，無錯誤訊息
- [ ] 可訪問 [http://localhost:8080](http://localhost:8080)
- [ ] Google 登入功能正常運作
- [ ] 測試通過率達 97% 以上

---

### 🛠️ 常見問題排除 (Troubleshooting)

#### 問題 1：無法連線資料庫

**錯誤訊息**：`Access denied for user 'root'@'localhost'`

**解決方式**：
- 確認 MySQL 服務已啟動
- 檢查 `application.yml` 中的使用者名稱與密碼是否正確
- 確認資料庫 `exchange_db` 已建立

```sql
-- 重新授權（若有權限問題）
GRANT ALL PRIVILEGES ON exchange_db.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

---

#### 問題 2：OAuth2 登入失敗

**錯誤訊息**：`redirect_uri_mismatch`

**解決方式**：
- 確認 Google Cloud Console 中的重新導向 URI 為：
  ```
  http://localhost:8080/login/oauth2/code/google
  ```
- 確認 `client-id` 和 `client-secret` 無多餘空格

---

#### 問題 3：Maven 編譯失敗

**錯誤訊息**：`Failed to execute goal`

**解決方式**：
```bash
# 清除快取並重新編譯
mvn clean
mvn install -U

# 若仍失敗，刪除本地 Maven 儲存庫快取
rm -rf ~/.m2/repository
mvn install
```

---

#### 問題 4：Port 8080 已被佔用

**錯誤訊息**：`Port 8080 is already in use`

**解決方式**：
```yaml
# 修改 application.yml 中的 port
server:
  port: 8081  # 改為其他可用埠號
```

---

### 📞 技術支援

若遇到其他問題，請聯繫：
- **專案負責人**：張謦麒 (114423051@cc.ncu.edu.tw)

---

## 🏁 結語
本專案已完成系統測試與驗收測試，核心功能 (UC01-UC08) 運作穩定。透過標準化的軟體工程文件與測試流程，我們確保了系統具備**高可維護性**、**高安全性**與**高擴充性**，隨時準備進入生產環境部署階段 (Conditional Go)。