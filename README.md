# Pattern Hatch (TJ) — 1.12.2 GTCEu 样板仓

专用于 **Technological Journey 1.5 Beta-hot_fix** 整合包（MC 1.12.2 / GTCEu fork + Gregicality fork）的“样板仓”：把样板直接塞进仓室，仓室直连 ME 网络，每个样板槽独立隔离缓存（物品 + 流体），多方块机器自动按样板执行配方，不再需要每个机器额外挂一个 ME 接口。

> 当前版本：**v0.1.40**（2026-08-01）

## 功能

- **36 个样板槽**（4×9），只允许放入 AE2 已编码样板；每槽独立隔离缓存（物品 + 流体，每槽 9 罐），按样板优先级逐个执行、互不串料
- **9 个催化剂槽 + 虚拟电路槽**（`notConsumed` 只匹配不消耗；编程电路支持 NBT 配置，电路槽 0 为虚拟电路，无需手动放置）
- **ME 直连**：样板即插即用、样板终端可见；放新样板即时刷新，无需重连网络
- **物品 / 流体 / 复合样板全支持**：含 ae2fc 扩展样板（Cnt 长整型数量）
- **缓存查看**：GUI 右侧整列显示各槽缓存明细（自动换行 + 滚动条），一键弹回 AE
- **NAE2 兼容**：多功能样板工具右键样板仓，可像打开 AE 接口一样读写样板槽（可选依赖）
- **并行安全**：摊平缓存视图按快照记账，大批量并行不漏扣、不增产；活动槽保持锁防缓存空档期吃普通总线
- **方块材质**：GTNH 风格深色机壳（自制贴图，含工作状态贴图）

## 机器支持

- **GTCEu 系多方块**（`RecipeMapMultiblockController`）：完整支持
- **GA LargeSimple / 多配方图机器**（如大型灌装机：罐头 / 灌装 / 流体固化 模式切换）：支持，活动槽选择跟随当前模式的配方图
- **TJ 平行机**（`ParallelRecipeMapMultiblockController`，共 28 台：平行化学反应釜、平行组装机、平行高炉、平行离心机等）：支持（P0）——样板仓可**完全替代输入总线 / 输入仓**，结构通用槽位直接成型；多层并行共享活动槽；distinct 模式在活动槽激活时统一走样板视图
- 单方块机器：不支持（与 GTNH 一致，样板仓只服务多方块）

## 稳定性 / 防护

- 并行大批量不增产：FlattenedCacheView 切片按构造快照记账，模拟抽取与真实抽取一致（修复“160 只抽 96、白出 8 个”类问题）
- TJ 配方 LRU 自动清理：活动槽切换 / 模式切换 / 槽内材料种类变化时强制重搜，避免跑旧配方
- TJ 崩溃防护：无输入总线时 `getInputBus` 越界保护；distinct 但无总线时自动关闭，不再崩服
- 空闲自动弹回：缓存残余超过 5 秒自动退回 AE（也可 GUI 手动弹回）

## 配置（config/patternhatch.cfg）

- `recipe.*`：组装机配方（材料、数量、时长、EU/t；各材料可用 `xxxItem` 覆盖，格式 `modid:item:meta@count`）
- `cache.busyThreshold` / `cache.cacheCapItems`：缓存上限（默认最大，可调小限流）
- `debug.enabled`：调试日志开关（默认 false；排查问题时开 true）
- `tjParallel.enabled`：TJ 平行机支持开关（默认 true）

注意：组装机配方最多 9 个输入，材料总数量 ≤9。

## 依赖

- Minecraft 1.12.2 + Forge 14.23.5.x（实测于 Cleanroom 0.6.6）
- GTCEu 系（TJ fork 实测；标准 GTCEu 需验证 `IMultiAbilityProvider` 存在性）
- AE2（rv6 / UEL 均可，物品样板）
- ae2fc（流体样板解码与流体包投递）
- NAE2（可选：多功能样板工具兼容）
- MixinBooter / Cleanroom（mixin 补丁）

## 构建

环境：JDK 8 + Gradle（本机缓存含 ForgeGradle 3.0.197）。

```
gradle --offline --no-daemon reobfJar
```

离线构建注意：
- `build.gradle` 依赖指向整合包 mods 目录，按需修改路径；
- FG3 会在配置期检查 `build/downloadMCMeta/version.json` 与 `manifest.json`，离线环境需手动从 Gradle 缓存 `forge_gradle/minecraft_repo/versions/` 复制；
- `gradle.properties` 已加 `-Dnet.minecraftforge.gradle.test_certs=false` 跳过证书校验。

## 已知边界

- TJ 平行机支持为 P0：所有层共享同一活动槽；distinct 模式下的完整语义尚未逐台验证，建议使用样板仓时不开启 distinct
- 方块被破坏时样板、催化剂、电路与缓存物品全部掉落
- 无活动样板槽时机器回退普通输入，手动合成照常

## 项目文档

- `docs/样板仓-需求与设计.md`
- `docs/样板仓-实现方案.md`
- `CHANGELOG.md`（完整更新日志）

## 致谢与许可

- 方块贴图参考 GTNH（GT5-Unofficial，LGPL-3.0）：https://github.com/GTNewHorizons/GT5-Unofficial
- 使用 AE2（LGPL-3.0）与 ae2fc 的公开 API
- 本 mod 由 AI 大模型（Codex / OpenAI）辅助开发，并与整合包作者 ben23322 联调迭代
- 本 mod 以 **LGPL-3.0** 开源：https://www.gnu.org/licenses/lgpl-3.0.html
