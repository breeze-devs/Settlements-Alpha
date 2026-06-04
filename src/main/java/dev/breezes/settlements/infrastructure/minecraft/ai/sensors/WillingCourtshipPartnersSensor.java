package dev.breezes.settlements.infrastructure.minecraft.ai.sensors;

import com.google.common.collect.ImmutableSet;
import dev.breezes.settlements.bootstrap.registry.memory.MemoryModuleTypeRegistry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Scans visible entities for BaseVillagers that currently satisfy canBreed().
 * Writes their UUIDs to WILLING_COURTSHIP_PARTNERS so the initiator precondition
 * can cheaply check willingness without a live entity scan each time.
 * <p>
 * Registry/invite exclusion is intentionally NOT done here — the initiator precondition
 * is the authority, avoiding a Dagger injection dependency into the sensor infrastructure.
 */
public class WillingCourtshipPartnersSensor extends Sensor<Villager> {

    private static final ClockTicks SCAN_RATE = ClockTicks.seconds(10);

    public WillingCourtshipPartnersSensor() {
        super(SCAN_RATE.getTicksAsInt());
    }

    @Override
    protected void doTick(@Nonnull ServerLevel level, @Nonnull Villager entity) {
        if (!(entity instanceof BaseVillager self)) {
            return;
        }

        List<UUID> partners = StreamSupport.stream(getVisibleEntities(self).findAll(e -> isWillingPartner(self, e)).spliterator(), false)
                .map(LivingEntity::getUUID)
                .toList();

        if (partners.isEmpty()) {
            self.getBrain().eraseMemory(MemoryModuleTypeRegistry.WILLING_COURTSHIP_PARTNERS.get());
        } else {
            self.getBrain().setMemory(MemoryModuleTypeRegistry.WILLING_COURTSHIP_PARTNERS.get(), partners);
        }
    }

    private boolean isWillingPartner(@Nonnull BaseVillager self, @Nonnull LivingEntity entity) {
        return entity instanceof BaseVillager other
                && !other.getUUID().equals(self.getUUID())
                && other.canBreed();
    }

    private NearestVisibleLivingEntities getVisibleEntities(@Nonnull Villager entity) {
        return entity.getBrain()
                .getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                .orElse(NearestVisibleLivingEntities.empty());
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleTypeRegistry.WILLING_COURTSHIP_PARTNERS.get());
    }

}
