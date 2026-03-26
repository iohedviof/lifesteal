package dev.lifesteal.lifesteal.client.config;

import dev.lifesteal.lifesteal.client.LifestealClient;
import dev.lifesteal.lifesteal.config.LifestealClientConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public final class LifestealConfigScreen {
    private LifestealConfigScreen() {
    }

    public static Screen create(Screen parent) {
        LifestealClientConfig config = LifestealClient.getClientConfig().copy();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Lifesteal"));
        builder.setEditable(true);
        builder.setAfterInitConsumer(screen -> {
            for (Element element : screen.children()) {
                if (!(element instanceof ClickableWidget widget)) {
                    continue;
                }
                if (widget.getY() < screen.height - 40 || widget.getWidth() < 90) {
                    continue;
                }
                if (!widget.getMessage().getString().isBlank()) {
                    continue;
                }
                widget.setMessage(Text.translatable("gui.done"));
            }
        });
        builder.setSavingRunnable(() -> LifestealClient.applyClientConfig(config));

        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(Text.literal("Config"));
        builder.setFallbackCategory(category);

        category.addEntry(entries.startBooleanToggle(Text.literal("Allow God Apples"), config.allowGodApples)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Disables enchanted golden apple effects."))
                .setSaveConsumer(value -> config.allowGodApples = value)
                .build());
        category.addEntry(entries.startBooleanToggle(Text.literal("Allow Strength II"), config.allowStrengthII)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Disables Strength II brewing and downgrades Strength II effects to Strength I."))
                .setSaveConsumer(value -> config.allowStrengthII = value)
                .build());
        category.addEntry(entries.startBooleanToggle(Text.literal("Enable Enchantment Limits"), config.enableEnchantmentLimits)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Clamps specific enchantments to reduced maximum levels."))
                .setSaveConsumer(value -> config.enableEnchantmentLimits = value)
                .build());
        category.addEntry(entries.startBooleanToggle(Text.literal("Allow Netherite Upgrades"), config.allowNetheriteUpgrades)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Allows upgrading diamond gear to netherite in smithing tables."))
                .setSaveConsumer(value -> config.allowNetheriteUpgrades = value)
                .build());
        category.addEntry(entries.startBooleanToggle(Text.literal("Balanced Mace"), config.balancedMace)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Disables mace enchants and applies a long attack cooldown."))
                .setSaveConsumer(value -> config.balancedMace = value)
                .build());
        category.addEntry(entries.startBooleanToggle(Text.literal("Disable Ender Pearls"), config.disableEnderPearls)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Prevents players from using ender pearls."))
                .setSaveConsumer(value -> config.disableEnderPearls = value)
                .build());
        category.addEntry(entries.startBooleanToggle(Text.literal("Disable End Crystal Damage"), config.disableCrystalDamage)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Removes player and environment damage from end crystals."))
                .setSaveConsumer(value -> config.disableCrystalDamage = value)
                .build());
        category.addEntry(entries.startBooleanToggle(Text.literal("Enable Riptide Cooldown"), config.enableRiptideCooldown)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Adds a cooldown after using Riptide."))
                .setSaveConsumer(value -> config.enableRiptideCooldown = value)
                .build());
        category.addEntry(entries.startIntField(Text.literal("Max Hearts"), config.maxHearts)
                .setDefaultValue(20)
                .setMin(1)
                .setMax(1000)
                .setTooltip(Text.literal("Maximum heart count allowed for players."))
                .setSaveConsumer(value -> config.maxHearts = value)
                .build());
        category.addEntry(entries.startIntField(Text.literal("Riptide Cooldown"), config.riptideCooldown)
                .setDefaultValue(200)
                .setMin(0)
                .setMax(72000)
                .setTooltip(Text.literal("Cooldown in ticks for Riptide usage."))
                .setSaveConsumer(value -> config.riptideCooldown = value)
                .build());
        category.addEntry(new ImmediateBooleanListEntry(
                Text.literal("Enable Custom Discord RPC"),
                config.enableCustomDiscordRpc,
                true,
                value -> {
                    config.enableCustomDiscordRpc = value;
                    LifestealClient.applyClientConfig(config);
                }
        ));

        return builder.build();
    }

    private static final class ImmediateBooleanListEntry extends BooleanListEntry {
        private boolean lastValue;
        private final java.util.function.Consumer<Boolean> onValueChanged;

        private ImmediateBooleanListEntry(Text fieldName, boolean value, boolean defaultValue, java.util.function.Consumer<Boolean> onValueChanged) {
            super(
                    fieldName,
                    value,
                    Text.translatable("text.cloth-config.reset_value"),
                    () -> defaultValue,
                    ignored -> {
                    }
            );
            this.lastValue = value;
            this.onValueChanged = onValueChanged;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
            super.render(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, delta);
            boolean currentValue = getValue();
            if (currentValue == lastValue) {
                return;
            }
            lastValue = currentValue;
            onValueChanged.accept(currentValue);
        }
    }
}
