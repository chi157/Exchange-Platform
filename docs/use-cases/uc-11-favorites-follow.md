## UC-11 / 收藏與關注
Title: Favorites and Follow

優先級: Should

參與者: User

前置條件:
- 使用者已登入。

觸發條件:
- 使用者在 Listing 或使用者頁面點選「收藏」或「追蹤」。

主要流程:
1. 使用者點擊收藏按鈕，系統在 favorites 資料表新增紀錄（user_id, listing_id）。
2. 使用者可於「我的收藏」頁查看、排序或取消收藏。
3. 系統可依收藏觸發通知（例如該 listing 有更新或參與活動）。

例外 / 替代流程:
- 若 listing 已下架或已交換，系統自動在收藏中標示為不可用或移除。

後置條件 / 產出:
- favorites 資料變更；若觸發通知則發送 email / 站內通知。

關聯 UI / API:
- POST `/users/{id}/favorites`, GET `/users/{id}/favorites`

相關資料 / 實體:
- favorites(user_id, listing_id, created_at)

可度量的驗收標準（Gherkin）:
1. Given 使用者在 listing/detail 頁
   When 點擊「收藏」
   Then 該 listing 加入使用者收藏並可於「我的收藏」看到

2. Given listing 下架
   When 使用者檢視收藏清單
   Then 下架項目顯示為不可用並提供移除選項

3. Given 使用者取消收藏
   When 執行取消操作
   Then favorites 資料移除並回傳成功

備註:
- 收藏機制可作為推薦/通知的來源，用於提升留存。
