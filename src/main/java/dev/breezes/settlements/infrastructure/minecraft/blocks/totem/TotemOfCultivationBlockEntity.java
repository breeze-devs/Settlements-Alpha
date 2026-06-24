package dev.breezes.settlements.infrastructure.minecraft.blocks.totem;

import dev.breezes.settlements.bootstrap.registry.blockentities.BlockEntityTypeRegistry;
import dev.breezes.settlements.domain.farming.CultivationZoneCategorizer;
import dev.breezes.settlements.domain.tags.SettlementsBlockTags;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.rendering.particles.OrbParticles;
import dev.breezes.settlements.infrastructure.rendering.particles.ZoneBorderParticles;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Stores all per-instance configuration for a placed Totem of Cultivation.
 * <p>
 * The zone half-extents drive the (2·hx+1)×(2·hz+1) cell grid centred on the totem column.
 * Both default to 4, matching the maximum vanilla hydration radius (Chebyshev 4 from the
 * water source), so the default 9×9 is always fully hydrated without placing extra water.
 */
@Getter
public class TotemOfCultivationBlockEntity extends BlockEntity {

    private static final String NBT_HALF_EXTENT_X = "halfExtentX";
    private static final String NBT_HALF_EXTENT_Z = "halfExtentZ";
    private static final String NBT_CROP_FILTER = "cropFilter";
    private static final String NBT_CROP_FILTER_DISPLAY_ITEM = "cropFilterDisplayItem";
    private static final String NBT_VALID = "valid";

    private static final int DEFAULT_HALF_EXTENT = 4;
    private static final int MIN_HALF_EXTENT = 1;
    private static final int MAX_HALF_EXTENT = 4;

    /**
     * Height in blocks at which the floating totem mesh is rendered above the lily-pad block.
     * Shared with {@link TotemOfCultivationRenderer} and {@link OrbParticles} so the particle
     * emit target and the render position never drift apart from independent magic numbers.
     */
    public static final float FLOAT_HEIGHT_BLOCKS = 3.0F;

    /**
     * Server-tick throttle for the {@code valid} flag: slightly longer than the old 10s value
     * to reduce redundant full-zone scans while still reacting to structural changes quickly.
     */
    private static final long VALIDITY_RECHECK_INTERVAL_TICKS = ClockTicks.seconds(16).getTicks();

    /**
     * Server-tick throttle for the {@code needsCultivation} flag
     */
    private static final long NEEDS_CULTIVATION_RECHECK_INTERVAL_TICKS = ClockTicks.seconds(60).getTicks();

    /**
     * Client-tick cadence for the ambient orb aura — one burst every N ticks.
     * Sparse emission keeps the GPU batch small and avoids particle stacking when many
     * totems are in view simultaneously.
     */
    private static final int ORB_EMIT_CADENCE_TICKS = 15;

    /**
     * Locality gate: do not emit particles when no player is within this distance.
     */
    private static final double ORB_EMIT_PLAYER_RADIUS = 24.0;

    /**
     * How long the border outline persists after a player interaction triggers it
     * Multiple rapid re-emissions during this window give the visual a sustained appearance
     */
    private static final long BORDER_PREVIEW_DURATION_TICKS = ClockTicks.seconds(4).getTicks();

    /**
     * How often the border is re-emitted during the preview window
     */
    private static final long BORDER_EMIT_INTERVAL_TICKS = ClockTicks.seconds(0.5).getTicks();

    private int halfExtentX;
    private int halfExtentZ;

    @Nullable
    private ResourceLocation cropFilter;
    @Nullable
    private ResourceLocation cropFilterDisplayItem;

    private boolean valid;
    private boolean validityDirty;

    private final long validityRecheckPhaseOffset;
    private final long needsCultivationRecheckPhaseOffset;

    /**
     * Transient server-only flag: true when the zone currently contains at least one cell
     * that needs tilling, planting, or replanting. Recomputed on a periodic server-tick throttle
     * ({@link #NEEDS_CULTIVATION_RECHECK_INTERVAL_TICKS}); a finished zone reads stale-true until the
     * next boundary, at which point the behavior simply finds no actionable cells and bails.
     * Not persisted or synced to the client — the renderer has no use for it.
     */
    private boolean needsCultivation;

    /**
     * Transient server-side expiry tick for the border-outline preview
     */
    private long borderPreviewExpiryTick;

    public TotemOfCultivationBlockEntity(@Nonnull BlockPos pos,
                                         @Nonnull BlockState blockState) {
        super(BlockEntityTypeRegistry.TOTEM_OF_CULTIVATION.get(), pos, blockState);
        this.halfExtentX = DEFAULT_HALF_EXTENT;
        this.halfExtentZ = DEFAULT_HALF_EXTENT;
        this.cropFilter = null;
        this.cropFilterDisplayItem = null;
        this.valid = false;
        this.validityDirty = false;
        this.validityRecheckPhaseOffset = phaseOffset(pos, VALIDITY_RECHECK_INTERVAL_TICKS);
        this.needsCultivationRecheckPhaseOffset = phaseOffset(pos, NEEDS_CULTIVATION_RECHECK_INTERVAL_TICKS);
        this.needsCultivation = false;
        this.borderPreviewExpiryTick = 0L;
    }

    /**
     * Called by the block ticker every game tick, server-side only.
     * Delegates to separate concerns so neither blocks the other.
     */
    public static void serverTick(@Nonnull Level level,
                                  @Nonnull BlockPos pos,
                                  @Nonnull BlockState state,
                                  @Nonnull TotemOfCultivationBlockEntity entity) {
        entity.maybeRecomputeValidity(level);
        entity.maybeRecomputeNeedsCultivation(level);
        entity.maybeEmitBorderPreview(level);
    }

    /**
     * Called by the block ticker every game tick, client-side only.
     * Drives the ambient magic-orb aura while the totem is valid.
     * <p>
     * Emission is rate-gated on a coarse cadence and a player-proximity check so distant
     * or off-screen totems contribute nothing to the particle pool.
     */
    public static void clientTick(@Nonnull Level level,
                                  @Nonnull BlockPos pos,
                                  @Nonnull BlockState state,
                                  @Nonnull TotemOfCultivationBlockEntity entity) {
        if (!level.isClientSide()) {
            return;
        }
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
            OrbParticles.spawnScatter(level, pos, FLOAT_HEIGHT_BLOCKS, entity.getHalfExtentX(), entity.getHalfExtentZ(), random);
        }
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
     * Only runs when the totem is valid — an invalid totem has no actionable zone.
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
                streamZoneCells(this.getBlockPos()), level, this.cropFilter);
    }

    /**
     * Spreads periodic zone scans across the interval so loaded totems do not all rescan on the same global tick.
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
     * Emits a border-outline burst at a fixed cadence while the preview window is active.
     * The window is opened by player interactions so the outline persists long enough to be useful.
     */
    private void maybeEmitBorderPreview(@Nonnull Level level) {
        if (level.getGameTime() > this.borderPreviewExpiryTick) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (level.getGameTime() % BORDER_EMIT_INTERVAL_TICKS != 0L) {
            return;
        }

        ZoneBorderParticles.spawnCultivationZone(serverLevel, this.worldPosition, this.halfExtentX, this.halfExtentZ, this.valid);
    }

    /**
     * Starts a multi-second border-preview window so repeated particle bursts keep the
     * zone outline visible after a player interaction.
     */
    public void beginBorderPreview() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }

        this.borderPreviewExpiryTick = this.level.getGameTime() + BORDER_PREVIEW_DURATION_TICKS;
    }

    /**
     * Recomputes and caches the valid flag.
     * <p>
     * Validity requires two conditions to hold simultaneously:
     * 1. A still water source directly below the totem (the hydration contract).
     * 2. At least one workable cell in the zone — a cell that is farmland or can be tilled
     * to farmland (i.e. it is in the dirt tag). This ensures the sensor skips totems placed
     * over water that is entirely surrounded by non-tillable surfaces (stone, paths, etc.).
     * <p>
     * Structured as a separate named method so Phase 3/4 can reuse {@link #streamZoneCells(BlockPos)}
     * for their own tile enumeration without duplicating this check.
     */
    public void recomputeValidity(@Nonnull Level level) {
        boolean wasValid = this.valid;
        this.valid = computeIsValid(level, this.getBlockPos());
        this.validityDirty = false;
        if (this.valid != wasValid) {
            setChangedAndSync();
        }
    }

    /**
     * Schedules validity recomputation on the block entity ticker.
     * This coalesces bursts of neighbor updates into one scan while still reacting before
     * the slower periodic safety check would run.
     */
    public void markValidityDirty() {
        this.validityDirty = true;
    }

    /**
     * Computes validity without mutating state — allows callers to probe validity without a side effect.
     * Pure function over the world state at the given position.
     */
    private boolean computeIsValid(@Nonnull Level level, @Nonnull BlockPos totemPos) {
        // The water source must be directly below the lily-pad block
        var fluidBelow = level.getFluidState(totemPos.below());
        if (!fluidBelow.is(Fluids.WATER) || !fluidBelow.isSource()) {
            return false;
        }

        // At least one zone cell must be tillable
        return streamZoneCells(totemPos).anyMatch(cellPos -> isWorkableCell(level, cellPos));
    }

    /**
     * Enumerates all candidate zone cell positions at water Y, excluding the center water source.
     * <p>
     * This is the authoritative zone enumeration seam — Phase 3/4 tilling and planting behaviors
     * will call it to discover which cells need attention, rather than re-deriving the geometry.
     * Positions are constructed lazily so short-circuiting terminal operations, such as
     * {@code anyMatch}, do not materialize the remainder of the grid.
     */
    public Stream<BlockPos> streamZoneCells(@Nonnull BlockPos totemPos) {
        // Zone cells are at the water's Y level, one below the totem.
        int zoneY = totemPos.below().getY();
        int centerX = totemPos.getX();
        int centerZ = totemPos.getZ();
        int hx = this.halfExtentX;
        int hz = this.halfExtentZ;

        return IntStream.rangeClosed(-hx, hx)
                .boxed()
                .flatMap(dx -> IntStream.rangeClosed(-hz, hz)
                        // The center cell (dx=0, dz=0) is the water source
                        .filter(dz -> dx != 0 || dz != 0)
                        .mapToObj(dz -> new BlockPos(centerX + dx, zoneY, centerZ + dz)));
    }

    /**
     * Returns true if the cell is farmland or can be tilled to farmland.
     * Delegates to the mod tag so the list of tillable blocks is authoritative in the datapack.
     */
    private static boolean isWorkableCell(@Nonnull Level level, @Nonnull BlockPos cellPos) {
        BlockState cellState = level.getBlockState(cellPos);
        return cellState.is(Blocks.FARMLAND) || cellState.is(SettlementsBlockTags.TILLABLE);
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag,
                                  @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(NBT_HALF_EXTENT_X, this.halfExtentX);
        tag.putInt(NBT_HALF_EXTENT_Z, this.halfExtentZ);
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
        this.halfExtentX = clampHalfExtent(tag.getInt(NBT_HALF_EXTENT_X));
        this.halfExtentZ = clampHalfExtent(tag.getInt(NBT_HALF_EXTENT_Z));
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
     * Adjusts the X-axis half-extent by {@code delta}, wrapping within [MIN, MAX].
     * Phase 2 resize interaction drives this via sneak+right-click.
     */
    public void cycleHalfExtentX(int delta) {
        this.halfExtentX = wrapHalfExtent(this.halfExtentX + delta);
        markValidityDirty();
        setChangedAndSync();
    }

    /**
     * Adjusts the Z-axis half-extent by {@code delta}, wrapping within [MIN, MAX].
     */
    public void cycleHalfExtentZ(int delta) {
        this.halfExtentZ = wrapHalfExtent(this.halfExtentZ + delta);
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
        setChangedAndSync();
    }

    public boolean hasCropFilter(@Nonnull ResourceLocation cropBlockId) {
        return cropBlockId.equals(this.cropFilter);
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

    /**
     * Wraps a half-extent value around the valid [MIN, MAX] range in a cycle.
     * Values below MIN wrap to MAX, values above MAX wrap to MIN.
     */
    private static int wrapHalfExtent(int value) {
        if (value < MIN_HALF_EXTENT) {
            return MAX_HALF_EXTENT;
        }
        if (value > MAX_HALF_EXTENT) {
            return MIN_HALF_EXTENT;
        }
        return value;
    }

    /**
     * Clamps a half-extent read from NBT into the valid [MIN, MAX] range.
     * Uses clamp rather than wrap so a corrupt save converges to a valid state.
     */
    private static int clampHalfExtent(int value) {
        return Math.clamp(value, MIN_HALF_EXTENT, MAX_HALF_EXTENT);
    }

}
