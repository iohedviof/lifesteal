package dev.lifesteal.lifesteal.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
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
    public boolean disableEnderPearls = false;
    public boolean disableCrystalDamage = true;
    public boolean enableRiptideCooldown = false;
    public int maxHearts = 20;
    public int riptideCooldown = 200;

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
                config.disableEnderPearls = getBoolean(lines, "disableEnderPearls", config.disableEnderPearls);
                config.disableCrystalDamage = getBoolean(lines, "disableEndCrystalDamage", getBoolean(lines, "disableCrystalDamage", config.disableCrystalDamage));
                config.enableRiptideCooldown = getBoolean(lines, "enableRiptideCooldown", config.enableRiptideCooldown);
                config.maxHearts = getInt(lines, "maxHearts", config.maxHearts);
                config.riptideCooldown = getInt(lines, "riptideCooldown", config.riptideCooldown);
            }
        } catch (Exception ignored) {
        }

        clamp(config);
        INSTANCE = config;
        save();
        return INSTANCE;
    }

    public static void applySingleplayerOverrides(LifestealClientConfig clientConfig) {
        LifestealConfig config = new LifestealConfig();
        clientConfig.applyTo(config);
        clamp(config);
        INSTANCE = config;
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String contents = """
# Lifesteal Mod configuration.

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

# Disables usage of ender pearls if enabled. Set to false be default
disableEnderPearls: %s

# Makes crystals be unable to do any environment and player damage
disableEndCrystalDamage: %s

# Enables cooldown for Riptide tridents
enableRiptideCooldown: %s

# Maximum amount of hearts. Defaults to 20
maxHearts: %s

# Cooldown for Riptide enchantment
riptideCooldown: %s
""".formatted(
                    INSTANCE.allowGodApples,
                    INSTANCE.allowStrengthII,
                    INSTANCE.enableEnchantmentLimits,
                    INSTANCE.allowNetheriteUpgrades,
                    INSTANCE.balancedMace,
                    INSTANCE.disableEnderPearls,
                    INSTANCE.disableCrystalDamage,
                    INSTANCE.enableRiptideCooldown,
                    INSTANCE.maxHearts,
                    INSTANCE.riptideCooldown
            );
            Files.writeString(CONFIG_PATH, contents);
        } catch (Exception ignored) {
        }
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

    private static void migrateFromJson(Path legacyJsonPath, LifestealConfig config) {
        try {
            String json = Files.readString(legacyJsonPath);
            config.allowGodApples = extractBoolean(json, "allowGodApples", config.allowGodApples);
            config.allowStrengthII = extractBoolean(json, "allowStrengthII", config.allowStrengthII);
            config.enableEnchantmentLimits = extractBoolean(json, "enableEnchantmentLimits", config.enableEnchantmentLimits);
            config.allowNetheriteUpgrades = extractBoolean(json, "allowNetheriteUpgrades", config.allowNetheriteUpgrades);
            config.balancedMace = extractBoolean(json, "balancedMace", config.balancedMace);
            config.disableEnderPearls = extractBoolean(json, "disableEnderPearls", config.disableEnderPearls);
            config.disableCrystalDamage = extractBoolean(json, "disableEndCrystalDamage", extractBoolean(json, "disableCrystalDamage", config.disableCrystalDamage));
            config.enableRiptideCooldown = extractBoolean(json, "enableRiptideCooldown", config.enableRiptideCooldown);
            config.maxHearts = extractInt(json, "maxHearts", config.maxHearts);
            config.riptideCooldown = extractInt(json, "riptideCooldown", config.riptideCooldown);
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

    private static int extractInt(String json, String key, int fallback) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void clamp(LifestealConfig config) {
        config.maxHearts = Math.max(1, Math.min(1000, config.maxHearts));
        config.riptideCooldown = Math.max(0, Math.min(72000, config.riptideCooldown));
    }

    public static final class ConfigOption {
        public enum Type {
            BOOLEAN,
            INTEGER
        }

        public final String key;
        public final String description;
        public final Type type;
        private final boolean defaultBooleanValue;
        private final int defaultIntegerValue;
        private final BooleanSupplier booleanGetter;
        private final Consumer<Boolean> booleanSetter;
        private final IntSupplier integerGetter;
        private final IntConsumer integerSetter;
        private final int minInteger;
        private final int maxInteger;

        private ConfigOption(String key, String description, boolean defaultValue, BooleanSupplier getter, Consumer<Boolean> setter) {
            this.key = key;
            this.description = description;
            this.type = Type.BOOLEAN;
            this.defaultBooleanValue = defaultValue;
            this.defaultIntegerValue = 0;
            this.booleanGetter = getter;
            this.booleanSetter = setter;
            this.integerGetter = null;
            this.integerSetter = null;
            this.minInteger = 0;
            this.maxInteger = 0;
        }

        private ConfigOption(String key, String description, int defaultValue, int minValue, int maxValue, IntSupplier getter, IntConsumer setter) {
            this.key = key;
            this.description = description;
            this.type = Type.INTEGER;
            this.defaultBooleanValue = false;
            this.defaultIntegerValue = defaultValue;
            this.booleanGetter = null;
            this.booleanSetter = null;
            this.integerGetter = getter;
            this.integerSetter = setter;
            this.minInteger = minValue;
            this.maxInteger = maxValue;
        }

        public boolean currentBooleanValue() {
            return booleanGetter != null && booleanGetter.getAsBoolean();
        }

        public int currentIntegerValue() {
            return integerGetter != null ? integerGetter.getAsInt() : 0;
        }

        public boolean defaultBooleanValue() {
            return defaultBooleanValue;
        }

        public int defaultIntegerValue() {
            return defaultIntegerValue;
        }

        public int minInteger() {
            return minInteger;
        }

        public int maxInteger() {
            return maxInteger;
        }

        public String currentValueAsText() {
            return type == Type.BOOLEAN
                    ? Boolean.toString(currentBooleanValue())
                    : Integer.toString(currentIntegerValue());
        }

        public boolean isDefaultValue() {
            if (type == Type.BOOLEAN) {
                return currentBooleanValue() == defaultBooleanValue;
            }
            return currentIntegerValue() == defaultIntegerValue;
        }

        public void setValueFromString(String value) {
            if (type == Type.BOOLEAN) {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new IllegalArgumentException("Expected true or false.");
                }
                booleanSetter.accept(Boolean.parseBoolean(value));
                return;
            }

            int parsed;
            try {
                parsed = Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Expected a number.");
            }

            if (parsed < minInteger || parsed > maxInteger) {
                throw new IllegalArgumentException("Value must be between " + minInteger + " and " + maxInteger + ".");
            }
            integerSetter.accept(parsed);
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
            ),
            new ConfigOption(
                    "disableEnderPearls",
                    "Disables ender pearls from being used",
                    DEFAULTS.disableEnderPearls,
                    () -> LifestealConfig.get().disableEnderPearls,
                    value -> LifestealConfig.get().disableEnderPearls = value
            ),
            new ConfigOption(
                    "disableEndCrystalDamage",
                    "Makes crystals be unable to do any environment and player damage",
                    DEFAULTS.disableCrystalDamage,
                    () -> LifestealConfig.get().disableCrystalDamage,
                    value -> LifestealConfig.get().disableCrystalDamage = value
            ),
            new ConfigOption(
                    "enableRiptideCooldown",
                    "Enables cooldown for Riptide tridents",
                    DEFAULTS.enableRiptideCooldown,
                    () -> LifestealConfig.get().enableRiptideCooldown,
                    value -> LifestealConfig.get().enableRiptideCooldown = value
            ),
            new ConfigOption(
                    "maxHearts",
                    "The max amount of hearts, default being 20",
                    DEFAULTS.maxHearts,
                    1,
                    1000,
                    () -> LifestealConfig.get().maxHearts,
                    value -> LifestealConfig.get().maxHearts = value
            ),
            new ConfigOption(
                    "riptideCooldown",
                    "Cooldown for Riptide enchantment",
                    DEFAULTS.riptideCooldown,
                    0,
                    72000,
                    () -> LifestealConfig.get().riptideCooldown,
                    value -> LifestealConfig.get().riptideCooldown = value
            )
    );
}
