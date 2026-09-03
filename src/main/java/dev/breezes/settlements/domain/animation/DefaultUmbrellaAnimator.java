package dev.breezes.settlements.domain.animation;

import javax.annotation.Nonnull;

/**
 * Sequences the umbrella's raise against its deploy, and its lower against its undeploy. The carry
 * motion and the canopy motion overlap: the canopy clip starts a fixed offset into the carry clip while
 * the arm is still traveling, in both directions.
 * <p>
 * A gate change commits only once it has stood for this instance's gate delay. A gate that returns to
 * the committed direction inside that window abandons the change outright rather than resuming it, so a
 * blip shorter than the delay never moves the umbrella at all.
 * <p>
 * A committed change restarts the opposite clip from its first frame rather than cross-fading.
 * This implementation assumes a gate that changes far more slowly than a carry clip runs, so a reversal
 * usually happens on a settled pose; a gate driven faster than that may cause animation snaps.
 */
public final class DefaultUmbrellaAnimator implements UmbrellaAnimator {

    private static final KeyframeAnimation DEPLOY = UmbrellaAnimations.deploy();
    private static final KeyframeAnimation UNDEPLOY = UmbrellaAnimations.undeploy();
    private static final KeyframeAnimation RAISE = UmbrellaCarryAnimations.raise();
    private static final KeyframeAnimation LOWER = UmbrellaCarryAnimations.lower();

    private enum Direction {
        DEPLOYING,
        RETRACTING
    }

    private final int gateDelayTicks;

    private Direction direction = Direction.RETRACTING;

    /**
     * Anchors elapsed-tick arithmetic for the current direction. Initialized as though a retraction had
     * already finished at world tick 0, so a freshly created animator starts fully stowed without a
     * separate "never deployed" flag to special-case in {@link #isVisible}.
     */
    private long directionStartGameTime = -UmbrellaCarryAnimations.LOWER_DURATION_TICKS;

    private boolean gateChangePending;
    private long gateChangeCommitGameTime;

    /**
     * @param gateDelayTicks how long a gate change must stand before it commits, which staggers
     *                       villagers whose gates flip on the same tick; 0 commits immediately
     */
    DefaultUmbrellaAnimator(int gateDelayTicks) {
        this.gateDelayTicks = gateDelayTicks;
    }

    @Override
    public void advance(@Nonnull UmbrellaAnimationContext context) {
        boolean currentlyDeploying = this.direction == Direction.DEPLOYING;
        if (context.shouldDeploy() == currentlyDeploying) {
            this.gateChangePending = false;
            return;
        }

        if (!this.gateChangePending) {
            this.gateChangePending = true;
            this.gateChangeCommitGameTime = context.gameTime() + this.gateDelayTicks;
        }

        if (context.gameTime() < this.gateChangeCommitGameTime) {
            return;
        }

        this.gateChangePending = false;
        this.direction = context.shouldDeploy() ? Direction.DEPLOYING : Direction.RETRACTING;
        this.directionStartGameTime = context.gameTime();
    }

    @Override
    public AnimationFrame sample(@Nonnull UmbrellaAnimationContext context) {
        float elapsedTicks = this.elapsedTicksSinceDirectionStart(context.gameTime(), context.partialTicks());
        boolean deploying = this.direction == Direction.DEPLOYING;

        KeyframeAnimation carryClip = deploying ? RAISE : LOWER;
        int canopyStartTick = deploying
                ? UmbrellaCarryAnimations.RAISE_CANOPY_START_TICK
                : UmbrellaCarryAnimations.LOWER_CANOPY_START_TICK;
        KeyframeAnimation canopyClip = deploying ? DEPLOY : UNDEPLOY;

        AnimationFrame frame = carryClip.sample(elapsedTicks);
        // Before its start tick the canopy clip samples at 0 -- the closed/furled pose -- so the
        // attachment appears already furled rather than snapping open partway through the carry motion.
        float canopyElapsedTicks = Math.max(0.0F, elapsedTicks - canopyStartTick);
        return frame.composeOver(canopyClip.sample(canopyElapsedTicks), 1.0F);
    }

    @Override
    public boolean isVisible(@Nonnull UmbrellaAnimationContext context) {
        if (this.direction == Direction.DEPLOYING) {
            return true;
        }

        // Present for the whole lower-and-undeploy: gated on both clips finishing, not just the carry
        // clip, so a future retune that lengthens the canopy offset past the carry duration cannot pop
        // the attachment out while the canopy is still visibly closing.
        float elapsedTicks = this.elapsedTicksSinceDirectionStart(context.gameTime(), context.partialTicks());
        boolean carryFinished = elapsedTicks >= UmbrellaCarryAnimations.LOWER_DURATION_TICKS;
        float canopyElapsedTicks = elapsedTicks - UmbrellaCarryAnimations.LOWER_CANOPY_START_TICK;
        boolean canopyFinished = canopyElapsedTicks >= UmbrellaAnimations.UNDEPLOY_DURATION_TICKS;
        return !(carryFinished && canopyFinished);
    }

    private float elapsedTicksSinceDirectionStart(long gameTime, float partialTicks) {
        return Math.max(0.0F, (gameTime - this.directionStartGameTime) + partialTicks);
    }

}
