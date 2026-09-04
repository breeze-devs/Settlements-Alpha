package dev.breezes.settlements.infrastructure.rendering.particles;

import dev.breezes.settlements.domain.time.ClockTicks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A single "Z" drifting up off a sleeping villager, following {@link SleepingZMotion}.
 */
public class SleepingZParticle extends TextureSheetParticle {

    private static final ClockTicks LIFETIME = ClockTicks.seconds(2.4);
    private static final ClockTicks LIFETIME_JITTER = ClockTicks.seconds(0.6);

    /**
     * Lazy tilt off vertical, in radians either way. Held constant for the particle's whole life.
     */
    private static final float MAX_TILT = 0.18f;

    /**
     * Floor the block-light term so the Zs stay readable in a dim bedroom.
     */
    private static final int MIN_BLOCK_LIGHT = 6;

    private final double anchorX;
    private final double anchorY;
    private final double anchorZ;
    private final double swayPhase;

    private SleepingZParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet) {
        super(level, x, y, z);

        this.anchorX = x;
        this.anchorY = y;
        this.anchorZ = z;

        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;

        this.lifetime = LIFETIME.getTicksAsInt() + this.random.nextInt(LIFETIME_JITTER.getTicksAsInt());

        // Independent phases keep two Zs in flight from tracing one another
        this.swayPhase = this.random.nextDouble() * 2.0 * Math.PI;

        this.roll = (this.random.nextFloat() * 2.0f - 1.0f) * MAX_TILT;
        this.oRoll = this.roll;

        this.pickSprite(spriteSet);
        this.applyFrame(SleepingZMotion.frameAt(0.0f, this.swayPhase));
    }

    @Override
    @Nonnull
    public ParticleRenderType getRenderType() {
        // Translucent sheet so the fade in and out actually blend
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        int packed = super.getLightColor(partialTick);
        return LightTexture.pack(Math.max(LightTexture.block(packed), MIN_BLOCK_LIGHT), LightTexture.sky(packed));
    }

    @Override
    public void tick() {
        // Store the previous position so the renderer can interpolate between frames
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        this.applyFrame(SleepingZMotion.frameAt(this.age / (float) this.lifetime, this.swayPhase));
    }

    private void applyFrame(SleepingZMotion.Frame frame) {
        this.x = this.anchorX + frame.lateralX();
        this.y = this.anchorY + frame.rise();
        this.z = this.anchorZ + frame.lateralZ();
        this.quadSize = frame.size();
        this.alpha = frame.alpha();
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
            return new SleepingZParticle(level, x, y, z, spriteSet);
        }

    }

}
