package com.yukimods.journeymap.tfcintegration.plugin.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yukimods.journeymap.tfcintegration.network.CacheDataPayload;
import com.yukimods.journeymap.tfcintegration.plugin.server.data.TFCDataAccess;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端 TFC 数据缓存。
 * 3×3 区块分组 + 世界存档持久化。
 */
public class ServerCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCache.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CACHE_FILE = "jmtfc_cache.json";

    record RawChunkData(String rockId, float temperature, float rainfall) {}

    private final Map<ResourceKey<Level>, Map<ChunkPos, RawChunkData>> cache = new ConcurrentHashMap<>();
    private boolean loadedFromDisk;

    // ========================================================================
    // ChunkEvent.Load — 仅 base 位置
    // ========================================================================

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        ChunkPos cp = chunk.getPos();
        if (!isBasePos(cp)) return;

        ChunkPos base = cp;
        var dimCache = cache.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>());
        if (dimCache.containsKey(base)) return;

        dimCache.put(base, queryData(chunk, level));
        LOGGER.debug("[Server] ChunkLoad cached base({},{}): rock={}, temp={}, rain={}, cacheSize={}",
            base.x, base.z, dimCache.get(base).rockId(),
            dimCache.get(base).temperature(), dimCache.get(base).rainfall(),
            dimCache.size());
    }

    // ========================================================================
    // 预热
    // ========================================================================

    public void warmup(ServerLevel level, BlockPos center, int radiusChunks) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            LOGGER.warn("[Server] warmup() skipped: dim is not OVERWORLD ({})",
                level.dimension().location());
            return;
        }

        var dimCache = cache.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>());
        ChunkPos cp = new ChunkPos(center);

        int minCX = cp.x - radiusChunks;
        int maxCX = cp.x + radiusChunks;
        int minCZ = cp.z - radiusChunks;
        int maxCZ = cp.z + radiusChunks;

        int cachedBefore = dimCache.size();

        for (int bx = Math.floorDiv(minCX, 3) * 3; bx <= maxCX; bx += 3) {
            for (int bz = Math.floorDiv(minCZ, 3) * 3; bz <= maxCZ; bz += 3) {
                ChunkPos base = new ChunkPos(bx, bz);
                if (dimCache.containsKey(base)) continue;
                if (bx < minCX || bx > maxCX || bz < minCZ || bz > maxCZ) continue;

                if (level.getChunkSource().hasChunk(bx, bz)) {
                    LevelChunk chunk = level.getChunk(bx, bz);
                    dimCache.put(base, queryData(chunk, level));
                }
            }
        }

        LOGGER.debug("[Server] warmup() done: +{} entries (total: {})",
            dimCache.size() - cachedBefore, dimCache.size());
    }

    private static RawChunkData queryData(LevelChunk chunk, ServerLevel level) {
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        int wx = (cx << 4) + 8, wz = (cz << 4) + 8;
        int wy = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, 8, 8);
        BlockPos wsp = new BlockPos(wx, wy, wz);

        String rockId = TFCDataAccess.queryRockId(chunk, wx, wy, wz);
        var climate = Climate.get(level);
        float temperature = climate != null ? climate.getAverageTemperature(level, wsp) : 0f;
        float rainfall = climate != null ? climate.getAverageRainfall(level, wsp) : 0f;

        return new RawChunkData(rockId, temperature, rainfall);
    }

    // ========================================================================
    // 构建网络响应
    // ========================================================================

    public CacheDataPayload buildPayload(ResourceKey<Level> dim, int centerCX, int centerCZ, int radiusChunks) {
        var dimCache = cache.get(dim);
        List<CacheDataPayload.ChunkEntry> entries = new ArrayList<>();

        int totalInCache = dimCache != null ? dimCache.size() : 0;

        if (dimCache != null) {
            for (var e : dimCache.entrySet()) {
                ChunkPos cp = e.getKey();
                if (Math.abs(cp.x - centerCX) > radiusChunks || Math.abs(cp.z - centerCZ) > radiusChunks) continue;
                RawChunkData d = e.getValue();
                entries.add(new CacheDataPayload.ChunkEntry(
                    cp.x, cp.z, d.rockId(), d.temperature(), d.rainfall()));
            }
        }

        LOGGER.debug("[Server] buildPayload(): dim={}, center=({},{}), totalInCache={}, filtered={}",
            dim.location(), centerCX, centerCZ, totalInCache, entries.size());

        return new CacheDataPayload(dim.location(), entries);
    }

    // ========================================================================
    // 持久化
    // ========================================================================

    public void loadOnce(ServerLevel level) {
        if (loadedFromDisk) return;
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        // 到达这里 = 主世界 + 首次调用。无论文件存不存在、加载成不成功，
        // 方法结束时 loadedFromDisk 都会置 true，让 save() 知道可以安全持久化。
        Path path = getSavePath(level);
        LOGGER.debug("[Server] loadOnce() path: {}", path.toAbsolutePath());

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                DiskFile data = GSON.fromJson(reader, DiskFile.class);
                if (data == null || data.entries == null) {
                    LOGGER.warn("[Server] loadOnce() failed: parsed data is null");
                } else {
                    var dimCache = cache.computeIfAbsent(Level.OVERWORLD, k -> new ConcurrentHashMap<>());
                    int memoryBefore = dimCache.size();
                    int fromDisk = 0, overwritten = 0;

                    for (DiskEntry e : data.entries) {
                        ChunkPos cp = new ChunkPos(e.cx, e.cz);
                        boolean existed = dimCache.containsKey(cp);
                        dimCache.put(cp, new RawChunkData(e.rock, e.temp, e.rain));
                        fromDisk++;
                        if (existed) overwritten++;
                    }

                    LOGGER.info("[Server] loadOnce() OK: disk={}, memoryBefore={}, overwritten={}, new={}, total={}",
                        fromDisk, memoryBefore, overwritten, fromDisk - overwritten, dimCache.size());
                }
            } catch (Exception e) {
                LOGGER.warn("[Server] loadOnce() failed to load cache: {}", e.toString(), e);
            }
        } else {
            LOGGER.info("[Server] loadOnce() no cache file (new world?), starting fresh");
        }

        loadedFromDisk = true;
    }

    public void save(ServerLevel level) {
        if (!loadedFromDisk) {
            LOGGER.debug("[Server] save() skipped: loadOnce() not yet called, holding to protect disk cache");
            return;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            LOGGER.debug("[Server] save() skipped: dim is not OVERWORLD ({})",
                level.dimension().location());
            return;
        }

        var dimCache = cache.get(Level.OVERWORLD);
        if (dimCache == null || dimCache.isEmpty()) {
            LOGGER.debug("[Server] save() skipped: cache is empty");
            return;
        }

        List<DiskEntry> entries = new ArrayList<>();
        for (var e : dimCache.entrySet()) {
            RawChunkData d = e.getValue();
            entries.add(new DiskEntry(e.getKey().x, e.getKey().z, d.rockId(), d.temperature(), d.rainfall()));
        }

        Path path = getSavePath(level);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(new DiskFile(entries), writer);
            }
            LOGGER.info("[Server] save() OK: {} entries saved to {}", entries.size(), path.toAbsolutePath());
        } catch (Exception e) {
            LOGGER.warn("[Server] save() failed: {} — {}", path.toAbsolutePath(), e.toString(), e);
        }
    }

    private static Path getSavePath(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT)
            .resolve("data").resolve(CACHE_FILE);
    }

    private static class DiskFile { List<DiskEntry> entries; DiskFile(List<DiskEntry> e) { this.entries = e; } }
    private static class DiskEntry { int cx, cz; String rock; float temp, rain;
        DiskEntry(int cx, int cz, String r, float t, float p) { this.cx=cx; this.cz=cz; this.rock=r; this.temp=t; this.rain=p; } }

    // ========================================================================
    // 缓存清理（由 /jmtfc clearcache 命令调用）
    // ========================================================================

    public void clearAll(ServerLevel level) {
        var dimCache = cache.remove(Level.OVERWORLD);
        int memoryCleared = dimCache != null ? dimCache.size() : 0;
        // 不重置 loadedFromDisk —— 清除是主动行为，后续新数据应正常持久化。

        Path path = getSavePath(level);
        try {
            Files.deleteIfExists(path);
            LOGGER.info("[Server] clearAll(): removed {} memory entries, deleted {}",
                memoryCleared, path.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.warn("[Server] clearAll(): cleared {} memory entries, but failed to delete {}: {}",
                memoryCleared, path.toAbsolutePath(), e.toString());
        }
    }

    // ========================================================================
    // 工具
    // ========================================================================

    private static boolean isBasePos(ChunkPos cp) {
        return cp.x % 3 == 0 && cp.z % 3 == 0;
    }
}
