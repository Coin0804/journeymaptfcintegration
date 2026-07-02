package com.yukimods.journeymap.tfcintegration;

import com.yukimods.journeymap.tfcintegration.config.ModConfig;
import com.yukimods.journeymap.tfcintegration.network.CacheDataPayload;
import com.yukimods.journeymap.tfcintegration.network.RequestCachePayload;
import com.yukimods.journeymap.tfcintegration.plugin.client.JMTFCClientPlugin;
import com.yukimods.journeymap.tfcintegration.plugin.server.JMTFCServerPlugin;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
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

        LOGGER.info("JourneyMap TFC Integration initialized successfully.");
    }
}
