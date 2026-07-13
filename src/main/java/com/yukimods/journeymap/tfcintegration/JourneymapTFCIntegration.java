package com.yukimods.journeymap.tfcintegration;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.yukimods.journeymap.tfcintegration.config.ModConfig;
import com.yukimods.journeymap.tfcintegration.network.CacheDataPayload;
import com.yukimods.journeymap.tfcintegration.network.RequestCachePayload;
import com.yukimods.journeymap.tfcintegration.plugin.client.JMTFCClientPlugin;
import com.yukimods.journeymap.tfcintegration.plugin.server.JMTFCServerPlugin;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(JourneymapTFCIntegration.MODID)
public class JourneymapTFCIntegration {

    public static final String MODID = "journeymaptfcintegration";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public JourneymapTFCIntegration(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("JourneyMap TFC Integration initializing...");

        modContainer.registerConfig(Type.CLIENT, ModConfig.CLIENT_SPEC);

        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar(MODID);

            // C→S: 客户端请求全量缓存
            registrar.playToServer(
                RequestCachePayload.TYPE,
                RequestCachePayload.CODEC,
                JMTFCServerPlugin::handleRequestCache);

            // S→C: 服务端返回缓存数据
            registrar.playToClient(
                CacheDataPayload.TYPE,
                CacheDataPayload.CODEC,
                JMTFCClientPlugin::handleCacheData);
        });

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, this::onRegisterCommands);

        LOGGER.info("JourneyMap TFC Integration initialized successfully.");
    }

    // ========================================================================
    // /jmtfc clearcache — 清除全部 TFC 缓存（内存 + 磁盘）
    // ========================================================================

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("jmtfc")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("clearcache")
                .executes(this::executeClearCache)));
        LOGGER.info("[Server] Registered /jmtfc clearcache command");
    }

    private int executeClearCache(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();

        var plugin = JMTFCServerPlugin.getInstance();
        if (plugin == null || plugin.getServerCache() == null) {
            source.sendFailure(Component.literal("JMTFC server plugin not initialized yet."));
            return 0;
        }

        plugin.getServerCache().clearAll(level);
        source.sendSuccess(() -> Component.literal("JMTFC cache cleared (memory + disk)."), true);
        return 1;
    }
}
