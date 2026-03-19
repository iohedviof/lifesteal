package dev.lifesteal.lifesteal.mixin.client;

import dev.lifesteal.lifesteal.config.LifestealConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMaceCooldownMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true, require = 0)
    private void lifesteal$balancedMaceAttack(Entity target, CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
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
