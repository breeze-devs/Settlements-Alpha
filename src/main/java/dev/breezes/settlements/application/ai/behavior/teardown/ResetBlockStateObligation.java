package dev.breezes.settlements.application.ai.behavior.teardown;

import dev.breezes.settlements.domain.world.blocks.BlockFlag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Obligation to reset a block state property to a target value after a crash.
 * <p>
 * Canonical use-case: resetting the {@code LIT} property on a smoker or blast furnace
 * that was left lit when the server crashed mid-behavior.
 * <p>
 * The block identity guard ({@code expectedBlockId}) prevents a discharge from
 * clobbering a block that a player placed in the same position after the crash.
 * The value guard (current value == reset value) makes {@link #stillValid} return
 * {@code false} when the property is already at its target, so teardown after normal
 * completion is always a safe no-op.
 * <p>
 * {@code propertyName} and {@code resetValue} are stored as strings so this record
 * is serializable without MC registry access at construction time, enabling clean
 * unit tests of the codec.
 */
public record ResetBlockStateObligation(@Nonnull BlockPos pos,
                                        @Nonnull ResourceLocation expectedBlockId,
                                        @Nonnull String propertyName,
                                        @Nonnull String resetValue) implements TeardownObligation {

    @Override
    public BlockPos targetPos() {
        return this.pos;
    }

    @Override
    public boolean stillValid(@Nonnull ServerLevel level) {
        if (!level.isLoaded(this.pos)) {
            return false;
        }

        BlockState state = level.getBlockState(this.pos);
        if (!state.is(resolveExpectedBlock())) {
            return false;
        }

        Property<?> property = findProperty(state, this.propertyName);
        if (property == null) {
            return false;
        }

        Optional<?> targetValueOpt = property.getValue(this.resetValue);
        if (targetValueOpt.isEmpty()) {
            return false;
        }

        // We need to discharge only if the property hasn't already been reset
        Object currentValue = getCurrentValue(state, property);
        return !currentValue.equals(targetValueOpt.get());
    }

    @Override
    public void discharge(@Nonnull ServerLevel level) {
        BlockState currentState = level.getBlockState(this.pos);
        if (!currentState.is(resolveExpectedBlock())) {
            return;
        }

        Property<?> property = findProperty(currentState, this.propertyName);
        if (property == null) {
            return;
        }

        Optional<?> targetValueOpt = property.getValue(this.resetValue);
        if (targetValueOpt.isEmpty()) {
            return;
        }

        BlockState newState = applyPropertyValue(currentState, property, targetValueOpt.get());
        level.setBlock(this.pos, newState, BlockFlag.of(BlockFlag.SEND_BLOCK_UPDATE, BlockFlag.SEND_CLIENT_UPDATE));
    }

    @Override
    public boolean durable() {
        return true;
    }

    @Override
    public String describe() {
        return "reset block state " + this.propertyName + "=" + this.resetValue
                + " on " + this.expectedBlockId + " at " + this.pos.toShortString();
    }

    @Nullable
    private Block resolveExpectedBlock() {
        return BuiltInRegistries.BLOCK.get(this.expectedBlockId);
    }

    @Nullable
    private static Property<?> findProperty(@Nonnull BlockState state, @Nonnull String name) {
        for (Property<?> p : state.getProperties()) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> T getCurrentValue(@Nonnull BlockState state, @Nonnull Property<?> property) {
        return state.getValue((Property<T>) property);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState applyPropertyValue(@Nonnull BlockState state, @Nonnull Property<?> property, @Nonnull Object value) {
        return state.setValue((Property) property, (Comparable) value);
    }

}
