package com.yukimods.journeymap.tfcintegration.network;

import com.yukimods.journeymap.tfcintegration.JourneymapTFCIntegration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * S→C：服务端返回某维度的全部原始 TFC 缓存数据。
 * 不含颜色——颜色由客户端根据原始数据自行计算。
 * <p>
 * Server returns all raw TFC chunk data for a dimension.
 * Contains no color info — the client computes colors from raw data.
 */
public record CacheDataPayload(ResourceLocation dimension, List<ChunkEntry> chunks)
    implements CustomPacketPayload {

    /**
     * 单个区块的原始 TFC 数据。
     * rockId: "namespace:path" 格式，空字符串表示无数据。
     */
    public record ChunkEntry(int chunkX, int chunkZ, String rockId, float temperature, float rainfall) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ChunkEntry> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.VAR_INT,      ChunkEntry::chunkX,
                ByteBufCodecs.VAR_INT,      ChunkEntry::chunkZ,
                ByteBufCodecs.STRING_UTF8,  ChunkEntry::rockId,
                ByteBufCodecs.FLOAT,        ChunkEntry::temperature,
                ByteBufCodecs.FLOAT,        ChunkEntry::rainfall,
                ChunkEntry::new);
    }

    public static final Type<CacheDataPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(JourneymapTFCIntegration.MODID, "cache_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CacheDataPayload> CODEC =
        new StreamCodec<>() {
            @Override
            public CacheDataPayload decode(RegistryFriendlyByteBuf buf) {
                ResourceLocation dim = ResourceLocation.parse(buf.readUtf());
                int count = buf.readVarInt();
                List<ChunkEntry> list = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    list.add(ChunkEntry.STREAM_CODEC.decode(buf));
                }
                return new CacheDataPayload(dim, list);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, CacheDataPayload p) {
                buf.writeUtf(p.dimension().toString());
                buf.writeVarInt(p.chunks().size());
                for (ChunkEntry e : p.chunks()) {
                    ChunkEntry.STREAM_CODEC.encode(buf, e);
                }
            }
        };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}