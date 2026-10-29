# Cele-Tweaks

Minecraft **26.1.2 / 26.2 / 26.3** 向け Fabric クライアント専用 Tweaks MOD。

- 自動化なし。すべての機能は「描画を変える（Visual）」か「操作を止める（Guard）」のどちらか
- サーバー導入不要、独自パケットなし
- Fabric API 以外の依存なし。Litematica / Tweakeroo / TweakerMore / Item Scroller / Mod Menu は入っていれば連携、無ければ no-op
- 1 ソースツリーから 3 バージョンの JAR をビルド（共通ソース `src/main` ＋ バージョン別の差分 `src/mc<version>`）

| 文書 | 内容 |
| --- | --- |
| [docs/DESIGN.md](docs/DESIGN.md) | アーキテクチャ、ビルド構成、設定・Mixin・外部 MOD 連携の方針、決定事項 |
| [docs/FEATURE_CATALOG.md](docs/FEATURE_CATALOG.md) | 全 25 機能の仕様 |

## ビルド

```bash
./gradlew buildAll                    # 3 バージョンすべて → versions/<mc>/build/libs/
./gradlew :26.3:build -Pmc=26.3       # 1 バージョンだけ（他バージョンの Minecraft を取得しない）
./gradlew :26.3:runClient -Pmc=26.3   # 開発用クライアント起動
./gradlew :26.3:runClientGameTest -Pmc=26.3   # 起動テスト（CI と同じもの。実クライアントが起動する）
```

JDK 25 が必要です。

## 設定

Mod Menu から、または `/celetweaks settings`、または設定画面で割り当てたホットキーで開きます。
設定ファイルは `config/celetweaks/features.json` と `hotkeys.json`。

## ライセンス

MIT。[NOTICE](NOTICE) を参照。
