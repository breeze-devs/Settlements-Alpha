package dev.breezes.settlements.infrastructure.rendering.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Translucent orb particle
 * <p>
 * Color and motion behavior are driven by {@link OrbParticleOptions}, keeping the particle
 * class free of hard-coded per-mode constants. The motion-mode switch in {@link #tick()} is
 * the single seam for extending behavior — adding SPIRAL or ORBIT is a localized change here
 * plus a new enum value in {@link OrbParticleOptions.MotionMode}.
 * <p>
 * The texture is a grayscale orb atlas sprite; {@link #setColor} tints it to the options color
 * so no additional texture assets are needed for new color/mode combinations.
 */
public class OrbParticle extends TextureSheetParticle {

    private static final float QUAD_SIZE = 0.09f;

    // Alpha ramp-in covers the first 20% of life so orbs pop into existence softly
    private static final float FADE_IN_END = 0.2f;
    // Alpha ramp-out begins at 65% of life, giving a clear trail before vanishing
    private static final float FADE_OUT_START = 0.65f;

    // RISE mode: gentle upward drift with slight inward bias toward the center column
    private static final float RISE_GRAVITY = -0.004f;
    private static final float RISE_DRAG = 0.98f;
    private static final float RISE_CENTER_PULL = 0.003f;

    // SCATTER mode: initial downward velocity decays; horizontal spread lingers
    private static final float SCATTER_GRAVITY = 0.006f;
    private static final float SCATTER_DRAG = 0.96f;

    private final OrbParticleOptions.MotionMode motionMode;

    // For RISE mode: remembered spawn X/Z so orbs can weakly converge to center column
    private final double spawnX;
    private final double spawnZ;

    private OrbParticle(@Nonnull ClientLevel level,
                        double x, double y, double z,
                        double dx, double dy, double dz,
                        @Nonnull OrbParticleOptions options,
                        @Nonnull SpriteSet spriteSet) {
        super(level, x, y, z);

        this.motionMode = options.motionMode();
        this.spawnX = x;
        this.spawnZ = z;

        // Seed velocities from the caller so the emit helper has full control over spread
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;

        // Short lifetime keeps the aura feeling alive and avoids stacking too many quads
        this.lifetime = 20 + this.random.nextInt(21);

        this.gravity = 0.0f;
        // Physics engine handles collision; for orbs we want translucent drift, not bounce
        this.hasPhysics = false;

        this.quadSize = QUAD_SIZE;
        this.alpha = 0.0f;

        // Tint the grayscale texture with the options color — must come before pickSprite
        this.setColor(options.red(), options.green(), options.blue());

        this.pickSprite(spriteSet);
    }

    @Override
    @Nonnull
    public ParticleRenderType getRenderType() {
        // Translucent sheet so alpha blending and color tinting both work correctly
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        // Bookkeeping required for smooth interpolation between frames
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        float progress = this.age / (float) this.lifetime;

        applyMotion();
        updateAlpha(progress);

        // Integrate velocity into position after applying per-mode forces
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
    }

    /**
     * Applies per-tick forces and drag according to the active motion mode.
     * Each mode branch is self-contained so new modes can be added without touching existing ones.
     */
    private void applyMotion() {
        switch (this.motionMode) {
            case RISE -> applyRiseMotion();
            case SCATTER -> applyScatterMotion();
        }
    }

    /**
     * RISE: upward lift with a gentle inward pull toward the center column so orbs loosely
     * funnel toward the floating totem rather than drifting in random directions forever.
     */
    private void applyRiseMotion() {
        // Gravity is negative to push upward
        this.yd += RISE_GRAVITY;
        this.yd *= RISE_DRAG;
        this.xd *= RISE_DRAG;
        this.zd *= RISE_DRAG;

        // Soft center attraction in XZ prevents orbs from wandering too far laterally
        this.xd += (this.spawnX - this.x) * RISE_CENTER_PULL;
        this.zd += (this.spawnZ - this.z) * RISE_CENTER_PULL;
    }

    /**
     * SCATTER: downward acceleration with lateral drag so orbs peel away from the totem
     * and settle into the field at roughly farmland height before fading.
     */
    private void applyScatterMotion() {
        this.yd -= SCATTER_GRAVITY;
        this.yd *= SCATTER_DRAG;
        this.xd *= SCATTER_DRAG;
        this.zd *= SCATTER_DRAG;
    }

    /**
     * Smooth fade-in / fade-out alpha envelope keyed to normalized lifetime progress.
     * The ramp shape keeps particles from popping harshly into or out of existence.
     */
    private void updateAlpha(float progress) {
        if (progress < FADE_IN_END) {
            this.alpha = progress / FADE_IN_END;
        } else if (progress > FADE_OUT_START) {
            this.alpha = 1.0f - (progress - FADE_OUT_START) / (1.0f - FADE_OUT_START);
        } else {
            this.alpha = 1.0f;
        }
    }

    /**
     * NeoForge/Minecraft calls this factory to construct orb particles from the
     * registered sprite set. Handles both client-emitted and any future server-emitted cases.
     */
    public static class Provider implements ParticleProvider<OrbParticleOptions> {

        private final SpriteSet spriteSet;

        public Provider(@Nonnull SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        @Nullable
        public Particle createParticle(@Nonnull OrbParticleOptions options,
                                       @Nonnull ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new OrbParticle(level, x, y, z, dx, dy, dz, options, spriteSet);
        }

    }

}
