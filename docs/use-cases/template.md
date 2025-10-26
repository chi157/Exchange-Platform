# Use Case 標準模板

此檔為專案 Use Case（需求）之標準模板，請依此格式為每個 Use Case 建檔並填寫完整資料。模板以中文為主，欄位皆為必填（若無資料請寫 N/A）。

---

## UC-ID / 標題
- ID（格式）：UC-XX
- 標題（中文）
- Title (English)（可選）

## 優先級
- Must / Should / Could

## 參與者（Actors）
- 例：Guest、User、Admin、Shipping Carrier

## 前置條件（Preconditions）
- 列出必要的系統或資料前提（例如：使用者需登入、Listing 必須為 ACTIVE）

## 觸發條件（Trigger）
- 說明何時/誰啟動此 Use Case

## 主要流程（Main Flow）
- 以步驟列出理想/典型的成功流程，盡量清楚描述系統行為與使用者互動

## 例外 / 替代流程（Exceptions / Alternate Flow）
- 列出可能發生的例外情況與系統如何處理

## 後置條件 / 產出（Postconditions / Outputs）
- 列出此 Use Case 完成後，系統應達成的狀態或產出資料

## 關聯 UI / API
- 列出相關的頁面路由、API 端點或 webhook

## 相關資料 / 實體（Data / Entities）
- 列出會被讀寫的資料表、欄位或重要欄位（例如：listings.status, proposals.expiresAt）

## 可度量的驗收標準（Acceptance Criteria）
- 使用 Gherkin 格式（Given / When / Then），提供 3~5 條可自動化或手動驗收的範例

## 備註（Notes / Implementation Hints）
- 可選：提供實作時需注意的技術或風險要點

---

範例（簡短示範）

## UC-ID / 標題
UC-01 / 註冊與登入

## 優先級
Must

## 參與者
Guest, User

## 前置條件
使用者可連線網站，email 未被註冊

## 觸發條件
使用者在首頁點選「註冊」或「登入」

## 主要流程
1. 使用者填寫註冊表單並送出。
2. 系統驗證格式並建立帳號。
3. 系統發送驗證信（如啟用）。

## 例外
- email 已存在 → 顯示錯誤並提示使用忘記密碼

## 後置條件
帳號建立且可登入（若啟用驗證則為 UNVERIFIED）

## 關聯 UI / API
- GET `/auth/register`, POST `/auth/register`, POST `/auth/login`

## 相關資料 / 實體
- users(email, password_hash, verified, created_at)

## 可度量的驗收標準
Given Guest 在註冊頁面
When 我輸入合法 email 與密碼並提交
Then 系統回傳成功並建立 user record

## 備註
- 密碼使用 BCrypt 儲存，並限制新帳號發 proposal 的速率
