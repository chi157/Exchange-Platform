## UC-15 / 主題交換市集
Title: Themed Marketplace / Event Market

優先級: Could

參與者: User, Admin

前置條件:
- 系統支援活動分類與市集頁面，Admin 有權建立活動。

觸發條件:
- Admin 建立主題市集活動或社群成員申請建立活動。

主要流程:
1. Admin 設定主題市集（名稱、時間區間、允許的品類、活動規則）；活動頁建立並開放報名或將 Listing 標註為參加。
2. 使用者上架或在既有 listing 加入活動標籤，系統在活動期間提供專屬排序與推薦。
3. 活動期間可提供專屬消息、主題配對或獎勵機制以提高參與度。

例外 / 替代流程:
- 若使用者違反活動規則（如商品非許可品類），Admin 可移除該參與資格或退回該 listing。

後置條件 / 產出:
- 活動結束後系統統計參與數據（交易量、成功率、使用者回饋）並產生報表。

關聯 UI / API:
- GET `/marketplaces/{theme}`, POST `/marketplaces/{theme}/join`, Admin endpoints: POST `/admin/marketplaces`

相關資料 / 實體:
- marketplaces(id, title, start_at, end_at, allowed_categories, metrics)

可度量的驗收標準（Gherkin）:
1. Given Admin 建立一個主題市集並開放參與
   When 使用者將 listing 標註為參加活動
   Then 該 listing 在活動頁可見並被計入參與統計

2. Given 活動規則限定品類為 X
   When 使用者嘗試將不符類別之 listing 加入活動
   Then 系統拒絕並顯示錯誤訊息

3. Given 活動結束
   When Admin 下載活動報表
   Then 系統提供參與數、成交數、評價分佈等匯總資料

備註:
- 市集活動可作為行銷工具，建議支援活動期間的推薦加權與排序權重調整。
