package dev.lifesteal.lifesteal.client;

import dev.lifesteal.lifesteal.client.screen.MeteorDetectedScreen;
import dev.lifesteal.lifesteal.client.discord.DiscordRpcManager;
import dev.lifesteal.lifesteal.config.LifestealClientConfig;
import dev.lifesteal.lifesteal.config.LifestealConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LifestealClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("lifesteal-client");
    private static final String METEOR_MOD_ID = "meteor-client";
    private static final String METEOR_ALT_MOD_ID = "meteor_client";
    private static final int WARNING_DELAY_TICKS = 40;
    private static boolean meteorDetected;
    private static boolean warningShown;
    private static int warningDelayTicks;
    private static int rpcConfigSyncTicker;
    private static LifestealClientConfig clientConfig;

    @Override
    public void onInitializeClient() {
        clientConfig = LifestealClientConfig.load();
        clientConfig.save();

        meteorDetected = FabricLoader.getInstance().isModLoaded(METEOR_MOD_ID)
                || FabricLoader.getInstance().isModLoaded(METEOR_ALT_MOD_ID);
        LOGGER.info("Meteor detection at init: {}", meteorDetected);
        warningDelayTicks = meteorDetected ? WARNING_DELAY_TICKS : 0;

        ClientTickEvents.END_CLIENT_TICK.register(LifestealClient::onClientTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> DiscordRpcManager.shutdown());
    }

    private static void onClientTick(MinecraftClient client) {
        if (++rpcConfigSyncTicker >= 20) {
            rpcConfigSyncTicker = 0;
            LifestealClientConfig.loadDiscordRpcToggle().ifPresent(value -> {
                if (clientConfig.enableCustomDiscordRpc != value) {
                    clientConfig.enableCustomDiscordRpc = value;
                    LOGGER.info("Synced RPC toggle from disk: enableCustomDiscordRpc={}", value);
                    if (!value) {
                        DiscordRpcManager.shutdown();
                    } else {
                        DiscordRpcManager.resetState();
                    }
                }
            });
        }

        DiscordRpcManager.tick(client, clientConfig.enableCustomDiscordRpc);

        if (!meteorDetected || warningShown) {
            return;
        }

        if (warningDelayTicks > 0) {
            warningDelayTicks--;
            return;
        }

        if (client.getOverlay() != null) {
            return;
        }

        if (client.currentScreen == null && client.world == null) {
            return;
        }

        warningShown = true;
        LOGGER.info("Opening Meteor warning screen on tick.");
        if (client.world != null) {
            client.disconnect(Text.literal("Lifesteal Mod detected Meteor Client."));
        }
        client.setScreen(new MeteorDetectedScreen());
    }

    public static LifestealClientConfig getClientConfig() {
        if (clientConfig == null) {
            clientConfig = LifestealClientConfig.load();
        }
        return clientConfig;
    }

    public static void applyClientConfig(LifestealClientConfig updatedConfig) {
        boolean oldRpcEnabled = clientConfig != null && clientConfig.enableCustomDiscordRpc;
        clientConfig = updatedConfig.copy();
        clientConfig.save();
        LOGGER.info("Applied client config. enableCustomDiscordRpc={}", clientConfig.enableCustomDiscordRpc);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() != null && !client.getServer().isDedicated()) {
            LifestealConfig.applySingleplayerOverrides(clientConfig);
        }
        if (!clientConfig.enableCustomDiscordRpc) {
            DiscordRpcManager.shutdown();
        } else if (!oldRpcEnabled) {
            DiscordRpcManager.resetState();
        }
    }
}
