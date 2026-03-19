package dev.lifesteal.lifesteal.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import dev.lifesteal.lifesteal.config.LifestealConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingRecipeRegistry.class)
public class BrewingRecipeRegistryMixin {
    @Inject(method = "hasRecipe", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockStrengthTwoRecipe(ItemStack input, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        if (isBannedStrengthTwoRecipe(input, ingredient)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "craft", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockStrengthTwoCraft(ItemStack ingredient, ItemStack input, CallbackInfoReturnable<ItemStack> cir) {
        if (isBannedStrengthTwoRecipe(input, ingredient)) {
            ItemStack potionStack = getStrengthPotionStack(input, ingredient);
            if (!potionStack.isEmpty()) {
                cir.setReturnValue(potionStack.copy());
            }
        }
    }

    private static boolean isBannedStrengthTwoRecipe(ItemStack stackA, ItemStack stackB) {
        if (LifestealConfig.get().allowStrengthII) {
            return false;
        }
        ItemStack potionStack = getStrengthPotionStack(stackA, stackB);
        if (potionStack.isEmpty()) {
            return false;
        }

        ItemStack ingredientStack = potionStack == stackA ? stackB : stackA;
        if (!ingredientStack.isOf(Items.GLOWSTONE_DUST)) {
            return false;
        }

        PotionContentsComponent contents = potionStack.getOrDefault(
                DataComponentTypes.POTION_CONTENTS,
                PotionContentsComponent.DEFAULT
        );

        return isAnyStrengthContents(contents);
    }

    private static ItemStack getStrengthPotionStack(ItemStack stackA, ItemStack stackB) {
        if (isAnyStrengthPotion(stackA)) {
            return stackA;
        }
        if (isAnyStrengthPotion(stackB)) {
            return stackB;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isAnyStrengthPotion(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        PotionContentsComponent contents = stack.getOrDefault(
                DataComponentTypes.POTION_CONTENTS,
                PotionContentsComponent.DEFAULT
        );

        return isAnyStrengthContents(contents);
    }

    private static boolean isAnyStrengthContents(PotionContentsComponent contents) {
        return contents.potion()
                .filter(potion -> potion.equals(Potions.STRENGTH)
                        || potion.equals(Potions.LONG_STRENGTH)
                        || potion.equals(Potions.STRONG_STRENGTH))
                .isPresent();
    }
}
