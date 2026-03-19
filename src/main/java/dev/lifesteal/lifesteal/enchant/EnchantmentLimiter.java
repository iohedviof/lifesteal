package dev.lifesteal.lifesteal.enchant;

import dev.lifesteal.lifesteal.config.LifestealConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

public final class EnchantmentLimiter {
    private EnchantmentLimiter() {
    }

    public static void clampStack(ItemStack stack) {
        if (!LifestealConfig.get().enableEnchantmentLimits) {
            return;
        }

        clampComponentOnStack(stack, DataComponentTypes.ENCHANTMENTS);
        clampComponentOnStack(stack, DataComponentTypes.STORED_ENCHANTMENTS);
    }

    public static ItemEnchantmentsComponent clampComponent(ItemEnchantmentsComponent component) {
        if (!LifestealConfig.get().enableEnchantmentLimits) {
            return component;
        }
        if (component.isEmpty()) {
            return component;
        }

        boolean changed = false;
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(component);

        for (RegistryEntry<Enchantment> enchantment : component.getEnchantments()) {
            int level = component.getLevel(enchantment);
            int maxLevel = getMaxLevel(enchantment);
            if (maxLevel > 0 && level > maxLevel) {
                builder.set(enchantment, maxLevel);
                changed = true;
            }
        }

        return changed ? builder.build() : component;
    }

    private static void clampComponentOnStack(ItemStack stack, ComponentType<ItemEnchantmentsComponent> type) {
        ItemEnchantmentsComponent component = stack.getOrDefault(type, ItemEnchantmentsComponent.DEFAULT);
        ItemEnchantmentsComponent clamped = clampComponent(component);
        if (clamped != component) {
            stack.set(type, clamped);
        }
    }

    private static int getMaxLevel(RegistryEntry<Enchantment> enchantment) {
        if (matchesEnchantment(enchantment, Enchantments.PROTECTION)
                || matchesEnchantment(enchantment, Enchantments.FIRE_PROTECTION)
                || matchesEnchantment(enchantment, Enchantments.BLAST_PROTECTION)
                || matchesEnchantment(enchantment, Enchantments.PROJECTILE_PROTECTION)) {
            return 3;
        }
        if (matchesEnchantment(enchantment, Enchantments.SHARPNESS)
                || matchesEnchantment(enchantment, Enchantments.POWER)
                || matchesEnchantment(enchantment, Enchantments.DENSITY)) {
            return 4;
        }
        return -1;
    }

    private static boolean matchesEnchantment(RegistryEntry<Enchantment> entry, RegistryKey<Enchantment> key) {
        return entry.matchesKey(key);
    }
}
