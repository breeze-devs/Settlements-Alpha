package dev.breezes.settlements.infrastructure.rendering.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Camera-facing egg splat that pops open quickly then slowly shrinks and fades.
 */
public class EggSplatParticle extends TextureSheetParticle {

    private static final float INITIAL_SIZE = 0.03f;
    private static final float PEAK_SIZE = 0.3f;
    private static final float END_SIZE = 0.1f;

    // Fraction of lifetime over which the fast-grow phase runs
    private static final float GROW_PHASE_END = 0.2f;
    // Fraction of lifetime over which alpha stays fully opaque
    private static final float FADE_START = 0.4f;

    private EggSplatParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet) {
        super(level, x, y, z);

        // Lifetime range gives enough time for the pop + linger, but keeps it snappy
        this.lifetime = 16 + this.random.nextInt(9);

        this.gravity = 0.0f;
        this.hasPhysics = false;

        // No movement
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;

        // Random initial roll so identical impacts don't all look the same
        this.roll = this.random.nextFloat() * (float) (2.0 * Math.PI);
        this.oRoll = this.roll;

        // Single-frame texture; pickSprite selects based on the sprite set (supports future multi-frame)
        this.pickSprite(spriteSet);

        this.quadSize = INITIAL_SIZE;
        this.alpha = 1.0f;
    }

    @Override
    @Nonnull
    public ParticleRenderType getRenderType() {
        // Translucent sheet required so alpha blending (fade-out) works correctly
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        float progress = this.age / (float) this.lifetime;

        if (progress <= GROW_PHASE_END) {
            // Fast grow: ease-out from INITIAL_SIZE to PEAK_SIZE
            float t = progress / GROW_PHASE_END;
            float eased = 1.0f - (1.0f - t) * (1.0f - t);
            this.quadSize = INITIAL_SIZE + (PEAK_SIZE - INITIAL_SIZE) * eased;
        } else {
            // Slow shrink: linear from PEAK_SIZE to END_SIZE over the remainder
            float t = (progress - GROW_PHASE_END) / (1.0f - GROW_PHASE_END);
            this.quadSize = PEAK_SIZE + (END_SIZE - PEAK_SIZE) * t;
        }

        if (progress <= FADE_START) {
            this.alpha = 1.0f;
        } else {
            // Linear fade from 1.0 to 0.0 over the trailing 60% of life
            this.alpha = 1.0f - (progress - FADE_START) / (1.0f - FADE_START);
        }
    }

    /**
     * NeoForge/Minecraft calls this factory via event registration to produce
     * particles from server-sent particle packets.
     */
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        @Nullable
        public Particle createParticle(@Nonnull SimpleParticleType type,
                                       @Nonnull ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new EggSplatParticle(level, x, y, z, spriteSet);
        }

    }

}
