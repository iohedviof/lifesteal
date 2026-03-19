package dev.lifesteal.lifesteal.mixin;

import dev.lifesteal.lifesteal.config.LifestealConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMaceCooldownMixin {
    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void lifesteal$blockMaceSwing(Hand hand, CallbackInfo ci) {
        if (!shouldBlockSwing(hand)) {
            return;
        }
        ci.cancel();
    }

    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void lifesteal$blockMaceSwing(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
        if (!shouldBlockSwing(hand)) {
            return;
        }
        ci.cancel();
    }

    private boolean shouldBlockSwing(Hand hand) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!LifestealConfig.get().balancedMace) {
            return false;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(Items.MACE)) {
            return false;
        }
        return player.getItemCooldownManager().isCoolingDown(stack);
    }
}
