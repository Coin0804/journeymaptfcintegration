package com.yukimods.journeymap.tfcintegration.plugin.client;

import com.yukimods.journeymap.tfcintegration.JourneymapTFCIntegration;
import com.yukimods.journeymap.tfcintegration.config.ModConfig;
import com.yukimods.journeymap.tfcintegration.network.CacheDataPayload;
import com.yukimods.journeymap.tfcintegration.network.RequestCachePayload;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.event.DisplayUpdateEvent;
import journeymap.api.v2.client.event.FullscreenDisplayEvent;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * JourneyMap 客户端插件 — 仅主世界生效。
 * Toggle 按钮 + 维度切换 + 接收缓存数据。
 */
@JourneyMapPlugin(apiVersion = "2.0")
public class JMTFCClientPlugin implements IClientPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMTFCClientPlugin.class);

    // ---- 叠加层定义（仅客户端使用，name/desc 是 i18n key） ----
    private static final List<OverlayDef> OVERLAYS = List.of(
        new OverlayDef("tfc_rock_layers",   "journeymap.overlay.tfc_rock_layers.name",   "journeymap.overlay.tfc_rock_layers.description",   "textures/gui/overlay_rock_layers.png"),
        new OverlayDef("tfc_temperature",   "journeymap.overlay.tfc_temperature.name",   "journeymap.overlay.tfc_temperature.description",   "textures/gui/overlay_temperature.png"),
        new OverlayDef("tfc_precipitation", "journeymap.overlay.tfc_precipitation.name", "journeymap.overlay.tfc_precipitation.description", "textures/gui/overlay_precipitation.png")
    );

    private static JMTFCClientPlugin instance;
    public static JMTFCClientPlugin getInstance() { return instance; }

    private OverlayRenderer overlayRenderer;
    /** 当前激活的叠加层 ID（null = 无），互斥单选 */
    private String activeOverlayId;

    @Override
    public String getModId() {
        return JourneymapTFCIntegration.MODID;
    }

    @Override
    public void initialize(IClientAPI api) {
        instance = this;
        this.overlayRenderer = new OverlayRenderer(api, ModConfig.overlayOpacity.get().floatValue());

        ClientEventRegistry.ADDON_BUTTON_DISPLAY_EVENT.subscribe(getModId(), this::onAddonButtonDisplay);
        ClientEventRegistry.DISPLAY_UPDATE_EVENT.subscribe(getModId(), this::onDisplayUpdate);

        api.removeAll(getModId());
        LOGGER.info("[Client] Init done. activeOverlayId={}, overlayRenderer={}",
            activeOverlayId, overlayRenderer != null);
    }

    // ========================================================================
    // Toggle 按钮 — 互斥单选，仅主世界
    // ========================================================================

    public void onAddonButtonDisplay(FullscreenDisplayEvent.AddonButtonDisplayEvent event) {
        var display = event.getThemeButtonDisplay();

        for (OverlayDef overlay : OVERLAYS) {
            ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(
                JourneymapTFCIntegration.MODID, overlay.iconPath);
            String displayName = Component.translatable(overlay.name).getString();

            overlay.button = display.addThemeToggleButton(displayName, icon, false, button -> {
                // 回调只负责表达意图：切换目标 overlay
                String target = overlay.id.equals(activeOverlayId) ? null : overlay.id;
                setActiveOverlay(target);
            });
            overlay.button.setTooltip(Component.translatable(overlay.desc).getString());
        }
    }

    /**
     * 激活/取消叠加层。幂等操作——相同 target 不会重复执行。
     * syncButtons 中 setToggled 即使触发回调也不会造成重入。
     */
    private void setActiveOverlay(String overlayId) {
        if (Objects.equals(activeOverlayId, overlayId)) return; // 幂等

        // 关闭旧层
        if (activeOverlayId != null) {
            overlayRenderer.toggleOverlay(activeOverlayId, false);
            LOGGER.info("[Client] Toggle OFF: {}", activeOverlayId);
        }

        // 开启新层
        activeOverlayId = overlayId;
        if (overlayId != null) {
            overlayRenderer.toggleOverlay(overlayId, true);
            LOGGER.info("[Client] Toggle ON: {}", overlayId);
            if (isOverworld()) sendCacheRequest();
        }

        // UI 按钮同步——setToggled 可能触发回调，但 setActiveOverlay 幂等，回调变 no-op
        syncButtons();
    }

    /** 同步所有按钮的 toggle 视觉状态到当前 activeOverlayId */
    private void syncButtons() {
        for (OverlayDef o : OVERLAYS) {
            if (o.button != null) {
                o.button.setToggled(o.id.equals(activeOverlayId));
            }
        }
    }

    // ========================================================================
    // 维度切换
    // ========================================================================

    public void onDisplayUpdate(DisplayUpdateEvent event) {
        if (overlayRenderer == null) return;
        ResourceKey<Level> newDim = event.uiState.dimension;
        ResourceKey<Level> current = overlayRenderer.getCurrentDimension();

        if (newDim.equals(current)) return;

        LOGGER.info("[Client] Dimension change: {} → {}", current, newDim);

        if (!Level.OVERWORLD.equals(newDim)) {
            overlayRenderer.clearAll();
        } else {
            overlayRenderer.clearAll();
            sendCacheRequest();
        }
        overlayRenderer.onDimensionChange(newDim);
    }

    // ========================================================================
    // 网络
    // ========================================================================

    private void sendCacheRequest() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) {
            LOGGER.warn("[Client] sendCacheRequest() skipped: mc.player is null");
            return;
        }
        ResourceKey<Level> dim = mc.player.level().dimension();
        if (!Level.OVERWORLD.equals(dim)) {
            LOGGER.warn("[Client] sendCacheRequest() skipped: not overworld (dim={})", dim.location());
            return;
        }

        int baseCX = Math.floorDiv(mc.player.blockPosition().getX() >> 4, 3) * 3;
        int baseCZ = Math.floorDiv(mc.player.blockPosition().getZ() >> 4, 3) * 3;
        LOGGER.debug("[Client] sendCacheRequest(): dim={}, base=({},{})",
            dim.location(), baseCX, baseCZ);
        PacketDistributor.sendToServer(new RequestCachePayload(dim.location(), baseCX, baseCZ));
    }

    public static void handleCacheData(CacheDataPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var plugin = getInstance();
            if (plugin == null) {
                LOGGER.warn("[Client] handleCacheData() skipped: plugin instance is null (not initialized yet?)");
                return;
            }
            if (plugin.overlayRenderer == null) {
                LOGGER.warn("[Client] handleCacheData() skipped: overlayRenderer is null");
                return;
            }
            LOGGER.debug("[Client] handleCacheData() received: dim={}, {} entries",
                payload.dimension(), payload.chunks().size());
            plugin.overlayRenderer.onCacheDataReceived(payload);
        });
    }

    // ========================================================================
    // 工具
    // ========================================================================

    private static boolean isOverworld() {
        return Level.OVERWORLD.equals(getClientDimension());
    }

    private static ResourceKey<Level> getClientDimension() {
        var mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.dimension() : null;
    }
}