package com.yukimods.journeymap.tfcintegration.plugin.server;

import com.yukimods.journeymap.tfcintegration.JourneymapTFCIntegration;
import com.yukimods.journeymap.tfcintegration.network.RequestCachePayload;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.server.IServerAPI;
import journeymap.api.v2.server.IServerPlugin;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JourneyMap 服务端插件 — 极简入口。
 * 只负责 JM 发现 + 创建 ServerCache，不涉及渲染。
 */
@JourneyMapPlugin(apiVersion = "2.0")
public class JMTFCServerPlugin implements IServerPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMTFCServerPlugin.class);

    private static JMTFCServerPlugin instance;
    public static JMTFCServerPlugin getInstance() { return instance; }

    private ServerCache serverCache;

    @Override
    public String getModId() {
        return JourneymapTFCIntegration.MODID;
    }

    @Override
    public void initialize(IServerAPI api) {
        instance = this;
        this.serverCache = new ServerCache();
        NeoForge.EVENT_BUS.register(serverCache);
        LOGGER.info("[Server] Init done.");
    }

    private static final int WARMUP_RADIUS_CHUNKS = 16;

    /** 处理客户端缓存请求 */
    public static void handleRequestCache(RequestCachePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var plugin = getInstance();
            if (plugin == null || plugin.serverCache == null) return;
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            var level = player.serverLevel();
            plugin.serverCache.warmup(level, player.blockPosition(), WARMUP_RADIUS_CHUNKS);
            var response = plugin.serverCache.buildPayload(level.dimension());
            LOGGER.info("[Server] Cache request: {} entries sent to {}",
                response.chunks().size(), player.getGameProfile().getName());
            PacketDistributor.sendToPlayer(player, response);
        });
    }
}