# JourneyMap TFC Integration

在 JourneyMap 全屏地图上叠加显示 TerraFirmaCraft 的岩层类型、温度和降水数据。

Adds TerraFirmaCraft rock layer, temperature, and precipitation overlays to the JourneyMap fullscreen map.

## 依赖 / Requirements

- Minecraft **1.21.1**
- **NeoForge** 21.1+
- **[TerraFirmaCraft](https://modrinth.com/mod/terrafirmacraft)** (TFC) 4.1+
- **[JourneyMap](https://modrinth.com/mod/journeymap)** 1.21.1-6.0.0+

## 功能 / Features

| 叠加层 | 说明 |
|--------|------|
| **岩层类型** / Rock Layers | 地表岩层颜色填充，支持 20 种岩石 |
| **温度** / Temperature | -25°C ~ +40°C 热力图，蓝 → 红 |
| **降水量** / Precipitation | 0 ~ 500mm 热力图，棕 → 蓝 |

- 仅主世界生效
- 三个叠加层互斥切换
- 支持中 / English 切换

## 安装 / Installation

将 `journeymaptfcintegration-neoforge-1.0.0.jar` 放入 `mods/` 目录。

## 使用 / Usage

1. 打开 JourneyMap 全屏地图（默认 `J`）
2. 点击左侧 TFC 按钮（岩层 / 温度 / 降水）
3. 地图上显示对应的覆盖层

## 技术说明 / Technical Notes

- **数据粒度**：覆盖层以 3×3 chunk（48×48 方块）为最小采样单位。同一 3×3 区块内所有位置显示相同数据，区块边界处可能与实际地形有偏差。
- **视口请求**：Toggle ON 时客户端向服务端发送当前地图视口位置，服务端返回该位置半径 600 chunk （9600格）范围内的缓存数据，避免载荷过大。
- **缓存持久化**：探索过的区域数据会在世界保存时存入 `<world>/data/jmtfc_cache.json`，重启后自动恢复，无需重新探索。

> Overlay data is sampled at 3×3 chunk (48×48 block) granularity. The same data is shown for the entire 3×3 block, which may cause inaccuracies at biome/rock boundaries.  
> Toggling ON sends the current map viewport center to the server, which returns cached data within a 600-chunk radius.

## 构建 / Building

```bash
./gradlew jar
# output: build/libs/journeymaptfcintegration-neoforge-1.0.0.jar
```

- Java 21
- Gradle 8.10+

## 许可 / License

MIT © 2026 Coin0804
