package dev.lifesteal.lifesteal.recipe;

import dev.lifesteal.lifesteal.config.LifestealConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.input.SmithingRecipeInput;

public final class NetheriteUpgradeBlocker {
    private NetheriteUpgradeBlocker() {
    }

    public static boolean isBlockedUpgrade(SmithingRecipeInput input) {
        if (LifestealConfig.get().allowNetheriteUpgrades) {
            return false;
        }

        ItemStack base = input.base();
        if (!isDiamondUpgradeTarget(base)) {
            return false;
        }

        ItemStack addition = input.addition();
        if (!addition.isOf(Items.NETHERITE_INGOT)) {
            return false;
        }

        ItemStack template = input.template();
        if (!template.isEmpty() && !template.isOf(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
            return false;
        }

        return true;
    }

    private static boolean isDiamondUpgradeTarget(ItemStack base) {
        return base.isOf(Items.DIAMOND_HELMET)
                || base.isOf(Items.DIAMOND_CHESTPLATE)
                || base.isOf(Items.DIAMOND_LEGGINGS)
                || base.isOf(Items.DIAMOND_BOOTS)
                || base.isOf(Items.DIAMOND_SWORD)
                || base.isOf(Items.DIAMOND_AXE);
    }
}
