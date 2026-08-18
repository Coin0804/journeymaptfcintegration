# Changelog

[English](#english) | [中文](#中文)

---

## English

### [1.0.3] — 2026-08-18

#### Fixed

- **Crash in non-TFC worlds (Critical)**: entering a superflat (or any non-TFC-generator) world crashed the server with `NullPointerException` in `RockData.getSurfaceRock` during chunk generation. TFC's `CHUNK_DATA` attachment degrades to an empty `ChunkData` (null generator) when the level's chunk generator is not TFC's, and `getSurfaceRock` dereferences it unconditionally.
  - `TFCDataAccess.isTFCGenerator()` — checks `level.getChunkSource().getGenerator() instanceof TFCChunkGenerator`.
  - `ServerCache.queryData()` — skips the rock query for non-TFC worlds (records `rockId=""`, temperature/rainfall unchanged); result memoized per generator instance (auto-invalidates on world switch) with a one-time INFO log.
  - Covers both `ChunkEvent.Load` and `warmup()` paths (both go through `queryData`).

### [1.0.2] — 2026-07-13

#### Fixed

- **Cache loss (Critical)**: `loadOnce()` was previously triggered by client requests. External mods (e.g. SimpleBackups) could fire `LevelEvent.Save` immediately after server init, when memory only contained incomplete spawn chunk data, causing `save()` to irreversibly overwrite the full historical disk cache.
  - `loadOnce()` moved to `JMTFCServerPlugin.initialize()` — loads at server startup, no longer depends on client.
  - `save()` now guarded by `loadedFromDisk` flag — refuses to overwrite disk before `loadOnce()` completes.
  - `loadOnce()` merge with existing in-memory data from `ChunkEvent.Load`, disk takes priority, with merge count logging.

#### Added

- **`/jmtfc clearcache` command**: OP level 2, clears all TFC cache (memory + disk file).
- **Full-chain diagnostic logging**: covers loadOnce / save / warmup / buildPayload / handleRequestCache / handleCacheData / onCacheDataReceived / toggleOverlay / renderOverlay; INFO level reserved for lifecycle events only.

#### Changed

- **Log downgrade**: per-request logs (warmup, buildPayload, toggleOverlay, etc.) lowered to DEBUG to reduce noise.
- Fixed fully-qualified `ChunkAccess` reference in `TFCDataAccess`.

### [1.0.1] — 2026-07-06

#### Fixed

- **Precipitation "above" threshold** (`DataColorMaps.forRainfall`): last range covers 400~450mm; the else branch (>450mm) incorrectly displayed "≥ 500 mm", corrected to "≥ 450 mm".
- **Toggle button re-entry risk** (`JMTFCClientPlugin`): redesigned overlay state management:
  - Old: `Set<String> toggledOverlays` + manual `setToggled()` in callback, re-entry risk
  - New: `String activeOverlayId` + idempotent `setActiveOverlay()` + `syncButtons()` for UI sync
  - Guarantee: `setActiveOverlay()` uses `Objects.equals` for idempotency; even if `syncButtons()` → `setToggled()` triggers callback, it becomes a no-op.

#### Technical

- Rebuilt jar and synced to `mods/`, eliminating source-runtime desync.

### [1.0.0] — 2026-07-03

#### Added

- Three JourneyMap fullscreen map overlays: rock layer types, temperature heatmap, precipitation heatmap
- Server-side TFC raw data cache (3×3 chunk granularity) + JSON persistence to world save
- C→S / S→C network protocol (`RequestCachePayload` / `CacheDataPayload`)
- Client-side color computation (`DataColorMaps`) + rectangle merge rendering
- EN / ZH i18n support
- Client config (overlay visibility, opacity, legend toggle)
- Overworld only

---

## 中文

### [1.0.3] — 2026-08-18

#### Fixed

- **非 TFC 世界崩溃（Critical）**：进入超平坦（或任何非 TFC 生成器）世界时，区块生成过程中 `RockData.getSurfaceRock` 抛 `NullPointerException` 直接崩服。原因：世界生成器不是 TFC 时，TFC 的 `CHUNK_DATA` attachment 退化为无 generator 的空数据，`getSurfaceRock` 无条件解引用 null。
  - 新增 `TFCDataAccess.isTFCGenerator()` —— 判定 `level.getChunkSource().getGenerator() instanceof TFCChunkGenerator`。
  - `ServerCache.queryData()` —— 非 TFC 世界跳过岩石查询（`rockId=""`，温度/降雨逻辑不变）；判定按生成器实例缓存（世界切换自动重判），每个世界打一次 INFO 日志。
  - `ChunkEvent.Load` 与 `warmup()` 两条路径都经 `queryData`，一处守卫全覆盖。

### [1.0.2] — 2026-07-13

#### Fixed

- **缓存丢失（Critical）**：`loadOnce()` 原本依赖客户端请求触发，外部模组（如 SimpleBackups）可在服务端初始化后立即触发 `LevelEvent.Save`，此时内存中仅有 spawn chunks 的不完整数据，`save()` 直接覆盖磁盘上完整的历史缓存文件，造成不可逆的数据丢失。
  - `loadOnce()` 移至 `JMTFCServerPlugin.initialize()` 中执行——服务端就绪即加载，不再依赖客户端。
  - `save()` 新增 `loadedFromDisk` 守卫——`loadOnce()` 未完成前拒绝覆盖磁盘。
  - `loadOnce()` 加载时与内存已有 `ChunkEvent.Load` 数据合并，磁盘优先，追加合并计数日志。

#### Added

- **`/jmtfc clearcache` 命令**：OP 权限（level 2），清除全部 TFC 缓存（内存 + 磁盘文件）。
- **全链路诊断日志**：覆盖 loadOnce / save / warmup / buildPayload / handleRequestCache / handleCacheData / onCacheDataReceived / toggleOverlay / renderOverlay 各环节，INFO 仅保留关键生命周期事件。

#### Changed

- **日志降级**：per-request 日志（warmup、buildPayload、toggleOverlay 等）降至 DEBUG，减少正常使用时的日志噪音。
- 修复 `TFCDataAccess` 中 `ChunkAccess` 全限定名引用。

### [1.0.1] — 2026-07-06

#### Fixed

- **降水量 "above" 阈值修正** (`DataColorMaps.forRainfall`)：最后一个区间覆盖 400~450mm，else 分支 (>450mm) 原本错误地显示 "≥ 500 mm"，修正为 "≥ 450 mm"。
- **Toggle 按钮重入风险** (`JMTFCClientPlugin`)：从设计层面重构了叠加层状态管理——
  - 旧：`Set<String> toggledOverlays` + 回调内手动操作其他按钮 `setToggled()`，存在重入风险
  - 新：`String activeOverlayId` + 幂等 `setActiveOverlay()` + `syncButtons()` 分离 UI 同步
  - 核心保障：`setActiveOverlay()` 通过 `Objects.equals` 实现幂等，即使 `syncButtons()` → `setToggled()` 触发回调，也会因为状态已是目标值而直接返回

#### Technical

- 重新构建 jar 并同步到 `mods/`，消除源码与运行时 jar 不同步的问题

### [1.0.0] — 2026-07-03

#### Added

- 三个 JourneyMap 全屏地图叠加层：岩层类型、温度热力图、降水量热力图
- 服务端 TFC 原始数据缓存（3×3 chunk 粒度）+ JSON 持久化到世界存档
- C→S / S→C 网络协议（`RequestCachePayload` / `CacheDataPayload`）
- 客户端颜色计算（`DataColorMaps`）+ 矩形合并渲染
- 中英文 i18n 支持
- 客户端配置（覆盖层可见性、不透明度、图例开关）
- 仅主世界生效
