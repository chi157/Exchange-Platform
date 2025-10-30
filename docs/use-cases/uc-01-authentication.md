UC-01 / 註冊與登入
========
Title: Authentication (Register / Login)
--------

優先級: Must

參與者: Guest, User

## 前置條件
- 使用者能連線網站。
- email 尚未被註冊（註冊流程）。

## 觸發條件
- 使用者在首頁點選「註冊」或「登入」。

## 主要流程
1. Guest 開啟註冊頁面（GET `/auth/register`），填寫 email、密碼、顯示名稱等欄位。
2. 系統驗證輸入格式（email 格式、密碼強度），若通過則建立 user record，password 使用 BCrypt 儲存。
3. 系統依設定發送驗證信；若驗證為必要步驟，使用者需點擊驗證信連結完成啟用。
4. 使用者使用帳密登入（POST `/auth/login`），系統建立 session 或回傳 JWT，並導向會員首頁。

## 例外 / 替代流程
- 若 email 已存在 → 系統回傳錯誤並提示使用者登入或使用忘記密碼流程。
- 若驗證信發送失敗 → 系統提示並允許重新發送驗證信。
- 若登入輸入錯誤多次 → 系統暫時鎖定帳號並要求驗證步驟（例如 captcha 或 email 驗證）。

## 後置條件 / 產出
- 使用者帳號建立於資料庫（users table），狀態視驗證流程為 VERIFIED 或 UNVERIFIED。
- 使用者取得可用權限（ROLE_USER），可進行上架、發提案等操作。

## 關聯 UI / API
- GET `/auth/register`, POST `/auth/register`
- POST `/auth/login`, POST `/auth/logout`, POST `/auth/forgot-password`, POST `/auth/reset-password`

## 相關資料 / 實體
- users(id, email, password_hash, display_name, verified, roles, created_at, last_login)

## 可度量的驗收標準（Gherkin）
1. **註冊成功**
   Given Guest 在註冊頁面
   When 我輸入新的 email 與符合強度的密碼並提交
   Then 系統回傳 201 並建立 user record，且可使用該帳號登入（若不需 email 驗證）

2. **重複註冊**
   Given email 已被註冊
   When 我嘗試使用相同 email 註冊
   Then 系統回傳 400 並顯示錯誤訊息「email 已被使用」

3. **忘記密碼流程**
   Given 使用者忘記密碼
   When 使用者提交忘記密碼請求
   Then 系統寄出重設密碼信並回傳 200

4. **登入失敗鎖定**
   Given 使用者連續登入失敗達到限制
   When 超過失敗次數
   Then 系統暫時鎖定該帳號並通知使用者以 email 完成解鎖

5. **信箱驗證成功**
   Given 使用者已註冊且 email 尚未驗證（若啟用驗證）
   When 使用者點擊驗證信中的連結
   Then 系統更新 users.verified = true 並允許進一步操作

## 備註
- 密碼使用 BCrypt 儲存。
- 建議密碼使用 BCrypt 儲存，並對新帳號限制一定時間內的交易操作以防濫用。
