package dev.lifesteal.lifesteal.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public final class LifestealClientConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("lifesteal-client-config");

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("lifesteal-client.properties");
    }

    public boolean allowGodApples = false;
    public boolean allowStrengthII = false;
    public boolean enableEnchantmentLimits = false;
    public boolean allowNetheriteUpgrades = true;
    public boolean balancedMace = false;
    public boolean disableEnderPearls = false;
    public boolean disableCrystalDamage = true;
    public boolean enableRiptideCooldown = false;
    public int maxHearts = 20;
    public int riptideCooldown = 200;
    public boolean enableCustomDiscordRpc = true;

    public static LifestealClientConfig load() {
        LifestealClientConfig config = new LifestealClientConfig();
        Path configPath = getConfigPath();
        migrateLegacyPathIfNeeded(configPath);
        try {
            if (Files.exists(configPath)) {
                List<String> lines = Files.readAllLines(configPath);
                config.allowGodApples = getBoolean(lines, "allowGodApples", config.allowGodApples);
                config.allowStrengthII = getBoolean(lines, "allowStrengthII", config.allowStrengthII);
                config.enableEnchantmentLimits = getBoolean(lines, "enableEnchantmentLimits", config.enableEnchantmentLimits);
                config.allowNetheriteUpgrades = getBoolean(lines, "allowNetheriteUpgrades", config.allowNetheriteUpgrades);
                config.balancedMace = getBoolean(lines, "balancedMace", config.balancedMace);
                config.disableEnderPearls = getBoolean(lines, "disableEnderPearls", config.disableEnderPearls);
                config.disableCrystalDamage = getBoolean(lines, "disableEndCrystalDamage", getBoolean(lines, "disableCrystalDamage", config.disableCrystalDamage));
                config.enableRiptideCooldown = getBoolean(lines, "enableRiptideCooldown", config.enableRiptideCooldown);
                config.maxHearts = getInt(lines, "maxHearts", config.maxHearts);
                config.riptideCooldown = getInt(lines, "riptideCooldown", config.riptideCooldown);
                config.enableCustomDiscordRpc = getBoolean(lines, "enableCustomDiscordRpc", config.enableCustomDiscordRpc);
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to load client config from {}", configPath, exception);
        }
        config.maxHearts = Math.max(1, Math.min(1000, config.maxHearts));
        config.riptideCooldown = Math.max(0, Math.min(72000, config.riptideCooldown));
        return config;
    }

    public static Optional<Boolean> loadDiscordRpcToggle() {
        Path configPath = getConfigPath();
        migrateLegacyPathIfNeeded(configPath);
        try {
            if (!Files.exists(configPath)) {
                return Optional.empty();
            }
            List<String> lines = Files.readAllLines(configPath);
            String value = getRawValue(lines, "enableCustomDiscordRpc");
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(Boolean.parseBoolean(value));
        } catch (Exception exception) {
            LOGGER.warn("Failed to read enableCustomDiscordRpc from {}", configPath, exception);
            return Optional.empty();
        }
    }

    public LifestealClientConfig copy() {
        LifestealClientConfig copy = new LifestealClientConfig();
        copy.allowGodApples = allowGodApples;
        copy.allowStrengthII = allowStrengthII;
        copy.enableEnchantmentLimits = enableEnchantmentLimits;
        copy.allowNetheriteUpgrades = allowNetheriteUpgrades;
        copy.balancedMace = balancedMace;
        copy.disableEnderPearls = disableEnderPearls;
        copy.disableCrystalDamage = disableCrystalDamage;
        copy.enableRiptideCooldown = enableRiptideCooldown;
        copy.maxHearts = maxHearts;
        copy.riptideCooldown = riptideCooldown;
        copy.enableCustomDiscordRpc = enableCustomDiscordRpc;
        return copy;
    }

    public void save() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            String contents = """
# Lifesteal singleplayer/client configuration.
# Gameplay options in this file are applied only to integrated singleplayer servers.

allowGodApples: %s
allowStrengthII: %s
enableEnchantmentLimits: %s
allowNetheriteUpgrades: %s
balancedMace: %s
disableEnderPearls: %s
disableEndCrystalDamage: %s
enableRiptideCooldown: %s
maxHearts: %s
riptideCooldown: %s
enableCustomDiscordRpc: %s
""".formatted(
                    allowGodApples,
                    allowStrengthII,
                    enableEnchantmentLimits,
                    allowNetheriteUpgrades,
                    balancedMace,
                    disableEnderPearls,
                    disableCrystalDamage,
                    enableRiptideCooldown,
                    maxHearts,
                    riptideCooldown,
                    enableCustomDiscordRpc
            );
            Files.writeString(configPath, contents);
        } catch (Exception exception) {
            LOGGER.warn("Failed to save client config to {}", configPath, exception);
        }
    }

    public void applyTo(LifestealConfig config) {
        config.allowGodApples = allowGodApples;
        config.allowStrengthII = allowStrengthII;
        config.enableEnchantmentLimits = enableEnchantmentLimits;
        config.allowNetheriteUpgrades = allowNetheriteUpgrades;
        config.balancedMace = balancedMace;
        config.disableEnderPearls = disableEnderPearls;
        config.disableCrystalDamage = disableCrystalDamage;
        config.enableRiptideCooldown = enableRiptideCooldown;
        config.maxHearts = maxHearts;
        config.riptideCooldown = riptideCooldown;
    }

    private static boolean getBoolean(List<String> lines, String key, boolean defaultValue) {
        String value = getRawValue(lines, key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private static int getInt(List<String> lines, String key, int defaultValue) {
        String value = getRawValue(lines, key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static String getRawValue(List<String> lines, String key) {
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int separatorIndex = trimmed.indexOf(':');
            if (separatorIndex < 0) {
                separatorIndex = trimmed.indexOf('=');
            }
            if (separatorIndex < 0) {
                continue;
            }
            String foundKey = trimmed.substring(0, separatorIndex).trim();
            if (!foundKey.equals(key)) {
                continue;
            }
            return trimmed.substring(separatorIndex + 1).trim();
        }
        return null;
    }

    private static void migrateLegacyPathIfNeeded(Path configPath) {
        try {
            Path legacyPath = Paths.get("config", "lifesteal-client.properties");
            if (legacyPath.equals(configPath)) {
                return;
            }
            if (!Files.exists(configPath) && Files.exists(legacyPath)) {
                Files.createDirectories(configPath.getParent());
                Files.copy(legacyPath, configPath);
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to migrate legacy client config path to {}", configPath, exception);
        }
    }
}
