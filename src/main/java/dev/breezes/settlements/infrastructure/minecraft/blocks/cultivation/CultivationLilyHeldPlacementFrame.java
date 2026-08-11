package dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation;

import dev.breezes.settlements.bootstrap.registry.items.ItemRegistry;
import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import jakarta.inject.Inject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Optional;

/**
 * The single resolution of "where would the held Cultivation Lily land"
 * <p>
 * Memoized against the exact inputs a resolution depends on: whichever caller asks first within a
 * frame pays for {@link CultivationLilyPlacementAim#resolve}'s raycast, and every later caller in that
 * same frame reads the cached answer instead of repeating it. The next frame's different partial tick
 * invalidates the cache on its own.
 */
@ClientSide
@ClientScope
public final class CultivationLilyHeldPlacementFrame {

    @Nullable
    private WeakReference<Level> cachedLevelRef;
    @Nullable
    private WeakReference<LocalPlayer> cachedPlayerRef;
    private float cachedPartialTick;
    private boolean hasCached;
    @Nullable
    private Optional<CultivationLilyPlacementCandidate> cachedCandidate;

    @Inject
    CultivationLilyHeldPlacementFrame() {
    }

    public Optional<CultivationLilyPlacementCandidate> resolve(@Nonnull Level level, @Nonnull LocalPlayer player, float partialTick) {
        if (isCacheValid(level, player, partialTick)) {
            return this.cachedCandidate;
        }

        Optional<CultivationLilyPlacementCandidate> candidate = computeCandidate(level, player, partialTick);
        this.cachedLevelRef = new WeakReference<>(level);
        this.cachedPlayerRef = new WeakReference<>(player);
        this.cachedPartialTick = partialTick;
        this.hasCached = true;
        this.cachedCandidate = candidate;
        return candidate;
    }

    private boolean isCacheValid(@Nonnull Level level, @Nonnull LocalPlayer player, float partialTick) {
        return this.hasCached
                && this.cachedPartialTick == partialTick
                && this.cachedLevelRef != null && this.cachedLevelRef.get() == level
                && this.cachedPlayerRef != null && this.cachedPlayerRef.get() == player;
    }

    private static Optional<CultivationLilyPlacementCandidate> computeCandidate(@Nonnull Level level, @Nonnull LocalPlayer player, float partialTick) {
        return resolveHeldHand(player).flatMap(hand -> resolveForHand(level, player, hand, partialTick));
    }

    private static Optional<InteractionHand> resolveHeldHand(@Nonnull LocalPlayer player) {
        if (isLily(player.getItemInHand(InteractionHand.MAIN_HAND))) {
            return Optional.of(InteractionHand.MAIN_HAND);
        }
        if (isLily(player.getItemInHand(InteractionHand.OFF_HAND))) {
            return Optional.of(InteractionHand.OFF_HAND);
        }
        return Optional.empty();
    }

    private static Optional<CultivationLilyPlacementCandidate> resolveForHand(@Nonnull Level level, @Nonnull LocalPlayer player,
                                                                              @Nonnull InteractionHand hand, float partialTick) {
        ItemStack stack = player.getItemInHand(hand);
        return CultivationLilyPlacementAim.resolve(level, player, hand, stack, partialTick)
                .map(pos -> new CultivationLilyPlacementCandidate(hand, stack, pos));
    }

    private static boolean isLily(@Nonnull ItemStack stack) {
        return stack.is(ItemRegistry.CULTIVATION_LILY.get());
    }

}
