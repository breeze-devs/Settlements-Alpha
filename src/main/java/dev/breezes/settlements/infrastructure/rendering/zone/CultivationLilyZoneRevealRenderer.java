package dev.breezes.settlements.infrastructure.rendering.zone;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.di.ClientSessionResettable;
import dev.breezes.settlements.domain.farming.CultivationZone;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation.CultivationLilyBlockEntity;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.DwellReveal;
import dev.breezes.settlements.shared.util.DwellRevealTracker;
import jakarta.inject.Inject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Reveals a placed Cultivation Lily's zone footprint while the player looks at the pad.
 * Decides *when* a footprint is worth drawing and *which* one; {@link ZoneWallRenderer} draws it.
 */
@ClientSide
@ClientScope
public final class CultivationLilyZoneRevealRenderer implements ClientSessionResettable {

    /**
     * How long the footprint holds at full opacity after the player looks away.
     * Generous, because looking around is how a player see the bounding box away from the lily.
     */
    private static final ClockTicks REVEAL_LINGER = ClockTicks.seconds(2);

    private final DwellRevealTracker dwellTracker = new DwellRevealTracker(REVEAL_LINGER);

    // Retained through the fade-out so a lily that just left the crosshair still has a footprint and
    // color to draw at decaying alpha. Left stale once the fade completes; opacity is what decides
    // whether either is read again.
    @Nullable
    private CultivationZone lastZone;
    @Nullable
    private Vector3f lastColor;

    @Inject
    CultivationLilyZoneRevealRenderer() {
    }

    @Override
    public void onClientSessionEnded() {
        this.dwellTracker.reset();
        this.lastZone = null;
        this.lastColor = null;
    }

    public void render(@Nonnull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        Optional<CultivationLilyBlockEntity> lookedAtLily = resolveLookedAtLily(minecraft);
        LilyTargetKey targetKey = lookedAtLily.map(lily -> new LilyTargetKey(minecraft.level, lily.getBlockPos())).orElse(null);
        DwellReveal reveal = this.dwellTracker.observe(targetKey);

        // Only a lily that has earned the reveal may replace what is retained. The linger holds opacity at
        // full for seconds after the last earned frame, so adopting a footprint on sight alone would paint
        // a plot the crosshair merely swept past, at full strength, for as long as the linger runs.
        if (reveal.revealed() && lookedAtLily.isPresent()) {
            CultivationLilyBlockEntity lily = lookedAtLily.get();
            this.lastZone = lily.getZone();
            this.lastColor = CultivationZoneStyle.outlineColor(lily.isValid());
        }

        if (!reveal.paints() || this.lastZone == null || this.lastColor == null) {
            return;
        }
        ZoneWallRenderer.render(event, CultivationZoneGeometry.canopyPlane(this.lastZone), this.lastColor,
                reveal.alpha(), minecraft.level.getGameTime());
    }

    /**
     * The crosshair's block ray trace, narrowed to a Cultivation Lily. A miss is reported as a
     * BlockHitResult too, so the hit type is checked rather than the class.
     */
    private static Optional<CultivationLilyBlockEntity> resolveLookedAtLily(@Nonnull Minecraft minecraft) {
        HitResult hit = minecraft.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
        return blockEntity instanceof CultivationLilyBlockEntity lily ? Optional.of(lily) : Optional.empty();
    }

    /**
     * Stable identity for the lily under the crosshair: level plus block position, so that dimension
     * travel cannot present an unrelated lily at the same position as the one already being read.
     */
    private record LilyTargetKey(@Nonnull Level level, @Nonnull BlockPos pos) {
    }

}
