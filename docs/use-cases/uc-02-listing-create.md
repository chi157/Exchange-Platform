UC-02 / 建立交換清單（上架）
========
Title: Create Listing / Post Item
--------

優先級: Must

參與者: User, Admin

## 前置條件
- 使用者已登入。
- 使用者有權上架（未被封禁）。

## 觸發條件
- 使用者在網站點選「上架物品」或進入 /listings/new。

## 主要流程
1. 使用者填寫上架表單：類別、標題、品牌、版本、品況 (S/A/B/C)、缺陷說明、期望交換條件、可接受物流選項，並上傳照片/影片。
2. 系統驗證必填欄位、檔案型別與大小；執行內容檢測（禁止聯絡資訊/廣告）。
3. 系統建立 Listing，初始狀態為 `PENDING_REVIEW` 或 `ACTIVE`（依配置）。
4. 若需人工審核，Admin 在後台審核並批准或拒絕；系統通知上架者。

## 例外 / 替代流程
- 圖片含聯絡資訊或違規內容 → 系統標記為需修改並通知使用者。
- 上傳失敗或格式錯誤 → 返回錯誤並要求重新上傳。

## 後置條件 / 產出
- Listing 建立於資料庫，若狀態為 ACTIVE 則可被搜尋。

## 關聯 UI / API
- GET `/listings/new`, POST `/listings`
- Admin: GET `/admin/listings/pending`, POST `/admin/listings/{id}/approve`

## 相關資料 / 實體:
- listings(id, owner_id, title, description, condition, photos, status, locked_by_proposal_id)

## 可度量的驗收標準（Gherkin）
1. **建立成功**
   Given 已登入的 User
   When 我提交完整上架表單並上傳合法圖片
   Then 系統建立 Listing 並回傳 201 與 listing id

2. **圖片違規**
   Given 圖片內含電話或 email
   When 我提交上架表單
   Then 系統拒絕上架並提示「照片含聯絡資訊，請移除」

3. **需審核流程**
   Given 上架需審核的系統設定為 true
   When User 提交上架
   Then listing.status 為 PENDING_REVIEW 並顯示在 Admin 待審列表

4. **審核通過**
   Given 上架被 Admin 核准
   When Admin 執行 approve
   Then listing.status 變更為 ACTIVE 並通知上架者

## 備註
- 圖片/影片存儲應計算並保存 sha256 以備爭議時佐證。
