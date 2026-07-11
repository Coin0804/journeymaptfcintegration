# JourneyMap TFC Integration

[English](#english) | [中文](#中文)

---

## English

Adds TerraFirmaCraft rock layer, temperature, and precipitation overlays to the JourneyMap fullscreen map.

### Overlays

| Overlay | Description |
|---------|-------------|
| **Rock Layers** | Surface rock type color fill, 20 rock types supported |
| **Temperature** | -25°C ~ +40°C heatmap, blue → red |
| **Precipitation** | 0 ~ 500mm heatmap, brown → blue |

- Overworld only
- Three overlays toggle between each other
- Supports English / Chinese

### Usage

1. Open JourneyMap fullscreen map (default `J`)
2. Click the TFC button on the left (Rock / Temperature / Precipitation)
3. The selected overlay appears on the map

### Technical Notes

- **Data granularity**: Overlay data is sampled at 3×3 chunk (48×48 block) resolution. The same data is shown for the entire 3×3 area, which may cause inaccuracies at rock/biome boundaries.
- **Viewport-based loading**: toggling ON sends the current map viewport center to the server, which returns cached data within a 600-chunk radius to avoid excessive payload.
- **Cache persistence**: explored area data is saved to `<world>/data/jmtfc_cache.json` on world save and automatically restored after restart.

### Dependencies

- Minecraft 1.21.1
- NeoForge 21.1+
- TerraFirmaCraft 4.1+
- JourneyMap 1.21.1-6.0.0+

### Build

```bash
./gradlew jar
```

Requires Java 21, Gradle 8.10+.

### License

MIT © 2026 Coin0804

---

## 中文

在 JourneyMap 全屏地图上叠加显示 TerraFirmaCraft 的岩层类型、温度和降水数据。

### 叠加层

| 叠加层 | 说明 |
|--------|------|
| **岩层类型** | 地表岩层颜色填充，支持 20 种岩石 |
| **温度** | -25°C ~ +40°C 热力图，蓝 → 红 |
| **降水量** | 0 ~ 500mm 热力图，棕 → 蓝 |

- 仅主世界生效
- 三个叠加层互斥切换
- 支持中英文切换

### 使用

1. 打开 JourneyMap 全屏地图（默认 `J`）
2. 点击左侧 TFC 按钮（岩层 / 温度 / 降水）
3. 地图上显示对应的覆盖层

### 技术说明

- **数据粒度**：覆盖层以 3×3 chunk（48×48 方块）为最小采样单位。同一 3×3 区块内所有位置显示相同数据，区块边界处可能与实际地形有偏差。
- **视口请求**：Toggle ON 时客户端向服务端发送当前地图视口中心，服务端返回半径 600 chunk 范围内的缓存数据，避免载荷过大。
- **缓存持久化**：探索过的区域数据在存档时存入 `<world>/data/jmtfc_cache.json`，重启后自动恢复。

### 依赖

- Minecraft 1.21.1
- NeoForge 21.1+
- TerraFirmaCraft 4.1+
- JourneyMap 1.21.1-6.0.0+

### 构建

```bash
./gradlew jar
```

需要 Java 21、Gradle 8.10+。

### 许可

MIT © 2026 Coin0804
