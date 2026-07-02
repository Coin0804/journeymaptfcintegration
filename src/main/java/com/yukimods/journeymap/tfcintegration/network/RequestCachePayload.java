package com.yukimods.journeymap.tfcintegration.network;

import com.yukimods.journeymap.tfcintegration.JourneymapTFCIntegration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * C→S：客户端请求某维度的全量 TFC 缓存数据。
 * Client requests all cached TFC data for a dimension.
 */
public record RequestCachePayload(ResourceLocation dimension) implements CustomPacketPayload {

    public static final Type<RequestCachePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(JourneymapTFCIntegration.MODID, "request_cache"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestCachePayload> CODEC =
        new StreamCodec<>() {
            @Override
            public RequestCachePayload decode(RegistryFriendlyByteBuf buf) {
                return new RequestCachePayload(ResourceLocation.parse(buf.readUtf()));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, RequestCachePayload p) {
                buf.writeUtf(p.dimension().toString());
            }
        };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
