package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import dev.breezes.settlements.domain.time.ClockTicks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class DefaultIdleLifeAnimator implements IdleLifeAnimator {

    private static final int MIN_BLINK_DELAY_TICKS = 50;
    private static final int BLINK_DELAY_RANGE_TICKS = 90;
    private static final int MIN_FIDGET_DELAY_TICKS = ClockTicks.seconds(30).getTicksAsInt();
    private static final int FIDGET_DELAY_RANGE_TICKS = ClockTicks.seconds(45).getTicksAsInt();

    private final IdleLifeAnimationLibrary library;
    private final Random random;

    /**
     * Fixed phase shift into the ambient breathe loop, so villagers do not all inhale on the same beat.
     */
    private final float breathePhaseOffsetTicks;

    private long nextBlinkGameTime;
    private long blinkStartGameTime = Long.MIN_VALUE;
    private long nextFidgetGameTime;
    private long fidgetStartGameTime = Long.MIN_VALUE;
    private long fidgetClearStartGameTime = Long.MIN_VALUE;
    private int lastFidgetIndex = -1;
    @Nullable
    private KeyframeAnimation activeFidget;

    public DefaultIdleLifeAnimator(@Nonnull IdleLifeAnimationLibrary library, int entityId) {
        this.library = library;
        this.random = new Random(entityId * 31L + 17L);
        this.breathePhaseOffsetTicks = this.random.nextInt(Math.max(1, library.baseIdle().getDurationTicks()));
        this.nextBlinkGameTime = this.randomDelay(MIN_BLINK_DELAY_TICKS, BLINK_DELAY_RANGE_TICKS);
        this.nextFidgetGameTime = this.randomDelay(MIN_FIDGET_DELAY_TICKS, FIDGET_DELAY_RANGE_TICKS);
    }

    @Override
    public void advance(@Nonnull IdleLifeAnimationContext context) {
        this.advanceBlink(context);
        this.advanceFidget(context);
    }

    @Override
    public AnimationFrame sample(@Nonnull IdleLifeAnimationContext context) {
        float breatheTicks = context.gameTime() + context.partialTicks() + this.breathePhaseOffsetTicks;
        AnimationFrame frame = this.library.baseIdle().sample(breatheTicks);
        frame = frame.composeOver(this.sampleBlink(context), 1.0F);
        frame = frame.composeOver(this.sampleFidget(context), 1.0F);
        return frame;
    }

    @Override
    public Optional<ArmConfiguration> activeArmConfiguration(@Nonnull IdleLifeAnimationContext context) {
        if (this.activeFidget == null) {
            return Optional.empty();
        }
        return this.activeFidget.armConfigurationAt(this.elapsedSince(this.fidgetStartGameTime, context));
    }

    private AnimationFrame sampleBlink(@Nonnull IdleLifeAnimationContext context) {
        if (this.blinkStartGameTime == Long.MIN_VALUE) {
            return AnimationFrame.EMPTY;
        }

        float elapsedTicks = this.elapsedSince(this.blinkStartGameTime, context);
        KeyframeAnimation blink = this.library.blink();
        if (elapsedTicks > blink.getDurationTicks()) {
            return AnimationFrame.EMPTY;
        }
        return blink.sample(elapsedTicks);
    }

    private AnimationFrame sampleFidget(@Nonnull IdleLifeAnimationContext context) {
        if (this.activeFidget == null) {
            return AnimationFrame.EMPTY;
        }

        float elapsedTicks = this.elapsedSince(this.fidgetStartGameTime, context);
        float clearWeight = this.fidgetClearWeight(context, this.activeFidget);
        if (elapsedTicks > this.activeFidget.getDurationTicks() + this.activeFidget.getBlendOutTicks()
                || clearWeight <= 0.0F) {
            return AnimationFrame.EMPTY;
        }

        float weight = this.fidgetWeight(elapsedTicks, this.activeFidget) * clearWeight;
        return AnimationFrame.EMPTY.composeOver(this.activeFidget.sample(elapsedTicks), weight);
    }

    private void advanceBlink(@Nonnull IdleLifeAnimationContext context) {
        if (this.blinkStartGameTime != Long.MIN_VALUE) {
            if (this.elapsedSince(this.blinkStartGameTime, context) > this.library.blink().getDurationTicks()) {
                this.blinkStartGameTime = Long.MIN_VALUE;
                this.nextBlinkGameTime = context.gameTime()
                        + this.randomDelay(MIN_BLINK_DELAY_TICKS, BLINK_DELAY_RANGE_TICKS);
            }
            return;
        }

        if (context.gameTime() >= this.nextBlinkGameTime) {
            this.blinkStartGameTime = context.gameTime();
        }
    }

    private void advanceFidget(@Nonnull IdleLifeAnimationContext context) {
        if (context.actionActive()) {
            // Idle-life composites under the action, which rarely authors the body and eye targets a
            // fidget drives, so one left running would keep tugging those bones against the work. Fade it
            // out the moment an action takes over, and hold the next-fidget timer out past the action so
            // finished work is never immediately followed by a fidget.
            if (this.activeFidget != null && this.fidgetClearStartGameTime == Long.MIN_VALUE) {
                this.fidgetClearStartGameTime = context.gameTime();
            }
            this.nextFidgetGameTime = Math.max(this.nextFidgetGameTime, context.gameTime() + MIN_FIDGET_DELAY_TICKS);
        } else if (this.activeFidget == null && context.gameTime() >= this.nextFidgetGameTime) {
            this.startFidgetOrReschedule(context.gameTime());
        }

        if (this.activeFidget != null && this.isFidgetFinished(context, this.activeFidget)) {
            this.finishActiveFidget(context.gameTime());
        }
    }

    private void startFidgetOrReschedule(long gameTime) {
        List<KeyframeAnimation> fidgets = this.library.fidgets();
        if (fidgets.isEmpty()) {
            this.nextFidgetGameTime = gameTime + this.randomDelay(MIN_FIDGET_DELAY_TICKS, FIDGET_DELAY_RANGE_TICKS);
            return;
        }

        int fidgetIndex = this.nextFidgetIndex(fidgets.size());
        this.activeFidget = fidgets.get(fidgetIndex);
        this.lastFidgetIndex = fidgetIndex;
        this.fidgetStartGameTime = gameTime;
        this.fidgetClearStartGameTime = Long.MIN_VALUE;
    }

    private boolean isFidgetFinished(@Nonnull IdleLifeAnimationContext context,
                                     @Nonnull KeyframeAnimation animation) {
        float elapsedTicks = this.elapsedSince(this.fidgetStartGameTime, context);
        return elapsedTicks > animation.getDurationTicks() + animation.getBlendOutTicks()
                || this.fidgetClearWeight(context, animation) <= 0.0F;
    }

    private void finishActiveFidget(long gameTime) {
        this.activeFidget = null;
        this.fidgetStartGameTime = Long.MIN_VALUE;
        this.fidgetClearStartGameTime = Long.MIN_VALUE;
        this.nextFidgetGameTime = gameTime + this.randomDelay(MIN_FIDGET_DELAY_TICKS, FIDGET_DELAY_RANGE_TICKS);
    }

    private int nextFidgetIndex(int size) {
        if (size == 1) {
            return 0;
        }

        int index = this.random.nextInt(size - 1);
        if (index >= this.lastFidgetIndex) {
            index++;
        }
        return index;
    }

    private float fidgetWeight(float elapsedTicks, @Nonnull KeyframeAnimation animation) {
        int blendInTicks = animation.getBlendInTicks();
        float blendInWeight = blendInTicks <= 0
                ? 1.0F
                : Math.clamp(elapsedTicks / blendInTicks, 0.0F, 1.0F);
        if (elapsedTicks <= animation.getDurationTicks()) {
            return blendInWeight;
        }

        int blendOutTicks = animation.getBlendOutTicks();
        if (blendOutTicks <= 0) {
            return 0.0F;
        }

        float blendOutWeight = 1.0F - Math.clamp((elapsedTicks - animation.getDurationTicks()) / blendOutTicks, 0.0F, 1.0F);
        return blendInWeight * blendOutWeight;
    }

    private float fidgetClearWeight(@Nonnull IdleLifeAnimationContext context,
                                    @Nonnull KeyframeAnimation animation) {
        if (this.fidgetClearStartGameTime == Long.MIN_VALUE) {
            return 1.0F;
        }

        int blendOutTicks = animation.getBlendOutTicks();
        if (blendOutTicks <= 0) {
            return 0.0F;
        }

        float elapsedTicks = this.elapsedSince(this.fidgetClearStartGameTime, context);
        return 1.0F - Math.clamp(elapsedTicks / blendOutTicks, 0.0F, 1.0F);
    }

    private float elapsedSince(long startGameTime, @Nonnull IdleLifeAnimationContext context) {
        return Math.max(0.0F, (context.gameTime() - startGameTime) + context.partialTicks());
    }

    private long randomDelay(int minimumTicks, int rangeTicks) {
        return minimumTicks + this.random.nextInt(rangeTicks + 1);
    }

}
