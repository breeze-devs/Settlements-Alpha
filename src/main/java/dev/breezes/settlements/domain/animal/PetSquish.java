package dev.breezes.settlements.domain.animal;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Pure squash-and-stretch curve for an animal's "petting" reaction.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PetSquish {

    /**
     * Total length of the reaction, in client ticks (~0.4s at 20 TPS).
     */
    public static final float DURATION_TICKS = 8.0F;

    /**
     * Peak vertical squash: the fraction the animal flattens at the hardest point of the bounce.
     */
    private static final float AMPLITUDE = 0.28F;

    /**
     * Number of half-bounces before the spring settles; a value above 1.0 gives the rebound overshoot.
     */
    private static final float HALF_BOUNCES = 1.5F;

    /**
     * How much of the vertical squash is compensated as girth, so the animal keeps roughly its volume.
     */
    private static final float GIRTH_RATIO = 0.5F;

    public static final Factors IDENTITY = new Factors(1.0F, 1.0F);

    /**
     * @param elapsedTicks time since the pet started, in client ticks (may carry a partial-tick fraction)
     * @return per-axis scale factors to multiply onto the model; {@link #IDENTITY} outside the animation window
     */
    public static Factors factorsAt(float elapsedTicks) {
        if (elapsedTicks < 0.0F || elapsedTicks >= DURATION_TICKS) {
            return IDENTITY;
        }

        float progress = elapsedTicks / DURATION_TICKS;
        // Linear envelope damps the oscillation to rest by the end of the window.
        float envelope = 1.0F - progress;
        float wave = (float) Math.sin(progress * Math.PI * 2.0F * HALF_BOUNCES);
        // Positive squash flattens (early impact); negative squash stretches (the rebound overshoot).
        float squash = AMPLITUDE * envelope * wave;

        float verticalScale = 1.0F - squash;
        float girthScale = 1.0F + (squash * GIRTH_RATIO);
        return new Factors(girthScale, verticalScale);
    }

    /**
     * Per-axis render scale. {@code xz} is applied to both horizontal axes (girth), {@code y} to height.
     */
    public record Factors(float xz, float y) {
    }

}
