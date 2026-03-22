package dev.lifesteal.lifesteal.mixin;

import dev.lifesteal.lifesteal.config.LifestealConfig;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemCooldownManager.class)
public class EnderPearlCooldownMixin {
    @Inject(method = "isCoolingDown", at = @At("HEAD"), cancellable = true)
    private void lifesteal$noPearlCooldown(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!LifestealConfig.get().disableEnderPearls) {
            return;
        }
        if (stack.isOf(Items.ENDER_PEARL)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getCooldownProgress", at = @At("HEAD"), cancellable = true)
    private void lifesteal$noPearlCooldownBar(ItemStack stack, float tickProgress, CallbackInfoReturnable<Float> cir) {
        if (!LifestealConfig.get().disableEnderPearls) {
            return;
        }
        if (stack.isOf(Items.ENDER_PEARL)) {
            cir.setReturnValue(0.0F);
        }
    }
}
