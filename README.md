# Pattern Hatch (TJ) — 1.12.2 GTCEu 样板仓

给 GTCEu 系 1.12.2 整合包做的“样板仓”：把样板直接塞进仓室，仓室直连 ME 网络，
每个样板槽独立隔离缓存（物品 + 流体），多方块机器自动按样板执行配方，
不再需要每个机器额外挂一个 ME 接口。

## 功能

- **36 个样板槽**（4×9），只允许放入 AE2 已编码样板
- **每槽独立隔离缓存**：物品 + 流体（每槽 9 罐），机器按样板优先级逐个执行，不串料
- **4 个催化剂槽 + 1 个虚拟电路槽**（`notConsumed` 只匹配不消耗）
- **ME 直连**：样板即插即用，样板终端可见；放新样板即时刷新，无需重连网络
- **物品/流体样板全支持**：含 ae2fc 扩展样板（Cnt 长整型数量）
- **缓存查看**：GUI 右侧整列显示各槽缓存明细（自动换行 + 滚动条）
- **配方**：组装机配方，材料数量可在 `config/patternhatch.cfg` 自定义
- 方块材质：GTNH 风格深色机壳（自制贴图）

## 依赖

- Minecraft 1.12.2 + Forge 14.23.5.x（实测于 Cleanroom 0.6.6）
- GTCEu 系（TJ fork 实测；标准 GTCEu 需验证 `IMultiAbilityProvider` 存在性）
- AE2（rv6 / UEL 均可，物品样板）
- ae2fc（流体样板解码与流体包投递）
- MixinBooter / Cleanroom（mixin 补丁）

## 构建

环境：JDK 8 + Gradle（本机缓存含 ForgeGradle 3.0.197）。

```
gradle --offline --no-daemon reobfJar
```

离线构建注意：
- `build.gradle` 依赖指向整合包 mods 目录，按需修改路径；
- FG3 会在配置期检查 `build/downloadMCMeta/version.json` 与 `manifest.json`，
  离线环境需手动从 Gradle 缓存 `forge_gradle/minecraft_repo/versions/` 复制；
- `gradle.properties` 已加 `-Dnet.minecraftforge.gradle.test_certs=false` 跳过证书校验。

## 配置

`config/patternhatch.cfg`：

- `recipe.enabled` / `outputCount` / `duration` / `eut`
- `hvInputBusCount` / `hvInputHatchCount` / `patternCapacityCardCount` / `meInterfaceCount` / `fluidInterfaceCount`
- 各材料可用 `xxxItem` 覆盖（格式 `modid:item:meta@count`）

注意：组装机配方上限 9 个输入，材料总数量 ≤9。

## 注意事项

- 机器装上样板仓后使用“活动槽”模式，**同一机器上其它普通输入仓/总线会被忽略**
- 单方块机器不支持（与 GTNH 一致，样板仓只服务多方块）
- 方块被破坏时样板、催化剂、电路与缓存物品全部掉落

## 致谢与许可

- 方块贴图参考 GTNH（GT5-Unofficial，LGPL-3.0）：https://github.com/GTNewHorizons/GT5-Unofficial
- 使用 AE2（LGPL-3.0）与 ae2fc 的公开 API
- 本 mod 以 **LGPL-3.0** 开源：https://www.gnu.org/licenses/lgpl-3.0.html

## 项目文档

- `docs/样板仓-需求与设计.md`
- `docs/样板仓-实现方案.md`

