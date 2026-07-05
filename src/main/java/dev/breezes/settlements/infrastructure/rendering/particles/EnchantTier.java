package dev.breezes.settlements.infrastructure.rendering.particles;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import javax.annotation.Nonnull;

/**
 * Quality band of a finished enchant, bundling the presentation config that makes the outcome read
 * at a glance: orb color, emission cadence, and rise speed for the ascending aura — white = ordinary,
 * purple = notable, gold = a heavily-enchanted "jackpot".
 * <p>
 * This lives in the rendering layer rather than the domain because every field it carries is a
 * visual concern; {@link #classify} is only the mapping from an enchant result onto that visual band.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum EnchantTier {

    WHITE(0xFFFFFF, 10, 0.1D),
    PURPLE(0x9B30FF, 6, 0.15D),
    GOLD(0xFFD700, 2, 0.2D);

    // Summed enchantment levels at or above which a result reads as the top "jackpot" band
    private static final int GOLD_POWER_THRESHOLD = 9;
    // Summed enchantment levels at or above which a result reads as "notable" instead of ordinary
    private static final int PURPLE_POWER_THRESHOLD = 4;

    private final int packedRgb;
    /**
     * Ticks between single-orb emissions during the reveal; a shorter interval yields a denser rising
     * column. Every tier's value must be a multiple of the emitter's granularity (see
     * {@code EnchantItemBehavior}) so the modulo cadence gate lands cleanly.
     */
    private final int emitIntervalTicks;
    /**
     * Initial upward speed of each emitted orb. Kept as an explicit per-tier value rather than derived
     * from {@code ordinal()} so reordering or inserting a tier can't silently shift the rise physics.
     */
    private final double riseVelocity;

    /**
     * Classifies a freshly enchanted result from the summed level of its applied enchantments.
     * <p>
     * {@code getEnchantmentsForCrafting} is used rather than reading the {@code ENCHANTMENTS}
     * component directly so enchanted books (which store their enchantments under
     * {@code STORED_ENCHANTMENTS}) are read correctly too.
     */
    public static EnchantTier classify(@Nonnull ItemStack enchantedResult) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(enchantedResult);

        int totalPower = 0;
        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            totalPower += enchantments.getLevel(enchantment);
        }

        if (totalPower >= GOLD_POWER_THRESHOLD) {
            return GOLD;
        }
        if (totalPower >= PURPLE_POWER_THRESHOLD) {
            return PURPLE;
        }
        return WHITE;
    }

}
