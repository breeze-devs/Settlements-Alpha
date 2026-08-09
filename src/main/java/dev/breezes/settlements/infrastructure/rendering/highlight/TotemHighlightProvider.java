package dev.breezes.settlements.infrastructure.rendering.highlight;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.di.ClientSessionResettable;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.items.TotemMode;
import dev.breezes.settlements.infrastructure.minecraft.items.TotemModeStyle;
import dev.breezes.settlements.infrastructure.minecraft.items.VillagerTotemItem;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.ClientMonotonicClock;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Highlights villagers the currently held totem could convert. Ineligible villagers get no contribution at
 * all — absence is the signal that they are already the target type.
 */
@ClientSide
@ClientScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class TotemHighlightProvider implements EntityHighlightProvider, ClientSessionResettable {

    private static final double SCAN_RADIUS_BLOCKS = 16.0;
    private static final double SCAN_RADIUS_BLOCKS_SQUARED = SCAN_RADIUS_BLOCKS * SCAN_RADIUS_BLOCKS;

    // Paces the world query only. Eligibility is re-evaluated every contribute() against whatever mode is
    // currently held, so cycling relights the outlines on the next frame instead of waiting out the interval.
    private static final ClockTicks SCAN_INTERVAL = ClockTicks.seconds(1);

    private List<Villager> nearbyVillagers = List.of();
    // Weak so that this cache never keep a departed level loaded
    @Nullable
    private WeakReference<Level> scannedLevel;
    private long nextScanAtMillis = Long.MIN_VALUE;

    @Override
    public void contribute(@Nonnull EntityHighlightSink sink, @Nonnull EntityHighlightFrame frame) {
        Player player = frame.localPlayer();
        // Main hand only
        ItemStack totemStack = player.getMainHandItem();
        if (!(totemStack.getItem() instanceof VillagerTotemItem)) {
            // Dropping the cache here matters beyond tidiness: every cached villager holds its level, so
            // keeping the list while nothing is highlighted pins a ClientLevel the player may have left.
            forgetScan();
            return;
        }

        rescanIfDue(player);

        TotemMode mode = VillagerTotemItem.getMode(totemStack);
        Entity aimedAt = frame.aimedAtEntity();
        int restingColor = TotemModeStyle.restingOutlineColor(mode);
        int hoverColor = TotemModeStyle.hoverOutlineColor();

        for (Villager villager : nearbyVillagers) {
            if (!villager.isAlive() || !VillagerTotemItem.isEligibleForConversion(villager, mode)) {
                continue;
            }

            sink.highlight(villager, villager == aimedAt ? hoverColor : restingColor);
        }
    }

    @Override
    public void onClientSessionEnded() {
        forgetScan();
        nextScanAtMillis = Long.MIN_VALUE;
    }

    private void rescanIfDue(@Nonnull Player player) {
        Level level = player.level();
        // Invalidated immediately on a level swap rather than waiting out the interval: travelling between
        // dimensions replaces the level without ending the client session, and every cached villager then
        // belongs to a level that is no longer rendered.
        boolean levelChanged = scannedLevel == null || scannedLevel.get() != level;

        long now = ClientMonotonicClock.nowMillis();
        if (!levelChanged && now < nextScanAtMillis) {
            return;
        }
        nextScanAtMillis = ClientMonotonicClock.deadlineFrom(SCAN_INTERVAL);
        scannedLevel = new WeakReference<>(level);

        AABB scanBounds = player.getBoundingBox().inflate(SCAN_RADIUS_BLOCKS);
        nearbyVillagers = level
                .getEntities(player, scanBounds, entity -> entity instanceof Villager && entity.distanceToSqr(player) <= SCAN_RADIUS_BLOCKS_SQUARED)
                .stream()
                .map(Villager.class::cast)
                .toList();
    }

    private void forgetScan() {
        nearbyVillagers = List.of();
        scannedLevel = null;
    }

}
