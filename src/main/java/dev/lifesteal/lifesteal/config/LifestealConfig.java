package dev.lifesteal.lifesteal.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LifestealConfig {
    private static final Path CONFIG_PATH = Paths.get("config", "lifesteal.properties");

    private static final LifestealConfig DEFAULTS = new LifestealConfig();

    public boolean allowGodApples = false;
    public boolean allowStrengthII = false;
    public boolean enableEnchantmentLimits = false;
    public boolean allowNetheriteUpgrades = true;
    public boolean balancedMace = false;

    private static LifestealConfig INSTANCE = new LifestealConfig();

    public static LifestealConfig get() {
        return INSTANCE;
    }

    public static List<ConfigOption> getOptions() {
        return OPTIONS;
    }

    public static ConfigOption getOption(String key) {
        if (key == null) {
            return null;
        }
        for (ConfigOption option : OPTIONS) {
            if (option.key.equalsIgnoreCase(key)) {
                return option;
            }
        }
        return null;
    }

    public static LifestealConfig load() {
        LifestealConfig config = new LifestealConfig();

        try {
            Path legacyJsonPath = Paths.get("config", "lifesteal.json");
            if (!Files.exists(CONFIG_PATH) && Files.exists(legacyJsonPath)) {
                migrateFromJson(legacyJsonPath, config);
            }

            if (Files.exists(CONFIG_PATH)) {
                List<String> lines = Files.readAllLines(CONFIG_PATH);
                config.allowGodApples = getBoolean(lines, "allowGodApples", config.allowGodApples);
                config.allowStrengthII = getBoolean(lines, "allowStrengthII", config.allowStrengthII);
                config.enableEnchantmentLimits = getBoolean(lines, "enableEnchantmentLimits", config.enableEnchantmentLimits);
                config.allowNetheriteUpgrades = getBoolean(lines, "allowNetheriteUpgrades", config.allowNetheriteUpgrades);
                config.balancedMace = getBoolean(lines, "balancedMace", config.balancedMace);
            }
        } catch (Exception ignored) {
        }

        INSTANCE = config;
        save();
        return INSTANCE;
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String contents = """
Lifesteal Mod configuration.

# If enabled, enchanted golden apples do not give you any effects.
allowGodApples: %s

# If enabled, disables Strength II brewing and converts it to Strength I every time you try to apply it to yourself using commands.
allowStrengthII: %s

# If enabled, limits enchantment levels to these for specific enchantments:
# Protection IV -> Protection III
# Fire Protection IV -> Fire Protection III
# Blast Protection IV -> Blast Protection III
# Projectile Protection IV -> Projectile Protection III
# Sharpness V -> Sharpness IV
# Power V -> Power IV
# Density V -> Density IV
enableEnchantmentLimits: %s

# If enabled, allows netherite upgrades for diamond armor, sword, and axe.
allowNetheriteUpgrades: %s

# If set to true, maces cannot be enchanted and have a 60-second attack cooldown (shown like ender pearls).
balancedMace = %s
""".formatted(
                    INSTANCE.allowGodApples,
                    INSTANCE.allowStrengthII,
                    INSTANCE.enableEnchantmentLimits,
                    INSTANCE.allowNetheriteUpgrades,
                    INSTANCE.balancedMace
            );
            Files.writeString(CONFIG_PATH, contents);
        } catch (Exception ignored) {
        }
    }

    private static boolean getBoolean(List<String> lines, String key, boolean defaultValue) {
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
            String value = trimmed.substring(separatorIndex + 1).trim();
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    private static void migrateFromJson(Path legacyJsonPath, LifestealConfig config) {
        try {
            String json = Files.readString(legacyJsonPath);
            config.allowGodApples = extractBoolean(json, "allowGodApples", config.allowGodApples);
            config.allowStrengthII = extractBoolean(json, "allowStrengthII", config.allowStrengthII);
            config.enableEnchantmentLimits = extractBoolean(json, "enableEnchantmentLimits", config.enableEnchantmentLimits);
            config.allowNetheriteUpgrades = extractBoolean(json, "allowNetheriteUpgrades", config.allowNetheriteUpgrades);
            config.balancedMace = extractBoolean(json, "balancedMace", config.balancedMace);
            Files.deleteIfExists(legacyJsonPath);
        } catch (Exception ignored) {
        }
    }

    private static boolean extractBoolean(String json, String key, boolean fallback) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        return Boolean.parseBoolean(matcher.group(1));
    }

    public static final class ConfigOption {
        public final String key;
        public final String description;
        public final boolean defaultValue;
        private final BooleanSupplier getter;
        private final Consumer<Boolean> setter;

        private ConfigOption(String key, String description, boolean defaultValue, BooleanSupplier getter, Consumer<Boolean> setter) {
            this.key = key;
            this.description = description;
            this.defaultValue = defaultValue;
            this.getter = getter;
            this.setter = setter;
        }

        public boolean currentValue() {
            return getter.getAsBoolean();
        }

        public void setValue(boolean value) {
            setter.accept(value);
        }
    }

    private static final List<ConfigOption> OPTIONS = List.of(
            new ConfigOption(
                    "allowGodApples",
                    "Disables enchanted golden apple effects.",
                    DEFAULTS.allowGodApples,
                    () -> LifestealConfig.get().allowGodApples,
                    value -> LifestealConfig.get().allowGodApples = value
            ),
            new ConfigOption(
                    "allowStrengthII",
                    "Disables Strength II brewing and downgrades command-applied Strength II to Strength I.",
                    DEFAULTS.allowStrengthII,
                    () -> LifestealConfig.get().allowStrengthII,
                    value -> LifestealConfig.get().allowStrengthII = value
            ),
            new ConfigOption(
                    "enableEnchantmentLimits",
                    "Limits specific enchantments to lower max levels (Protection III, Sharpness IV, Power IV, Density IV).",
                    DEFAULTS.enableEnchantmentLimits,
                    () -> LifestealConfig.get().enableEnchantmentLimits,
                    value -> LifestealConfig.get().enableEnchantmentLimits = value
            ),
            new ConfigOption(
                    "allowNetheriteUpgrades",
                    "Disables upgrading diamond armor and weapons to netherite (weapons: swords and axes).",
                    DEFAULTS.allowNetheriteUpgrades,
                    () -> LifestealConfig.get().allowNetheriteUpgrades,
                    value -> LifestealConfig.get().allowNetheriteUpgrades = value
            ),
            new ConfigOption(
                    "balancedMace",
                    "If enabled, maces cannot be enchanted and have a 60-second attack cooldown.",
                    DEFAULTS.balancedMace,
                    () -> LifestealConfig.get().balancedMace,
                    value -> LifestealConfig.get().balancedMace = value
            )
    );
}
