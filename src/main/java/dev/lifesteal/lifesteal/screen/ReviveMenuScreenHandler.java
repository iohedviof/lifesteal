package dev.lifesteal.lifesteal.screen;

import dev.lifesteal.lifesteal.Lifesteal;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public class ReviveMenuScreenHandler extends GenericContainerScreenHandler {
    private final MinecraftServer server;
    private final List<String> bannedPlayers;
    private final boolean consumeFromOffhand;
    private final int menuSize;

    private ReviveMenuScreenHandler(
            ScreenHandlerType<?> type,
            int rows,
            int syncId,
            PlayerInventory playerInventory,
            SimpleInventory inventory,
            MinecraftServer server,
            List<String> bannedPlayers,
            boolean consumeFromOffhand
    ) {
        super(type, syncId, playerInventory, inventory, rows);
        this.server = server;
        this.bannedPlayers = bannedPlayers;
        this.consumeFromOffhand = consumeFromOffhand;
        this.menuSize = inventory.size();
    }

    public static ReviveMenuScreenHandler create(
            int syncId,
            PlayerInventory playerInventory,
            MinecraftServer server,
            List<String> bannedPlayers,
            boolean consumeFromOffhand
    ) {
        int rows = Math.max(1, Math.min(6, (bannedPlayers.size() + 8) / 9));
        int size = rows * 9;
        SimpleInventory inventory = new SimpleInventory(size);
        ItemStack filler = createFillerPane();

        for (int i = 0; i < size; i++) {
            inventory.setStack(i, filler.copy());
        }

        for (int i = 0; i < bannedPlayers.size() && i < size; i++) {
            inventory.setStack(i, Lifesteal.createHeadFor(server, bannedPlayers.get(i)));
        }

        return new ReviveMenuScreenHandler(
                getTypeForRows(rows),
                rows,
                syncId,
                playerInventory,
                inventory,
                server,
                bannedPlayers,
                consumeFromOffhand
        );
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (actionType == SlotActionType.THROW) {
            return;
        }

        if (slotIndex >= 0 && slotIndex < menuSize) {
            if (slotIndex < bannedPlayers.size() && player instanceof ServerPlayerEntity serverPlayer) {
                if (Lifesteal.reviveBannedPlayer(server, serverPlayer, bannedPlayers.get(slotIndex))) {
                    consumeBeacon(serverPlayer);
                }
                serverPlayer.closeHandledScreen();
            }
            return;
        }

        // Block interactions with player inventory while this menu is open.
    }

    private static ItemStack createFillerPane() {
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
        return filler;
    }

    private void consumeBeacon(ServerPlayerEntity player) {
        if (player.isCreative()) {
            return;
        }

        if (consumeFromOffhand) {
            if (consumeIfBeacon(player.getOffHandStack())) {
                return;
            }
            if (consumeIfBeacon(player.getMainHandStack())) {
                return;
            }
        } else {
            if (consumeIfBeacon(player.getMainHandStack())) {
                return;
            }
            if (consumeIfBeacon(player.getOffHandStack())) {
                return;
            }
        }

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (consumeIfBeacon(stack)) {
                return;
            }
        }
    }

    private static boolean consumeIfBeacon(ItemStack stack) {
        if (!stack.isOf(Lifesteal.BEACON_OF_LIFE) || stack.isEmpty()) {
            return false;
        }
        stack.decrement(1);
        return true;
    }

    private static ScreenHandlerType<?> getTypeForRows(int rows) {
        return switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            default -> ScreenHandlerType.GENERIC_9X6;
        };
    }
}
