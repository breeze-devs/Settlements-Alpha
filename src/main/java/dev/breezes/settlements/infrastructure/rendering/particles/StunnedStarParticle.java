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
 * A star that orbits a fixed point above the villager's head, giving the classic "seeing stars"
 * visual after a blast misfire. Each particle gets a random starting angle so bursts spread
 * evenly around the ring rather than clumping at the same point.
 */
public class StunnedStarParticle extends TextureSheetParticle {

    private static final float ORBIT_RADIUS = 0.4f;
    // One full orbit every ~60 ticks gives a leisurely spin
    private static final float ANGULAR_SPEED = (float) (2.0 * Math.PI / 60.0);
    private static final float VERTICAL_BOB_AMPLITUDE = 0.06f;
    private static final float FADE_START = 0.7f;
    private static final float QUAD_SIZE = 0.12f;

    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final float initialAngle;

    private StunnedStarParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet) {
        super(level, x, y, z);

        // Capture the spawn point as the orbit center; incoming velocity is intentionally discarded
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;

        // Long enough for the star ring to feel "dizzy" for a couple of seconds
        this.lifetime = 30 + this.random.nextInt(11);

        this.gravity = 0.0f;
        this.hasPhysics = false;

        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;

        // Spread stars around the ring immediately rather than all starting at angle 0
        this.initialAngle = this.random.nextFloat() * (float) (2.0 * Math.PI);

        this.quadSize = QUAD_SIZE;
        this.alpha = 1.0f;

        this.pickSprite(spriteSet);
    }

    @Override
    @Nonnull
    public ParticleRenderType getRenderType() {
        // Translucent sheet so the fade-out alpha blending works correctly
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        // Store previous position so the renderer can interpolate smoothly between frames
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        float progress = this.age / (float) this.lifetime;

        // Orbit position computed from a fixed center; physics engine is bypassed entirely
        float angle = this.initialAngle + this.age * ANGULAR_SPEED;
        this.x = this.centerX + ORBIT_RADIUS * Math.cos(angle);
        this.z = this.centerZ + ORBIT_RADIUS * Math.sin(angle);
        // Gentle vertical bob adds a third dimension to the "seeing stars" ring
        this.y = this.centerY + VERTICAL_BOB_AMPLITUDE * Math.sin(angle * 2.0f);

        if (progress >= FADE_START) {
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
            return new StunnedStarParticle(level, x, y, z, spriteSet);
        }

    }

}
