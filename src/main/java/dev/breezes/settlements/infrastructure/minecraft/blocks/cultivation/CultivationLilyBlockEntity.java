package dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation;

import dev.breezes.settlements.bootstrap.registry.blockentities.BlockEntityTypeRegistry;
import dev.breezes.settlements.domain.farming.CultivationLilyValidity;
import dev.breezes.settlements.domain.farming.CultivationZone;
import dev.breezes.settlements.domain.farming.CultivationZoneCategorizer;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.LevelBlockStateView;
import dev.breezes.settlements.infrastructure.rendering.particles.OrbParticles;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Stores all per-instance configuration for a placed Cultivation Lily.
 * <p>
 * The block entity's {@link CultivationZone} owns the cell grid centred on the lily column; see
 * {@link CultivationZone#atDefault(BlockPos)} for the default extent this lily starts with.
 */
public class CultivationLilyBlockEntity extends BlockEntity {

    private static final String NBT_HALF_EXTENT_X = "halfExtentX";
    private static final String NBT_HALF_EXTENT_Z = "halfExtentZ";
    private static final String NBT_CROP_FILTER = "cropFilter";
    private static final String NBT_CROP_FILTER_DISPLAY_ITEM = "cropFilterDisplayItem";
    private static final String NBT_VALID = "valid";

    /**
     * Height in blocks at which the floating mesh is rendered above the lily-pad block.
     * Shared with {@link CultivationLilyRenderer} and {@link OrbParticles} so the particle
     * emit target and the render position never drift apart from independent magic numbers.
     */
    public static final float FLOAT_HEIGHT_BLOCKS = 3.0F;

    /**
     * Server-tick throttle for the {@code valid} flag, long enough to avoid redundant
     * full-zone scans while still reacting to structural changes quickly.
     */
    private static final long VALIDITY_RECHECK_INTERVAL_TICKS = ClockTicks.seconds(16).getTicks();

    /**
     * Server-tick throttle for the {@code needsCultivation} flag
     */
    private static final long NEEDS_CULTIVATION_RECHECK_INTERVAL_TICKS = ClockTicks.seconds(60).getTicks();

    /**
     * Client-tick cadence for the ambient orb aura — one burst every N ticks.
     */
    private static final int ORB_EMIT_CADENCE_TICKS = 15;

    /**
     * Locality gate: do not emit particles when no player is within this distance.
     */
    private static final double ORB_EMIT_PLAYER_RADIUS = 24.0;

    /**
     * How long the bloom's rise animation runs, in client ticks.
     */
    private static final long BLOOM_DURATION_TICKS = ClockTicks.seconds(1).getTicks();

    /**
     * Sentinel for {@link #bloomStartGameTime}: no bloom animation is currently in flight.
     */
    private static final long NO_BLOOM_IN_PROGRESS = -1L;

    @Getter
    private CultivationZone zone;

    @Getter
    @Nullable
    private ResourceLocation cropFilter;
    @Getter
    @Nullable
    private ResourceLocation cropFilterDisplayItem;

    @Getter
    private boolean valid;
    private boolean validityDirty;

    private final long validityRecheckPhaseOffset;
    private final long needsCultivationRecheckPhaseOffset;

    /**
     * Transient server-only flag: true when the zone currently contains at least one cell
     * that needs tilling, planting, or replanting.
     */
    private boolean needsCultivation;

    /**
     * Client-only: observes the {@code valid} flag for a bloom-worthy transition.
     */
    private final CultivationLilyBloomTracker bloomTracker = new CultivationLilyBloomTracker();

    /**
     * Client-only: the game time the current bloom animation began, or {@link #NO_BLOOM_IN_PROGRESS}
     * when none is in flight. Drives {@link #bloomProgress} for the renderer and the chime schedule in
     * {@link #tickBloom}.
     */
    private long bloomStartGameTime = NO_BLOOM_IN_PROGRESS;

    public CultivationLilyBlockEntity(@Nonnull BlockPos pos,
                                      @Nonnull BlockState blockState) {
        super(BlockEntityTypeRegistry.CULTIVATION_LILY.get(), pos, blockState);
        this.zone = CultivationZone.atDefault(pos);
        this.cropFilter = null;
        this.cropFilterDisplayItem = null;
        this.valid = false;
        this.validityDirty = true;
        this.validityRecheckPhaseOffset = phaseOffset(pos, VALIDITY_RECHECK_INTERVAL_TICKS);
        this.needsCultivationRecheckPhaseOffset = phaseOffset(pos, NEEDS_CULTIVATION_RECHECK_INTERVAL_TICKS);
        this.needsCultivation = false;
    }

    /**
     * The two recomputes below are independent and separately throttled for performance optimization.
     */
    public static void serverTick(@Nonnull Level level,
                                  @Nonnull BlockPos pos,
                                  @Nonnull BlockState state,
                                  @Nonnull CultivationLilyBlockEntity entity) {
        entity.maybeRecomputeValidity(level);
        entity.maybeRecomputeNeedsCultivation(level);
    }

    /**
     * The client-side per-tick entry point, driving the bloom and the ambient orb aura.
     * <p>
     * Emission is rate-gated on a coarse cadence and a player-proximity check so distant or
     * off-screen lilies contribute nothing to the particle pool.
     */
    public static void clientTick(@Nonnull Level level,
                                  @Nonnull BlockPos pos,
                                  @Nonnull BlockState state,
                                  @Nonnull CultivationLilyBlockEntity entity) {
        if (!level.isClientSide()) {
            return;
        }

        entity.tickBloom(level);

        if (!entity.isValid()) {
            return;
        }
        if (level.getGameTime() % ORB_EMIT_CADENCE_TICKS != 0L) {
            return;
        }

        // Skip emission when no player is nearby
        Player nearestPlayer = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                ORB_EMIT_PLAYER_RADIUS, false);
        if (nearestPlayer == null) {
            return;
        }

        RandomSource random = level.getRandom();
        OrbParticles.spawnRise(level, pos, random);
        for (int i = 0; i < 2; i++) {
            OrbParticles.spawnScatter(level, pos, FLOAT_HEIGHT_BLOCKS, entity.zone.halfExtentX(), entity.zone.halfExtentZ(), random);
        }
    }

    /**
     * Drives the bloom: starts the rise animation on invalid to valid transition.
     */
    private void tickBloom(@Nonnull Level level) {
        long gameTime = level.getGameTime();

        if (this.bloomTracker.observe(this.valid)) {
            this.bloomStartGameTime = gameTime;
            OrbParticles.spawnBloomRing(level, this.worldPosition, FLOAT_HEIGHT_BLOCKS);
        }

        if (this.bloomStartGameTime == NO_BLOOM_IN_PROGRESS) {
            return;
        }

        if (gameTime - this.bloomStartGameTime >= BLOOM_DURATION_TICKS) {
            this.bloomStartGameTime = NO_BLOOM_IN_PROGRESS;
        }
    }

    /**
     * Progress through the bloom's rise animation, in [0, 1]. Reads as 1 (fully risen) whenever no
     * bloom is in flight, so a lily whose bloom the player never witnessed renders at its settled
     * height rather than replaying the rise the moment it comes back into view.
     */
    float bloomProgress(long gameTime, float partialTick) {
        if (this.bloomStartGameTime == NO_BLOOM_IN_PROGRESS) {
            return 1.0F;
        }

        float elapsedTicks = (gameTime - this.bloomStartGameTime) + partialTick;
        return Math.min(1.0F, elapsedTicks / BLOOM_DURATION_TICKS);
    }

    /**
     * Throttled validity recompute
     */
    private void maybeRecomputeValidity(@Nonnull Level level) {
        if (!this.validityDirty && !isPeriodicScanTick(level, VALIDITY_RECHECK_INTERVAL_TICKS, this.validityRecheckPhaseOffset)) {
            return;
        }

        recomputeValidity(level);
    }

    /**
     * Throttled cultivation-need recompute, independent of the validity check.
     * Only runs when the lily is valid — an invalid lily has no actionable zone.
     */
    private void maybeRecomputeNeedsCultivation(@Nonnull Level level) {
        if (!isPeriodicScanTick(level, NEEDS_CULTIVATION_RECHECK_INTERVAL_TICKS, this.needsCultivationRecheckPhaseOffset)) {
            return;
        }
        if (!this.valid) {
            this.needsCultivation = false;
            return;
        }

        this.needsCultivation = CultivationZoneCategorizer.hasAnyCultivationWork(
                this.zone.streamCells(), new LevelBlockStateView(level), this.cropFilter);
    }

    /**
     * Spreads periodic zone scans across the interval so loaded lilies do not all rescan on the same global tick.
     */
    private static boolean isPeriodicScanTick(@Nonnull Level level, long intervalTicks, long phaseOffset) {
        return (level.getGameTime() + phaseOffset) % intervalTicks == 0L;
    }

    private static long phaseOffset(@Nonnull BlockPos pos, long intervalTicks) {
        return Math.floorMod(pos.hashCode(), intervalTicks);
    }

    /**
     * Returns the cached cultivation-need flag.
     * True when the zone contains at least one cell requiring tilling, planting, or replanting.
     * This is a server-only transient — it is never persisted or synced to the client.
     */
    public boolean needsCultivation() {
        return this.needsCultivation;
    }

    /**
     * Recomputes and caches the valid flag, per {@link CultivationLilyValidity}.
     * <p>
     * The sync and the light-state write are skipped unless the flag actually moved.
     */
    public void recomputeValidity(@Nonnull Level level) {
        boolean wasValid = this.valid;
        this.valid = computeIsValid(level);
        this.validityDirty = false;
        if (this.valid != wasValid) {
            syncLitState(level);
            setChangedAndSync();
        }
    }

    /**
     * Mirrors the freshly-computed {@code valid} flag onto the block's {@link BlockStateProperties#LIT}
     * property so the light engine turns the lily's glow on or off.
     */
    private void syncLitState(@Nonnull Level level) {
        BlockState current = getBlockState();
        if (!current.hasProperty(BlockStateProperties.LIT) || current.getValue(BlockStateProperties.LIT) == this.valid) {
            return;
        }
        level.setBlock(this.worldPosition, current.setValue(BlockStateProperties.LIT, this.valid), Block.UPDATE_ALL);
    }

    /**
     * Schedules validity recomputation on the block entity ticker.
     * This coalesces bursts of neighbor updates into one scan while still reacting before
     * the slower periodic safety check would run.
     */
    public void markValidityDirty() {
        this.validityDirty = true;
    }

    private boolean computeIsValid(@Nonnull Level level) {
        return CultivationLilyValidity.isValid(new LevelBlockStateView(level), this.zone, this.cropFilter);
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag,
                                  @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(NBT_HALF_EXTENT_X, this.zone.halfExtentX());
        tag.putInt(NBT_HALF_EXTENT_Z, this.zone.halfExtentZ());
        tag.putBoolean(NBT_VALID, this.valid);
        if (this.cropFilter != null) {
            tag.putString(NBT_CROP_FILTER, this.cropFilter.toString());
        }

        // The display item is persisted alongside the crop filter
        if (this.cropFilterDisplayItem != null) {
            tag.putString(NBT_CROP_FILTER_DISPLAY_ITEM, this.cropFilterDisplayItem.toString());
        }
    }

    @Override
    public void loadAdditional(@Nonnull CompoundTag tag,
                               @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.zone = new CultivationZone(this.worldPosition, tag.getInt(NBT_HALF_EXTENT_X), tag.getInt(NBT_HALF_EXTENT_Z));
        this.valid = tag.getBoolean(NBT_VALID);

        if (tag.contains(NBT_CROP_FILTER)) {
            // Gracefully discard invalid resource locations from corrupt/old saves
            this.cropFilter = ResourceLocation.tryParse(tag.getString(NBT_CROP_FILTER));
        } else {
            this.cropFilter = null;
        }

        if (tag.contains(NBT_CROP_FILTER_DISPLAY_ITEM)) {
            // Gracefully discard invalid resource locations from corrupt/old saves
            this.cropFilterDisplayItem = ResourceLocation.tryParse(tag.getString(NBT_CROP_FILTER_DISPLAY_ITEM));
        } else {
            this.cropFilterDisplayItem = null;
        }
    }

    /**
     * Advances the X-axis half-extent one step.
     */
    public void cycleHalfExtentX() {
        this.zone = this.zone.withNextHalfExtentX();
        markValidityDirty();
        setChangedAndSync();
    }

    /**
     * Advances the Z-axis half-extent one step.
     */
    public void cycleHalfExtentZ() {
        this.zone = this.zone.withNextHalfExtentZ();
        markValidityDirty();
        setChangedAndSync();
    }

    /**
     * Sets or clears the crop filter.
     * Passing {@code null} for both args clears the filter (any-crop mode).
     * <p>
     * The display item is stored alongside the crop block id so the client renderer can show
     * the filter item without access to the server-only crop registry.
     */
    public void setCropFilter(@Nullable ResourceLocation cropBlock, @Nullable ResourceLocation displayItem) {
        this.cropFilter = cropBlock;
        this.cropFilterDisplayItem = displayItem;
        // The filter is an input to the validity rule, set it dirty
        markValidityDirty();
        setChangedAndSync();
    }

    @Override
    public CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void setChangedAndSync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

}
