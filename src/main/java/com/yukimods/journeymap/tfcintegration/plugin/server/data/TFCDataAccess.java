package com.yukimods.journeymap.tfcintegration.plugin.server.data;

import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.settings.RockSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * TFC 数据访问层 — 只封装 TFC API 调用，不涉及颜色逻辑。
 * 颜色计算在客户端 {@code DataColorMaps} 中。
 */
public final class TFCDataAccess {

    private TFCDataAccess() {}

    // ---- ChunkData 封装 ----

    private static ChunkData getChunkData(LevelChunk chunk) {
        return ChunkData.get((ChunkAccess) chunk);
    }

    public static String queryRockId(LevelChunk chunk, int wx, int wy, int wz) {
        var tfcData = getChunkData(chunk);
        if (tfcData == null) return "";
        var rd = tfcData.getRockData();
        if (rd == null) return "";
        var rock = rd.getSurfaceRock(wx, wz);
        if (rock == null) return "";
        return TFCDataAccess.getRockId(rock).toString();
    }

    /**
     * 从 RockSettings 提取岩石 ID（去掉 "raw/" 前缀）。
     * rock.raw() → "tfc:rock/raw/granite" → 返回 "tfc:granite"
     */
    public static ResourceLocation getRockId(RockSettings rock) {
        ResourceLocation rawId = BuiltInRegistries.BLOCK.getKey(rock.raw());
        String path = rawId.getPath();
        if (path.contains("/")) {
            path = path.substring(path.lastIndexOf('/') + 1);
        }
        return ResourceLocation.fromNamespaceAndPath(rawId.getNamespace(), path);
    }
}
