package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class DefaultIdleLifeAnimator implements IdleLifeAnimator {

    private static final int MIN_BLINK_DELAY_TICKS = 50;
    private static final int BLINK_DELAY_RANGE_TICKS = 90;
    private static final int MIN_FIDGET_DELAY_TICKS = 120;
    private static final int FIDGET_DELAY_RANGE_TICKS = 180;

    private final IdleLifeAnimationLibrary library;
    private final Random random;
    private long nextBlinkGameTime;
    private long blinkStartGameTime = Long.MIN_VALUE;
    private long nextFidgetGameTime;
    private long fidgetStartGameTime = Long.MIN_VALUE;
    private int lastFidgetIndex = -1;
    @Nullable
    private KeyframeAnimation activeFidget;

    public DefaultIdleLifeAnimator(@Nonnull IdleLifeAnimationLibrary library, int entityId) {
        this.library = library;
        this.random = new Random(entityId * 31L + 17L);
        this.nextBlinkGameTime = this.randomDelay(MIN_BLINK_DELAY_TICKS, BLINK_DELAY_RANGE_TICKS);
        this.nextFidgetGameTime = this.randomDelay(MIN_FIDGET_DELAY_TICKS, FIDGET_DELAY_RANGE_TICKS);
    }

    @Override
    public AnimationFrame sample(@Nonnull IdleLifeAnimationContext context) {
        this.advanceBlink(context.gameTime());
        this.advanceFidget(context);

        AnimationFrame frame = this.library.baseIdle().sample(context.gameTime() + context.partialTicks());
        frame = frame.composeOver(this.sampleBlink(context), 1.0F);
        frame = frame.composeOver(this.sampleFidget(context), 1.0F);
        return frame;
    }

    @Override
    public Optional<ArmConfiguration> activeArmConfiguration(@Nonnull IdleLifeAnimationContext context) {
        this.advanceFidget(context);
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
            this.blinkStartGameTime = Long.MIN_VALUE;
            this.nextBlinkGameTime = context.gameTime() + this.randomDelay(MIN_BLINK_DELAY_TICKS, BLINK_DELAY_RANGE_TICKS);
            return AnimationFrame.EMPTY;
        }
        return blink.sample(elapsedTicks);
    }

    private AnimationFrame sampleFidget(@Nonnull IdleLifeAnimationContext context) {
        if (this.activeFidget == null) {
            return AnimationFrame.EMPTY;
        }

        float elapsedTicks = this.elapsedSince(this.fidgetStartGameTime, context);
        if (elapsedTicks > this.activeFidget.getDurationTicks() + this.activeFidget.getBlendOutTicks()) {
            this.activeFidget = null;
            this.fidgetStartGameTime = Long.MIN_VALUE;
            this.nextFidgetGameTime = context.gameTime() + this.randomDelay(MIN_FIDGET_DELAY_TICKS, FIDGET_DELAY_RANGE_TICKS);
            return AnimationFrame.EMPTY;
        }

        float weight = this.fidgetWeight(elapsedTicks, this.activeFidget);
        return AnimationFrame.EMPTY.composeOver(this.activeFidget.sample(elapsedTicks), weight);
    }

    private void advanceBlink(long gameTime) {
        if (this.blinkStartGameTime == Long.MIN_VALUE && gameTime >= this.nextBlinkGameTime) {
            this.blinkStartGameTime = gameTime;
        }
    }

    private void advanceFidget(@Nonnull IdleLifeAnimationContext context) {
        if (context.actionActive()) {
            return;
        }
        if (this.activeFidget != null || context.gameTime() < this.nextFidgetGameTime) {
            return;
        }

        List<KeyframeAnimation> fidgets = this.library.fidgets();
        if (fidgets.isEmpty()) {
            this.nextFidgetGameTime = context.gameTime() + this.randomDelay(MIN_FIDGET_DELAY_TICKS, FIDGET_DELAY_RANGE_TICKS);
            return;
        }

        int fidgetIndex = this.nextFidgetIndex(fidgets.size());
        this.activeFidget = fidgets.get(fidgetIndex);
        this.lastFidgetIndex = fidgetIndex;
        this.fidgetStartGameTime = context.gameTime();
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
        if (elapsedTicks <= animation.getDurationTicks()) {
            return 1.0F;
        }

        int blendOutTicks = animation.getBlendOutTicks();
        if (blendOutTicks <= 0) {
            return 0.0F;
        }
        return 1.0F - Math.clamp((elapsedTicks - animation.getDurationTicks()) / blendOutTicks, 0.0F, 1.0F);
    }

    private float elapsedSince(long startGameTime, @Nonnull IdleLifeAnimationContext context) {
        return Math.max(0.0F, (context.gameTime() - startGameTime) + context.partialTicks());
    }

    private long randomDelay(int minimumTicks, int rangeTicks) {
        return minimumTicks + this.random.nextInt(rangeTicks + 1);
    }

}
