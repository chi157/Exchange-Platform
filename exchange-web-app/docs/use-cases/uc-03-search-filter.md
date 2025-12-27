## UC-03 / 搜尋與篩選
Title: Search and Filter Listings

優先級: Must

參與者: Guest, User

前置條件:
- 系統有已上架 (ACTIVE) 的 listings。

觸發條件:
- 使用者在首頁或 /listings 輸入關鍵字或選擇篩選條件。

主要流程:
1. 使用者輸入關鍵字與/或選擇篩選條件：
   - 小卡欄位：團體 (idol_group)、成員 (member_name)、專輯/時期 (album/era)、版本 (version)、卡號 (card_code)、是否官方 (is_official)。
   - 其他：品況、地區、物流（僅顯示「交貨便 / 面交」）。
2. 系統根據條件在 listings 資料表查詢並回傳分頁結果、排序與聚合資訊（總數、分頁）。
3. 使用者可點進任一 listing 查看詳情。

例外 / 替代流程:
- 無符合結果 → 系統顯示「無符合項目」並建議相近搜尋或加入通知/關注。

後置條件 / 產出:
- 無資料變更；回傳搜尋結果頁面或 JSON。

關聯 UI / API:
- GET `/listings?query=&idol_group=&member=&album=&version=&card_code=&official=&condition=&shipping=&page=`

相關資料 / 實體:
- listings (status = ACTIVE), index: (idol_group, member_name, album, era, version, card_code, is_official, condition, location, delivery_options)

可度量的驗收標準（Gherkin）：
1. Given 有多筆 listings
   When 我以團體 = "ABC" 與成員 = "張三" 且品況 = S 篩選
   Then 回傳的每個 listing 皆為團體 ABC、成員 張三，且品況為 S

2. Given 無任何 listings 滿足條件
   When 我執行搜尋
   Then 系統回傳空結果並顯示建議資訊

3. Given 有超過一頁結果
   When 我要求 page=2
   Then 系統回傳第二頁之 listings 並包含分頁 metadata

備註:
- 搜尋可使用簡易全文索引（Postgres tsvector 或外部 ElasticSearch）以提升效能。
