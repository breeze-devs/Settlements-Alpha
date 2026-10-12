package dev.breezes.settlements.application.ai.behavior.usecases.villager.cartographer;

import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.infrastructure.network.features.flare.packet.ClientBoundFlarePacket;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * The flares that show the players in a raid where the raiders are coming from, and the signal rocket that marks the
 * cartographer charting them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ChartFlares {

    /**
     * The horizontal distance from a launch point within which a player is shown no flare, since the raider's glow
     * already marks it.
     */
    private static final double HIDE_WITHIN = 32;

    // How far a flare rises before it bursts, drawn per flare so a raider's flares burst apart
    private static final double MIN_APEX_RISE = 16;
    private static final double MAX_APEX_RISE = 32;

    private static final IntList RED = IntList.of(DyeColor.RED.getFireworkColor());

    private static final FireworkExplosion SMALL_BURST =
            new FireworkExplosion(FireworkExplosion.Shape.BURST, RED, IntList.of(), true, true);
    private static final FireworkExplosion LARGE_BURST =
            new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL, RED, IntList.of(), true, true);

    // A signal rocket's flight duration, as on a crafted firework rocket, drawn per rocket so a cartographer's rockets
    // end apart. The shortest flies for about as long as the sketch
    private static final int MIN_SIGNAL_FLIGHT_DURATION = 3;
    private static final int MAX_SIGNAL_FLIGHT_DURATION = 5;

    /**
     * Launches the expertise's number of starless firework rockets from the open sky above the cartographer, so players
     * near it can see it is charting. A cartographer hiding indoors launches them from its roof.
     */
    static void launchSignal(@Nonnull ServerLevel level,
                             @Nonnull Entity cartographer,
                             @Nonnull Expertise expertise,
                             @Nonnull RandomSource random) {
        double x = cartographer.getX();
        double z = cartographer.getZ();
        int surface = surfaceAt(level, x, z);
        for (int i = 0; i < flareCount(expertise); i++) {
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
            int flightDuration = Mth.nextInt(random, MIN_SIGNAL_FLIGHT_DURATION, MAX_SIGNAL_FLIGHT_DURATION);
            rocket.set(DataComponents.FIREWORKS, new Fireworks(flightDuration, List.of()));
            // A real rocket is safe here because a starless one deals no damage when it ends
            level.addFreshEntity(new FireworkRocketEntity(level, cartographer, x, surface, z, rocket));
        }
    }

    /**
     * Sends one payload with the expertise's number of flares to every live player inside the raid's area. The flares
     * mark the charted raiders in turn, marking a raider again when there are more flares than raiders, and a flare
     * whose raider has no surface to launch from is left out.
     * Sends nothing when no flare has a launch point or no player is in the area.
     *
     * @param charted   the raiders this pulse lit, in the order the flares mark them
     * @param expertise the charting cartographer's expertise, which sets the flare count and each flare's spread and
     *                  burst
     */
    static void send(@Nonnull ServerLevel level,
                     @Nonnull Raid raid,
                     @Nonnull List<Raider> charted,
                     @Nonnull Expertise expertise,
                     @Nonnull RandomSource random) {
        if (charted.isEmpty()) {
            return;
        }

        double spread = spread(expertise);
        List<FireworkExplosion> burst = List.of(expertise.getLevel() >= Expertise.JOURNEYMAN.getLevel() ? LARGE_BURST : SMALL_BURST);

        List<ClientBoundFlarePacket.Flare> flares = IntStream.range(0, flareCount(expertise))
                .mapToObj(i -> launchPoint(level, charted.get(i % charted.size()), spread, random))
                .flatMap(Optional::stream)
                .map(launch -> new ClientBoundFlarePacket.Flare(launch,
                        launch.y + Mth.nextDouble(random, MIN_APEX_RISE, MAX_APEX_RISE), burst))
                .toList();
        if (flares.isEmpty()) {
            return;
        }

        // The players vanilla shows the raid's bar to
        List<ServerPlayer> recipients =
                level.getPlayers(player -> player.isAlive() && level.getRaidAt(player.blockPosition()) == raid);
        ClientBoundFlarePacket packet = new ClientBoundFlarePacket(HIDE_WITHIN, flares);
        for (ServerPlayer recipient : recipients) {
            PacketDistributor.sendToPlayer(recipient, packet);
        }
    }

    /**
     * How many flares a cartographer sends each time it charts raiders, and how many signal rockets it launches.
     */
    private static int flareCount(@Nonnull Expertise expertise) {
        return switch (expertise) {
            case NOVICE, APPRENTICE, JOURNEYMAN -> 1;
            case EXPERT -> 2;
            case MASTER -> 3;
        };
    }

    /**
     * How far from its raider a flare may launch: the cartographer's imprecision, which the raider's glow corrects once
     * a player is near.
     */
    private static double spread(@Nonnull Expertise expertise) {
        return switch (expertise) {
            case NOVICE -> 24;
            case APPRENTICE -> 18;
            case JOURNEYMAN -> 12;
            case EXPERT -> 6;
            case MASTER -> 0;
        };
    }

    /**
     * The surface of a random column within spread blocks of the raider, or of the raider's own column when that one
     * has no surface.
     *
     * @return empty when the raider's own column has no surface either
     */
    private static Optional<Vec3> launchPoint(@Nonnull ServerLevel level,
                                              @Nonnull Raider raider,
                                              double spread,
                                              @Nonnull RandomSource random) {
        double angle = random.nextDouble() * Mth.TWO_PI;
        // The square root spreads launch points evenly over the disc instead of bunching them toward the raider
        double distance = spread * Math.sqrt(random.nextDouble());
        double x = raider.getX() + distance * Math.cos(angle);
        double z = raider.getZ() + distance * Math.sin(angle);

        // A column reads as the bottom of the world both in an unloaded chunk
        int surface = surfaceAt(level, x, z);
        if (surface <= level.getMinBuildHeight()) {
            x = raider.getX();
            z = raider.getZ();
            surface = surfaceAt(level, x, z);
        }

        // The raider's own column is empty too when it is over the void (unlikely)
        if (surface <= level.getMinBuildHeight()) {
            return Optional.empty();
        }

        return Optional.of(new Vec3(x, surface, z));
    }

    /**
     * The lowest height in the column at x, z with nothing but air above it.
     */
    private static int surfaceAt(@Nonnull ServerLevel level, double x, double z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(x), Mth.floor(z));
    }

}
