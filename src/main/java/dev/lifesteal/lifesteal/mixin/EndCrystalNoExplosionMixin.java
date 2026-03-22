package dev.lifesteal.lifesteal.mixin;

import dev.lifesteal.lifesteal.config.LifestealConfig;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndCrystalEntity.class)
public class EndCrystalNoExplosionMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true, require = 0)
    private void lifesteal$removeWithoutExplosion(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!LifestealConfig.get().disableCrystalDamage) {
            return;
        }

        EndCrystalEntity crystal = (EndCrystalEntity) (Object) this;
        double x = crystal.getX();
        double y = crystal.getY();
        double z = crystal.getZ();

        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        world.spawnParticles(ParticleTypes.EXPLOSION, x, y, z, 16, 0.4D, 0.4D, 0.4D, 0.02D);
        world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, 1.0F);

        crystal.discard();
        cir.setReturnValue(true);
    }
}
