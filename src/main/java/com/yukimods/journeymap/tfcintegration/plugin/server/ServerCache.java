package com.yukimods.journeymap.tfcintegration.plugin.server;

import com.yukimods.journeymap.tfcintegration.network.CacheDataPayload;
import com.yukimods.journeymap.tfcintegration.plugin.server.data.TFCDataAccess;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端 TFC 数据缓存。
 * 3×3 区块分组：只存 base pos（floorDiv/3*3），客户端渲染时自行展开。
 * 只存原始数据（岩层ID、温度、降水），不做颜色计算。
 */
public class ServerCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCache.class);

    /** 单个 3×3 base 区块的原始 TFC 数据 */
    record RawChunkData(String rockId, float temperature, float rainfall) {}

    /** 缓存: dimension → baseChunkPos(3×3对齐) → RawChunkData */
    private final Map<ResourceKey<Level>, Map<ChunkPos, RawChunkData>> cache = new ConcurrentHashMap<>();

    // ========================================================================
    // ChunkEvent.Load
    // ========================================================================

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        ChunkPos base = basePos(chunk.getPos());
        var dimCache = cache.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>());
        if (dimCache.containsKey(base)) return;

        dimCache.put(base, queryData(chunk, level));
        LOGGER.info("[Server] Cached base({},{}): rock={}",
            base.x, base.z, dimCache.get(base).rockId());
    }

    // ========================================================================
    // 预热：扫描玩家周围已加载的区块，补齐出生点附近缺失的缓存
    // ========================================================================

    public void warmup(ServerLevel level, BlockPos center, int radiusChunks) {
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        var dimCache = cache.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>());
        ChunkPos cp = new ChunkPos(center);

        int minCX = cp.x - radiusChunks;
        int maxCX = cp.x + radiusChunks;
        int minCZ = cp.z - radiusChunks;
        int maxCZ = cp.z + radiusChunks;

        int cachedBefore = dimCache.size();

        for (int bx = Math.floorDiv(minCX, 3) * 3; bx <= maxCX; bx += 3) {
            outer:
            for (int bz = Math.floorDiv(minCZ, 3) * 3; bz <= maxCZ; bz += 3) {
                ChunkPos base = new ChunkPos(bx, bz);
                if (dimCache.containsKey(base)) continue; // 已有

                // 在 3×3 base 区域内找一个已加载的 chunk 来采样
                for (int dx = 0; dx < 3; dx++) {
                    for (int dz = 0; dz < 3; dz++) {
                        int cx = bx + dx, cz = bz + dz;
                        if (cx < minCX || cx > maxCX || cz < minCZ || cz > maxCZ) continue;

                        if (level.getChunkSource().hasChunk(cx, cz)) {
                            LevelChunk chunk = level.getChunk(cx, cz);
                            dimCache.put(base, queryData(chunk, level));
                            LOGGER.info("[Server] Warmup base({},{}): rock={}",
                                base.x, base.z, dimCache.get(base).rockId());
                            continue outer; // 找到采样 → 下一个 base
                        }
                    }
                }
            }
        }

        LOGGER.info("[Server] Warmup done: +{} base entries (total: {})",
            dimCache.size() - cachedBefore, dimCache.size());
    }

    /** 用触发 chunk 的中心位置查询 TFC 数据 */
    private static RawChunkData queryData(LevelChunk chunk, ServerLevel level) {
        int sx = (chunk.getPos().x << 4) + 8, sz = (chunk.getPos().z << 4) + 8;
        int sy = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, 8, 8);
        BlockPos sp = new BlockPos(sx, sy, sz);

        String rockId = queryRockId(chunk);
        var climate = Climate.get(level);
        float temperature = climate != null ? climate.getAverageTemperature(level, sp) : 0f;
        float rainfall = climate != null ? climate.getAverageRainfall(level, sp) : 0f;

        return new RawChunkData(rockId, temperature, rainfall);
    }

    // ========================================================================
    // 构建网络响应（每个 base pos → 一个 ChunkEntry，客户端展开到 3×3）
    // ========================================================================

    public CacheDataPayload buildPayload(ResourceKey<Level> dim) {
        var dimCache = cache.get(dim);
        List<CacheDataPayload.ChunkEntry> entries = new ArrayList<>();

        if (dimCache != null) {
            for (var e : dimCache.entrySet()) {
                ChunkPos cp = e.getKey();
                RawChunkData d = e.getValue();
                entries.add(new CacheDataPayload.ChunkEntry(
                    cp.x, cp.z, d.rockId(), d.temperature(), d.rainfall()));
            }
        }

        return new CacheDataPayload(dim.location(), entries);
    }

    // ========================================================================
    // 工具
    // ========================================================================

    static ChunkPos basePos(ChunkPos cp) {
        return new ChunkPos(Math.floorDiv(cp.x, 3) * 3, Math.floorDiv(cp.z, 3) * 3);
    }

    private static String queryRockId(LevelChunk chunk) {
        try {
            var tfcData = TFCDataAccess.getChunkData(chunk);
            if (tfcData == null) return "";
            var rd = TFCDataAccess.getRockData(tfcData);
            if (rd == null) return "";
            var rock = TFCDataAccess.getSurfaceRock(rd);
            if (rock == null) return "";
            ResourceLocation id = TFCDataAccess.getRockId(rock);
            return id.toString();
        } catch (Exception e) {
            LOGGER.warn("[Server] Failed to query rock for chunk ({},{}): {}",
                chunk.getPos().x, chunk.getPos().z, e.toString());
            return "";
        }
    }
}