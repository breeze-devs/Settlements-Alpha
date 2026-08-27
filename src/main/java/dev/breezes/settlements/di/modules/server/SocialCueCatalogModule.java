package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import dev.breezes.settlements.application.ai.dialogue.AmbientDialogueContextAssembler;
import dev.breezes.settlements.application.ai.dialogue.DialogueContext;
import dev.breezes.settlements.application.ai.dialogue.DialogueLine;
import dev.breezes.settlements.application.ai.dialogue.DialogueProvider;
import dev.breezes.settlements.application.ai.dialogue.Occasion;
import dev.breezes.settlements.application.ai.gossip.GossipSessionRegistry;
import dev.breezes.settlements.application.ai.inference.InferenceGate;
import dev.breezes.settlements.application.ai.socialcue.CueStep;
import dev.breezes.settlements.application.ai.socialcue.SocialCueCatalogEntry;
import dev.breezes.settlements.application.ai.socialcue.SocialCueConfig;
import dev.breezes.settlements.application.ai.socialcue.SocialCueScript;
import dev.breezes.settlements.application.ai.speech.SpeechRegister;
import dev.breezes.settlements.di.BaseLane;
import dev.breezes.settlements.di.CognitionScoped;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorChannel;
import dev.breezes.settlements.domain.ai.eventlane.EventLaneConfig;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.TimeOfDay;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.attachments.PlayerGreetCooldownAttachment;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Registers all {@link SocialCueCatalogEntry} instances as Dagger multibindings.
 * <p>
 * One method per cue type; the arbiter iterates the collected {@code Set<>} each tick.
 * Mirrors the {@link BehaviorCatalogModule} / {@link SensorCatalogModule} pattern.
 */
@Module
public abstract class SocialCueCatalogModule {

    private static final ClockTicks PLAYER_GREET_GLOBAL_COOLDOWN = ClockTicks.minutes(2);
    private static final ClockTicks SITUATIONAL_DIALOGUE_COOLDOWN = ClockTicks.minutes(5);
    private static final ClockTicks ZOMBIE_SIGHTED_COOLDOWN = ClockTicks.minutes(2);
    private static final int CLOCK_WINDOW_TICKS = ClockTicks.seconds(30).getTicksAsInt();

    private static final double SITUATIONAL_DIALOGUE_FIRE_CHANCE = 0.33;
    private static final double ZOMBIE_REACTION_FIRE_CHANCE = 0.5;

    /**
     * Per-villager spread applied to the occasion window's start tick so a settlement's morning
     * greetings do not all become eligible on the exact same tick.
     */
    private static final ClockTicks WINDOW_JITTER = ClockTicks.minutes(2);

    /**
     * Cues that fire regardless of the SIS kill-switch.
     */
    @Multibinds
    @BaseLane
    abstract Set<SocialCueCatalogEntry> baseSocialCueCatalogEntries();

    /**
     * Cues that only make sense with the SIS cognition lane on.
     */
    @Multibinds
    @CognitionScoped
    abstract Set<SocialCueCatalogEntry> cognitionSocialCueCatalogEntries();

    /**
     * The effective cue catalog {@link dev.breezes.settlements.application.ai.socialcue.SocialCueArbiter}
     * iterates — always the base lane, plus the cognition lane only when {@link InferenceGate} is on.
     * This is the sole unqualified {@code Set<SocialCueCatalogEntry>} binding; the arbiter injects it
     * directly and never sees the {@link BaseLane}/{@link CognitionScoped} split.
     * <p>
     * The gate is a load-time snapshot (restart-only application), so this merge is computed once per
     * server session.
     */
    @Provides
    @ServerScope
    static Set<SocialCueCatalogEntry> socialCueCatalogEntries(@BaseLane Set<SocialCueCatalogEntry> baseCues,
                                                              @CognitionScoped Set<SocialCueCatalogEntry> cognitionCues,
                                                              InferenceGate inferenceGate) {
        if (!inferenceGate.isEnabled()) {
            return baseCues;
        }

        Set<SocialCueCatalogEntry> merged = new HashSet<>(baseCues);
        merged.addAll(cognitionCues);
        return Set.copyOf(merged);
    }

    /**
     * Greet-player cue: wave + FLAVOR bubble when a player is nearby and not recently greeted.
     * TODO: this greets all players, not just high reputation ones
     */
    @Provides
    @IntoSet
    @BaseLane
    static SocialCueCatalogEntry greetPlayer() {
        return SocialCueCatalogEntry.builder()
                .key("greet_player")
                .channel(BehaviorChannel.SOCIAL)
                .channel(BehaviorChannel.INTERACTION)
                .cooldown(ClockTicks.seconds(60))
                .perTargetCooldown(ClockTicks.minutes(10))
                .bypassLaneRefractory(true)
                .scriptFactory(villager -> {
                    Optional<Player> nearestPlayer = villager.getSettlementsBrain()
                            .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                            .orElse(PerceivedEntities.empty())
                            .closest(Player.class, p -> !p.isSpectator(), villager);

                    Location gazeTarget = nearestPlayer.map(p -> Location.fromEntity(p, true))
                            .orElse(null);

                    return SocialCueScript.of(List.of(
                            new CueStep.Gaze(gazeTarget),
                            new CueStep.Gesture(AnimationArchetype.WAVE),
                            new CueStep.Wait(ClockTicks.seconds(2)) // hold until the wave animation is complete
                    ));
                })
                .trigger(villager -> {
                    Optional<Player> player = villager.getSettlementsBrain()
                            .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                            .orElse(PerceivedEntities.empty())
                            .closest(Player.class, p -> !p.isSpectator(), villager);
                    if (player.isEmpty()) {
                        return Optional.empty();
                    }
                    // Global per-player greet cooldown: do not greet a player another villager just greeted.
                    if (!PlayerGreetCooldownAttachment.canBeGreeted(player.get(), villager.level().getGameTime())) {
                        return Optional.empty();
                    }
                    return player.map(p -> p.getUUID().toString());
                })
                .onAdmit((villager, playerUuidStr) -> {
                    // Stamp the global per-player greet cooldown
                    Player player = resolvePlayer(villager, playerUuidStr);
                    if (player != null) {
                        long nextGreetable = villager.level().getGameTime() + PLAYER_GREET_GLOBAL_COOLDOWN.getTicks();
                        PlayerGreetCooldownAttachment.markGreeted(player, nextGreetable);
                    }
                })
                .build();
    }

    /**
     * Ambient chatter cue: the villager speaks a short flavor line from the dialogue provider
     * <p>
     * When the provider is disabled the trigger returns empty, so there is no cue, no bubble, and zero overhead
     */
    @Provides
    @IntoSet
    @BaseLane
    static SocialCueCatalogEntry villagerChatter(DialogueProvider dialogueProvider,
                                                 SocialCueConfig socialCueConfig,
                                                 AmbientDialogueContextAssembler contextAssembler) {
        return SocialCueCatalogEntry.builder()
                .key("villager_chatter")
                .channel(BehaviorChannel.SOCIAL)
                .channel(BehaviorChannel.INTERACTION)
                .cooldown(ClockTicks.seconds(socialCueConfig.villagerChatterCooldownSeconds()))
                .perTargetCooldown(ClockTicks.ZERO)
                .trigger(villager -> {
                    if (!dialogueProvider.isEnabled()) {
                        return Optional.empty();
                    }
                    return Optional.of("villager_chatter");
                })
                .scriptFactory(villager -> dialogueProvider
                        .sampleAmbientLine(villager.getUUID(), contextAssembler.assemble(villager))
                        // Has ambient line: show it for 10s
                        .map(line -> SocialCueScript.of(List.of(
                                new CueStep.Speak(line, SpeechRegister.MONOLOGUE, ClockTicks.seconds(10)),
                                new CueStep.Wait(ClockTicks.seconds(3))
                        )))
                        // No line: an empty script occupies no time and shows nothing
                        .orElseGet(() -> SocialCueScript.of(List.of())))
                .build();
    }

    @Provides
    @IntoSet
    @BaseLane
    static SocialCueCatalogEntry zombieSighted(DialogueProvider dialogueProvider,
                                               AmbientDialogueContextAssembler contextAssembler) {
        return SocialCueCatalogEntry.builder()
                .key("zombie_sighted")
                .channel(BehaviorChannel.SOCIAL)
                .channel(BehaviorChannel.INTERACTION)
                .cooldown(ZOMBIE_SIGHTED_COOLDOWN)
                .perTargetCooldown(ClockTicks.minutes(5))
                .fireChance(ZOMBIE_REACTION_FIRE_CHANCE)
                .bypassLaneRefractory(true)
                .trigger(villager -> {
                    if (!dialogueProvider.isEnabled()) {
                        return Optional.empty();
                    }

                    return nearestZombie(villager).map(zombie -> zombie.getUUID().toString());
                })
                .scriptFactory(villager -> buildDialogueScript(dialogueProvider, villager.getUUID(),
                        contextAssembler.assemble(villager, Occasion.ZOMBIE_SIGHTED)))
                .build();
    }

    @Provides
    @IntoSet
    @BaseLane
    static SocialCueCatalogEntry morningDialogue(DialogueProvider dialogueProvider,
                                                 AmbientDialogueContextAssembler contextAssembler) {
        return fixedOccasionCue("morning", Occasion.MORNING,
                villager -> dialogueProvider.isEnabled()
                        && isCurrentDayTickInWindowJittered(villager, TimeOfDay.AT_07_00.getMinecraftTick(), WINDOW_JITTER.getTicksAsInt()),
                dialogueProvider, contextAssembler, SITUATIONAL_DIALOGUE_FIRE_CHANCE);
    }

    @Provides
    @IntoSet
    @BaseLane
    static SocialCueCatalogEntry eveningDialogue(DialogueProvider dialogueProvider,
                                                 AmbientDialogueContextAssembler contextAssembler) {
        return fixedOccasionCue("evening", Occasion.EVENING,
                villager -> dialogueProvider.isEnabled()
                        && isCurrentDayTickInWindowJittered(villager, TimeOfDay.AT_18_00.getMinecraftTick(), WINDOW_JITTER.getTicksAsInt()),
                dialogueProvider, contextAssembler, SITUATIONAL_DIALOGUE_FIRE_CHANCE);
    }

    @Provides
    @IntoSet
    @BaseLane
    static SocialCueCatalogEntry restDayDialogue(DialogueProvider dialogueProvider,
                                                 AmbientDialogueContextAssembler contextAssembler) {
        return fixedOccasionCue("rest_day", Occasion.REST_DAY,
                villager -> dialogueProvider.isEnabled() && isRestDay(villager)
                        && isCurrentDayTickInWindowJittered(villager, TimeOfDay.AT_10_00.getMinecraftTick(), WINDOW_JITTER.getTicksAsInt()),
                dialogueProvider, contextAssembler, SITUATIONAL_DIALOGUE_FIRE_CHANCE);
    }

    /**
     * Gossip-initiate cue: the initiating villager leans toward a nearby peer and plays a "psst" bubble
     * <p>
     * Gossip is presentation only — the pair mimes an exchange and no information moves between them.
     * <p>
     * Fires when: neither the initiator nor the candidate is already in a gossip session, and the
     * candidate is within gossip range.
     * <p>
     * Context key = receiver UUID string. The trigger is a pure predicate — it only selects a
     * receiver and returns their UUID; it never mutates the registry. The {@code onAdmit} callback
     * calls {@code sendInvite}.
     */
    @Provides
    @IntoSet
    @BaseLane
    static SocialCueCatalogEntry gossipInitiate(GossipSessionRegistry gossipSessionRegistry,
                                                EventLaneConfig eventLaneConfig) {
        int gossipMaxDistanceSquared = eventLaneConfig.gossipMaxDistanceSquared();
        return SocialCueCatalogEntry.builder()
                .key("gossip_initiate")
                .channel(BehaviorChannel.SOCIAL)
                .channel(BehaviorChannel.INTERACTION)
                .cooldown(ClockTicks.seconds(eventLaneConfig.gossipInitiateCooldownSeconds()))
                .perTargetCooldown(ClockTicks.seconds(eventLaneConfig.gossipTargetCooldownSeconds()))
                .trigger(initiator -> {
                    // Pure predicate: determine whether this cue should fire and who the receiver
                    // is, without touching the registry.
                    if (gossipSessionRegistry.isParticipating(initiator.getUUID())) {
                        return Optional.empty();
                    }

                    // Find any nearby villager that is not the initiator and not already in a gossip session
                    Optional<BaseVillager> receiver = findGossipReceiver(initiator, gossipSessionRegistry, gossipMaxDistanceSquared);
                    return receiver.map(baseVillager -> baseVillager.getUUID().toString());
                })
                .onAdmit((initiator, receiverUuidStr) -> {
                    // Registry mutation happens here, after all per-target and duration checks
                    // have already passed.
                    UUID receiverId = UUID.fromString(receiverUuidStr);
                    gossipSessionRegistry.sendInvite(
                            initiator.getUUID(),
                            receiverId,
                            initiator.level().getGameTime());
                })
                .scriptFactory(initiator -> {
                    // Re-selecting lands on the villager the trigger picked: the factory runs in the
                    // same tick, and the registry mutation that would change the answer is in onAdmit,
                    // which the arbiter fires only after this script is built.
                    Optional<BaseVillager> receiver = findGossipReceiver(initiator, gossipSessionRegistry, gossipMaxDistanceSquared);
                    Location gazeTarget = receiver
                            .map(v -> Location.fromEntity(v, true))
                            .orElse(null);

                    return SocialCueScript.of(List.of(
                            new CueStep.Gaze(gazeTarget),
                            new CueStep.Speak(DialogueLine.translatable("dialogue.settlements.social_cue.gossip.psst"), SpeechRegister.AMBIENT, ClockTicks.seconds(3)),
                            new CueStep.Wait(ClockTicks.seconds(2))
                    ));
                })
                // Nothing to do on completion: the receiver's cue closes the session, and the
                // registry times the invite out if the receiver never accepts.
                .build();
    }

    /**
     * Gossip-accept cue: mirrors the initiator's cue on the receiver's side so both villagers
     * turn to each other and bubble, then closes the session on completion.
     * <p>
     * Fires when: there is a pending gossip invite for this villager in the GossipSessionRegistry.
     * Context key = session UUID string (used by the onAdmit and onComplete callbacks).
     * <p>
     * The trigger is a pure predicate — it only reads the invite and returns the session id;
     * {@code acceptInvite} is called in {@code onAdmit} once the arbiter has confirmed admission.
     * <p>
     * The per-target cooldown is zero: the context key is the unique session UUID, so a
     * per-target limit keyed on it could never fire twice for the same session and would
     * only create a misleading configuration value. Rate limiting is handled entirely by
     * the per-key cooldown and the registry's isParticipating check.
     */
    @Provides
    @IntoSet
    @BaseLane
    static SocialCueCatalogEntry gossipAccept(GossipSessionRegistry gossipSessionRegistry,
                                              EventLaneConfig eventLaneConfig) {
        return SocialCueCatalogEntry.builder()
                .key("gossip_accept")
                .channel(BehaviorChannel.SOCIAL)
                .channel(BehaviorChannel.INTERACTION)
                .cooldown(ClockTicks.seconds(eventLaneConfig.gossipAcceptCooldownSeconds()))
                // Per-target cooldown is zero: the context key is a unique session UUID, so a
                // per-target rate limit keyed on it can never match across sessions and would
                // misleadingly imply a limit that does not actually fire.
                .perTargetCooldown(ClockTicks.ZERO)
                // A prompt response to a pending invite from another villager; the receiver's own
                // quiet period must not suppress it or the invite would silently expire unanswered.
                .bypassLaneRefractory(true)
                .trigger(receiver -> {
                    // Pure predicate: check whether an invite exists and return its session id.
                    // No registry mutation happens here.
                    if (!gossipSessionRegistry.hasInviteFor(receiver.getUUID())) {
                        return Optional.empty();
                    }

                    return gossipSessionRegistry.getInvite(receiver.getUUID())
                            .map(invite -> invite.sessionId().toString());
                })
                .onAdmit((receiver, sessionIdStr) -> {
                    // Accept the invite here, after all admission checks have passed.
                    // This advances the session to ACCEPTED and removes the invite so no
                    // second receiver can steal it (first-accept-wins via the registry).
                    gossipSessionRegistry.acceptInvite(receiver.getUUID());
                })
                .scriptFactory(receiver -> {
                    // Mirror the initiator's lean-in so the pair face each other for the exchange;
                    // a receiver staring elsewhere reads as two unrelated villagers talking to nobody.
                    Location gazeTarget = gossipSessionRegistry.getActiveSession(receiver.getUUID())
                            .flatMap(session -> findPerceivedVillager(receiver, session.getInitiatorId()))
                            .map(initiator -> Location.fromEntity(initiator, true))
                            .orElse(null);

                    return SocialCueScript.of(List.of(
                            new CueStep.Gaze(gazeTarget),
                            new CueStep.Speak(DialogueLine.translatable("dialogue.settlements.social_cue.gossip.listening"), SpeechRegister.AMBIENT, ClockTicks.seconds(3)),
                            new CueStep.Wait(ClockTicks.seconds(2))
                    ));
                })
                .onComplete((receiver, sessionIdStr) -> {
                    // Teardown is unconditional: an open session permanently locks both
                    // participants out of future gossip via isParticipating.
                    gossipSessionRegistry.completeSession(UUID.fromString(sessionIdStr));
                })
                .build();
    }

    @Nullable
    private static Player resolvePlayer(BaseVillager villager, String playerUuidStr) {
        if (!(villager.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(UUID.fromString(playerUuidStr));
        return entity instanceof Player player ? player : null;
    }

    /**
     * Resolves a villager the perceiver can currently sense, by UUID. Sensing is the source of
     * truth rather than a world-wide entity lookup: a session partner who has walked out of
     * perception range is one this villager cannot plausibly aim at.
     */
    private static Optional<BaseVillager> findPerceivedVillager(BaseVillager perceiver, UUID villagerId) {
        return perceiver.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                .orElse(PerceivedEntities.empty())
                .closest(BaseVillager.class, candidate -> candidate.getUUID().equals(villagerId), perceiver);
    }

    /**
     * Finds the nearest villager who: is not the initiator, is not already in a gossip session,
     * and is within gossip range.
     */
    private static Optional<BaseVillager> findGossipReceiver(BaseVillager initiator,
                                                             GossipSessionRegistry registry,
                                                             int gossipMaxDistanceSquared) {
        Predicate<BaseVillager> canGossipToPredicate = candidate -> !candidate.getUUID().equals(initiator.getUUID())
                && !registry.isParticipating(candidate.getUUID())
                && candidate.distanceToSqr(initiator) < gossipMaxDistanceSquared;
        return initiator.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                .orElse(PerceivedEntities.empty())
                .closest(BaseVillager.class, canGossipToPredicate, initiator);
    }

    private static SocialCueCatalogEntry fixedOccasionCue(String key,
                                                          Occasion occasion,
                                                          Predicate<BaseVillager> triggerPredicate,
                                                          DialogueProvider dialogueProvider,
                                                          AmbientDialogueContextAssembler contextAssembler,
                                                          double fireChance) {
        return SocialCueCatalogEntry.builder()
                .key(key)
                .channel(BehaviorChannel.SOCIAL)
                .channel(BehaviorChannel.INTERACTION)
                .cooldown(SITUATIONAL_DIALOGUE_COOLDOWN)
                .perTargetCooldown(ClockTicks.ZERO)
                .fireChance(fireChance)
                .trigger(villager -> triggerPredicate.test(villager)
                        ? Optional.of(key)
                        : Optional.empty())
                .scriptFactory(villager -> buildDialogueScript(dialogueProvider, villager.getUUID(),
                        contextAssembler.assemble(villager, occasion)))
                .build();
    }

    private static SocialCueScript buildDialogueScript(DialogueProvider dialogueProvider, UUID villagerUuid, DialogueContext context) {
        return dialogueProvider.sampleAmbientLine(villagerUuid, context)
                .map(line -> SocialCueScript.of(List.of(
                        new CueStep.Speak(line, SpeechRegister.MONOLOGUE, ClockTicks.seconds(10)),
                        new CueStep.Wait(ClockTicks.seconds(3)))))
                .orElseGet(() -> SocialCueScript.of(List.of()));
    }

    private static Optional<Zombie> nearestZombie(BaseVillager villager) {
        return villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                .orElse(PerceivedEntities.empty())
                .closest(Zombie.class, Zombie::isAlive, villager);
    }

    /**
     * Like a fixed-window check, but the window's start tick is shifted by a per-villager offset
     * in {@code [-jitterTicks, +jitterTicks]} so a whole settlement does not become eligible for
     * the same occasion cue on the exact same tick.
     */
    private static boolean isCurrentDayTickInWindowJittered(BaseVillager villager, int startTick, int jitterTicks) {
        long offset = Math.floorMod(villager.getUUID().getLeastSignificantBits(), 2L * jitterTicks + 1) - jitterTicks;
        int jitteredStartTick = Math.floorMod(startTick + (int) offset, TimeOfDay.TICKS_PER_DAY);

        int currentTick = Math.floorMod(villager.level().getDayTime(), TimeOfDay.TICKS_PER_DAY);
        int elapsed = Math.floorMod(currentTick - jitteredStartTick, TimeOfDay.TICKS_PER_DAY);
        return elapsed < CLOCK_WINDOW_TICKS;
    }

    private static boolean isRestDay(BaseVillager villager) {
        DayPlan dayPlan = villager.getDayPlan();
        return dayPlan != null && dayPlan.getDayType() == PlanDayType.REST_DAY;
    }

}
