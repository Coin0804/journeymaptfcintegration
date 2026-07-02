package com.yukimods.journeymap.tfcintegration.plugin.client;

import com.yukimods.journeymap.tfcintegration.JourneymapTFCIntegration;
import com.yukimods.journeymap.tfcintegration.network.CacheDataPayload;
import com.yukimods.journeymap.tfcintegration.plugin.client.data.DataColorMaps;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端叠加层渲染器。
 * 以 3×3 chunk 为最小粒度——直接从服务端 base pos 计算颜色、合并、渲染，不做展开。
 */
public class OverlayRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverlayRenderer.class);

    private final IClientAPI api;
    private final float opacity;

    /** 活跃的 overlayId → PolygonOverlay 列表（用于 remove） */
    private final Map<String, List<PolygonOverlay>> activeOverlays = new LinkedHashMap<>();

    /**
     * 客户端缓存：overlayId → 3×3 base ChunkPos → (color, title)。
     * key 是服务端发来的 base pos（floorDiv/3*3），一个条目代表 3×3 区块。
     */
    private final Map<String, Map<ChunkPos, DataColorMaps.Info>> overlayCache = new HashMap<>();

    private ResourceKey<Level> currentDimension;

    /** 合并后的矩形（base 坐标，每个单位 = 3×3 chunk） */
    private record MergedRect(int minCX, int minCZ, int maxCX, int maxCZ) {}

    public OverlayRenderer(IClientAPI api, float opacity) {
        this.api = api;
        this.opacity = opacity;
    }

    // ========================================================================
    // 接收服务端缓存
    // ========================================================================

    public void onCacheDataReceived(CacheDataPayload payload) {
        currentDimension = ResourceKey.create(Registries.DIMENSION, payload.dimension());
        overlayCache.clear();

        for (var e : payload.chunks()) {
            ChunkPos cp = new ChunkPos(e.chunkX(), e.chunkZ());
            putCache(DataColorMaps.OVERLAY_ROCK,   cp, DataColorMaps.forRock(e.rockId()));
            putCache(DataColorMaps.OVERLAY_TEMP,   cp, DataColorMaps.forTemperature(e.temperature()));
            putCache(DataColorMaps.OVERLAY_PRECIP, cp, DataColorMaps.forRainfall(e.rainfall()));
        }

        LOGGER.debug("[Client] Cache processed: {} base entries, dim={}",
            payload.chunks().size(), currentDimension);

        for (String id : activeOverlays.keySet()) {
            renderOverlay(id);
        }
    }

    private void putCache(String overlayId, ChunkPos cp, DataColorMaps.Info info) {
        overlayCache.computeIfAbsent(overlayId, k -> new LinkedHashMap<>()).put(cp, info);
    }

    // ========================================================================
    // Toggle
    // ========================================================================

    public void toggleOverlay(String overlayId, boolean enabled) {
        if (enabled) {
            activeOverlays.put(overlayId, new ArrayList<>());
            if (overlayCache.containsKey(overlayId)) {
                renderOverlay(overlayId);
            }
        } else {
            removeOverlay(overlayId);
        }
    }

    // ========================================================================
    // 渲染
    // ========================================================================

    private void renderOverlay(String overlayId) {
        removeOverlay(overlayId);

        Map<ChunkPos, DataColorMaps.Info> chunks = overlayCache.get(overlayId);
        if (chunks == null || chunks.isEmpty()) return;

        // 按颜色分组
        Map<Integer, Set<ChunkPos>> byColor = new LinkedHashMap<>();
        for (var e : chunks.entrySet()) {
            byColor.computeIfAbsent(e.getValue().color(), k -> new HashSet<>())
                   .add(e.getKey());
        }

        List<PolygonOverlay> newOverlays = new ArrayList<>();

        for (var group : byColor.entrySet()) {
            int color = group.getKey();
            Set<ChunkPos> sameColor = group.getValue();
            String title = chunks.get(sameColor.iterator().next()).title();

            List<MergedRect> rects = mergeChunks(sameColor);
            for (MergedRect r : rects) {
                newOverlays.add(createPolygon(overlayId, color, title, r));
            }
        }

        for (PolygonOverlay o : newOverlays) {
            try { api.show(o); } catch (Exception ignored) {}
        }
        activeOverlays.put(overlayId, newOverlays);
        LOGGER.debug("[Client] Rendered {}: {} colors → {} polygons",
            overlayId, byColor.size(), newOverlays.size());
    }

    // ========================================================================
    // 多边形：3×3 base 坐标 → 方块坐标（baseCX*16 到 (baseCX+3)*16）
    // ========================================================================

    @SuppressWarnings("deprecation")
    private PolygonOverlay createPolygon(String overlayId, int color, String title, MergedRect r) {
        int bx1 = r.minCX(), bx2 = r.maxCX() + 3; // +3 chunk
        int bz1 = r.minCZ(), bz2 = r.maxCZ() + 3;

        int minX = bx1 << 4, maxX = bx2 << 4;
        int minZ = bz1 << 4, maxZ = bz2 << 4;

        MapPolygon poly = new MapPolygon(
            new BlockPos(minX, 0, minZ),
            new BlockPos(maxX, 0, minZ),
            new BlockPos(maxX, 0, maxZ),
            new BlockPos(minX, 0, maxZ));

        ShapeProperties shape = new ShapeProperties()
            .setFillColor(color)
            .setFillOpacity(opacity)
            .setStrokeColor(0x00000000)
            .setStrokeWidth(0f);

        PolygonOverlay overlay = new PolygonOverlay(
            JourneymapTFCIntegration.MODID, currentDimension, shape, poly);
        overlay.setTitle(title);
        overlay.setOverlayGroupName(overlayId);
        overlay.setDisplayOrder(100);
        overlay.setActiveUIs(Context.UI.Fullscreen);
        overlay.setActiveMapTypes(Context.MapType.all());
        return overlay;
    }

    // ========================================================================
    // 移除
    // ========================================================================

    void removeOverlay(String overlayId) {
        List<PolygonOverlay> overlays = activeOverlays.remove(overlayId);
        if (overlays != null) {
            for (PolygonOverlay o : overlays) {
                try { api.remove(o); } catch (Exception ignored) {}
            }
            LOGGER.debug("[Client] Removed {}: {} polygons", overlayId, overlays.size());
        }
    }

    public void clearAll() {
        for (String id : new ArrayList<>(activeOverlays.keySet())) {
            removeOverlay(id);
        }
        overlayCache.clear();
        LOGGER.debug("[Client] All overlays cleared");
    }

    // ========================================================================
    // 维度切换
    // ========================================================================

    public void onDimensionChange(ResourceKey<Level> newDim) {
        if (!newDim.equals(currentDimension)) {
            clearAll();
            currentDimension = newDim;
        }
    }

    public ResourceKey<Level> getCurrentDimension() {
        return currentDimension;
    }

    // ========================================================================
    // 合并算法：贪婪矩形扩张，step=3（base pos 间隔为 3）
    // ========================================================================

    static List<MergedRect> mergeChunks(Set<ChunkPos> chunks) {
        Set<ChunkPos> unmerged = new HashSet<>(chunks);
        List<MergedRect> result = new ArrayList<>();

        while (!unmerged.isEmpty()) {
            ChunkPos seed = unmerged.iterator().next();
            int minX = seed.x, maxX = seed.x;
            int minZ = seed.z, maxZ = seed.z;

            boolean expanded;
            do {
                expanded = false;
                while (rowExists(unmerged, maxX + 3, minZ, maxZ)) { maxX += 3; expanded = true; }
                while (rowExists(unmerged, minX - 3, minZ, maxZ)) { minX -= 3; expanded = true; }
                while (colExists(unmerged, minX, maxX, maxZ + 3)) { maxZ += 3; expanded = true; }
                while (colExists(unmerged, minX, maxX, minZ - 3)) { minZ -= 3; expanded = true; }
            } while (expanded);

            for (int x = minX; x <= maxX; x += 3)
                for (int z = minZ; z <= maxZ; z += 3)
                    unmerged.remove(new ChunkPos(x, z));

            result.add(new MergedRect(minX, minZ, maxX, maxZ));
        }
        return result;
    }

    private static boolean rowExists(Set<ChunkPos> s, int x, int minZ, int maxZ) {
        for (int z = minZ; z <= maxZ; z += 3)
            if (!s.contains(new ChunkPos(x, z))) return false;
        return true;
    }

    private static boolean colExists(Set<ChunkPos> s, int minX, int maxX, int z) {
        for (int x = minX; x <= maxX; x += 3)
            if (!s.contains(new ChunkPos(x, z))) return false;
        return true;
    }
}