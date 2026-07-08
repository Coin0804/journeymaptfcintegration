package com.yukimods.journeymap.tfcintegration.plugin.client.data;

import net.minecraft.network.chat.Component;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端颜色映射：原始 TFC 数据 → (ARGB颜色, 标题)。
 * 标题通过 i18n 解析，支持多语言。
 */
public final class DataColorMaps {

    private DataColorMaps() {}

    public static final String OVERLAY_ROCK   = "tfc_rock_layers";
    public static final String OVERLAY_TEMP   = "tfc_temperature";
    public static final String OVERLAY_PRECIP = "tfc_precipitation";

    public record Info(int color, String title) {}

    // ========================================================================
    // i18n 快捷方法
    // ========================================================================

    private static final String RK = "journeymap.overlay.tfc_rock_layers.rock.";
    private static final String TK = "journeymap.overlay.tfc_temperature.";
    private static final String PK = "journeymap.overlay.tfc_precipitation.";

    private static String t(String key, Object... args) {
        String template = Component.translatable(key).getString();
        return args.length == 0 ? template : MessageFormat.format(template, args);
    }

    // ========================================================================
    // 岩石：rockId → (color, resolved_title)
    // ========================================================================

    private static final Map<String, Info> ROCK_MAP = new HashMap<>();
    static {
        rock("chalk",        0xFFF5F5DC);
        rock("chert",        0xFF8B8682);
        rock("claystone",    0xFFB89A7A);
        rock("conglomerate", 0xFFCD853F);
        rock("dolomite",     0xFFFFB6C1);
        rock("limestone",    0xFFFFFDD0);
        rock("shale",        0xFF696969);
        rock("gneiss",       0xFFBEBEBE);
        rock("marble",       0xFFF0F0F0);
        rock("phyllite",     0xFF8FBC8F);
        rock("quartzite",    0xFFFFE4E1);
        rock("schist",       0xFFA9A9A9);
        rock("slate",        0xFF708090);
        rock("diorite",      0xFFD3D3D3);
        rock("gabbro",       0xFF4A4A4A);
        rock("basalt",       0xFF2F2F2F);
        rock("rhyolite",     0xFFDEB887);
        rock("granite",      0xFFE8D8C0);
        rock("dacite",       0xFFA0937A);
        rock("andesite",     0xFF8C7C6C);
    }

    private static void rock(String name, int color) {
        ROCK_MAP.put("tfc:" + name, new Info(color, RK + name));
    }

    private static final Info ROCK_NONE = new Info(0x40000000, RK + "none");

    public static Info forRock(String rockId) {
        if (rockId == null || rockId.isEmpty()) return ROCK_NONE;
        var info = ROCK_MAP.get(rockId);
        if (info == null) return new Info(0xFF808080, t(RK + "unknown"));
        return new Info(info.color(), t(info.title())); // 解析 i18n key → 显示文本
    }

    // ========================================================================
    // 温度：float → (color, resolved_title)
    // ========================================================================

    public static Info forTemperature(float t) {
        if (t <= -25) return ti(0xFF00008B, "below", -25);
        if (t <= -20) return ti(0xFF0000CD, "range", -25, -20);
        if (t <= -15) return ti(0xFF4169E1, "range", -20, -15);
        if (t <= -10) return ti(0xFF6495ED, "range", -15, -10);
        if (t <= -5)  return ti(0xFF87CEEB, "range", -10, -5);
        if (t <= 0)   return ti(0xFFB0E0E6, "range", -5, 0);
        if (t <= 5)   return ti(0xFF90EE90, "range", 0, 5);
        if (t <= 10)  return ti(0xFF7CFC00, "range", 5, 10);
        if (t <= 15)  return ti(0xFFFFD700, "range", 10, 15);
        if (t <= 20)  return ti(0xFFFFA500, "range", 15, 20);
        if (t <= 25)  return ti(0xFFFF8C00, "range", 20, 25);
        if (t <= 30)  return ti(0xFFFF6347, "range", 25, 30);
        if (t <= 35)  return ti(0xFFFF4500, "range", 30, 35);
        if (t <= 40)  return ti(0xFFDC143C, "range", 35, 40);
        return ti(0xFF8B0000, "above", 40);
    }

    private static Info ti(int color, String suffix, Object... args) {
        String template = Component.translatable(TK + suffix).getString();
        return new Info(color, MessageFormat.format(template, args));
    }

    // ========================================================================
    // 降水：float → (color, resolved_title)
    // ========================================================================

    // Note: TFC rainfall values range up to 500mm+; the "above" threshold is 450
    public static Info forRainfall(float r) {
        if (r <= 50)   return pi(0xFF8B4513, "range", 0, 50);
        if (r <= 100)  return pi(0xFFDAA520, "range", 50, 100);
        if (r <= 150)  return pi(0xFFF0E68C, "range", 100, 150);
        if (r <= 200)  return pi(0xFF90EE90, "range", 150, 200);
        if (r <= 250)  return pi(0xFF3CB371, "range", 200, 250);
        if (r <= 300)  return pi(0xFF20B2AA, "range", 250, 300);
        if (r <= 350)  return pi(0xFF4682B4, "range", 300, 350);
        if (r <= 400)  return pi(0xFF4169E1, "range", 350, 400);
        if (r <= 450)  return pi(0xFF0000CD, "range", 400, 450);
        return pi(0xFF00008B, "above", 450);
    }

    private static Info pi(int color, String suffix, Object... args) {
        String template = Component.translatable(PK + suffix).getString();
        return new Info(color, MessageFormat.format(template, args));
    }
}
