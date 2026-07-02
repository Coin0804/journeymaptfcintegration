package com.yukimods.journeymap.tfcintegration.plugin.client.data;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端颜色映射：原始 TFC 数据 → (ARGB颜色, 标题)。
 * 一次查询同时获得颜色和标题，无需两步查找。
 */
public final class DataColorMaps {

    private DataColorMaps() {}

    public static final String OVERLAY_ROCK   = "tfc_rock_layers";
    public static final String OVERLAY_TEMP   = "tfc_temperature";
    public static final String OVERLAY_PRECIP = "tfc_precipitation";

    /** 颜色 + 标题 */
    public record Info(int color, String title) {}

    // ========================================================================
    // 岩石：rockId → (color, name)
    // ========================================================================

    private static final Map<String, Info> ROCK_MAP = new HashMap<>();
    static {
        rock("chalk",        0xFFF5F5DC, "Chalk");
        rock("chert",        0xFF8B8682, "Chert");
        rock("claystone",    0xFFB89A7A, "Claystone");
        rock("conglomerate", 0xFFCD853F, "Conglomerate");
        rock("dolomite",     0xFFFFB6C1, "Dolomite");
        rock("limestone",    0xFFFFFDD0, "Limestone");
        rock("shale",        0xFF696969, "Shale");
        rock("gneiss",       0xFFBEBEBE, "Gneiss");
        rock("marble",       0xFFF0F0F0, "Marble");
        rock("phyllite",     0xFF8FBC8F, "Phyllite");
        rock("quartzite",    0xFFFFE4E1, "Quartzite");
        rock("schist",       0xFFA9A9A9, "Schist");
        rock("slate",        0xFF708090, "Slate");
        rock("diorite",      0xFFD3D3D3, "Diorite");
        rock("gabbro",       0xFF4A4A4A, "Gabbro");
        rock("basalt",       0xFF2F2F2F, "Basalt");
        rock("rhyolite",     0xFFDEB887, "Rhyolite");
        rock("granite",      0xFFE8D8C0, "Granite");
        rock("dacite",       0xFFA0937A, "Dacite");
        rock("andesite",     0xFF8C7C6C, "Andesite");
    }

    private static void rock(String name, int color, String title) {
        ROCK_MAP.put("tfc:" + name, new Info(color, title));
    }

    private static final Info ROCK_UNKNOWN = new Info(0xFF808080, "Unknown");
    private static final Info ROCK_NONE    = new Info(0x40000000, "None");

    public static Info forRock(String rockId) {
        if (rockId == null || rockId.isEmpty()) return ROCK_NONE;
        return ROCK_MAP.getOrDefault(rockId, ROCK_UNKNOWN);
    }

    // ========================================================================
    // 温度：float → (color, title)，0°C 分界，每 5°C 一档
    // ========================================================================

    public static Info forTemperature(float t) {
        if (t <= -25) return new Info(0xFF00008B, "≤ -25°C");
        if (t <= -20) return new Info(0xFF0000CD, "-25 ~ -20°C");
        if (t <= -15) return new Info(0xFF4169E1, "-20 ~ -15°C");
        if (t <= -10) return new Info(0xFF6495ED, "-15 ~ -10°C");
        if (t <= -5)  return new Info(0xFF87CEEB, "-10 ~ -5°C");
        if (t <= 0)   return new Info(0xFFB0E0E6, "-5 ~ 0°C");
        if (t <= 5)   return new Info(0xFF90EE90, "0 ~ 5°C");
        if (t <= 10)  return new Info(0xFF7CFC00, "5 ~ 10°C");
        if (t <= 15)  return new Info(0xFFFFD700, "10 ~ 15°C");
        if (t <= 20)  return new Info(0xFFFFA500, "15 ~ 20°C");
        if (t <= 25)  return new Info(0xFFFF8C00, "20 ~ 25°C");
        if (t <= 30)  return new Info(0xFFFF6347, "25 ~ 30°C");
        if (t <= 35)  return new Info(0xFFFF4500, "30 ~ 35°C");
        if (t <= 40)  return new Info(0xFFDC143C, "35 ~ 40°C");
        return new Info(0xFF8B0000, "≥ 40°C");
    }

    // ========================================================================
    // 降水：float → (color, title)，每 50mm 一档
    // ========================================================================

    public static Info forRainfall(float r) {
        if (r <= 50)   return new Info(0xFF8B4513, "0 ~ 50 mm");
        if (r <= 100)  return new Info(0xFFDAA520, "50 ~ 100 mm");
        if (r <= 150)  return new Info(0xFFF0E68C, "100 ~ 150 mm");
        if (r <= 200)  return new Info(0xFF90EE90, "150 ~ 200 mm");
        if (r <= 250)  return new Info(0xFF3CB371, "200 ~ 250 mm");
        if (r <= 300)  return new Info(0xFF20B2AA, "250 ~ 300 mm");
        if (r <= 350)  return new Info(0xFF4682B4, "300 ~ 350 mm");
        if (r <= 400)  return new Info(0xFF4169E1, "350 ~ 400 mm");
        if (r <= 450)  return new Info(0xFF0000CD, "400 ~ 450 mm");
        return new Info(0xFF00008B, "≥ 500 mm");
    }
}