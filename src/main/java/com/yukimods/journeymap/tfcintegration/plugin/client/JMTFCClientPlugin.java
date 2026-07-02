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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    /** 自己追踪 toggle 状态——JM 按钮的 getToggled() 在回调中返回的是点击前的旧值 */
    private final Set<String> toggledOverlays = new HashSet<>();

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
        LOGGER.info("[Client] Init done.");
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
                boolean wasOn = toggledOverlays.contains(overlay.id);
                if (wasOn) {
                    // 关闭自己
                    button.setToggled(false);
                    toggledOverlays.remove(overlay.id);
                    overlayRenderer.toggleOverlay(overlay.id, false);
                    LOGGER.info("[Client] Toggle OFF: {}", overlay.id);
                } else {
                    // 关闭其他 overlay
                    for (OverlayDef other : OVERLAYS) {
                        if (!other.id.equals(overlay.id)
                            && toggledOverlays.contains(other.id)) {
                            if (other.button != null) other.button.setToggled(false);
                            toggledOverlays.remove(other.id);
                            overlayRenderer.removeOverlay(other.id);
                        }
                    }
                    button.setToggled(true);
                    toggledOverlays.add(overlay.id);
                    overlayRenderer.toggleOverlay(overlay.id, true);
                    LOGGER.info("[Client] Toggle ON: {}", overlay.id);
                    if (isOverworld()) sendCacheRequest();
                }
            });
            overlay.button.setTooltip(Component.translatable(overlay.desc).getString());
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
        ResourceKey<Level> dim = getClientDimension();
        if (dim != null && Level.OVERWORLD.equals(dim)) {
            PacketDistributor.sendToServer(new RequestCachePayload(dim.location()));
        }
    }

    public static void handleCacheData(CacheDataPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var plugin = getInstance();
            if (plugin != null && plugin.overlayRenderer != null) {
                LOGGER.debug("[Client] Cache data received: dim={}, {} entries",
                    payload.dimension(), payload.chunks().size());
                plugin.overlayRenderer.onCacheDataReceived(payload);
            }
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