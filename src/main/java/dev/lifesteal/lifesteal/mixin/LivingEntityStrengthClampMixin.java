package dev.lifesteal.lifesteal.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import dev.lifesteal.lifesteal.config.LifestealConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class LivingEntityStrengthClampMixin {
    @ModifyVariable(
            method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"),
            argsOnly = true
    )
    private StatusEffectInstance lifesteal$clampStrengthWithSource(StatusEffectInstance effect) {
        return clampStrength(effect);
    }

    @ModifyVariable(
            method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;)Z",
            at = @At("HEAD"),
            argsOnly = true
    )
    private StatusEffectInstance lifesteal$clampStrength(StatusEffectInstance effect) {
        return clampStrength(effect);
    }

    private static StatusEffectInstance clampStrength(StatusEffectInstance effect) {
        if (LifestealConfig.get().allowStrengthII) {
            return effect;
        }
        if (effect == null || effect.getEffectType() != StatusEffects.STRENGTH || effect.getAmplifier() <= 0) {
            return effect;
        }

        return new StatusEffectInstance(
                effect.getEffectType(),
                effect.getDuration(),
                0,
                effect.isAmbient(),
                effect.shouldShowParticles(),
                effect.shouldShowIcon(),
                null
        );
    }
}
