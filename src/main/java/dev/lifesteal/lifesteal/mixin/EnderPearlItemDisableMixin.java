package dev.lifesteal.lifesteal.mixin;

import dev.lifesteal.lifesteal.config.LifestealConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderPearlItem.class)
public class EnderPearlItemDisableMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void lifesteal$disablePearlUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!LifestealConfig.get().disableEnderPearls) {
            return;
        }
        user.setCurrentHand(hand);
        cir.setReturnValue(ActionResult.CONSUME);
    }

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true, require = 0)
    private void lifesteal$setUseTime(ItemStack stack, LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        if (!LifestealConfig.get().disableEnderPearls) {
            return;
        }
        cir.setReturnValue(1);
    }

    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true, require = 0)
    private void lifesteal$setUseAction(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
        if (!LifestealConfig.get().disableEnderPearls) {
            return;
        }
        cir.setReturnValue(UseAction.DRINK);
    }
}
