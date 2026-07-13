package com.yukimods.journeymap.tfcintegration.plugin.server;

import com.yukimods.journeymap.tfcintegration.JourneymapTFCIntegration;
import com.yukimods.journeymap.tfcintegration.network.RequestCachePayload;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.server.IServerAPI;
import journeymap.api.v2.server.IServerPlugin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JourneyMapPlugin(apiVersion = "2.0")
public class JMTFCServerPlugin implements IServerPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMTFCServerPlugin.class);
    private static final int WARMUP_RADIUS_CHUNKS = 16;
    private static final int RESPONSE_RADIUS_CHUNKS = 600;

    private static JMTFCServerPlugin instance;
    public static JMTFCServerPlugin getInstance() { return instance; }

    private ServerCache serverCache;
    public ServerCache getServerCache() { return serverCache; }

    @Override
    public String getModId() {
        return JourneymapTFCIntegration.MODID;
    }

    @Override
    public void initialize(IServerAPI api) {
        instance = this;
        this.serverCache = new ServerCache();
        NeoForge.EVENT_BUS.register(serverCache); // ChunkEvent.Load
        NeoForge.EVENT_BUS.register(this);        // LevelEvent.Save

        // 服务端初始化完成，立即加载磁盘缓存——不依赖客户端请求。
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            serverCache.loadOnce(server.overworld());
        } else {
            LOGGER.warn("[Server] Init: cannot get current server, deferring cache load");
        }

        LOGGER.info("[Server] Init done.");
    }

    // ========================================================================
    // 处理缓存请求
    // ========================================================================

    public static void handleRequestCache(RequestCachePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var plugin = getInstance();
            if (plugin == null) {
                LOGGER.warn("[Server] handleRequestCache() skipped: plugin instance is null (not initialized yet?)");
                return;
            }
            if (plugin.serverCache == null) {
                LOGGER.warn("[Server] handleRequestCache() skipped: serverCache is null");
                return;
            }
            if (!(ctx.player() instanceof ServerPlayer player)) {
                LOGGER.warn("[Server] handleRequestCache() skipped: ctx.player() is not ServerPlayer");
                return;
            }

            var level = player.serverLevel();
            LOGGER.debug("[Server] handleRequestCache(): player={}, dim={}, base=({},{})",
                player.getGameProfile().getName(), level.dimension().location(),
                payload.baseCX(), payload.baseCZ());

            plugin.serverCache.warmup(level, player.blockPosition(), WARMUP_RADIUS_CHUNKS);
            var response = plugin.serverCache.buildPayload(level.dimension(),
                payload.baseCX(), payload.baseCZ(), RESPONSE_RADIUS_CHUNKS);
            LOGGER.debug("[Server] handleRequestCache() response: {} entries",
                response.chunks().size());
            PacketDistributor.sendToPlayer(player, response);
        });
    }

    // ========================================================================
    // 世界保存时持久化缓存
    // ========================================================================

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel level) {
            serverCache.save(level);
        }
    }
}
