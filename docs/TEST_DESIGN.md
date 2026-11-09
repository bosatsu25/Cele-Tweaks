# Cele-Tweaks テスト設計書

JSTQB Foundation Level シラバス v4.0（以下「シラバス」）の用語と技法で、Cele-Tweaks のテストを整理したもの。シラバスは技法の出典であって要求仕様ではない。テストベース（シラバス 1.4.3）は [FEATURE_CATALOG.md](FEATURE_CATALOG.md) の仕様と、機能クラスの実装そのものである。

節番号の参照（例: §4.2.1）はシラバスの節を指す。

---

## 1. テスト方針

### 1.1 テストレベル（§2.2.1）とテストピラミッド（§5.1.6）

| レベル | 何を | どこで | 1 回の所要 | 実行者 |
| --- | --- | --- | --- | --- |
| コンポーネントテスト（単体） | ルールの解析、ID のマッチング、判定ロジック、状態遷移、設定ファイルの読み書き | `src/test`（JUnit 5、`./gradlew :<mc>:test`） | 数十秒 | CI（3 版）と開発者 |
| コンポーネント統合テスト | Mixin が実クラスに織り込まれ、Feature が実世界へ効くこと | `src/gametest`（client game test、実クライアントを起動） | 約 3 分 | CI の `build` ジョブ（3 版） |
| システム統合テスト | 連携 MOD の実配布 JAR に対する判定の変化 | 同上、`-Pintegration_mods` 付き | 約 3 分 | CI の `integration` ジョブ（3 版） |
| 受け入れテスト | 見た目、ホットキーの操作感、実プレイでの違和感 | 手動 | — | 利用者 |

下ほど遅く、高く、不安定になる。だから **判定ロジックの網羅は単体で**、**ゲームとの接続の確認はゲームテストで 1 経路ずつ** という分担にする。ゲームテストに判定の全組み合わせを持ち込まない。

### 1.2 テストの原則（§1.3）の適用

- **欠陥の偏在**: この MOD で人が書き間違えやすいのは、設定として打ち込むルール文字列の解析と ID のマッチングである。単体テストはそこに集中させる。
- **殺虫剤のパラドックス / 「エラーがない」は幻想**: 止めるべき操作を止めたことだけでなく、**止めてはいけない対照の操作が通ったこと** を必ず同じテストで確かめる。対照が失敗したら、テストの操作自体が空振りしたとみなす（ゲームテストの既存方針。単体でも「ルールなし → 止めない」の行を必ず置く）。
- **テストはコンテキスト次第**: クライアント専用 MOD なので、サーバー側や通信の正しさは対象外。

### 1.3 リスクベース（§5.2）

| プロダクトリスク | 影響 | 可能性 | 対応 |
| --- | --- | --- | --- |
| 誤操作防止系が止めるべき操作を止めない | 高（ポータル枠や足場を壊す） | 中 | 判定を単体で網羅、ゲームテストで経路を確認 |
| 誤操作防止系が止めてはいけない操作を止める | 高（建築ができなくなる） | 中 | すべての判定テストに対照の行を置く |
| ルール文字列の解析違い（区切り、名前空間、タグ） | 中（ルールが黙って無効になる） | 高 | 同値分割と境界値で解析器を単体テスト |
| 連携 MOD の改名でフックが外れる | 中（機能が黙って無効になる） | 中（版ごと） | フック数の計測を単体でテストし、計測結果を integration ジョブで検査 |
| 設定ファイルの破損で起動しない | 高 | 低 | fail-soft の挙動を単体テスト |
| 翻訳キーの欠落 | 低（設定画面に生キーが出る） | 高（機能追加のたび） | チェックリストベースの単体テスト |

### 1.4 開始基準と終了基準（§5.1.3）

- 開始: 対象の 3 版すべてで `build` が通ること。
- 終了: 単体テストが 3 版すべてで合格、ゲームテストが 3 版 × 2 ジョブで合格。リリース（`release.yml`）はこれを前提にしている。

---

## 2. トレーサビリティ（§1.4.4）

機能（テストベース）から、どのテストがどの側面を確かめているかの対応。「—」はそのレベルのテストがない。

| 機能 | 単体テスト（判定・解析） | ゲームテスト（接続） |
| --- | --- | --- |
| アイテム使用ガード | `ItemUseGuardRulesTest` | `CeleGuardGameTest.itemUse` |
| ホットバーロック | `HotbarLockSlotsTest` | `CeleGuardGameTest.hotbarLock` |
| ポータル枠の保護 / 出入口の確保 | —（Level が必要） | `CeleGuardGameTest.portalFrame` / `portalDoorway` |
| 足元の掘削防止 | —（Player が必要） | `CeleGuardGameTest.noDigUnderfoot` |
| ゆっくり視点 | `SlowLookScaleTest` | 起動時に設定画面を開くのみ |
| 配置変換ルール | `PlacementConversionRulesTest` | `CeleIntegrationGameTest.placementConversions` |
| 同期チューニング | `LoadHysteresisTest` | `CeleIntegrationGameTest.syncerTune` |
| 工具切替スコープ / 自動ピックスコープ | `IdMatcherTest`、`HotbarLockSlotsTest`（部品） | `CeleIntegrationGameTest.toolSwitchScope` / `autoPickScope` |
| ブロック / エンティティフィルター | `IdMatcherTest`、`ListModeTest`（部品） | Block Filter の OFF→ON のみ |
| 連携 Mixin 全般 | `HookCountTest` | `CeleClientGameTest.checkIntegrationHooks` |
| 設定の読み書き | `JsonFilesTest` | 起動段 `config` の成否 |
| ホットキー | `KeyComboTest` | — |
| 機能一覧（翻訳・ID・階層） | `FeatureCatalogTest` | 全機能の設定画面を開く |

---

## 3. テスト設計

### 3.1 同値分割法（§4.2.1）

カバレッジアイテムは同値パーティション。有効・無効の両方を少なくとも 1 回ずつ通す。複数のパーティション集合がある場合はイーチチョイスカバレッジ。

**`Ids.normalize`（ID 文字列 → 正規形）** — `IdsTest`

| パーティション | 代表値 | 期待 |
| --- | --- | --- |
| 有効: 名前空間付き | `minecraft:stone` | そのまま |
| 有効: 名前空間なし | `stone` | `minecraft:stone` |
| 有効: 前後に空白 | `  stone  ` | `minecraft:stone` |
| 有効: 他 MOD の名前空間 | `tweakermore:some_thing` | そのまま |
| 無効: null / 空 / 空白のみ | `null`, `""`, `"   "` | null |
| 無効: 大文字 | `Stone`, `minecraft:Stone` | null |
| 無効: 内部の空白 | `mine craft:stone`, `minecraft:stone block` | null |
| 無効: タグ記号 | `#minecraft:logs` | null（タグは `IdMatcher` の担当） |
| 無効: コロンが 2 つ | `a:b:c` | null |

**`IdMatcher.of` / `parse`（ID リスト → マッチャー）** — `IdMatcherTest`

| パーティション | 代表値 | 期待 |
| --- | --- | --- |
| リストなし / 空 / 空白のみ | `null`, `[]`, `"   "` | 共有の `EMPTY`（同一インスタンス） |
| ID と無効要素の混在 | `[null, "", "Bad Id", "#bad id", "stone"]` | 無効要素は捨て、`stone` にだけ一致 |
| 無効要素のみ | `["Bad Id", "#bad id"]` | 空、何にも一致しない |
| 任意の空白区切り（`parse`） | `" stone \t minecraft:dirt\ncobblestone "` | 3 つに一致 |
| 同じ ID のブロック / アイテム / エンティティ | `["minecraft:stone", "pig"]` | それぞれの型で一致、別 ID は不一致 |
| 空のアイテムスタック / null | `ItemStack.EMPTY`, `null` | 不一致（`minecraft:air` を列挙していても） |
| タグ | `["#minecraft:logs"]` | 空ではない。データ未ロードでも例外を出さない |

タグの **所属判定** はゲームのデータロード後にしか意味を持たないので、単体では「解析されること」「例外を出さないこと」までとし、タグ経由の一致はゲームテストに委ねる（§5 参照）。

**ルール行の解析** — `ItemUseGuardRulesTest`、`PlacementConversionRulesTest`

| 構文 | 有効の代表 | 無効のパーティション |
| --- | --- | --- |
| `item_or_#tag: block_or_#tag ...`（最初の `": "` で分割） | `minecraft:bone_meal: #minecraft:saplings minecraft:grass_block` | 区切りなし（`cobblestone:stone`、`cobblestone :stone`、ID 単独）、null、空 |
| 右辺が空の行 | `cobblestone: ` | 有効だが「そのアイテムはどこにも使えない」という意味になることを確認 |
| `schematic = placeable` | ` stone = minecraft:cobblestone `、`minecraft:stone=cobblestone` | `=` なし、片側が無効 ID、片側が空 |
| `world -> schematic with tool` | ` oak_log birch_log -> stripped_oak_log with iron_axe minecraft:stone_axe ` | `->` なし、` with ` なし、` with ` が `->` より前、ターゲットが無効 ID、ターゲットが空 |
| 複数行の compile | 有効行は順序を保つ | 無効行と null は捨てる。null リストは空 |

**`JsonFiles`（設定ファイルの fail-soft）** — `JsonFilesTest`

| ディスクの状態 | 期待 |
| --- | --- |
| ファイルなし | 空オブジェクト。ファイルは作らない |
| JSON オブジェクト | そのまま読める。未知のキーも保持 |
| 有効な JSON だがオブジェクトでない（`[1,2,3]`） | 空オブジェクトを返し、元ファイルを `*.broken-<時刻>` へ退避（内容はそのまま、消さない） |
| JSON でない文字列 | 同上 |
| 書き込み | 親ディレクトリを作る。置き換えであって結合ではない。一時ファイルを残さない |

### 3.2 境界値分析（§4.2.2、3 値 BVA）

順序性のあるパーティションに対し、境界値とその両隣を通す。

**ホットバー枠番号（`HotbarLockFeature.parseLockedSlots`）** — `HotbarLockSlotsTest`

| 入力 | 0 | **1** | 2 | 8 | **9** | 10 |
| --- | --- | --- | --- | --- | --- | --- |
| 期待（0 始まりの枠） | なし | 0 | 1 | 7 | 8 | なし |

加えて経験ベース（§3.6）で、区切り記号の種類（空白・`,`・`;`）、重複、int に収まらない数、数字を含まない文字列、null を扱う。

**感度の倍率（`SlowLookFeature.scaledSensitivity`、1〜100 %）** — `SlowLookScaleTest`

| factorPercent | 0 | **1** | 2 | 99 | **100** | 101 |
| --- | --- | --- | --- | --- | --- | --- |
| 元 0.5 のとき | 0.005（1 % 扱い） | 0.005 | 0.010 | 0.495 | 0.5 | 0.5（100 % 扱い） |

結果側の境界: 元が 1.0 / 0.0 のとき範囲内に留まる。範囲外の元（1.5、-0.5）は 0〜1 に戻す。

**同期チューニングの閾値（`LoadHysteresis`）** — `LoadHysteresisTest`

- `enter` の下限 1: `enter=0` のとき負荷 0 では緩めず、負荷 1 で緩める。負の値も 1 扱い。
- `exit` は `enter` を超えない: `enter=4, exit=10` なら実効 `exit=4`。負荷 4 で緩み、4 では維持、3 で戻す。
- `exit` の下限 0: `exit=-5` なら負荷 0 でも戻らず、`reset` だけが戻す。

### 3.3 デシジョンテーブルテスト（§4.2.3）

**`ListMode.affects`** — `ListModeTest`（完全表、4 列）

| モード | リストに含まれる | 影響を受ける |
| --- | --- | --- |
| HIDE | T | T |
| HIDE | F | F |
| ALLOW | T | F |
| ALLOW | F | T |

**アイテム使用ガードの判定 `ItemUseGuardFeature.blocks`** — `ItemUseGuardRulesTest`（制限指定、実行不可能な列を除いた最小形）

| | R1 | R2 | R3 | R4 |
| --- | --- | --- | --- | --- |
| 手が空 | T | F | F | F |
| いずれかのルールがそのアイテムを対象にする | - | F | T | T |
| 対象にするルールのどれかが、そのブロックを列挙している | - | - | T | F |
| **止める** | | | | X |

追加の列: 同じアイテムに複数のルールがあるとき、どれか 1 つが列挙していれば通す（R3 の「どれか」の確認）。

**配置変換ルールの判定** — `PlacementConversionRulesTest`

- 代用（`standsIn`）: 設計図ブロックと置くブロックの **順序付き** 完全一致。逆順は不一致、片方が違えば不一致、ルールなしは不一致。
- 変換（`converts`、組み込みの斧剥ぎを OFF にして）: ワールドブロック・ターゲット・工具のうち 1 つでも違えば不一致、手が空なら不一致、ルールなしなら不一致。3 条件がすべて T の列だけが X。

### 3.4 状態遷移テスト（§4.2.4）

**`LoadHysteresis`（同期チューニングのスイッチ）** — `LoadHysteresisTest`

状態: NORMAL、RELAXED。イベント: `sample(load)`（ガード条件は閾値との比較）、`reset`。`enter=4, exit=2` で試験。

| 状態 ＼ イベント | load ≥ enter | exit ≤ load < enter | load < exit | reset |
| --- | --- | --- | --- | --- |
| NORMAL | → RELAXED / RELAX | （無効）維持 / なし | （無効）維持 / なし | （無効）維持 / なし |
| RELAXED | （無効）維持 / なし | （無効）維持 / なし | → NORMAL / RESTORE | → NORMAL / RESTORE |

カバレッジ: 全状態、有効遷移（3 本）、**全遷移**（無効な 5 セルを 1 セルずつ別テストで踏み、欠陥のマスキングを避ける）。さらに RELAX→維持→RESTORE→維持→RELAX の一巡で、副作用が各遷移で 1 回だけ要求されることを確かめる。`exit ≤ load < enter` の帯で状態が保たれることが、この部品の存在理由（ヒステリシス）である。

### 3.5 ホワイトボックス: ブランチテスト（§4.3.2）

**`IntegrationMixinPlugin.countHandlerCalls`** — `HookCountTest`

命令ごとの判定の各ブランチを通す。

| 命令 | owner が対象クラス | 名前がハンドラ | 数える |
| --- | --- | --- | --- |
| 対象クラスの `celetweaks$guard` | T | T（そのまま） | 1 |
| 対象クラスの `handler$zza000$celetweaks$guard` | T | T（Mixin の改名形） | 1 |
| 別クラスの `celetweaks$guard` | F | T | 0 |
| 対象クラスの `celetweaks$other` / `guard` / `tick` | T | F | 0 |
| 呼び出しのないクラス | — | — | 0 |

複数メソッド・複数ハンドラにまたがる合計も 1 例で確認する。CI はこの数が 0 のフックを「黙って無効化された機能」として失敗にしているので、この計測器の正しさが integration ジョブの前提になる。

### 3.6 経験ベースのテスト技法（§4.4）

**エラー推測（§4.4.1）**: 入力に関する典型的な欠陥を、各解析器に対して置く。

- null、空文字、空白のみ（全解析器）
- 区切り記号の揺れ（`1,5;9`、`cobblestone :stone`、ID 内のコロン）
- 整数のオーバーフロー（`9999999999999` → `NumberFormatException` を握りつぶして無視すること）
- 重複（`3 3`）
- 配列の後からの改変（`KeyCombo` が呼び出し側の配列をコピーしていること）
- 順序依存（`KeyCombo` の等価性とハッシュが順序に依らないこと。`L+K` が `L+K+X` で発火しないこと）

**チェックリストベースドテスト（§4.4.3）**: 機能を追加するときに人が忘れることのリスト。`FeatureCatalogTest`。

- [ ] 機能数が文書どおり（README / FEATURE_CATALOG の「25」）
- [ ] ID が重複せず、小文字スネークケース
- [ ] INTEGRATION 階層の機能だけが必要 MOD を持つ
- [ ] 全機能の名前・説明と全カテゴリ名が `en_us` と `ja_jp` の両方にある
- [ ] 2 つの言語ファイルのキー集合が一致する

**探索的テスト（§4.4.2）**: 描画・画面表示・ホットキーの操作感は、自動テストでは例外が出ないことまでしか確かめない。リリース前に利用者がセッションベースで行う（[DESIGN.md](DESIGN.md) §8）。

---

## 4. ゲームテストとの分担

単体テストに持ち込めない（持ち込むべきでない）もの。

| 対象 | 単体にできない理由 | 代わりに |
| --- | --- | --- |
| タグ経由の一致（`#minecraft:logs` 等）、組み込みの斧剥ぎ（`#minecraft:axes`） | タグの所属はデータパックのロード後にしか結びつかない | integration ゲームテスト（斧で原木 → 皮を剥いだ原木を GOOD_INTERACTION） |
| `NoDigUnderfootFeature.isUnderfoot` | `Player` の実体（接地・速度・当たり判定）が必要 | `CeleGuardGameTest.noDigUnderfoot` |
| `PortalProtectionFeature` | `Level` とポータルブロックの向きが必要 | `CeleGuardGameTest.portalFrame` / `portalDoorway` |
| `CeleConfig`（シングルトン、`FabricLoader` の設定ディレクトリ） | 共有状態。`JsonFiles` に委譲している部分だけ単体で確認 | 起動段 `config` の成否、全設定画面を開く |
| 各 Feature の `tick` / イベント登録 | `Minecraft` の実体が必要 | クライアントゲームテスト全体 |

---

## 5. 実行

```bash
./gradlew :26.3:test -Pmc=26.3    # 1 版の単体テスト。レポート: versions/26.3/build/reports/tests/test/index.html
./gradlew :26.3:build -Pmc=26.3   # build は test を含む
```

- テストは `fabric-loader-junit` の下で走る（Fabric Loader が JUnit の周りで起動し、Mixin とレジストリがゲームと同じ振る舞いをする）。レジストリを使うテストクラスは `@BeforeAll` で `GameBootstrap.ensure()` を呼ぶ。`GameBootstrap` は版ごとに `src/mc<版>/test/java` にあり、26.3 だけアイテムコンポーネントの初期化手順が違う。
- **`@BeforeAll` より先に走る static 初期化子で `Blocks` / `Items` に触れてはいけない**（レジストリ未初期化で落ちる）。テストの固定値はインスタンスフィールドかメソッド内に置く。
- CI では `build` ジョブの最初の Gradle ステップとして 3 版で実行し、失敗時は HTML レポートと JUnit XML を成果物に残す。
