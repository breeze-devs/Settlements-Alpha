package dev.breezes.settlements.domain.farming;

import dev.breezes.settlements.domain.tags.SettlementsBlockTags;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

/**
 * Inventory-agnostic categorizer for totem zone cells.
 * <p>
 * Shared between the BE's coarse {@code needsCultivation} flag and the behavior's live per-cell
 * scan so the categorization rule lives in exactly one place. "Inventory-agnostic" means this
 * class never checks whether a villager has seeds — the behavior enforces that separately and bails
 * per-cell when seeds are absent.
 * <p>
 * Geometry contract (from brief §3): {@code cellPos} is the ground block; {@code cellPos.above()}
 * is the canopy block where foliage and crops live.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@CustomLog
public final class CultivationZoneCategorizer {

    /**
     * Categorizes every cell yielded by the provided stream and returns the results as a list.
     * <p>
     * The crop filter is matched purely by resource-location comparison — no registry access
     * needed. Pass {@code null} to mean "no filter" (any existing crop is acceptable).
     *
     * @param cellStream   the zone cell positions to evaluate (ground plane)
     * @param level        the server-side world
     * @param cropFilterId the totem's configured crop block id, or {@code null} if no filter is set
     * @return one {@link CultivationCellResult} per cell position, in stream order
     */
    public static List<CultivationCellResult> categorize(@Nonnull Stream<BlockPos> cellStream,
                                                         @Nonnull Level level,
                                                         @Nullable ResourceLocation cropFilterId) {
        return cellStream
                .map(cellPos -> new CultivationCellResult(cellPos, categorizeCell(level, cellPos, cropFilterId)))
                .toList();
    }

    /**
     * Returns whether the given stream contains any cell requiring cultivation work —
     * i.e. any {@link CultivationCellCategory#NEEDS_TILL}, {@link CultivationCellCategory#NEEDS_PLANT},
     * or {@link CultivationCellCategory#NEEDS_REPLANT}. Short-circuits on the first actionable cell.
     * <p>
     * Used by the BE to cheaply maintain the {@code needsCultivation} flag without materializing
     * the full result list.
     */
    public static boolean hasAnyCultivationWork(@Nonnull Stream<BlockPos> cellStream,
                                                @Nonnull Level level,
                                                @Nullable ResourceLocation cropFilterId) {
        return cellStream.anyMatch(cellPos -> isActionable(categorizeCell(level, cellPos, cropFilterId)));
    }

    /**
     * Categorizes a single cell at {@code cellPos} (ground) and {@code cellPos.above()} (canopy).
     */
    public static CultivationCellCategory categorizeCell(@Nonnull Level level,
                                                         @Nonnull BlockPos cellPos,
                                                         @Nullable ResourceLocation cropFilterId) {
        BlockState ground = level.getBlockState(cellPos);
        BlockState canopy = level.getBlockState(cellPos.above());

        if (ground.is(SettlementsBlockTags.TILLABLE)) {
            return categorizeTillableGround(canopy);
        }

        if (ground.is(Blocks.FARMLAND)) {
            return categorizeFarmlandGround(canopy, cropFilterId);
        }

        // Stone, path, water, etc. — not workable
        return CultivationCellCategory.SKIP;
    }

    /**
     * Decides the category when the ground is in the tillable tag (dirt-likes).
     * The only blocking condition is a non-foliage solid in the canopy.
     */
    private static CultivationCellCategory categorizeTillableGround(@Nonnull BlockState canopy) {
        if (canopy.isAir() || canopy.is(SettlementsBlockTags.TILLABLE_FOLIAGE)) {
            return CultivationCellCategory.NEEDS_TILL;
        }

        // A solid non-foliage block (fence, log, another crop on dirt, stone…) — cannot till here
        return CultivationCellCategory.BLOCKED;
    }

    /**
     * Decides the category when the ground is farmland.
     * An existing crop is compared to the crop filter to distinguish OCCUPIED from NEEDS_REPLANT.
     */
    private static CultivationCellCategory categorizeFarmlandGround(@Nonnull BlockState canopy,
                                                                    @Nullable ResourceLocation cropFilterId) {
        if (canopy.isAir() || canopy.is(SettlementsBlockTags.TILLABLE_FOLIAGE)) {
            return CultivationCellCategory.NEEDS_PLANT;
        }

        if (!(canopy.getBlock() instanceof CropBlock)) {
            // A non-crop, non-foliage solid on farmland — not workable
            return CultivationCellCategory.SKIP;
        }

        // There is a crop in the canopy — check against the filter
        if (cropFilterId == null) {
            // No filter set: any crop is acceptable
            return CultivationCellCategory.OCCUPIED;
        }

        ResourceLocation existingCropId = BuiltInRegistries.BLOCK.getKey(canopy.getBlock());
        if (cropFilterId.equals(existingCropId)) {
            return CultivationCellCategory.OCCUPIED;
        }

        // The crop does not match the active filter — the behavior will scythe and replant
        return CultivationCellCategory.NEEDS_REPLANT;
    }

    /**
     * Returns true for categories that represent actionable work (till, plant, or replant).
     * BLOCKED and SKIP are passive — no action is taken on them.
     */
    public static boolean isActionable(@Nonnull CultivationCellCategory category) {
        return switch (category) {
            case NEEDS_TILL, NEEDS_PLANT, NEEDS_REPLANT -> true;
            case BLOCKED, OCCUPIED, SKIP -> false;
        };
    }

}
