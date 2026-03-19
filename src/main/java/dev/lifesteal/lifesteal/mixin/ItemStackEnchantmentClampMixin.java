package dev.lifesteal.lifesteal.mixin;

import dev.lifesteal.lifesteal.enchant.EnchantmentLimiter;
import dev.lifesteal.lifesteal.config.LifestealConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import net.minecraft.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackEnchantmentClampMixin {
    @Inject(method = "addEnchantment", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockMaceEnchantments(RegistryEntry<Enchantment> enchantment, int level, CallbackInfo ci) {
        if (!LifestealConfig.get().balancedMace) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;
        if (stack.isOf(Items.MACE)) {
            ci.cancel();
        }
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void lifesteal$clampEnchantments(World world, Entity entity, EquipmentSlot slot, CallbackInfo ci) {
        if (world.isClient()) {
            return;
        }
        EnchantmentLimiter.clampStack((ItemStack) (Object) this);

        if (!LifestealConfig.get().balancedMace) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;
        if (!stack.isOf(Items.MACE)) {
            return;
        }

        ItemEnchantmentsComponent enchantments = stack.getOrDefault(
                DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
        );
        ItemEnchantmentsComponent stored = stack.getOrDefault(
                DataComponentTypes.STORED_ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
        );

        if (!enchantments.isEmpty()) {
            stack.set(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        }
        if (!stored.isEmpty()) {
            stack.set(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        }
    }
}
