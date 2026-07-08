# Changelog

## [1.0.1] — 2026-07-06

### Fixed

- **降水量 "above" 阈值修正** (`DataColorMaps.forRainfall`)：最后一个区间覆盖 400~450mm，else 分支 (>450mm) 原本错误地显示 "≥ 500 mm"，修正为 "≥ 450 mm"。
- **Toggle 按钮重入风险** (`JMTFCClientPlugin`)：从设计层面重构了叠加层状态管理——
  - 旧：`Set<String> toggledOverlays` + 回调内手动操作其他按钮 `setToggled()`，存在重入风险
  - 新：`String activeOverlayId` + 幂等 `setActiveOverlay()` + `syncButtons()` 分离 UI 同步
  - 核心保障：`setActiveOverlay()` 通过 `Objects.equals` 实现幂等，即使 `syncButtons()` → `setToggled()` 触发回调，也会因为状态已是目标值而直接返回

### Technical

- 重新构建 jar 并同步到 `mods/`，消除源码与运行时 jar 不同步的问题

## [1.0.0] — 2026-07-03

### Added

- 三个 JourneyMap 全屏地图叠加层：岩层类型、温度热力图、降水量热力图
- 服务端 TFC 原始数据缓存（3×3 chunk 粒度）+ JSON 持久化到世界存档
- C→S / S→C 网络协议（`RequestCachePayload` / `CacheDataPayload`）
- 客户端颜色计算（`DataColorMaps`）+ 矩形合并渲染
- 中英文 i18n 支持
- 客户端配置（覆盖层可见性、不透明度、图例开关）
- 仅主世界生效
