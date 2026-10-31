# 機能カタログ

Cele-Tweaks の全 25 機能の仕様です。すべて既定 OFF。

凡例
- 種別: **V** = Visual（自分の画面の描画だけを変える）、**G** = Guard（しようとした操作を止めるだけで、操作を追加しない）
- 区分: **Core** = 基盤と同時に検証する機能、**Vanilla** = バニラだけで動く、**Integration** = 外部 MOD があるときだけ動く
- ID リストは `minecraft:stone` 形式の ID と `#minecraft:logs` 形式のタグを受け付ける
- 括弧内は設定ファイルとホットキーで使う機能 ID

## 誤操作防止（Guards）

| 機能 | 種別 | 区分 | 動作 | 主な設定 |
| --- | :-: | --- | --- | --- |
| ポータル枠の保護 / Portal Frame Protection（`portal_frame_protect`） | G | Vanilla | ポータルの向き（軸）を見て、同じ面にある枠の黒曜石だけを破壊不可にする | — |
| ポータル出入口の確保 / Clear Portal Doorway（`portal_doorway_clear`） | G | Vanilla | ポータル開口部の手前・奥（通り抜ける 2 マス）と、ポータル自体へのブロック設置を止める | — |
| 足元の掘削防止 / No Digging Underfoot（`no_dig_underfoot`） | G | Vanilla | 接地して実際に水平移動している間、自分の当たり判定の真下にあるブロックの破壊を止める。スニーク中は解除 | minSpeedHundredths, allowWhileSneaking |
| アイテム使用ガード / Item Use Guard（`item_use_guard`） | G | Vanilla | 規則 `アイテム: 対象ブロック…` に当てはまるアイテムは、列挙したブロックにしか使えない。規則の無いアイテムは影響なし | rules |
| ホットバーロック / Hotbar Lock（`hotbar_lock`） | G | Vanilla | ロックした枠を Pick Block で上書きさせない。空いている未ロック枠へ振り替え、選択をサーバーへ同期。無ければ中止。ホットキーで現在の枠をロック／解除 | lockedSlots |
| Shiftで一括クラフト / Shift to Mass Craft（`shift_to_mass_craft`） | G | Integration（Item Scroller） | プレイヤーインベントリでの Item Scroller の一括クラフトに Shift 押下を必須にする。止めるのは一括クラフトの処理だけで、Item Scroller の他の操作（クリック送信など）は止めない | — |
| Syncmatica削除ロック / Syncmatica Remove Lock（`syncmatica_remove_lock`） | G | Integration（Syncmatica） | 共有設置の削除ボタンを無効化、または Shift 必須にする | mode |

## ユーティリティ（Utility）

| 機能 | 種別 | 区分 | 動作 | 主な設定 |
| --- | :-: | --- | --- | --- |
| 低速視点 / Slow Look（`slow_look`） | V | Vanilla | 専用キーを押している間だけ、マウス感度を自分の設定値の指定 % にする。離す・切断・無効化で戻す | factorPercent |
| テキストノート / Text Notes（`text_notes`） | V | Vanilla | 64 行のローカルなテキストノート。ホットキーか `/celetweaks notes` で開く。本文は `config/celetweaks/notes.txt` | — |
| 行動ログ / Action Log（`action_log`） | V | Vanilla | 叩く・使う・エンティティ操作を、時刻・ディメンション付きでメモリ内に記録。`/celetweaks log [ページ]`、`/celetweaks log clear` | capacity |

## 画面表示（HUD）

| 機能 | 種別 | 区分 | 動作 | 主な設定 |
| --- | :-: | --- | --- | --- |
| サイドバー配置 / Sidebar Layout（`sidebar_layout`） | V | Core | サイドバーの目標を、9 方向のアンカー＋ピクセルオフセットで置くパネルとして描く。ページ送り（ホットキー）、ページ表示、拡大縮小、背景の不透明度 | anchor, offsetX/Y, scale, rowsPerPage, backgroundOpacity, showTitle, showValues, showPageIndicator |
| チーム色のグレー化 / Grey Team Colors（`grey_teams`） | V | Core | すべてのチームを同じ灰色で表示 | — |
| チャット色のグレー化 / Grey Chat Colors（`grey_chat`） | V | Vanilla | チャットの各色を、同じ明るさ（輝度）の灰色に変換。強弱は残り、色相のノイズだけ消える | — |

## ワールド表示（World View）

| 機能 | 種別 | 区分 | 動作 | 主な設定 |
| --- | :-: | --- | --- | --- |
| ビーコン／避雷針範囲 / Beacon / Lightning Rod Range（`range_overlay`） | V | Vanilla | 近くの読み込み済みチャンクのビーコン（段数はバニラと同じ基礎ブロック判定）と避雷針を一定間隔で探し、範囲をバニラ Gizmo で描く | showBeacons, showLightningRods, 各半径, 色, rescanIntervalTicks |
| エンティティフィルター / Entity Filter（`entity_filter`） | V | Vanilla | ID / タグでエンティティを隠す、またはそれだけ表示。プレイヤーは既定で常に表示 | mode, entries, alwaysShowPlayers |
| ブロックフィルター / Block Filter（`block_filter`） | V | Vanilla | ID / タグでブロックを隠す、またはそれだけ表示。変更時にチャンク再描画 | mode, entries |
| 溶岩源の色付け / Lava Source Tint（`lava_source_tint`） | V | Vanilla | 溶岩源に色を掛け、流れる溶岩を暗くする。Sodium 導入中は表示されない | sourceTint, flowingBrightnessPercent |

## 他MOD連携（Mod Links）

| 機能 | 種別 | 区分 | 動作 | 主な設定 |
| --- | :-: | --- | --- | --- |
| 工具切替スコープ / Tool Switch Scope（`tool_switch_scope`） | G | Integration（Tweakeroo） | 自動工具切替を、指定ブロックのみ／指定以外のみに制限。指定アイテムを持っている間は切り替えない | scope, blocks, keepInHand |
| Tweakeroo再同期 / Tweakeroo Resync（`tweakeroo_resync`） | V | Integration（Tweakeroo） | 参加後、指定した Tweakeroo 設定のうち ON のものの変更処理を再実行（既定: ガンマ上書き）。値は変えない | toggles |
| 自動ピックスコープ / Auto Pick Scope（`auto_pick_scope`） | G | Integration（TweakerMore） | 設計図からの自動ピックが、ホットバーロックのロック枠と指定アイテムを置き換えないようにする | respectHotbarLock, keepItems |
| 同期チューニング / Syncer Tune（`syncer_tune`） | V | Integration（TweakerMore） | 周囲のブロックエンティティ数が enter 閾値を超えたらサーバーデータ同期を緩め、exit 閾値を下回ったら利用者の元の値へ戻す（ヒステリシス） | chunkRadius, enter/exitThreshold, relaxed値 |
| 配置変換ルール / Placement Conversions（`placement_conversions`） | G | Integration（Litematica + TweakerMore） | 配置制限に、道具による変換（`ワールド -> 設計図 with 道具`、組み込みで斧による皮剥ぎ）と代用ブロック（`設計図 = 置くブロック`）を認めさせる。判定を緩める方向にしか作用しない | builtInAxeStripping, conversions, standIns |
| 素材代用 / Material Substitution（`material_substitution`） | G | Integration（Litematica） | 要求素材を持っていないとき、同じ代用グループの手持ちアイテムを使う。組み込みで `dead_` 付きと無しの組を代用可能にする | groups, builtInDeadVariants |
| 素材リストの再集計 / Material List Recount（`material_list_recount`） | V | Integration（Litematica） | 素材リストを再集計させるホットキー | — |
| 設計図のドラッグ＆ドロップ / Schematic Drag & Drop（`schematic_drag_drop`） | G | Integration（Litematica） | 設計図ブラウザへのファイルドロップを、表示中のフォルダへコピー。拡張子・サイズ・シンボリックリンク・上書き禁止を検証 | maxFileSizeMiB |

## 自動テストで確かめている挙動

CI の client game test（`CeleGuardGameTest`）が、3 版それぞれで次を確かめる。どれも、ガードが効くべき操作と、効いてはいけない対照の操作を同じ手順で行い、結果をサーバー側のワールドから読む（対照が失敗したら、テストの操作自体が空振りしたとみなして失敗にする）。

| 機能 | 止めること | 対照（止めてはいけないこと） |
| --- | --- | --- |
| ポータル枠の保護 | 稼働中ポータルの枠の黒曜石を壊す | ポータルと無関係な黒曜石は壊せる |
| ポータル出入口の確保 | ポータル開口部の手前にブロックを置く | 開けた地面には置ける |
| アイテム使用ガード | 規則 `minecraft:cobblestone: minecraft:stone` に反して、丸石を土の上に置く | 石の上には置ける |
| ホットバーロック | ホットバーが満杯でロック枠を選択中の Pick Block が、その枠を上書きする | ロックを外すと同じ Pick Block で上書きされる |
| 足元の掘削防止 | 移動中に足元のブロックを壊す | 止まっていれば壊せる |

画面表示・描画系の機能は、例外が出ないことと起動時のスクリーンショットまでで、見た目の正しさは判定していない。

## 連携先の確認状況

- 連携先のクラス・メソッドは、各 MOD の 26.1.2 / 26.2 / 26.3 向けソースで実在を確認済み。
- CI の `integration` ジョブは、Modrinth の最新 Fabric ビルドを入れて起動し、連携用 Mixin 8 個すべてが相手のメソッドにフックしたことを毎回確かめる。下の表は 2026-10-31 に通った版。

| MOD | 26.1.2 | 26.2 | 26.3 | 接続先 |
| --- | --- | --- | --- | --- |
| MaLiLib | 0.28.12 | 0.29.6 | 0.30.2 | （下の MOD の前提） |
| Tweakeroo | 0.28.10 | 0.29.5 | 0.30.1 | `InventoryUtils.trySwitchToEffectiveTool(BlockPos)`、`FeatureToggle` |
| Litematica | 0.27.14 | 0.28.8 | 0.29.1 | `MaterialCache.getRequiredBuildItemForState*`、`DataManager`、`MaterialListBase.refreshPreFilteredList()`、`GuiSchematicBrowserBase` |
| Item Scroller | 0.31.6 | 0.32.2 | 0.33.1 | `KeybindCallbacks.onClientTickMassCraftImpl(Minecraft)` |
| TweakerMore | 3.33.2 | 3.33.2 | 3.33.3-beta.1 | `PlacementRestrictor` / `LooseCaseChecker` / `BlockInteractionRestrictor` / `SchematicBlockPicker`、`TweakerMoreConfigs.SERVER_DATA_SYNCER_QUERY_*` |
| Syncmatica | 0.3.20 | 0.3.20 | 0.3.20 | `WidgetSyncmaticaServerPlacementEntry$ButtonListener.actionPerformedWithButton` |

フックしたことまでは CI で分かるが、連携機能の判定の中身（何を止め、何を通すか）が意図どおりかは、実際に遊んで確かめる必要がある。
