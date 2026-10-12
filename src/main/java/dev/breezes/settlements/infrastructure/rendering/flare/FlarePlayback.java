package dev.breezes.settlements.infrastructure.rendering.flare;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.di.ClientSessionResettable;
import dev.breezes.settlements.infrastructure.network.features.flare.packet.ClientBoundFlarePacket;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Plays out the firework rocket flares on this client.
 */
@ClientSide
@ClientScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class FlarePlayback implements ClientSessionResettable {

    // Flight parameters
    private static final double LAUNCH_SPEED = 0.05;
    private static final double ACCELERATION = 0.04;

    // Particle trails
    private static final double SPARK_DRIFT = 0.05;
    private static final double SPARK_FALL = 0.5;

    private static final float LAUNCH_VOLUME = 20.0F;

    private final List<Flight> flights = new ArrayList<>();

    /**
     * The level the flights belong to, or null exactly when there are no flights, so a level the client has left is
     * never held here.
     */
    @Nullable
    private ClientLevel level;

    /**
     * Launches the flares in the current level, each with a rocket's launch sound, skipping any whose launch point is
     * within hideWithin blocks of the camera horizontally. Flares still rising from another level are dropped.
     */
    public void launch(double hideWithin, @Nonnull List<ClientBoundFlarePacket.Flare> flares) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel current = minecraft.level;
        if (current == null) {
            return;
        }

        if (current != this.level) {
            this.dropFlights();
        }

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        double hideWithinSquared = hideWithin * hideWithin;
        for (ClientBoundFlarePacket.Flare flare : flares) {
            Vec3 launch = flare.launch();
            double dx = launch.x - camera.x;
            double dz = launch.z - camera.z;
            if (dx * dx + dz * dz < hideWithinSquared) {
                continue;
            }

            // Delayed by distance as the burst's sounds are, so the launch is never heard after the burst
            current.playLocalSound(launch.x, launch.y, launch.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT,
                    LAUNCH_VOLUME, 1.0F, true);
            this.flights.add(new Flight(flare));
            this.level = current;
        }
    }

    /**
     * Advances every rising flare by one tick and bursts those that reach their apex.
     */
    public void onClientTick() {
        ClientLevel flightLevel = this.level;
        if (flightLevel == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        // Drop all fireworks upon dimension change
        if (minecraft.level != flightLevel) {
            this.dropFlights();
            return;
        }

        // Advance only when the particle engine ticks, so pausing or freezing the game holds a flare still along with
        // the sparks it has already drawn
        if (minecraft.isPaused() || !flightLevel.tickRateManager().runsNormally()) {
            return;
        }

        // A rocket's own trail yields to the particle setting, while its burst always shows
        boolean drawTrail = minecraft.options.particles().get() != ParticleStatus.MINIMAL;
        this.flights.removeIf(flight -> flight.tick(flightLevel, drawTrail));
        if (this.flights.isEmpty()) {
            this.level = null;
        }
    }

    @Override
    public void onClientSessionEnded() {
        this.dropFlights();
    }

    private void dropFlights() {
        this.flights.clear();
        this.level = null;
    }

    private static final class Flight {

        private final ClientBoundFlarePacket.Flare flare;
        private double y;
        private double speed;

        private Flight(@Nonnull ClientBoundFlarePacket.Flare flare) {
            this.flare = flare;
            this.y = flare.launch().y;
            this.speed = LAUNCH_SPEED;
        }

        /**
         * Rises one tick, drawing a trail spark, and bursts on reaching the apex.
         *
         * @return whether the flare burst, which ends its flight
         */
        private boolean tick(@Nonnull ClientLevel level, boolean drawTrail) {
            this.speed += ACCELERATION;
            this.y = Math.min(this.y + this.speed, this.flare.apexY());

            Vec3 launch = this.flare.launch();
            if (drawTrail) {
                RandomSource random = level.getRandom();
                level.addParticle(ParticleTypes.FIREWORK, true, launch.x, this.y, launch.z,
                        random.nextGaussian() * SPARK_DRIFT, -this.speed * SPARK_FALL, random.nextGaussian() * SPARK_DRIFT);
            }

            if (this.y < this.flare.apexY()) {
                return false;
            }

            // A rocket bursts with its own motion
            level.createFireworks(launch.x, this.y, launch.z, 0.0, this.speed, 0.0, this.flare.explosions());
            return true;
        }

    }

}
