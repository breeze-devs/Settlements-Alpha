package dev.breezes.settlements.infrastructure.rendering.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleTypeRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nonnull;

/**
 * Particle options for the "orb" particle type, carrying a packed RGB color and a motion mode.
 * <p>
 * This follows the vanilla {@code DustParticleOptions} pattern for a colorable, data-carrying
 * particle type: a {@link MapCodec} for datapack / command use and a {@link StreamCodec} for
 * the network sync path that the {@link ParticleType} abstract API requires regardless of
 * whether server emission is in use today.
 * <p>
 * The grayscale orb texture is tinted at render time via
 * {@link net.minecraft.client.particle.TextureSheetParticle#setColor}, so one texture covers
 * every color/mode combination.
 */
public record OrbParticleOptions(int packedRgb, MotionMode motionMode) implements ParticleOptions {

    /**
     * Convenient white-rise preset for the ascending aura orbs.
     */
    public static final OrbParticleOptions RISE_WHITE = new OrbParticleOptions(0xFFFFFF, MotionMode.RISE);

    /**
     * Convenient lime-green scatter preset for the descending field orbs.
     */
    public static final OrbParticleOptions SCATTER_LIME = new OrbParticleOptions(0x55E028, MotionMode.SCATTER);

    /**
     * Controls the per-tick motion strategy applied inside {@link OrbParticle#tick()}.
     * The enum value is persisted through the codec so new modes (e.g. SPIRAL) can be
     * added without touching any serialization logic.
     */
    public enum MotionMode {
        /**
         * Orbs rise upward from the lily-pad base toward the floating totem height.
         */
        RISE,
        /**
         * Orbs scatter downward and outward from the floating totem position into the farm zone.
         */
        SCATTER
    }

    // MapCodec used by the ParticleType codec() contract (datapack / /particle command support).
    // Both fields must be present in every serialized form.
    public static final MapCodec<OrbParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.INT
                            .fieldOf("color")
                            .forGetter(OrbParticleOptions::packedRgb),
                    Codec.STRING
                            .fieldOf("mode")
                            .xmap(MotionMode::valueOf, MotionMode::name)
                            .forGetter(OrbParticleOptions::motionMode)
            ).apply(instance, OrbParticleOptions::new)
    );

    // StreamCodec used for the network packet path required by the ParticleType API.
    // INT (4 bytes) for the packed color, then a single ordinal byte for the mode enum.
    public static final StreamCodec<RegistryFriendlyByteBuf, OrbParticleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    OrbParticleOptions::packedRgb,
                    ByteBufCodecs.BYTE.map(
                            b -> MotionMode.values()[b & 0xFF],
                            mode -> (byte) mode.ordinal()
                    ),
                    OrbParticleOptions::motionMode,
                    OrbParticleOptions::new
            );

    @Override
    @Nonnull
    public ParticleType<OrbParticleOptions> getType() {
        // Resolved via the registry at runtime — the registry holds the canonical ParticleType instance.
        return ParticleTypeRegistry.ORB.get();
    }

    /**
     * Decomposes the packed RGB int into normalized [0.0, 1.0] red channel.
     */
    public float red() {
        return ((packedRgb >> 16) & 0xFF) / 255.0f;
    }

    /**
     * Decomposes the packed RGB int into normalized [0.0, 1.0] green channel.
     */
    public float green() {
        return ((packedRgb >> 8) & 0xFF) / 255.0f;
    }

    /**
     * Decomposes the packed RGB int into normalized [0.0, 1.0] blue channel.
     */
    public float blue() {
        return (packedRgb & 0xFF) / 255.0f;
    }

}
