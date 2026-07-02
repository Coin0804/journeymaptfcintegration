# JourneyMap TFC Integration

Minecraft 1.21.1 NeoForge 模组，在 JourneyMap 全屏地图上叠加显示 TerraFirmaCraft 的岩层、温度、降水数据。**仅主世界生效。**

## 技术栈

- Minecraft 1.21.1 / NeoForge 21.1.211 / Java 21
- JourneyMap API v2: `info.journeymap:journeymap-api-neoforge:2.0.0-1.21.1-SNAPSHOT`
- TFC: `maven.modrinth:terrafirmacraft:4.2.4`
- Parchment mappings 2024.11.17

## 架构

```
┌─ 服务端 ────────────────────────────┐  ┌─ 客户端 ───────────────────────────────┐
│                                      │  │                                         │
│  ServerCache ─── 存原始 TFC 数据 ───→│  │  DataColorMaps ── 原始数据→(颜色,标题) │
│  (3×3 base粒度)  (rockId,temp,rain)  │  │                                         │
│        ↑                             │  │  OverlayRenderer ── 合并同色→Polygon    │
│  ChunkEvent.Load + warmup()          │  │        ↑                                │
│        ↑                             │  │  CacheDataPayload ←── S→C              │
│  handleRequestCache ←── C→S ────────→│  │        ↑                                │
│                                      │  │  JMTFCClientPlugin ── Toggle按钮+维度   │
└──────────────────────────────────────┘  └─────────────────────────────────────────┘
```

### 数据流

```
Toggle ON (主世界) → sendCacheRequest → Server warmup + buildPayload
→ Client 收原始数据 → 计算颜色 → 按色分组 → 合并相邻 → PolygonOverlay → show

Toggle OFF → removeOverlay
离开主世界 → clearAll
进入主世界 → clearAll + sendCacheRequest
```

## 源文件

```
src/main/java/com/yukimods/journeymap/tfcintegration/
├── JourneymapTFCIntegration.java        @Mod 入口，注册网络包
├── config/
│   └── ModConfig.java                   客户端配置
├── network/
│   ├── RequestCachePayload.java         C→S：请求全量缓存
│   └── CacheDataPayload.java            S→C：原始缓存数据
├── plugin/
│   ├── client/
│   │   ├── JMTFCClientPlugin.java       Toggle按钮 + 维度切换 + handleCacheData
│   │   ├── OverlayDef.java              叠加层定义
│   │   ├── OverlayRenderer.java         合并 + PolygonOverlay 生命周期
│   │   └── data/
│   │       └── DataColorMaps.java       原始数据→(颜色,标题)
│   └── server/
│       ├── JMTFCServerPlugin.java       JM发现 + handleRequestCache
│       ├── ServerCache.java             缓存 + ChunkEvent + warmup + buildPayload
│       └── data/
│           └── TFCDataAccess.java       TFC API封装
```

## 关键设计

- **服务端不碰颜色**：只存 `rockId(string) + temperature(float) + rainfall(float)`
- **客户端计算颜色**：`DataColorMaps` 一步返回 `(color, title)`，无需两步查找
- **3×3 block 粒度**：服务端存 base pos（floorDiv/3*3），客户端直接合并，不展开
- **Warmup 机制**：首次请求时扫描玩家周围已加载区块，补齐出生点缓存
- **仅主世界**：其他维度不缓存、不响应、不渲染

## 三个叠加层

| 叠加层 | 数据来源 | 颜色方案 |
|--------|---------|---------|
| Rock Layers | `RockData.getSurfaceRock(8,8)` → rockId | 20种岩石固定颜色 |
| Temperature | `ClimateModel.getAverageTemperature()` | -25°C~+40°C 蓝→红 5°C档 |
| Precipitation | `ClimateModel.getAverageRainfall()` | 0~500mm 棕→蓝 50mm档 |
