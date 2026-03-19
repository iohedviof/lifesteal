package dev.lifesteal.lifesteal.mixin;

import dev.lifesteal.lifesteal.recipe.NetheriteUpgradeBlocker;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.SmithingTransformRecipe;
import net.minecraft.recipe.input.SmithingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SmithingTransformRecipe.class)
public class SmithingTransformRecipeMixin {
    @Inject(method = "craft", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockNetheriteUpgradeCraft(
            SmithingRecipeInput input,
            RegistryWrapper.WrapperLookup registries,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (NetheriteUpgradeBlocker.isBlockedUpgrade(input)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "craft", at = @At("RETURN"), cancellable = true)
    private void lifesteal$blockNetheriteUpgradeCraftReturn(
            SmithingRecipeInput input,
            RegistryWrapper.WrapperLookup registries,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (NetheriteUpgradeBlocker.isBlockedUpgrade(input) && !cir.getReturnValue().isEmpty()) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
