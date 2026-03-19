package dev.lifesteal.lifesteal.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.world.World;
import dev.lifesteal.lifesteal.config.LifestealConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ApplyEffectsConsumeEffect.class)
public class ApplyEffectsConsumeEffectMixin {
    @Inject(method = "onConsume", at = @At("HEAD"), cancellable = true)
    private void lifesteal$skipEnchantedGoldenAppleEffects(
            World world,
            ItemStack stack,
            LivingEntity user,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!LifestealConfig.get().allowGodApples && stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            cir.setReturnValue(false);
        }
    }
}
