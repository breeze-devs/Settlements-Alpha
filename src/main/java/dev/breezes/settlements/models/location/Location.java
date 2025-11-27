package dev.breezes.settlements.models.location;

import dev.breezes.settlements.models.blocks.PhysicalBlock;
import dev.breezes.settlements.models.conditions.ICondition;
import dev.breezes.settlements.util.MathUtil;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@AllArgsConstructor
@Getter
@Setter
@ToString
@CustomLog
public class Location implements Cloneable {

    private static final String EMPTY_LEVEL_ID = "empty";

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

    /**
     * Deserializes a location from a string format
     * <p>
     * Format: level_id,x,y,z,pitch,yaw
     * <p>
     * Example: minecraft:overworld,10.03,-60.0,5.34,0.0,323.92
     */
    public static Location deserialize(@Nonnull String serialized) {
        String[] parts = serialized.split(",");
        if (parts.length < 6) {
            throw new IllegalArgumentException("Invalid serialized location format: " + serialized);
        }

        String levelResourceLocation = parts[0];
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float pitch = parts[4].isEmpty() ? 0 : Float.parseFloat(parts[4]);
        float yaw = parts[5].isEmpty() ? 0 : Float.parseFloat(parts[5]);

        Location location = new Location(x, y, z, pitch, yaw, null);
        if (EMPTY_LEVEL_ID.equals(levelResourceLocation)) {
            // If the level ID is empty, we set the level to null
            return location;
        }

        // Parse the level resource location and set the level if it exists
        Optional.of(ResourceLocation.parse(levelResourceLocation))
                .map(l -> ResourceKey.create(Registries.DIMENSION, l))
                .flatMap(dimension -> Optional.ofNullable(ServerLifecycleHooks.getCurrentServer())
                        .map(server -> server.getLevel(dimension)))
                .ifPresent(location::setLevel);
        return location;
    }

    /*
     * Conversion methods
     */

    /**
     * Serializes this location to a string format
     * <p>
     * Format: level_id,x,y,z,pitch,yaw
     * <p>
     * Example: minecraft:overworld,10.03,-60.0,5.34,0.0,323.92
     */
    public String serialize() {
        // Format: "level_id,x,y,z,pitch,yaw"
        String levelResourceLocation = Optional.ofNullable(this.level)
                .map(Level::dimension)
                .map(ResourceKey::location)
                .map(Objects::toString)
                .orElse(EMPTY_LEVEL_ID);
        return String.join(",",
                levelResourceLocation,
                String.valueOf(MathUtil.round(this.x, 2)),
                String.valueOf(MathUtil.round(this.y, 2)),
                String.valueOf(MathUtil.round(this.z, 2)),
                String.valueOf(MathUtil.round(this.pitch, 1)),
                String.valueOf(MathUtil.round(this.yaw, 1)));
    }

    public BlockPos toBlockPos() {
        return new BlockPos(this.getBlockX(), this.getBlockY(), this.getBlockZ());
    }

    public Optional<GlobalPos> toGlobalPos() {
        return this.getLevel()
                .map((level) -> GlobalPos.of(level.dimension(), this.toBlockPos()));
    }

    public Vec3 toVec3() {
        return new Vec3(this.x, this.y, this.z);
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

    public double distanceSquared(@Nonnull Entity entity) {
        if (this.level != entity.level()) {
            return Double.POSITIVE_INFINITY;
        }

        return MathUtil.square(this.x - entity.getX()) + MathUtil.square(this.y - entity.getY()) + MathUtil.square(this.z - entity.getZ());
    }

    public double distance(@Nonnull Location other) {
        return Math.sqrt(this.distanceSquared(other));
    }

    public double distance(@Nonnull Entity entity) {
        return Math.sqrt(this.distanceSquared(entity));
    }

    public int distanceManhattan(@Nonnull Location other) {
        if (this.level != other.level) {
            return Integer.MAX_VALUE;
        }

        return Math.abs(this.getBlockX() - other.getBlockX())
                + Math.abs(this.getBlockY() - other.getBlockY())
                + Math.abs(this.getBlockZ() - other.getBlockZ());
    }

    public int distanceManhattan(@Nonnull Entity entity) {
        if (this.level != entity.level()) {
            return Integer.MAX_VALUE;
        }

        return Math.abs(this.getBlockX() - entity.getBlockX())
                + Math.abs(this.getBlockY() - entity.getBlockY())
                + Math.abs(this.getBlockZ() - entity.getBlockZ());
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

    public Location add(@Nonnull Direction direction, double magnitude, boolean clone) {
        double dx = direction.getStepX() * magnitude;
        double dy = direction.getStepY() * magnitude;
        double dz = direction.getStepZ() * magnitude;
        return this.add(dx, dy, dz, clone);
    }

    public Location add(@Nonnull Direction direction, boolean clone) {
        return this.add(direction, 1, clone);
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
    public Optional<PhysicalBlock> getBlock() {
        if (this.level == null) {
            log.warn("Attempted to get block from a location '{}' with no level", this.toString());
            return Optional.empty();
        }

        return Optional.of(PhysicalBlock.of(this, this.level.getBlockState(this.toBlockPos())));
    }

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
            log.error("Attempted to play sound from a location '{}' with no level", this.toString());
            return;
        }
        this.level.playSound(null, this.x, this.y, this.z, minecraftSound, soundSource, volume, pitch);
    }

    public <T extends ParticleOptions> void displayParticles(@Nonnull T type, int count, double dx, double dy, double dz, double speed) {
        if (this.level == null || !(this.level instanceof ServerLevel serverLevel)) {
            log.error("Attempted to spawn particles from a location '{}' with no server level", this.toString());
            return;
        }
        serverLevel.sendParticles(type, this.x, this.y, this.z, count, dx, dy, dz, speed);
    }

    public void teleportEntityHere(@Nonnull Entity entity) {
        entity.teleportTo(this.x, this.y, this.z);
    }

    /*
     * Additional getters
     */
    public Stream<Entity> getNearbyEntities(double radiusX, double radiusY, double radiusZ, @Nullable Entity except, ICondition<Entity> filter) {
        if (this.level == null) {
            log.error("Attempted to get nearby entities from a location '{}' with no level", this.toString());
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

    public void spawnEntity(@Nonnull Entity entity) {
        if (this.level == null) {
            log.error("Attempted to spawn entity at a location '{}' with no level", this.toString());
            return;
        }

        entity.moveTo(this.x, this.y, this.z, this.yaw, this.pitch);
        this.level.addFreshEntity(entity);
    }


}

