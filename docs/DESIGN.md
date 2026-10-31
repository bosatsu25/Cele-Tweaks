# Cele-Tweaks 設計書

対象: Minecraft Java Edition **26.1.2 / 26.2 / 26.3**、Fabric、クライアント専用。
確信度の表記: **[確実]** = CI の署名ダンプ等で確認済み、**[可能性が高い]** = 強い推論、**[推測]** = 未確認。

---

## 1. 製品の輪郭

### 1.1 原則

- **クライアント専用**。サーバー導入不要、独自パケットなし。
- **自動化しない**。どの機能も「描画を変える（Visual）」か「プレイヤーや他 MOD がしようとした操作を止める（Guard）」のどちらかで、操作を新たに発生させない。Mixin がパケットやクリックを追加する実装は採用しない。
- **既定はすべて OFF**。
- **外部 MOD は任意**。hard dependency は Fabric API のみ。Litematica / Tweakeroo / TweakerMore / Syncmatica / Item Scroller / Mod Menu は、入っていれば連携し、無ければ no-op。
- **1 ソースツリーから 3 版の JAR**。

### 1.2 やらないこと

| 項目 | 理由 |
| --- | --- |
| 自動化（食事・補充・投擲・修繕・移動保持・取引・整理など） | 利用者の要望外。サーバーから見て人間と区別できない範囲に留める |
| 外部通信（更新確認・自動ダウンロード） | 配布と更新は運用側で管理する |
| 特定サーバー専用の依存 | 汎用性を損なう |
| 他 MOD の設定値を恒久的に書き換えること | 一時的に変えた場合は必ず利用者の値に戻す（Syncer Tune） |

---

## 2. 対象バージョンとビルド

### 2.1 前提 [確実]

| 項目 | 26.1.2 | 26.2 | 26.3 |
| --- | --- | --- | --- |
| リリース | 2026-03 系 | 2026-06-16 | 2026-09-15 |
| 名前 | Mojang 公式名（難読化なし） | 同左 | 同左 |
| Java | 25 | 25 | 25 |
| Loom | 非 remap プラグイン `net.fabricmc.fabric-loom` | 同左 | 同左 |

### 2.2 構成: サブプロジェクト＋版別ソース重ね合わせ

```
Cele-Tweaks/
├── settings.gradle.kts          # :26.1.2 / :26.2 / :26.3 を versions/<mc> に割り当て。-Pmc=<v> で 1 版だけ
├── build.gradle.kts             # Loom プラグイン宣言と buildAll タスク
├── gradle/common.gradle         # 全版共通のビルド処理（依存、Java 25、processResources、ソース重ね合わせ）
├── gradle.properties            # mod_id, mod_version, maven_group
├── versions/<mc>/
│   ├── build.gradle             # Loom 適用 + common.gradle
│   └── gradle.properties        # minecraft / loader / fabric-api / modmenu のピン、source_overlays=mc<mc>
├── src/main/{java,resources}    # 全版共通
├── src/mc<mc>/{java,resources}  # その版だけの差分（同名クラスを各版が 1 つずつ持つ）
└── src/gametest/{java,resources} # CI 用の起動テスト（全版共通、JAR には入らない）
```

プリプロセッサ型のツール（Stonecutter）は採用しない。非アクティブ版にソースが渡らず `compileJava NO-SOURCE` で「成功」した実例があり（CI で確認）、誤った緑は赤より悪いため。[確実]

版ごとの差分は 2 種類に限定する。
- `compat.Compat`: 現在の画面取得、チャンク再描画、キー名表示、キーの保存名変換。
- `mixin.version.*` と `celetweaks.version.mixins.json`: 署名が版で違う Mixin。

| 項目 | 26.1.2 | 26.2 | 26.3 |
| --- | --- | --- | --- |
| `PlayerTeam.getColor()` | `ChatFormatting` | `Optional<TeamColor>` | `Optional<TeamColor>` |
| `EntityRenderDispatcher.shouldRender` | 5 引数 | 5 引数 | 6 引数（`float` 追加） |
| キー種別 | `InputConstants.Type.KEYSYM` | `KEYSYM` | `KEYBOARD`（入力が SDL へ移行） |
| サイドバー描画 | `Gui.displayScoreboardSidebar` | `Hud.displayScoreboardSidebar` | `Hud.displayScoreboardSidebar` |
| 現在の画面 | `Minecraft.screen` | `Minecraft.gui.screen()` | `Minecraft.gui.screen()` |
| チャンク再描画 | `LevelRenderer.allChanged()` | `invalidateCompiledGeometry(level, options, camera, blockColors)` | 同左 |

3 版共通で使う描画の入口（版差を避けるために選んだもの）[確実]:
- 範囲表示: `Minecraft.collectPerTickGizmos()` と `Gizmos.cuboid / circle`。版ごとに違うワールド描画 API を使わない。
- 溶岩: `FluidRenderer.tesselate(...)` と `FluidRenderer.vertex(..., int color, ...)`。Fabric の 26.x 流体 API には色指定の口が無いため。

Mixin のターゲット名は javac では検証されない（間違っていればゲーム起動時に落ちる）。CI の "Dump Minecraft API signatures" ステップは各版の jar から実署名を出力する（移植時の参照用）。ターゲットが実際に当たるかは、client game test（`src/gametest`）が各版の実クライアントを起動して確かめる。必須の Mixin は `defaultRequire: 1` なので、ターゲットが違えばクラスの読み込み時に落ち、テストが失敗する。

### 2.3 CI / リリース

- PR と main への push で、3 版をマトリクスで並列ビルド（`./gradlew :<mc>:build -Pmc=<mc>`）。
- 毎回、Maven リポジトリの最新版一覧と、各版の API 署名ダンプをログに出す。
- 続けて client game test を仮想ディスプレイ上で実行（`./gradlew :<mc>:runClientGameTest -Pmc=<mc>`）。次の順に進み、クラッシュ・起動段の失敗・隔離が 1 件でもあれば失敗。スクリーンショットとログは成果物として 7 日間保存。
  1. 使える機能をすべて ON にする（通常のジョブでは Core / Vanilla、integration ジョブでは連携機能も）。
  2. 機能一覧画面と、全 25 機能の設定画面を開く。
  3. シングルプレイのワールドに入り、サイドバーの目標・色付きチーム・色付きチャット・溶岩・ビーコン・避雷針を用意する。
  4. ワールド内で Block Filter を OFF→ON してチャンク再描画の経路を通す。
- 別ジョブ `integration (mc <版>)` で、Litematica / Tweakeroo / TweakerMore / Syncmatica / Item Scroller / MaLiLib の各版向け最新ビルドを Modrinth から取得し（`.github/scripts/fetch-integration-mods.sh`）、同じ client game test を `-Pintegration_mods=<dir>` 付きで実行する。連携機能も ON になり、さらに連携用 Mixin がすべて相手のメソッドに 1 か所以上フックしたことを確かめる。取得できた版はジョブの Summary に表で出る。
- タグ `v*` を push すると、`release.yml` がまず CI 全体（3 版のビルドと client game test）を `workflow_call` で実行する。失敗すればリリースしない。成功したら、CI がビルドして起動テストに使った JAR そのもの（再ビルドしない）を 1 つの GitHub Release に添付する。
- `release.yml` を手動実行（workflow_dispatch）すると、同じ確認と JAR の収集までを行い、Release は作らない（リハーサル）。
- バージョン表記は `MAJOR.MINOR.PATCH+mc<MC>`（例 `0.1.0+mc26.3`）。

---

## 3. ランタイム

```
io.github.bosatsu25.celetweaks
├── CeleTweaksClient            # 起動段ごとに SafeStartup で隔離
├── runtime/                    # RuntimeComponent, Feature, FeatureManager, SafeStartup, CeleCommands, FeatureSections
├── catalog/                    # FeatureDefinition（全機能の正本）, FeatureCategory, RiskKind, Tier
├── config/                     # CeleConfig, JsonFiles, IdMatcher, Ids, ListMode, Range
├── hotkey/                     # KeyCombo, HotkeyManager, HotkeyAction
├── gui/                        # 設定画面 3 枚 + ホットキー取得画面 + Mod Menu 入口
├── feature/<area>/             # 機能本体
├── mixin/                      # バニラ対象 / integration（外部 MOD 対象）
└── compat/ (src/mc<v>)         # 版差分
```

- **1 機能 = 1 `FeatureDefinition`**。設定キー、翻訳キー、設定画面の行、ホットキーはこの enum から導出する。
- **隔離（quarantine）**: init で例外を出した部品は登録しない。tick で 3 回連続失敗した部品、session reset やトグル通知で失敗した部品は止め、`onQuarantined` で変更を元に戻させる。設定画面では赤字で表示。
- **セッション状態**: ワールドが変わるたびに全部品の `resetSession` を呼び、ログ・一時状態・感度変更などを破棄／復元する。

---

## 4. 設定

| ファイル | 内容 |
| --- | --- |
| `config/celetweaks/features.json` | `enabled`（機能 ID → ON/OFF）と `sections`（機能 ID → 設定オブジェクト） |
| `config/celetweaks/hotkeys.json` | アクション ID → キー名の配列（`key.keyboard.k` 形式） |
| `config/celetweaks/notes.txt` | テキストノートの本文 |

- 欠けたフィールドは既定値、未知のフィールドは保持、壊れたファイルは `.broken-<時刻>` に退避して既定値で起動、書き込みは一時ファイルからの置き換え。
- 設定画面は各機能の設定クラスの public フィールドから自動生成（boolean / int / double / float / String / enum / `List<String>`）。`@Range` で数値の上下限を付ける。
- ID のリストは、すべて `minecraft:stone` 形式の ID と `#minecraft:logs` 形式のタグの両方を受け付ける（`IdMatcher`）。
- ホットキーを数値ではなく名前で保存するのは、26.3 で入力が SDL に移り、同じキーでも数値が変わったため。

---

## 5. Mixin 方針

| 設定ファイル | required | 内容 |
| --- | --- | --- |
| `celetweaks.mixins.json` | true | 3 版共通のバニラ対象。Mixin 内で必ず機能の ON/OFF を見て早期 return |
| `celetweaks.version.mixins.json` | true | 版ごとに署名が違うバニラ対象（`src/mc<v>/resources` から供給） |
| `celetweaks.integrations.mixins.json` | false | 外部 MOD 対象。`@Pseudo` + 文字列ターゲット + `require = 0` + プラグインで対象 MOD 導入時のみ適用 |

外部 MOD はコンパイル依存に入れない。型が必要な箇所はリフレクションで扱い、`ReflectiveOperationException` と `LinkageError` は境界内で握る。

`IntegrationMixinPlugin` は、連携用 Mixin を適用するたびに、相手のクラスへ織り込まれたハンドラ呼び出しの数を数える。0 件なら「この版の相手 MOD では機能が働かない」と警告ログを出す（`require = 0` のままでも黙って無効にならない）。CI の integration ジョブはこの数を検査する。

---

## 6. 機能

一覧と仕様は [FEATURE_CATALOG.md](FEATURE_CATALOG.md)。

---

## 7. 決定事項

| # | 決定 | 却下した代替 | 確信度 |
| --- | --- | --- | --- |
| 1 | 対象は 26.1.2 / 26.2 / 26.3 | 単一版 | [確実]（利用者決定） |
| 2 | サブプロジェクト＋版別ソース重ね合わせ | プリプロセッサ（NO-SOURCE で偽の成功）、ブランチ分割 | [確実] |
| 3 | Mojang 公式名のみ | — | [確実] |
| 4 | hard dependency は Fabric API のみ | 外部 MOD を必須にする | [確実] |
| 5 | 設定画面は自前 3 画面（設定クラスから自動生成） | 外部設定ライブラリ | [可能性が高い] |
| 6 | 設定は JSON、壊れても起動する | — | [確実] |
| 7 | ホットキーは複数キーの組み合わせ、名前で保存 | 機能ごとにバニラのキー割り当て | [確実] |
| 8 | 自動化はしない（Visual / Guard のみ） | — | [確実]（利用者決定） |
| 9 | 範囲表示はバニラ Gizmo、溶岩は FluidRenderer の頂点色 | 版ごとのワールド描画 API、Fabric 流体 API | [確実]（3 版で同署名） |
| 10 | 他 MOD の値を一時的に変える機能は、必ず利用者の値へ戻す | 自前の固定値で上書き | [確実] |

---

## 8. 未確認事項

1. 各機能が意図どおりに働くか。client game test が確かめるのは、起動・Mixin の適用・画面の表示・ワールド内で例外が出ないことまでで、ガードが本当に操作を止めるか、描画の見た目が正しいかは判定しない。
2. 外部 MOD 連携の接続先は、各 MOD の 26.1.2 / 26.2 / 26.3 向けソースで実在を確認済み（[FEATURE_CATALOG.md](FEATURE_CATALOG.md) 末尾の表）。[確実] 実際に配布されている JAR へのフックは CI の integration ジョブで確かめていて、3 版とも 8 個すべてがフックした（2026-10-31）。[確実] 連携機能が意図どおりに働くか（判定の中身）は未確認。
3. Sodium 導入時、溶岩源の色付けは表示されない（Sodium が流体描画を置き換える）。[可能性が高い]
