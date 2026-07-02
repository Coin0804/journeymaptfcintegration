package com.yukimods.journeymap.tfcintegration.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Client-side configuration for JourneyMap TFC Integration.
 * 客户端配置 — 控制叠加层可见性、不透明度和颜色设置。
 * <p>
 * Controls overlay visibility, opacity, and color settings.
 */
public class ModConfig {

    public static final ModConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;

    // 便捷静态引用 / Convenience accessors
    public static ModConfigSpec.BooleanValue rockLayerEnabled;
    public static ModConfigSpec.BooleanValue precipitationEnabled;
    public static ModConfigSpec.BooleanValue temperatureEnabled;
    public static ModConfigSpec.DoubleValue overlayOpacity;
    public static ModConfigSpec.BooleanValue showLegend;

    static {
        final Pair<ClientConfig, ModConfigSpec> specPair =
            new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public static class ClientConfig {

        public ClientConfig(ModConfigSpec.Builder builder) {
            builder.comment("JourneyMap TFC Integration - Client Configuration")
                   .push("overlays");

            rockLayerEnabled = builder
                .comment("在地图上显示 TFC 岩层类型叠加层 / Show TFC rock layer types overlay on the map.")
                .define("rockLayerEnabled", true);

            precipitationEnabled = builder
                .comment("在地图上显示 TFC 降水热力图叠加层 / Show TFC precipitation/rainfall heatmap overlay on the map.")
                .define("precipitationEnabled", true);

            temperatureEnabled = builder
                .comment("在地图上显示 TFC 温度热力图叠加层 / Show TFC temperature heatmap overlay on the map.")
                .define("temperatureEnabled", true);

            overlayOpacity = builder
                .comment("叠加层不透明度 (0.0 = 完全透明, 1.0 = 完全不透明) / Opacity of the overlay layers (0.0 = transparent, 1.0 = opaque).")
                .defineInRange("overlayOpacity", 0.6, 0.0, 1.0);

            showLegend = builder
                .comment("在地图上显示当前激活叠加层的颜色图例 / Show color legend on the map for active overlays.")
                .define("showLegend", true);

            builder.pop();
        }
    }
}
