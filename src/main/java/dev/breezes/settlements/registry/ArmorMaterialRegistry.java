package dev.breezes.settlements.registry;

import dev.breezes.settlements.SettlementsMod;
import lombok.AllArgsConstructor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

@AllArgsConstructor
public enum ArmorMaterialRegistry implements ArmorMaterial {

    SAPPHIRE("sapphire", 26, new int[]{5, 7, 5, 4}, 25, SoundEvents.ARMOR_EQUIP_GOLD, 1F, 0F, () -> Ingredient.of(ItemRegistry.SAPPHIRE.get()));

    // Base durability for [helmet, chestplate, leggings, boots], copied from vanilla
    private static final int[] BASE_DURABILITY = new int[]{13, 15, 16, 11};

    private final String name;
    private final int durabilityMultiplier;
    // Damage reduction values for [helmet, chestplate, leggings, boots]
    private final int[] protectionAmounts;
    private final int enchantability;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY[type.ordinal()] * this.durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return this.protectionAmounts[type.ordinal()];
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantability;
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    @Override
    public String getName() {
        return "%s:%s".formatted(SettlementsMod.MOD_ID, this.name);
    }

    @Override
    public float getToughness() {
        return this.toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }

}
