# 機能カタログ

Cele-Tweaks の全 25 機能の仕様です。すべて既定 OFF。

凡例
- 種別: **V** = Visual（自分の画面の描画だけを変える）、**G** = Guard（しようとした操作を止めるだけで、操作を追加しない）
- 区分: **Core** = 基盤と同時に検証する機能、**Vanilla** = バニラだけで動く、**Integration** = 外部 MOD があるときだけ動く
- ID リストは `minecraft:stone` 形式の ID と `#minecraft:logs` 形式のタグを受け付ける

## Focus

| 機能 | 種別 | 区分 | 動作 | 主な設定 |
| --- | :-: | --- | --- | --- |
| Scoreboard Panel | V | Core | サイドバーの目標を、9 方向のアンカー＋ピクセルオフセットで置くパネルとして描く。ページ送り（ホットキー）、ページ表示、拡大縮小、背景の不透明度 | anchor, offsetX/Y, scale, rowsPerPage, backgroundOpacity, showTitle, showValues, showPageIndicator |
| Quiet UI | V | Core | すべてのチームを同じ灰色で表示 | — |
| Quiet Chat | V | Vanilla | チャットの各色を、同じ明るさ（輝度）の灰色に変換。強弱は残り、色相のノイズだけ消える | — |

## Safety

| 機能 | 種別 | 区分 | 動作 | 主な設定 |
| --- | :-: | --- | --- | --- |
| Portal Frame Guard | G | Vanilla | ポータルの向き（軸）を見て、同じ面にある枠の黒曜石だけを破壊不可にする | — |
| Portal Face Guard | G | Vanilla | ポータル開口部の手前・奥（通り抜ける 2 マス）と、ポータル自体へのブロック設置を止める | — |
| Footing Guard | G | Vanilla | 接地して実際に水平移動している間、自分の当たり判定の真下にあるブロックの破壊を止める。スニーク中は解除 | minSpeedHundredths, allowWhileSneaking |
| Item Use Guard | G | Vanilla | 規則 `アイテム: 対象ブロック…` に当てはまるアイテムは、列挙したブロックにしか使えない。規則の無いアイテムは影響なし | rules |
| Craft Burst Guard | G | Integration（Item Scroller） | プレイヤーインベントリでの一括クラフトに Shift 押下を必須にする | — |
| Shared Plan Lock | G | Integration（Syncmatica） | 共有設置の削除ボタンを無効化、または Shift 必須にする | mode |

## Inventory

| 機能 | 種別 | 区分 | 動作 | 主な設定 |
| --- | :-: | --- | --- | --- |
| Hotbar Lock | G | Vanilla | ロックした枠を Pick Block で上書きさせない。空いている未ロック枠へ振り替え、選択をサーバーへ同期。無ければ中止。ホットキーで現在の枠をロック／解除 | lockedSlots |

## Building

| 機能 | 種別 | 区分 | 動作 | 主な設定 |
| --- | :-: | --- | --- | --- |
| Precision Aim | V | Vanilla | 専用キーを押している間だけ、マウス感度を自分の設定値の指定 % にする。離す・切断・無効化で戻す | factorPercent |
| Site Memo | V | Vanilla | 64 行のローカルメモ。ホットキーか `/celetweaks memo` で開く | — |
| Action Log | V | Vanilla | 叩く・使う・エンティティ操作を、時刻・ディメンション付きでメモリ内に記録。`/celetweaks log [ページ]`、`/celetweaks log clear` | capacity |
| Beacon / Lightning Rod Range | V | Vanilla | 近くの読み込み済みチャンクのビーコン（段数はバニラと同じ基礎ブロック判定）と避雷針を一定間隔で探し、範囲をバニラ Gizmo で描く | showBeacons, showLightningRods, 各半径, 色, rescanIntervalTicks |

## Rendering

| 機能 | 種別 | 区分 | 動作 | 主な設定 |
| --- | :-: | --- | --- | --- |
| Entity Filter | V | Vanilla | ID / タグでエンティティを隠す、またはそれだけ表示。プレイヤーは既定で常に表示 | mode, entries, alwaysShowPlayers |
| Block Filter | V | Vanilla | ID / タグでブロックを隠す、またはそれだけ表示。変更時にチャンク再描画 | mode, entries |
| Lava Highlight | V | Vanilla | 溶岩源に色を掛け、流れる溶岩を暗くする。Sodium 導入中は表示されない | sourceTint, flowingBrightnessPercent |

## Compatibility

| 機能 | 種別 | 区分 | 動作 | 主な設定 |
| --- | :-: | --- | --- | --- |
| Tool Switch Scope | G | Integration（Tweakeroo） | 自動工具切替を、指定ブロックのみ／指定以外のみに制限。指定アイテムを持っている間は切り替えない | scope, blocks, keepInHand |
| Tweakeroo Resync | V | Integration（Tweakeroo） | 参加後、指定した Tweakeroo 設定のうち ON のものの変更処理を再実行（既定: ガンマ上書き）。値は変えない | toggles |
| Auto Pick Scope | G | Integration（TweakerMore） | 設計図からの自動ピックが、Hotbar Lock のロック枠と指定アイテムを置き換えないようにする | respectHotbarLock, keepItems |
| Syncer Tune | V | Integration（TweakerMore） | 周囲のブロックエンティティ数が enter 閾値を超えたらサーバーデータ同期を緩め、exit 閾値を下回ったら利用者の元の値へ戻す（ヒステリシス） | chunkRadius, enter/exitThreshold, relaxed値 |
| Placement Conversions | G | Integration（Litematica + TweakerMore） | 配置制限に、道具による変換（`ワールド -> 設計図 with 道具`、組み込みで斧による皮剥ぎ）と代用ブロック（`設計図 = 置くブロック`）を認めさせる。判定を緩める方向にしか作用しない | builtInAxeStripping, conversions, standIns |
| Material Substitution | G | Integration（Litematica） | 要求素材を持っていないとき、同じ代用グループの手持ちアイテムを使う。組み込みで `dead_` 付きと無しの組を代用可能にする | groups, builtInDeadVariants |
| Material List Refresh | V | Integration（Litematica） | 素材一覧を再計算させるホットキー | — |
| Schematic Drop | G | Integration（Litematica） | 設計図ブラウザへのファイルドロップを、表示中のフォルダへコピー。拡張子・サイズ・シンボリックリンク・上書き禁止を検証 | maxFileSizeMiB |
