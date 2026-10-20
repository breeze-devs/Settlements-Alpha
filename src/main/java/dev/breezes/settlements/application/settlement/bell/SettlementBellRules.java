package dev.breezes.settlements.application.settlement.bell;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * What counts as a settlement's bell, and the name a bell or a name tag gives a settlement.
 * <p>
 * A bell is any block villagers gather at: a block whose point of interest type is meeting. Every reader answers on
 * either logical side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementBellRules {

    /**
     * Matches the point of interest type of a bell, for queries against a level's points of interest.
     */
    public static final Predicate<Holder<PoiType>> POI_TYPE = type -> type.is(PoiTypes.MEETING);

    /**
     * Returns whether the block is a bell. Reads the block state alone, so it answers before the level registers the
     * bell's point of interest, and on a client, which has none.
     */
    public static boolean isBell(@Nonnull BlockState state) {
        return PoiTypes.forState(state).filter(POI_TYPE).isPresent();
    }

    /**
     * Returns the name a custom name in the components gives a settlement: its text without surrounding whitespace, or
     * empty when the components carry no custom name or one that is only whitespace.
     */
    public static Optional<String> settlementName(@Nonnull DataComponentMap components) {
        Component customName = components.get(DataComponents.CUSTOM_NAME);
        // strip, not trim: the numeral rule rejects a name that is blank by Unicode whitespace, which trim keeps
        String name = customName == null ? "" : customName.getString().strip();
        return name.isEmpty() ? Optional.empty() : Optional.of(name);
    }

    /**
     * Returns the name the stack gives a bell or its settlement when it is a name tag carrying a name, or empty when it
     * is not a name tag or its name is only whitespace.
     */
    public static Optional<String> nameTagName(@Nonnull ItemStack stack) {
        return stack.getItem() instanceof NameTagItem ? settlementName(stack.getComponents()) : Optional.empty();
    }

}
