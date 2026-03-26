package dev.lifesteal.lifesteal.client.discord;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.entities.ActivityType;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.pipe.PipeStatus;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DiscordRpcManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("lifesteal-discord-rpc");
    private static final long MINECRAFT_APPLICATION_ID = 1486476415812632769L;
    private static final String LARGE_IMAGE_KEY = "lifesteal";
    private static final String LARGE_IMAGE_TEXT = "Lifesteal";
    private static final int UPDATE_INTERVAL_TICKS = 20;

    private static IPCClient client;
    private static int updateTicker;
    private static String lastDetails = "";
    private static String lastState = "";
    private static boolean shutdownHookRegistered;

    private DiscordRpcManager() {
    }

    public static void tick(MinecraftClient minecraftClient, boolean enabled) {
        ensureShutdownHookRegistered();
        if (!enabled) {
            shutdown();
            return;
        }

        if (!ensureConnected()) {
            return;
        }

        if (updateTicker++ < UPDATE_INTERVAL_TICKS) {
            return;
        }
        updateTicker = 0;

        String details = "Playing " + getMinecraftVersion();
        String state = getActivityState(minecraftClient);
        if (details.equals(lastDetails) && state.equals(lastState)) {
            return;
        }

        RichPresence.Builder builder = new RichPresence.Builder()
                .setActivityType(ActivityType.Playing)
                .setDetails(details)
                .setState(state)
                .setLargeImage(LARGE_IMAGE_KEY, LARGE_IMAGE_TEXT);

        try {
            client.sendRichPresence(builder.build());
            LOGGER.info("Updated Discord RPC: details='{}', state='{}'", details, state);
            lastDetails = details;
            lastState = state;
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to update Discord RPC presence.", throwable);
            shutdown();
        }
    }

    public static void shutdown() {
        IPCClient active = client;
        client = null;
        if (active == null) {
            updateTicker = 0;
            lastDetails = "";
            lastState = "";
            return;
        }

        PipeStatus status = PipeStatus.UNINITIALIZED;
        try {
            status = active.getStatus();
        } catch (Throwable throwable) {
            LOGGER.debug("Failed to query Discord RPC status during shutdown.", throwable);
        }

        try {
            active.close();
            LOGGER.info("Discord RPC shutdown completed (previous status: {}).", status);
        } catch (Throwable throwable) {
            LOGGER.warn("Discord RPC shutdown failed (previous status: {}).", status, throwable);
        } finally {
            updateTicker = 0;
            lastDetails = "";
            lastState = "";
        }
    }

    public static void resetState() {
        updateTicker = 0;
        lastDetails = "";
        lastState = "";
    }

    private static boolean ensureConnected() {
        if (client != null && client.getStatus() == PipeStatus.CONNECTED) {
            return true;
        }
        if (client != null && client.getStatus() == PipeStatus.CONNECTING) {
            return false;
        }

        shutdown();
        try {
            client = new IPCClient(MINECRAFT_APPLICATION_ID);
            client.connect();
            LOGGER.info("Discord RPC connected.");
            return client.getStatus() == PipeStatus.CONNECTED;
        } catch (Throwable throwable) {
            LOGGER.debug("Discord IPC unavailable, custom presence disabled for this session.", throwable);
            shutdown();
            return false;
        }
    }

    private static String getActivityState(MinecraftClient client) {
        if (client == null || client.world == null) {
            return "In menus";
        }
        if (client.getServer() != null) {
            return "Playing singleplayer";
        }
        return "Playing multiplayer";
    }

    private static String getMinecraftVersion() {
        return FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("Minecraft");
    }

    private static synchronized void ensureShutdownHookRegistered() {
        if (shutdownHookRegistered) {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(DiscordRpcManager::shutdown, "lifesteal-discord-rpc-shutdown"));
        shutdownHookRegistered = true;
    }
}
