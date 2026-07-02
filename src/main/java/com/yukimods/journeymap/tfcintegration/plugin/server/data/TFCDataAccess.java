package com.yukimods.journeymap.tfcintegration.plugin.server.data;

import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.RockData;
import net.dries007.tfc.world.settings.RockSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * TFC 数据访问层 — 只封装 TFC API 调用，不涉及颜色逻辑。
 * 颜色计算在客户端 {@code DataColorMaps} 中。
 */
public final class TFCDataAccess {

    private TFCDataAccess() {}

    // ---- ChunkData 封装 ----

    public static ChunkData getChunkData(LevelChunk chunk) {
        return ChunkData.get((net.minecraft.world.level.chunk.ChunkAccess) chunk);
    }

    public static RockData getRockData(ChunkData data) {
        return data.getRockData();
    }

    public static RockSettings getSurfaceRock(RockData rockData) {
        return rockData.getSurfaceRock(8, 8);
    }

    /**
     * 从 RockSettings 提取岩石 ID（去掉 "raw/" 前缀）。
     * rock.raw() → "tfc:raw/granite" → 返回 "tfc:granite"
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