package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.ai.dialogue.DialogueProvider;
import dev.breezes.settlements.application.ai.dialogue.VillagerDialogueContext;
import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
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
 * Triggers the evening PACKS sweep once per in-game evening.
 * <p>
 * The sweep generates per-villager utterance packs offline (zero daytime calls) so that
 * villagers have pre-generated lines ready by morning. The sweep fires once per calendar
 * evening at approximately 18:00 in-game time.
 * <p>
 * When the provider is in OFF mode {@link DialogueProvider#isEnabled()} returns false and
 * this event handler exits immediately with zero overhead.
 * <p>
 * Villager context is assembled from each villager's attached knowledge store and profession.
 * The persona card is a minimal two-liner; topic seeds are the top entries from the store
 * (up to 5) so the prompt stays under the ~512-token budget.
 */
@ServerScope
@CustomLog
public final class EveningDialoguePackSweepServerEvents {

    /**
     * The MC day tick at which the sweep fires
     * The check runs across a short window so a time jump cannot skip the trigger tick;
     * {@link #lastSweptDay} guarantees it still fires only once per evening.
     */
    private static final int SWEEP_START_TICK = TimeOfDay.AT_20_00.getTick();
    private static final int SWEEP_WINDOW_TICKS = 20;

    private final DialogueProvider dialogueProvider;

    /**
     * Absolute day index ({@code getDayTime() / TICKS_PER_DAY}) of the last evening already
     * swept. Without this latch the sweep would fire on every tick of the trigger window — and
     * repeatedly while the daylight cycle is frozen inside it. {@code -1} = never swept.
     */
    private long lastSweptDay;

    @Inject
    public EveningDialoguePackSweepServerEvents(DialogueProvider dialogueProvider) {
        this.dialogueProvider = dialogueProvider;
        this.lastSweptDay = -1L;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!this.dialogueProvider.isEnabled()) {
            return;
        }

        MinecraftServer server = event.getServer();
        long absoluteDayTime = server.overworld().getDayTime();
        long timeOfDay = absoluteDayTime % TimeOfDay.TICKS_PER_DAY;

        // Fire only within the brief window
        // TODO:CONFIRM -- i think this is a little fragile?
        if (timeOfDay < SWEEP_START_TICK || timeOfDay >= SWEEP_START_TICK + SWEEP_WINDOW_TICKS) {
            return;
        }

        // Fire only once per evening: the window spans many ticks, so latch on the day index.
        long currentDay = absoluteDayTime / TimeOfDay.TICKS_PER_DAY;
        if (currentDay == this.lastSweptDay) {
            return;
        }
        this.lastSweptDay = currentDay;

        log.debug("Evening dialogue pack sweep triggered at dayTime={}", timeOfDay);
        List<VillagerDialogueContext> contexts = collectVillagerContexts(server);
        if (contexts.isEmpty()) {
            return;
        }

        // The sweep is async internally and returns immediately. It will finish within
        // the configured packSweepDeadlineSeconds budget before morning.
        this.dialogueProvider.runEveningPackSweep(contexts);
        log.debug("Evening pack sweep dispatched for {} villagers", contexts.size());
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private List<VillagerDialogueContext> collectVillagerContexts(MinecraftServer server) {
        List<VillagerDialogueContext> contexts = new ArrayList<>();

        for (ServerLevel level : server.getAllLevels()) {
            // EntityType-typed lookup avoids an unchecked cast and correctly limits
            // to our custom BaseVillager type rather than the vanilla Villager supertype.
            level.getEntities(EntityRegistry.BASE_VILLAGER.get(), entity -> true)
                    .forEach(villager -> contexts.add(buildContext(villager)));
        }

        return contexts;
    }

    /**
     * TODO:LLM -- we need to likely rework this
     * Assembles a minimal per-villager context for the sweep. The persona card is intentionally
     * terse — a tiny evening model needs only a few trait words to produce in-character lines.
     * Topic seeds pull from the knowledge store (top 5 by insertion order — the store is FIFO
     * eviction so the oldest-admitted entries are also the ones with the most dwell time and
     * the most corroboration opportunity).
     */
    private VillagerDialogueContext buildContext(BaseVillager villager) {
        String professionKey = villager.getVillagerData().getProfession().toString();

        // Minimal persona card: two lines to keep the prompt short.
        String personaCard = "Profession: %s. Personality: diligent, sociable."
                .formatted(professionKey);

        // Pull at most 5 knowledge entries as topic seeds; entriesView() is insertion-ordered.
        VillagerDialogueContext.VillagerDialogueContextBuilder builder =
                VillagerDialogueContext.builder()
                        .villagerUuid(villager.getUUID())
                        .personaCard(personaCard);

        int seedCount = 0;
        for (KnowledgeEntry entry : villager.getKnowledgeStore().entriesView()) {
            if (seedCount >= 5) {
                break;
            }
            builder.groundingSeed(entry.getContent());
            seedCount++;
        }

        return builder.build();
    }

}
