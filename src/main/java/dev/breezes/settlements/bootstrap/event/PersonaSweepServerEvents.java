package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.ai.persona.PersonaConfig;
import dev.breezes.settlements.application.ai.persona.PersonaGenerationService;
import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Drives the async persona-generation pipeline off the server tick.
 */
@ServerScope
@CustomLog
public final class PersonaSweepServerEvents {

    /**
     * Installing personas is not latency-sensitive so the drain runs at 1 Hz rather than every tick
     */
    private static final ClockTicks DRAIN_INTERVAL = ClockTicks.seconds(1);

    private final PersonaGenerationService service;

    private final ITickable drainTickable;
    private final ITickable sweepTickable;

    @Inject
    public PersonaSweepServerEvents(PersonaGenerationService service, PersonaConfig config) {
        this.service = service;
        this.drainTickable = DRAIN_INTERVAL.asTickable();

        this.sweepTickable = ClockTicks.of(config.sweepIntervalTicks()).asTickable();
        this.sweepTickable.forceComplete();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        if (this.drainTickable.tickCheckAndReset(1)) {
            this.service.drain(server);
        }

        if (this.sweepTickable.tickCheckAndReset(1)) {
            this.service.sweep(() -> this.collectLoadedVillagers(server));
        }
    }

    /**
     * Collects all alive, loaded BaseVillagers across every server level.
     * <p>
     * Walks all loaded entities, no chunk-load side effects.
     */
    private List<BaseVillager> collectLoadedVillagers(@Nonnull MinecraftServer server) {
        List<BaseVillager> result = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            List<? extends BaseVillager> levelVillagers = level.getEntities(EntityRegistry.BASE_VILLAGER.get(), LivingEntity::isAlive);
            result.addAll(levelVillagers);
        }
        return result;
    }

}
