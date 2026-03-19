package dev.lifesteal.lifesteal.item;

import dev.lifesteal.lifesteal.Lifesteal;
import dev.lifesteal.lifesteal.screen.ReviveMenuScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;

public class BeaconOfLifeItem extends Item {
    public BeaconOfLifeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        List<String> bannedPlayers = Lifesteal.getBannedProfiles(serverPlayer.getEntityWorld().getServer());
        serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, player) -> ReviveMenuScreenHandler.create(
                        syncId,
                        playerInventory,
                        serverPlayer.getEntityWorld().getServer(),
                        bannedPlayers,
                        hand == Hand.OFF_HAND
                ),
                Text.translatable("item.lifesteal.beacon_of_life")
        ));

        return ActionResult.CONSUME;
    }
}
