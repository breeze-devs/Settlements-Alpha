package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.ai.dialogue.DialogueProvider;
import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.time.TimeOfDay;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Triggers the evening REHEARSED sweep once per in-game evening.
 * <p>
 * The sweep generates per-villager utterance packs offline (zero daytime calls) so that
 * villagers have pre-generated lines ready by morning. The sweep fires once per calendar
 * evening at approximately 20:00 in-game time.
 * <p>
 * When the provider has no batch-capable rung this event handler exits immediately with zero overhead.
 */
@ServerScope
@CustomLog
public final class RehearsedDialogueSweepServerEvents {

    /**
     * The MC day tick at which the sweep fires.
     * The check runs across a short window so a time jump cannot skip the trigger tick;
     * {@link #lastSweptDay} guarantees it still fires only once per evening.
     */
    private static final int SWEEP_START_TICK = TimeOfDay.AT_20_00.getMinecraftTick();
    private static final int SWEEP_WINDOW_TICKS = 20;

    private final DialogueProvider dialogueProvider;

    /**
     * Absolute day index ({@code getDayTime() / TICKS_PER_DAY}) of the last evening already
     * swept. Without this latch the sweep would fire on every tick of the trigger window — and
     * repeatedly while the daylight cycle is frozen inside it. {@code -1} = never swept.
     */
    private long lastSweptDay;

    @Inject
    public RehearsedDialogueSweepServerEvents(DialogueProvider dialogueProvider) {
        this.dialogueProvider = dialogueProvider;
        this.lastSweptDay = -1L;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!this.dialogueProvider.supportsRehearsedDialogSweep()) {
            return;
        }

        MinecraftServer server = event.getServer();
        long absoluteDayTime = server.overworld().getDayTime();
        long timeOfDay = absoluteDayTime % TimeOfDay.TICKS_PER_DAY;

        // Fire only within the brief window
        if (timeOfDay < SWEEP_START_TICK || timeOfDay >= SWEEP_START_TICK + SWEEP_WINDOW_TICKS) {
            return;
        }

        // Fire only once per evening: the window spans many ticks, so latch on the day index.
        long currentDay = absoluteDayTime / TimeOfDay.TICKS_PER_DAY;
        if (currentDay == this.lastSweptDay) {
            return;
        }
        this.lastSweptDay = currentDay;

        List<BaseVillager> villagers = this.collectLoadedVillagers(server);
        log.info("Evening dialogue pack sweep triggered at dayTime={}, loaded villagers={}", timeOfDay, villagers.size());
        this.dialogueProvider.runEveningPackSweep(villagers);
    }

    /**
     * Collects all alive, loaded BaseVillagers across every server level.
     * <p>
     * Iterating via {@code ServerLevel.getEntities(EntityType, predicate)} avoids a bounded AABB
     * scan and returns only truly loaded entities — no chunk-loading side effects.
     */
    private List<BaseVillager> collectLoadedVillagers(MinecraftServer server) {
        List<BaseVillager> result = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            List<? extends BaseVillager> levelVillagers = level.getEntities(
                    EntityRegistry.BASE_VILLAGER.get(),
                    v -> v.isAlive() && !v.isRemoved()
            );
            result.addAll(levelVillagers);
        }
        return result;
    }

}
