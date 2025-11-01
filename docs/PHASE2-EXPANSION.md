# Phase 2 功能擴充規劃（對齊原始 SRS 與 Use Cases）

> 目的：在既有 MVP（Auth / Listings / Proposals）基礎上，分階段擴充至 SRS 規劃的核心交換流程；保持「可執行、可驗證」原則，先做最小可用，再逐步細化。
>
> 依據：`docs/software-requirements-specification.md` 與 `docs/use-cases/uc-01 ~ uc-15`。

## 核心原則

- 漸進式落地：每一里程碑（Milestone）皆可單獨釋出與測試。
- 低風險相容：沿用目前 Session 型登入；對既有 MySQL schema 採「Entity 相容映射 + 保存時補值」策略，延後 Flyway。
- 測試先行：每項新增功能均附最小整合測試（H2）與 Web 層測試。
- 文件同步：API/資料表/狀態枚舉更新，需回寫到 SRS 與對應 Use Case。

## 里程碑與範圍

- M1 搜尋/分頁（UC-03）
	- 列表 API 支援分頁、排序（時間/熱門）、關鍵字；（先）允許 title/description 關鍵字。
	- 後續再擴展小卡結構化欄位（團體、成員、版本、卡號、是否官方）。
- M2 提案查詢與清單（UC-04 補強）
	- 提供列出「我提出的提案」、「我物品收到的提案」，以及依 listing 查提案的 API。
	- Listing 鎖定策略（初版可不鎖；後續在 M3 一起處理）。
- M3 交換交易（Swap）建立（UC-06）
	- 接受提案時建立 Swap，將 Listing 標記鎖定或變更狀態（避免重複成交）。
	- Swap 結構：雙方 userId、對應的 listing/proposal 關聯、狀態（PENDING/IN_PROGRESS/COMPLETED/CANCELED）。
- M4 手動物流（Shipment）與事件（UC-07）
	- 每個 Swap 允許雙向各一筆 Shipment（A→B, B→A）；delivery_method 僅「賣貨便」或「面交」。
	- 賣貨便需 tracking_number；事件 events[]（狀態、備註、時間）手動新增。
- M5 收貨確認（UC-08）
	- 雙方分別確認收貨；雙方皆確認或逾時自動完成 → Swap 標記 COMPLETED。
- M6 評價（UC-09）
	- 完成後允許彼此留下評價（分數 + 評語）；建立使用者信譽摘要查詢。
- M7 收藏與關注（UC-11，精簡）
	- 收藏 Listing、關注使用者；提供我的收藏/關注清單，與簡單通知（先用系統事件記錄）。
- M8 黑名單與風險（UC-12，精簡）
	- 允許使用者將人/Listing 加入黑名單（僅自用）；在建立 Proposal/Swap 前檢查並阻擋。
- M9 站內訊息（UC-05，基礎）
	- 以 Proposal 或 Swap 為主線的訊息串，支援純文字與基本附件欄位（僅紀錄 URL，不做上傳）。

> UC-13（品況分級）、UC-14（自動配對）、UC-15（主題市集）留作 Phase 3 規劃。

## 資料模型變更（初版）

- listings（新增/變更）
	- status: enum { ACTIVE, LOCKED, COMPLETED }（先從 ACTIVE/LOCKED/COMPLETED 起）
	- 可拓展欄位：idol_group, member_name, album, era, version, card_code, is_official, condition, tags[]
	- 索引：owner_id, created_at, status, (title like)；後續視查詢加覆蓋索引
- proposals（補強）
	- 已相容 proposee_listing_id / receiver_id（沿用 Entity 補值策略）
	- 可加入 expires_at（先預留，不必強制）
- 新增 swaps
	- id, a_user_id, b_user_id, proposal_id?, listing_id, status, created_at, updated_at, completed_at
- 新增 shipments
	- id, swap_id, sender_id, delivery_method {shipnow|face_to_face}（實作映射為 賣貨便|面交）
	- tracking_number?, tracking_url?, last_status, shipped_at, updated_at
	- shipment_events（可獨立表，或以 JSON 儲存；初期可 TEXT JSON）
- 新增 messages
	- id, context_type {PROPOSAL|SWAP}, context_id, from_user_id, content(TEXT), attachments(TEXT JSON), created_at
- 新增 reviews
	- id, swap_id, reviewer_id, reviewed_user_id, scores(JSON {integrity,packing,speed,accuracy}), comment, created_at
- 新增 favorites / follows / blacklists（簡化）
	- favorites(user_id, listing_id, created_at)
	- follows(follower_id, followee_user_id, created_at)
	- blacklists(owner_user_id, target_user_id|listing_id, created_at)

> 註：是否引入 Flyway 以遷移表結構，待 M3/M4 定稿後評估（避免過多 rework）。

## API 草案（選摘）

- Listings（增強）
	- GET /api/listings?page=&size=&q=&sort=createdAt,desc&status=ACTIVE（頁碼 page 為 1 起算）
	- GET /api/listings/{id}
	- POST /api/listings（需登入）
	- PATCH /api/listings/{id}/status（owner 或 admin）
- Proposals（增強）
	- GET /api/proposals/mine (我提出的)
	- GET /api/proposals/received (我的物品收到的)
	- GET /api/listings/{id}/proposals
	- POST /api/proposals（已實作）
	- POST /api/proposals/{id}/accept（接受時：建立 Swap, 鎖定 Listing）
	- POST /api/proposals/{id}/reject（已實作）
- Swaps
	- GET /api/swaps/mine
	- GET /api/swaps/{id}
- Shipments（每個 Swap 各自一筆）
	- POST /api/swaps/{id}/shipments/my（建立或更新：delivery_method, tracking_number）
	- POST /api/shipments/{id}/events（新增事件：status, note, at）
- Delivery Confirmation
	- POST /api/swaps/{id}/confirm-received（我已收到）
- Reviews
	- POST /api/swaps/{id}/reviews
	- GET /api/users/{id}/reputation（彙總）
- Favorites / Follows
	- POST /api/listings/{id}/favorite, DELETE 取消, GET /api/me/favorites
	- POST /api/users/{id}/follow, DELETE 取消, GET /api/me/follows
- Blacklist
	- POST /api/users/{id}/blacklist, GET /api/me/blacklist

> 權限原則：僅本人可操作自己的資源（例如：我只可更新我作為 sender 的 Shipment）。

## 測試與驗收（每個里程碑）

- Web 層：路由、參數驗證、狀態碼。
- 整合：以 H2 驗證主流程；涵蓋未登入 401、權限 403、資源不存在 404、狀態衝突 409。
- 基本資料相容：MySQL 與 H2 下皆可跑，Legacy 欄位以 Entity 補值通過。

## 開發順序與驗收條件（摘要）

1) M1 搜尋/分頁
	 - 驗收：能以 page/size/q 取得列表；空值/邊界皆正常。
2) M2 提案查詢
	 - 驗收：能列出我提出、我收到、依 listing 的提案清單。
3) M3 Swap 建立
	 - 驗收：接受提案後，產生 Swap 並鎖定 Listing；重複接受被阻擋。
4) M4 Shipment/事件
	 - 驗收：雙方可各自建立/更新自己的 Shipment，新增事件、更新 last_status。
5) M5 收貨確認
	 - 驗收：雙方皆確認（或逾時）→ Swap 自動/手動完成。
6) M6 評價
	 - 驗收：完成後可互評，能查使用者評價摘要。
7) M7 收藏/關注
	 - 驗收：能收藏 Listing、關注使用者，列表查詢可用。
8) M8 黑名單
	 - 驗收：黑名單生效，建立提案/建立 swap 前檢查阻擋。
9) M9 訊息
	 - 驗收：可在 Proposal/Swap 下留言，列表可讀。

## 非功能與後勤

- 監控/觀測性：以日誌記錄關鍵事件（accept/reject、swap 建立、shipment 更新、完成）。
- 資料遷移：M3/M4 完成後評估導入 Flyway，並將現有 Legacy 欄位策略轉為正式遷移腳本。
- 文件：每完成一里程碑，回寫 `MVP.md` 的「後續路線圖」與 SRS Use Case 註記落地狀態。

---

若此規劃無誤，我將從 M1（搜尋/分頁）開始實作，並在每個 Milestone 完成後提 PR 與測試報告供你驗收。請回覆「同意」或提出你想優先的順序調整。