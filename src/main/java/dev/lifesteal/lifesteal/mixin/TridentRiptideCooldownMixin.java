package dev.lifesteal.lifesteal.mixin;

import dev.lifesteal.lifesteal.config.LifestealConfig;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public class TridentRiptideCooldownMixin {
    @Inject(method = "onStoppedUsing", at = @At("TAIL"), require = 0)
    private void lifesteal$applyRiptideCooldown(
            ItemStack stack,
            World world,
            LivingEntity user,
            int remainingUseTicks,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!LifestealConfig.get().enableRiptideCooldown) {
            return;
        }
        if (world.isClient() || !(user instanceof PlayerEntity player)) {
            return;
        }
        if (!cir.getReturnValue()) {
            return;
        }
        if (EnchantmentHelper.getTridentSpinAttackStrength(stack, user) <= 0.0F) {
            return;
        }

        int cooldownTicks = LifestealConfig.get().riptideCooldown;
        if (cooldownTicks <= 0) {
            return;
        }
        player.getItemCooldownManager().set(stack, cooldownTicks);
    }
}
