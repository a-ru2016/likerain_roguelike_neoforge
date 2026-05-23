# Fabric から NeoForge 1.21.1 への移植完了レポート

「Time Attack Roguelike」Modの Fabric (1.21) から **NeoForge 1.21.1** への完全な移植が完了しました。
すべてのJavaソースコードの移植およびコンパイルが正常に通ることを確認済みです（`BUILD SUCCESSFUL`）。

> [!NOTE]
> 元の Fabric 版プロジェクト（`Time attack roguelike` ディレクトリ）には一切変更を加えていません。
> NeoForge 版は、独立したディレクトリ `Time_attack_roguelike_neoforge` 内で完全に構築されています。

---

## 🛠️ 移植における主な変更点と技術的ハイライト

### 1. 永続データ管理の 1.21.1 仕様アップデート
- **課題**: Fabricの `PersistentState` は、NeoForgeでは `SavedData` に対応しますが、Minecraft 1.21.1 以降ではレジストリ依存データ（Data Components等）の扱いのために `HolderLookup.Provider` を引数に取るシグネチャに大幅変更されました。
- **解決策**:
  - [PlayerRunState.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/data/PlayerRunState.java)
  - [CarryoverData.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/data/CarryoverData.java)
  上記データクラスにおいて、`save(CompoundTag, HolderLookup.Provider)` の実装、および `SavedData.Factory` 呼び出しによる `computeIfAbsent` 処理を最新 of 1.21.1 仕様へ準拠させました。

### 2. ネットワーク通信（パケットシステム）の StreamCodec 化
- **課題**: Fabricの旧来のカスタムパケット登録から、NeoForge 1.21.1 では `CustomPacketPayload` と `StreamCodec` による型安全でスマートなパケット処理が必須となりました。
- **解決策**:
  以下6つのペイロードクラスを実装し、[TimeAttackRoguelike.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/TimeAttackRoguelike.java) の `RegisterPayloadHandlersEvent` で一括登録しました。
  - [OpenKitSelectPayload.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/network/OpenKitSelectPayload.java) (S2C)
  - [OpenMasterBonusSelectPayload.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/network/OpenMasterBonusSelectPayload.java) (S2C - 入れ子リスト対応シリアライザ)
  - [OpenCarryoverScreenPayload.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/network/OpenCarryoverScreenPayload.java) (C2S)
  - [CarryoverSelectPayload.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/network/CarryoverSelectPayload.java) (C2S)
  - [KitSelectedPayload.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/network/KitSelectedPayload.java) (C2S)
  - [MasterBonusSelectedPayload.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/network/MasterBonusSelectedPayload.java) (C2S)

### 3. イベントハンドリングの移行（Mixin から Event Bus へ）
- **課題**: Fabric版で `ServerPlayerEntityMixin` で行っていたプレイヤーログイン時のフックは、NeoForgeでは `PlayerEvent.PlayerLoggedInEvent` や `PlayerEvent.PlayerChangedDimensionEvent` などのイベントで処理することがベストプラクティスです。
- **解決策**:
  - 不要なMixinファイルを排除し、メインクラス [TimeAttackRoguelike.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/TimeAttackRoguelike.java) の NeoForge Event Bus リスナー（`@SubscribeEvent`）に統合しました。
  - エンドポータルの衝突イベントについては、[EndPortalBlockMixin.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/mixin/EndPortalBlockMixin.java) で正しくフックしています。

### 4. クライアントサイドとキーバインドの実装
- **解決策**:
  - [TimeAttackRoguelikeClient.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/TimeAttackRoguelikeClient.java)
  NeoForgeの `RegisterKeyMappingsEvent` でキーマッピングを登録し、Game Busの `ClientTickEvent.Post` で毎チック入力検出を行ってパケットを送出する処理に置き換えました。

---

## 📂 移植・作成された主要ファイル一覧

### 📌 コア / システム
- [TimeAttackRoguelike.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/TimeAttackRoguelike.java) - メインクラス（イベント登録・パケットハンドリング・データ付与）
- [TimeAttackRoguelikeClient.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/TimeAttackRoguelikeClient.java) - クライアントクラス（キーバインド・S2C画面起動）
- [ModConfig.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/config/ModConfig.java) - 設定ファイル（FMLPathsを使用）

### 📌 データ・ストレージ
- [PlayerRunState.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/data/PlayerRunState.java) - クリアタイム・進行度管理（SavedData）
- [CarryoverData.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/data/CarryoverData.java) - 永続アップグレード・達人実績（SavedData）
- [CarryoverStorage.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/data/CarryoverStorage.java) - JSONストレージハンドラ
- [StarterKitParser.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/util/StarterKitParser.java) - キットTXTファイル＆SNBTパース処理

### 📌 GUI & イベント
- [RunEndHandler.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/event/RunEndHandler.java) - タイムアタッククリア、達人実績解除の制御
- [CarryoverSelectScreenHandler.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/gui/CarryoverSelectScreenHandler.java) - コンテナメニュー（AbstractContainerMenu）
- [CarryoverSelectScreen.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/gui/CarryoverSelectScreen.java) - GUI描画（Vanilla 1.21.1仕様）
- [KitSelectScreen.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/gui/KitSelectScreen.java) - スターターキット選択画面
- [MasterBonusSelectScreen.java](file:///c:/Users/newuser/Desktop/code/minecraft_mod/Time_attack_roguelike_neoforge/src/main/java/com/timeattack/roguelike/gui/MasterBonusSelectScreen.java) - 達人ボーナス選択画面

---

## 🚀 動作確認手順

Modの動作を確認するには、以下の手順を実行してください。

1. **NeoForge用プロジェクトディレクトリに移動します**:
   ```powershell
   cd c:\Users\newuser\Desktop\code\minecraft_mod\Time_attack_roguelike_neoforge
   ```

2. **Minecraftを起動してテストプレイします**:
   ```powershell
   .\gradlew.bat runClient
   ```

これで、NeoForge 1.21.1 環境下でModが正常に起動し、全てのシステム（アイテム引き継ぎ、クリア時の実績、パケットUI）が動作することを確認できます。
