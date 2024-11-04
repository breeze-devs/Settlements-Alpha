package dev.breezes.settlements.models.location;

import dev.breezes.settlements.models.conditions.ICondition;
import dev.breezes.settlements.util.MathUtil;
import lombok.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

@AllArgsConstructor
@Getter
@Setter
@ToString
@CustomLog
public class Location implements Cloneable {

    private double x;
    private double y;
    private double z;
    private float pitch;
    private float yaw;

    @Nullable
    private Level level;

    public Location(double x, double y, double z, @Nullable Level level) {
        this(x, y, z, 0, 0, level);
    }

    /*
     * Factory methods
     */
    public static Location of(double x, double y, double z, @Nullable Level level) {
        return new Location(x, y, z, level);
    }

    public static Location of(@Nonnull BlockPos blockPos, @Nullable Level level) {
        return new Location(blockPos.getX(), blockPos.getY(), blockPos.getZ(), level);
    }

    public static Location zero(@Nullable Level level) {
        return new Location(0, 0, 0, level);
    }

    public static Location fromEntity(@Nonnull Entity entity, boolean useEyeHeight) {
        return new Location(
                entity.getX(),
                useEyeHeight ? entity.getEyeY() : entity.getY(),
                entity.getZ(),
                entity.getXRot(),
                entity.getYRot(),
                entity.level()
        );
    }

    @Override
    public Location clone() {
        try {
            Location clone = (Location) super.clone();
            clone.x = this.x;
            clone.y = this.y;
            clone.z = this.z;
            clone.level = this.level;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Conversion methods
     */
    public BlockPos toBlockPos() {
        return new BlockPos(this.getBlockX(), this.getBlockY(), this.getBlockZ());
    }

    public Optional<GlobalPos> toGlobalPos() {
        return this.getLevel()
                .map((level) -> GlobalPos.of(level.dimension(), this.toBlockPos()));
    }

    /*
     * Comparison methods
     */

    /**
     * Returns the square of the distance between this location and another location
     * If the locations are in different worlds, returns {@link Double#POSITIVE_INFINITY}
     */
    public double distanceSquared(@Nonnull Location other) {
        if (this.level != other.level) {
            return Double.POSITIVE_INFINITY;
        }

        return MathUtil.square(this.x - other.x) + MathUtil.square(this.y - other.y) + MathUtil.square(this.z - other.z);
    }

    public double distance(@Nonnull Location other) {
        return Math.sqrt(this.distanceSquared(other));
    }

    /*
     * Modification methods
     */
    public Location add(double x, double y, double z, boolean clone) {
        if (clone) {
            return new Location(this.x + x, this.y + y, this.z + z, this.level);
        }

        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Location multiply(double factor, boolean clone) {
        if (clone) {
            return new Location(this.x * factor, this.y * factor, this.z * factor, this.level);
        }

        this.x *= factor;
        this.y *= factor;
        this.z *= factor;
        return this;
    }

    public Location center(boolean clone) {
        if (clone) {
            return new Location(this.getBlockX() + 0.5, this.getBlockY(), this.getBlockZ() + 0.5, this.level);
        }

        this.x = this.getBlockX() + 0.5;
        this.y = this.getBlockY() + 0.5;
        this.z = this.getBlockZ() + 0.5;
        return this;
    }

    /*
     * Miscellaneous methods
     */
    public boolean isChunkLoaded() {
        // TODO: implement this
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isChunkGenerated() {
        // TODO: implement this
        throw new UnsupportedOperationException("Not implemented");
    }

    public int getHighestBlockY() {
        // TODO: implement this
        throw new UnsupportedOperationException("Not implemented");
    }

    public void playSound(@Nonnull SoundEvent minecraftSound, float volume, float pitch, @Nonnull SoundSource soundSource) {
        if (this.level == null) {
            log.error("Attempted to play sound from a location '%s' with no level", this.toString());
            return;
        }
        this.level.playSound(null, this.x, this.y, this.z, minecraftSound, soundSource, volume, pitch);
    }

    public <T extends ParticleOptions> void displayParticles(@Nonnull T type, int count, double dx, double dy, double dz, double speed) {
        if (this.level == null || !(this.level instanceof ServerLevel serverLevel)) {
            log.error("Attempted to spawn particles from a location '%s' with no server level", this.toString());
            return;
        }
        serverLevel.sendParticles(type, this.x, this.y, this.z, count, dx, dy, dz, speed);
    }

    /*
     * Additional getters
     */
    public Stream<Entity> getNearbyEntities(double radiusX, double radiusY, double radiusZ, @Nullable Entity except, ICondition<Entity> filter) {
        if (this.level == null) {
            log.error("Attempted to get nearby entities from a location '%s' with no level", this.toString());
            return Stream.empty();
        }

        AABB scanBoundary = new AABB(this.x - radiusX, this.y - radiusY, this.z - radiusZ,
                this.x + radiusX, this.y + radiusY, this.z + radiusZ);
        // TODO: we might be able to optimize this with stream logic
        return this.level.getEntities(except, scanBoundary, filter).stream();
    }

    public Vector getDirectionTo(@Nonnull Location other) {
        return new Vector(other.x - this.x, other.y - this.y, other.z - this.z).normalize(false);
    }

    public Optional<Level> getLevel() {
        return Optional.ofNullable(this.level);
    }

    public int getBlockX() {
        return (int) Math.floor(this.x);
    }

    public int getBlockY() {
        return (int) Math.floor(this.y);
    }

    public int getBlockZ() {
        return (int) Math.floor(this.z);
    }

}
