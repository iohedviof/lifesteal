package dev.lifesteal.lifesteal.mixin;

import dev.lifesteal.lifesteal.config.LifestealConfig;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMaceCooldownMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void lifesteal$balancedMaceAttack(Entity target, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (!LifestealConfig.get().balancedMace) {
            return;
        }

        ItemStack stack = player.getMainHandStack();
        if (!stack.isOf(Items.MACE)) {
            return;
        }

        if (player.getItemCooldownManager().isCoolingDown(stack)) {
            ci.cancel();
            return;
        }

        player.getItemCooldownManager().set(stack, 20 * 60);
    }
}
