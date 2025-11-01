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

主要流程:
1. 使用者填寫上架表單（明星小卡專用欄位）：
   - 基本：類別、標題、描述、品況 (S/A/B/C)、缺陷說明、照片/影片。
   - 小卡資訊：團體 (idol_group)、成員 (member_name)、專輯/時期 (album/era)、版本 (version)、卡號 (card_code)、是否官方 (is_official)。
   - 交換偏好：期望交換條件（例如成員/版本/卡號）、可接受物流（僅限 賣貨便 或 面交）。
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

相關資料 / 實體:
- listings(id, owner_id, title, description, idol_group, member_name, album, era, version, card_code, is_official, condition, photos, status, locked_by_proposal_id, tags)

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

5. Given 使用者選擇物流
   When 我在表單選擇交付方式
   Then 僅能選擇「賣貨便」或「面交」，不得出現其他物流或金流選項

6. Given 小卡欄位為必填
   When 我未填團體或成員
   Then 系統回報驗證錯誤並提示補全必要欄位

7. Given 平台禁止金流
   When 我嘗試輸入價格或金額相關資訊
   Then 系統應不提供價格欄位，並在備註中提醒禁止金錢交易

備註:
- 圖片/影片存儲應計算並保存 sha256 以備爭議時佐證。
- 表單不提供金額/價格欄位；若偵測到含價格的文字內容可警示或擋回。
