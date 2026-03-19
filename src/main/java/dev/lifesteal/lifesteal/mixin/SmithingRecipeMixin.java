package dev.lifesteal.lifesteal.mixin;

import dev.lifesteal.lifesteal.recipe.NetheriteUpgradeBlocker;
import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.recipe.input.SmithingRecipeInput;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SmithingRecipe.class)
public interface SmithingRecipeMixin {
    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockNetheriteUpgradeMatch(SmithingRecipeInput input, World world, CallbackInfoReturnable<Boolean> cir) {
        if (NetheriteUpgradeBlocker.isBlockedUpgrade(input)) {
            cir.setReturnValue(false);
        }
    }
}
