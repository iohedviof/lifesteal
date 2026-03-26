package dev.lifesteal.lifesteal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.lifesteal.lifesteal.item.BeaconOfLifeItem;
import dev.lifesteal.lifesteal.item.HeartItem;
import dev.lifesteal.lifesteal.config.LifestealClientConfig;
import dev.lifesteal.lifesteal.config.LifestealConfig;
import dev.lifesteal.lifesteal.enchant.EnchantmentLimiter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class Lifesteal implements ModInitializer {
    public static final String MOD_ID = "lifesteal";

    public static final double DEFAULT_MAX_HEALTH = 20.0D;
    public static final double MIN_MAX_HEALTH = 2.0D;
    public static final double HEALTH_DELTA = 2.0D;
    public static final double MAX_CRAFTED_HEART_HEALTH = 20.0D;
    public static final int MAX_WITHDRAW_PER_COMMAND = 10;
    public static final String HEART_TYPE_KEY = "lifesteal_heart_type";
    public static final String HEART_TYPE_CRAFTED = "crafted";
    public static final String HEART_TYPE_WITHDRAWN = "withdrawn";
    public static final String ELIMINATION_BAN_REASON = "Ran out of hearts";
    public static final double REVIVED_MAX_HEALTH = 6.0D;
    private static final String TEST_MODE_KICK_MESSAGE = "Server is in test mode!";
    private static final Path TEST_MODE_CONFIG_PATH = Paths.get("config", "lifesteal-testmode.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<UUID> PENDING_REVIVED_PLAYERS = new HashSet<>();
    private static final Set<String> TEST_MODE_WHITELIST = new HashSet<>();
    private static boolean TEST_MODE_ENABLED = false;
    private static int enchantmentClampCooldown = 0;

    public static final Item HEART = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "heart"),
            new HeartItem(
                    new Item.Settings()
                            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "heart")))
                            .component(DataComponentTypes.CUSTOM_NAME, Text.translatable("item.lifesteal.heart").styled(style ->
                                    style.withColor(Formatting.RED).withItalic(false)
                            ))
                            .component(DataComponentTypes.LORE, new LoreComponent(List.of(
                                    Text.literal("Use to gain an extra heart").styled(style ->
                                            style.withColor(Formatting.WHITE).withItalic(false)
                                    ),
                                    Text.literal("Maximum hearts set by config").styled(style ->
                                            style.withColor(Formatting.WHITE).withItalic(false)
                                    )
                            )))
                            .maxCount(64)
            )
    );

    public static final Item BEACON_OF_LIFE = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "beacon_of_life"),
            new BeaconOfLifeItem(
                    new Item.Settings()
                            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "beacon_of_life")))
                            .rarity(Rarity.RARE)
                            .maxCount(1)
            )
    );

    @Override
    public void onInitialize() {
        LifestealConfig.load();
        loadTestModeConfig();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (!server.isDedicated()) {
                LifestealConfig.applySingleplayerOverrides(LifestealClientConfig.load());
            }
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(HEART));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(BEACON_OF_LIFE));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("withdraw")
                            .executes(context -> executeWithdraw(context.getSource(), 1))
                            .then(argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, MAX_WITHDRAW_PER_COMMAND))
                                    .executes(context -> executeWithdraw(
                                            context.getSource(),
                                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount")
                                    )))
            );

            dispatcher.register(
                    literal("add")
                            .requires(source -> source.hasPermissionLevel(4))
                            .then(argument("target", EntityArgumentType.players())
                                    .then(argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                            .executes(context -> executeAdjustHearts(
                                                    context.getSource(),
                                                    EntityArgumentType.getPlayers(context, "target"),
                                                    com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount"),
                                                    true
                                            ))))
            );

            dispatcher.register(
                    literal("remove")
                            .requires(source -> source.hasPermissionLevel(4))
                            .then(argument("target", EntityArgumentType.players())
                                    .then(argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                            .executes(context -> executeAdjustHearts(
                                                    context.getSource(),
                                                    EntityArgumentType.getPlayers(context, "target"),
                                                    com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount"),
                                                    false
                                            ))))
            );

            dispatcher.register(
                    literal("revive")
                            .requires(source -> source.hasPermissionLevel(4))
                            .then(argument("player", word())
                                    .suggests((context, builder) -> CommandSource.suggestMatching(
                                            Lifesteal.getBannedProfiles(context.getSource().getServer()),
                                            builder
                                    ))
                                    .executes(context -> executeRevive(
                                            context.getSource(),
                                            getString(context, "player")
                                    )))
            );

            dispatcher.register(
                    literal("testmode")
                            .requires(source -> source.hasPermissionLevel(4))
                            .then(literal("on").executes(context -> executeTestModeOn(context.getSource())))
                            .then(literal("off").executes(context -> executeTestModeOff(context.getSource())))
                            .then(literal("whitelist")
                                    .then(literal("add")
                                            .then(argument("player", com.mojang.brigadier.arguments.StringArgumentType.word())
                                                    .executes(context -> executeTestModeWhitelistAdd(
                                                            context.getSource(),
                                                            com.mojang.brigadier.arguments.StringArgumentType.getString(context, "player")
                                                    ))))
                                    .then(literal("remove")
                                            .then(argument("player", com.mojang.brigadier.arguments.StringArgumentType.word())
                                                    .executes(context -> executeTestModeWhitelistRemove(
                                                            context.getSource(),
                                                            com.mojang.brigadier.arguments.StringArgumentType.getString(context, "player")
                                                    ))))
                                    .then(literal("clear").executes(context -> executeTestModeWhitelistClear(context.getSource())))
                                    .then(literal("list").executes(context -> executeTestModeWhitelistList(context.getSource()))))
            );

            dispatcher.register(
                    literal("lifesteal")
                            .requires(source -> source.hasPermissionLevel(4))
                            .then(literal("reload").executes(context -> executeReload(context.getSource())))
                            .then(argument("setting", word())
                                    .suggests((context, builder) -> CommandSource.suggestMatching(
                                            LifestealConfig.getOptions().stream().map(option -> option.key).toList(),
                                            builder
                                    ))
                                    .executes(context -> executeShowConfigOption(
                                            context.getSource(),
                                            getString(context, "setting")
                                    ))
                                    .then(argument("value", word())
                                            .suggests(Lifesteal::suggestConfigValue)
                                            .executes(context -> executeSetConfigOption(
                                                    context.getSource(),
                                                    getString(context, "setting"),
                                                    getString(context, "value")
                                            ))))
            );
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (TEST_MODE_ENABLED && !isWhitelistedForTestMode(handler.player.getGameProfile().name())) {
                handler.player.networkHandler.disconnect(Text.literal(TEST_MODE_KICK_MESSAGE));
                return;
            }

            if (PENDING_REVIVED_PLAYERS.remove(handler.player.getUuid())) {
                setPlayerMaxHealth(handler.player, REVIVED_MAX_HEALTH);
                return;
            }

            double maxHealth = getPlayerMaxHealth(handler.player);
            if (maxHealth < MIN_MAX_HEALTH) {
                setPlayerMaxHealth(handler.player, DEFAULT_MAX_HEALTH);
            } else {
                setPlayerMaxHealth(handler.player, maxHealth);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (--enchantmentClampCooldown > 0) {
                return;
            }
            enchantmentClampCooldown = 1;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                clampPlayerEnchantments(player);
            }
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> setPlayerMaxHealth(newPlayer, getPlayerMaxHealth(oldPlayer)));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return;
            }

            boolean killedByPlayer = source.getAttacker() instanceof ServerPlayerEntity killer && killer != player;
            double updated = getPlayerMaxHealth(player) - HEALTH_DELTA;
            if (updated < MIN_MAX_HEALTH) {
                // Persist revival health target in player data so it survives server restarts/test mode downtime.
                setPlayerMaxHealth(player, REVIVED_MAX_HEALTH);
                banForRunningOutOfHearts(player);
            } else {
                setPlayerMaxHealth(player, updated);
            }

            if (killedByPlayer) {
                ServerPlayerEntity killer = (ServerPlayerEntity) source.getAttacker();
                if (getPlayerMaxHealth(killer) < getConfiguredMaxHealth()) {
                    double next = Math.min(getPlayerMaxHealth(killer) + HEALTH_DELTA, getConfiguredMaxHealth());
                    setPlayerMaxHealth(killer, next);
                }
            } else {
                // Natural/mob/void/etc. deaths drop one untagged heart item.
                ItemEntity heartDrop = new ItemEntity(
                        player.getEntityWorld(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        new ItemStack(HEART)
                );
                player.getEntityWorld().spawnEntity(heartDrop);
            }
        });
    }

    public static boolean consumeHeart(ServerPlayerEntity player, ItemStack heartStack) {
        if (isCraftedHeart(heartStack) && getPlayerMaxHealth(player) >= MAX_CRAFTED_HEART_HEALTH) {
            player.sendMessage(
                    Text.literal("You can't consume a crafted heart when you're on 10+ hearts").formatted(Formatting.RED),
                    true
            );
            return false;
        }

        if (getPlayerMaxHealth(player) >= getConfiguredMaxHealth()) {
            return false;
        }

        double next = Math.min(getPlayerMaxHealth(player) + HEALTH_DELTA, getConfiguredMaxHealth());
        setPlayerMaxHealth(player, next);
        player.getEntityWorld().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );
        return true;
    }

    public static boolean reviveBannedPlayer(MinecraftServer server, ServerPlayerEntity reviver, String targetName) {
        Optional<com.mojang.authlib.GameProfile> profile = server.getApiServices().profileResolver().getProfileByName(targetName);
        if (profile.isEmpty()) {
            reviver.sendMessage(Text.translatable("message.lifesteal.revive_failed", targetName), false);
            return false;
        }

        PlayerConfigEntry configEntry = new PlayerConfigEntry(profile.get());
        var banEntry = server.getPlayerManager().getUserBanList().get(configEntry);
        if (banEntry == null || !ELIMINATION_BAN_REASON.equals(banEntry.getReason())) {
            reviver.sendMessage(Text.translatable("message.lifesteal.revive_failed", targetName), false);
            return false;
        }

        server.getPlayerManager().getUserBanList().remove(configEntry);
        PENDING_REVIVED_PLAYERS.add(profile.get().id());

        ServerPlayerEntity onlineTarget = server.getPlayerManager().getPlayer(profile.get().id());
        if (onlineTarget != null) {
            setPlayerMaxHealth(onlineTarget, REVIVED_MAX_HEALTH);
            PENDING_REVIVED_PLAYERS.remove(profile.get().id());
        }

        server.getPlayerManager().broadcast(
                Text.literal(targetName + " was revived").formatted(Formatting.GREEN),
                false
        );
        return true;
    }

    public static List<String> getBannedProfiles(MinecraftServer server) {
        List<String> names = new ArrayList<>();
        for (var entry : server.getPlayerManager().getUserBanList().values()) {
            if (!ELIMINATION_BAN_REASON.equals(entry.getReason())) {
                continue;
            }
            Object key = entry.getKey();
            if (key instanceof PlayerConfigEntry playerEntry) {
                names.add(playerEntry.name());
            }
        }
        return names;
    }

    public static ItemStack createHeadFor(MinecraftServer server, String playerName) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        server.getApiServices()
                .profileResolver()
                .getProfileByName(playerName)
                .ifPresent(profile -> head.set(DataComponentTypes.PROFILE, ProfileComponent.ofStatic(profile)));
        head.set(DataComponentTypes.CUSTOM_NAME, Text.literal(playerName));
        return head;
    }

    public static void setPlayerMaxHealth(ServerPlayerEntity player, double maxHealth) {
        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }

        double clamped = Math.max(MIN_MAX_HEALTH, Math.min(getConfiguredMaxHealth(), maxHealth));
        attribute.setBaseValue(clamped);

        if (player.getHealth() > clamped) {
            player.setHealth((float) clamped);
        }
    }

    public static double getPlayerMaxHealth(ServerPlayerEntity player) {
        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (attribute == null) {
            return DEFAULT_MAX_HEALTH;
        }

        return attribute.getBaseValue();
    }

    private static void banForRunningOutOfHearts(ServerPlayerEntity player) {
        var banEntry = new net.minecraft.server.BannedPlayerEntry(
                new PlayerConfigEntry(player.getGameProfile()),
                null,
                "Lifesteal",
                null,
                ELIMINATION_BAN_REASON
        );
        MinecraftServer server = player.getEntityWorld().getServer();
        server.getPlayerManager().getUserBanList().add(banEntry);
        server.getPlayerManager().broadcast(
                Text.literal(player.getGameProfile().name() + " was banned").formatted(Formatting.RED),
                false
        );
        player.networkHandler.disconnect(Text.literal("You are banned").formatted(Formatting.RED));
    }

    private static int executeWithdraw(ServerCommandSource source, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();

        int removableHearts = (int) Math.floor((getPlayerMaxHealth(player) - MIN_MAX_HEALTH) / HEALTH_DELTA);
        int withdrawCount = Math.min(amount, removableHearts);
        if (withdrawCount <= 0) {
            return 0;
        }

        setPlayerMaxHealth(player, getPlayerMaxHealth(player) - (withdrawCount * HEALTH_DELTA));

        ItemStack withdrawnHearts = new ItemStack(HEART, withdrawCount);
        setHeartType(withdrawnHearts, HEART_TYPE_WITHDRAWN);
        if (!player.giveItemStack(withdrawnHearts)) {
            player.dropItem(withdrawnHearts, false);
        }
        return withdrawCount;
    }

    private static int executeAdjustHearts(ServerCommandSource source, Collection<ServerPlayerEntity> targets, int amount, boolean add) {
        int changed = 0;
        double delta = amount * HEALTH_DELTA * (add ? 1.0D : -1.0D);

        for (ServerPlayerEntity target : targets) {
            setPlayerMaxHealth(target, getPlayerMaxHealth(target) + delta);
            changed++;
        }

        final int finalChanged = changed;
        source.sendFeedback(
                () -> Text.translatable(
                        add ? "message.lifesteal.admin_add_success" : "message.lifesteal.admin_remove_success",
                        amount,
                        finalChanged
                ),
                true
        );

        return changed;
    }

    public static void setHeartType(ItemStack stack, String type) {
        NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = customData.copyNbt();
        nbt.putString(HEART_TYPE_KEY, type);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static boolean isWithdrawnHeart(ItemStack stack) {
        NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        String type = customData.copyNbt().getString(HEART_TYPE_KEY, "");
        return HEART_TYPE_WITHDRAWN.equals(type);
    }

    public static boolean isCraftedHeart(ItemStack stack) {
        NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        String type = customData.copyNbt().getString(HEART_TYPE_KEY, "");
        return HEART_TYPE_CRAFTED.equals(type);
    }

    private static int executeTestModeOn(ServerCommandSource source) {
        TEST_MODE_ENABLED = true;
        saveTestModeConfig();
        kickNonWhitelistedPlayers(source.getServer(), source);
        source.sendFeedback(() -> Text.literal("Test mode enabled."), true);
        return 1;
    }

    private static int executeTestModeOff(ServerCommandSource source) {
        TEST_MODE_ENABLED = false;
        saveTestModeConfig();
        source.sendFeedback(() -> Text.literal("Test mode disabled."), true);
        return 1;
    }

    private static int executeTestModeWhitelistAdd(ServerCommandSource source, String playerName) {
        String normalized = normalizeName(playerName);
        TEST_MODE_WHITELIST.add(normalized);
        saveTestModeConfig();
        source.sendFeedback(() -> Text.literal("Added to test mode whitelist: " + playerName), true);
        return 1;
    }

    private static int executeTestModeWhitelistRemove(ServerCommandSource source, String playerName) {
        String normalized = normalizeName(playerName);
        TEST_MODE_WHITELIST.remove(normalized);
        saveTestModeConfig();
        source.sendFeedback(() -> Text.literal("Removed from test mode whitelist: " + playerName), true);
        return 1;
    }

    private static int executeTestModeWhitelistClear(ServerCommandSource source) {
        TEST_MODE_WHITELIST.clear();
        saveTestModeConfig();
        source.sendFeedback(() -> Text.literal("Cleared test mode whitelist."), true);
        return 1;
    }

    private static int executeTestModeWhitelistList(ServerCommandSource source) {
        if (TEST_MODE_WHITELIST.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Test mode whitelist is empty."), false);
            return 0;
        }

        source.sendFeedback(() -> Text.literal("Test mode whitelist: " + String.join(", ", TEST_MODE_WHITELIST)), false);
        return TEST_MODE_WHITELIST.size();
    }

    private static int executeReload(ServerCommandSource source) {
        if (source.getServer().isDedicated()) {
            LifestealConfig.load();
        } else {
            LifestealConfig.applySingleplayerOverrides(LifestealClientConfig.load());
        }
        clampOnlinePlayersToConfiguredMax(source.getServer());
        enchantmentClampCooldown = 0;
        source.sendFeedback(() -> Text.literal("Lifesteal config reloaded."), true);
        return 1;
    }

    private static int executeRevive(ServerCommandSource source, String playerName) {
        Optional<com.mojang.authlib.GameProfile> profile = source.getServer().getApiServices().profileResolver().getProfileByName(playerName);
        if (profile.isEmpty()) {
            source.sendError(Text.translatable("message.lifesteal.revive_failed", playerName));
            return 0;
        }

        PlayerConfigEntry configEntry = new PlayerConfigEntry(profile.get());
        var banEntry = source.getServer().getPlayerManager().getUserBanList().get(configEntry);
        if (banEntry == null || !ELIMINATION_BAN_REASON.equals(banEntry.getReason())) {
            source.sendError(Text.translatable("message.lifesteal.revive_failed", playerName));
            return 0;
        }

        source.getServer().getPlayerManager().getUserBanList().remove(configEntry);
        PENDING_REVIVED_PLAYERS.add(profile.get().id());

        ServerPlayerEntity onlineTarget = source.getServer().getPlayerManager().getPlayer(profile.get().id());
        if (onlineTarget != null) {
            setPlayerMaxHealth(onlineTarget, REVIVED_MAX_HEALTH);
            PENDING_REVIVED_PLAYERS.remove(profile.get().id());
        }

        source.getServer().getPlayerManager().broadcast(
                Text.literal(playerName + " was revived").formatted(Formatting.GREEN),
                false
        );
        return 1;
    }

    private static int executeShowConfigOption(ServerCommandSource source, String key) {
        LifestealConfig.ConfigOption option = LifestealConfig.getOption(key);
        if (option == null) {
            source.sendError(Text.literal("Unknown Lifesteal config option: " + key));
            return 0;
        }

        source.sendFeedback(() -> Text.literal(option.key).formatted(Formatting.WHITE, Formatting.BOLD), false);
        source.sendFeedback(() -> Text.literal(option.description), false);

        boolean isDefaultValue = option.isDefaultValue();
        String valueText = option.currentValueAsText();
        Text currentValue = Text.literal(valueText + (isDefaultValue ? " (default value)" : " (modified value)"))
                .formatted(Formatting.BOLD);
        source.sendFeedback(() -> Text.literal("current value: ").append(currentValue), false);

        if (option.type == LifestealConfig.ConfigOption.Type.BOOLEAN) {
            Text options = Text.literal("Options: ")
                    .append(Text.literal("[ ").formatted(Formatting.YELLOW))
                    .append(buildOptionToggle(option, true, isDefaultValue))
                    .append(Text.literal("  "))
                    .append(buildOptionToggle(option, false, isDefaultValue))
                    .append(Text.literal(" ]").formatted(Formatting.YELLOW));
            source.sendFeedback(() -> options, false);
        } else {
            source.sendFeedback(
                    () -> Text.literal("Set with: /lifesteal " + option.key + " <" + option.minInteger() + "-" + option.maxInteger() + ">"),
                    false
            );
        }
        return 1;
    }

    private static int executeSetConfigOption(ServerCommandSource source, String key, String value) {
        LifestealConfig.ConfigOption option = LifestealConfig.getOption(key);
        if (option == null) {
            source.sendError(Text.literal("Unknown Lifesteal config option: " + key));
            return 0;
        }

        try {
            option.setValueFromString(value);
        } catch (IllegalArgumentException exception) {
            source.sendError(Text.literal(exception.getMessage()));
            return 0;
        }
        LifestealConfig.save();
        if ("enableEnchantmentLimits".equalsIgnoreCase(option.key)) {
            enchantmentClampCooldown = 0;
        }
        if ("maxHearts".equalsIgnoreCase(option.key)) {
            clampOnlinePlayersToConfiguredMax(source.getServer());
        }

        Text newValue = Text.literal(option.currentValueAsText()).formatted(Formatting.GREEN);
        source.sendFeedback(() -> Text.literal(option.key + " set to ").append(newValue), true);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestConfigValue(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        LifestealConfig.ConfigOption option = LifestealConfig.getOption(getString(context, "setting"));
        if (option == null) {
            return builder.buildFuture();
        }

        if (option.type == LifestealConfig.ConfigOption.Type.BOOLEAN) {
            return CommandSource.suggestMatching(List.of("true", "false"), builder);
        }
        return CommandSource.suggestMatching(List.of(
                Integer.toString(option.minInteger()),
                Integer.toString(option.currentIntegerValue()),
                Integer.toString(option.maxInteger())
        ), builder);
    }

    private static Text buildOptionToggle(LifestealConfig.ConfigOption option, boolean value, boolean isCurrentValueDefault) {
        boolean isSelected = option.currentBooleanValue() == value;
        Formatting optionColor = isCurrentValueDefault ? Formatting.WHITE : Formatting.GREEN;
        Text choice = Text.literal(value ? "true" : "false")
                .formatted(optionColor)
                .styled(style -> style.withClickEvent(
                        new ClickEvent.SuggestCommand("/lifesteal " + option.key + " " + value)
                ));
        if (isSelected) {
            return choice.copy().formatted(Formatting.BOLD);
        }
        return choice;
    }

    private static void kickNonWhitelistedPlayers(MinecraftServer server, ServerCommandSource source) {
        String executorName = source.getEntity() instanceof ServerPlayerEntity sourcePlayer
                ? normalizeName(sourcePlayer.getGameProfile().name())
                : null;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String playerName = normalizeName(player.getGameProfile().name());
            if (isWhitelistedForTestMode(playerName)) {
                continue;
            }
            if (executorName != null && executorName.equals(playerName)) {
                continue;
            }

            player.networkHandler.disconnect(Text.literal(TEST_MODE_KICK_MESSAGE));
        }
    }

    private static double getConfiguredMaxHealth() {
        return Math.max(MIN_MAX_HEALTH, LifestealConfig.get().maxHearts * HEALTH_DELTA);
    }

    private static void clampOnlinePlayersToConfiguredMax(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            setPlayerMaxHealth(player, getPlayerMaxHealth(player));
        }
    }

    private static boolean isWhitelistedForTestMode(String playerName) {
        return TEST_MODE_WHITELIST.contains(normalizeName(playerName));
    }

    private static String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static void loadTestModeConfig() {
        try {
            if (!Files.exists(TEST_MODE_CONFIG_PATH)) {
                return;
            }

            String json = Files.readString(TEST_MODE_CONFIG_PATH);
            TestModeConfig config = GSON.fromJson(json, TestModeConfig.class);
            if (config == null) {
                return;
            }

            TEST_MODE_ENABLED = config.enabled;
            TEST_MODE_WHITELIST.clear();
            if (config.whitelist != null) {
                for (String name : config.whitelist) {
                    if (name != null && !name.isBlank()) {
                        TEST_MODE_WHITELIST.add(normalizeName(name));
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveTestModeConfig() {
        try {
            Path parent = TEST_MODE_CONFIG_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            TestModeConfig config = new TestModeConfig();
            config.enabled = TEST_MODE_ENABLED;
            config.whitelist = new ArrayList<>(TEST_MODE_WHITELIST);
            Files.writeString(TEST_MODE_CONFIG_PATH, GSON.toJson(config));
        } catch (Exception ignored) {
        }
    }

    private static class TestModeConfig {
        boolean enabled;
        List<String> whitelist;
    }

    private static void clampPlayerEnchantments(ServerPlayerEntity player) {
        for (ItemStack stack : player.getInventory().getMainStacks()) {
            EnchantmentLimiter.clampStack(stack);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmorSlot() || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                EnchantmentLimiter.clampStack(player.getEquippedStack(slot));
            }
        }

        for (int i = 0; i < player.getEnderChestInventory().size(); i++) {
            EnchantmentLimiter.clampStack(player.getEnderChestInventory().getStack(i));
        }

        if (player.currentScreenHandler != null) {
            for (Slot slot : player.currentScreenHandler.slots) {
                EnchantmentLimiter.clampStack(slot.getStack());
            }
        }
    }

}
