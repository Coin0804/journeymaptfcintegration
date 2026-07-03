# JourneyMap TFC Integration

在 JourneyMap 全屏地图上叠加显示 TerraFirmaCraft 的岩层类型、温度和降水数据。

Adds TerraFirmaCraft rock layer, temperature, and precipitation overlays to the JourneyMap fullscreen map.

## 依赖 / Requirements

- Minecraft **1.21.1**
- **NeoForge** 21.1+
- **[TerraFirmaCraft](https://www.curseforge.com/minecraft/mc-mods/terrafirmacraft)** (TFC) 4.1+
- **[JourneyMap](https://www.curseforge.com/minecraft/mc-mods/journeymap)** 1.21.1-6.0.0+

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

## 构建 / Building

```bash
./gradlew jar
# output: build/libs/journeymaptfcintegration-neoforge-1.0.0.jar
```

- Java 21
- Gradle 8.10+

## 许可 / License

MIT © 2026 Coin0804
