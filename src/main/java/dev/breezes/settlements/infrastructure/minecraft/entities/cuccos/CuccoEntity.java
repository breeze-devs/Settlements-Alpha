package dev.breezes.settlements.infrastructure.minecraft.entities.cuccos;

import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.domain.exceptions.SpawnFailedException;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.cuccos.goals.DiveBombGoal;
import lombok.CustomLog;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.Optional;

@CustomLog
public class CuccoEntity extends Chicken {

    private static final double PANIC_SPEED = 2.45D;
    private static final double STROLL_SPEED = 2.0D;
    private static final double DIVE_THRUST = 0.15D;
    private static final double CUCCO_ATTACK_DAMAGE = 0.01D;
    private static final int POOF_PARTICLE_COUNT = 12;
    private static final double POOF_PARTICLE_OFFSET = 0.25D;
    private static final double POOF_PARTICLE_SPEED = 0.02D;

    private ITickable lifetime;
    private int ageTicks;

    public CuccoEntity(EntityType<? extends Chicken> entityType, Level level) {
        super(entityType, level);
        this.lifetime = ClockTicks.seconds(10).asTickable();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Chicken.createAttributes()
                .add(Attributes.ATTACK_DAMAGE, CUCCO_ATTACK_DAMAGE);
    }

    public static CuccoEntity spawn(@Nonnull Location location, @Nonnull ClockTicks lifetime) {
        ServerLevel serverLevel = location.getLevel()
                .filter(level -> level instanceof ServerLevel)
                .map(level -> (ServerLevel) level)
                .orElseThrow(() -> new SpawnFailedException("Failed to spawn CuccoEntity at %s: level is not server level".formatted(location.toString())));

        CuccoEntity cucco = Optional.ofNullable(EntityRegistry.CUCCO.get().create(serverLevel))
                .orElseThrow(() -> new SpawnFailedException("Failed to spawn CuccoEntity at %s".formatted(location.toString())));
        location.teleportEntityHere(cucco);

        cucco.setLifetimeTicks(lifetime);
        cucco.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(location.toBlockPos()), MobSpawnType.EVENT, null);
        serverLevel.addFreshEntity(cucco);

        cucco.spawnPoof();
        location.playSound(SoundEvents.CHICKEN_HURT, 1.0F, 1.0F, SoundSource.NEUTRAL);

        return cucco;
    }

    public void setLifetimeTicks(@Nonnull ClockTicks lifetime) {
        this.lifetime = lifetime.asTickable();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, PANIC_SPEED));
        this.goalSelector.addGoal(2, new DiveBombGoal(this, DIVE_THRUST));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, STROLL_SPEED));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        if (this.lifetime.tickAndCheck(1)) {
            this.discard();
        }
    }

    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @Nonnull DamageSource source) {
        return false;
    }

    private void spawnPoof() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(ParticleTypes.CLOUD,
                this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                POOF_PARTICLE_COUNT,
                POOF_PARTICLE_OFFSET, POOF_PARTICLE_OFFSET, POOF_PARTICLE_OFFSET,
                POOF_PARTICLE_SPEED);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.spawnPoof();
        super.remove(reason);
    }

}
