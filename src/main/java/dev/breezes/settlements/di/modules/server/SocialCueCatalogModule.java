package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import dev.breezes.settlements.application.ai.dialogue.DialogueContext;
import dev.breezes.settlements.application.ai.dialogue.DialoguePriority;
import dev.breezes.settlements.application.ai.dialogue.DialogueProvider;
import dev.breezes.settlements.application.ai.gossip.GossipSessionRegistry;
import dev.breezes.settlements.application.ai.socialcue.CueStep;
import dev.breezes.settlements.application.ai.socialcue.SocialCueCatalogEntry;
import dev.breezes.settlements.application.ai.socialcue.SocialCueScript;
import dev.breezes.settlements.domain.ai.catalog.BehaviorChannel;
import dev.breezes.settlements.domain.ai.credibility.ReputationQuery;
import dev.breezes.settlements.domain.ai.eventlane.EventLaneConfig;
import dev.breezes.settlements.domain.ai.knowledge.GossipWeightCalculator;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.attachments.PlayerGreetCooldownAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerWasCuredAttachment;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

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

    @Multibinds
    abstract Set<SocialCueCatalogEntry> socialCueCatalogEntries();

    /**
     * Greet-player cue: wave + FLAVOR bubble when a player is nearby and not recently greeted.
     * TODO: this greets all players, not just high reputation ones
     */
    @Provides
    @IntoSet
    static SocialCueCatalogEntry greetPlayer() {
        return SocialCueCatalogEntry.builder()
                .key("greet_player")
                .channel(BehaviorChannel.SOCIAL)
                .channel(BehaviorChannel.INTERACTION)
                .cooldown(ClockTicks.seconds(60))
                .perTargetCooldown(ClockTicks.minutes(10))
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
     * When the provider is OFF the trigger returns empty, so there is no cue, no bubble, and zero overhead
     */
    @Provides
    @IntoSet
    static SocialCueCatalogEntry villagerChatter(DialogueProvider dialogueProvider,
                                                 EventLaneConfig eventLaneConfig) {
        return SocialCueCatalogEntry.builder()
                .key("villager_chatter")
                .channel(BehaviorChannel.SOCIAL)
                .channel(BehaviorChannel.INTERACTION)
                .cooldown(ClockTicks.seconds(eventLaneConfig.villagerChatterCooldownSeconds()))
                .perTargetCooldown(ClockTicks.ZERO)
                .trigger(villager -> {
                    if (!dialogueProvider.isEnabled()) {
                        return Optional.empty();
                    }
                    return Optional.of("villager_chatter");
                })
                .scriptFactory(villager -> dialogueProvider
                        .sampleAmbientLine(villager.getUUID(), buildAmbientContext(villager))
                        // Has ambient line: show it for 4s
                        .map(text -> SocialCueScript.of(List.of(
                                new CueStep.Bubble(text, ClockTicks.seconds(4)),
                                new CueStep.Wait(ClockTicks.seconds(3))
                        )))
                        // No line: an empty script occupies no time and shows nothing
                        .orElseGet(() -> SocialCueScript.of(List.of())))
                .build();
    }

    /**
     * Gossip-initiate cue: the initiating villager leans toward a nearby peer and plays a "psst" bubble
     * <p>
     * Fires when: the villager has at least one shareable knowledge entry AND a nearby villager
     * that is not itself and not already participating AND neither is already in a gossip session.
     * <p>
     * Context key = receiver UUID string. The trigger is a pure predicate — it only selects a
     * receiver and returns their UUID; it never mutates the registry. The {@code onAdmit} callback
     * re-selects the best entry (same tick, store unchanged) and calls {@code sendInvite}.
     */
    @Provides
    @IntoSet
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

                    VillagerKnowledgeStore store = initiator.getKnowledgeStore();
                    List<KnowledgeEntry> shareable = store.shareableEntries();
                    if (shareable.isEmpty()) {
                        return Optional.empty();
                    }

                    // Find any nearby villager that is not the initiator and not already in a gossip session
                    Optional<BaseVillager> receiver = findGossipReceiver(initiator, gossipSessionRegistry, gossipMaxDistanceSquared);
                    return receiver.map(baseVillager -> baseVillager.getUUID().toString());
                })
                .onAdmit((initiator, receiverUuidStr) -> {
                    // Registry mutation happens here, after all per-target and duration checks
                    // have already passed. Re-select the best entry: onAdmit runs synchronously
                    // in the same tick as the trigger, so the store is unchanged and selection
                    // is consistent with what the trigger observed.
                    VillagerKnowledgeStore store = initiator.getKnowledgeStore();
                    List<KnowledgeEntry> shareable = store.shareableEntries();
                    if (shareable.isEmpty()) {
                        return;
                    }
                    KnowledgeEntry entryToShare = selectBestEntry(shareable);
                    UUID receiverId = UUID.fromString(receiverUuidStr);
                    gossipSessionRegistry.sendInvite(
                            initiator.getUUID(),
                            receiverId,
                            entryToShare,
                            initiator.level().getGameTime());
                })
                .scriptFactory(initiator -> {
                    // Aim gaze at the same villager the trigger selected as the gossip receiver,
                    Optional<BaseVillager> receiver = findGossipReceiver(initiator, gossipSessionRegistry, gossipMaxDistanceSquared);
                    Location gazeTarget = receiver
                            .map(v -> Location.fromEntity(v, true))
                            .orElse(null);

                    return SocialCueScript.of(List.of(
                            new CueStep.Gaze(gazeTarget),
                            new CueStep.Bubble("psst...", ClockTicks.seconds(3)),
                            new CueStep.Wait(ClockTicks.seconds(2))
                    ));
                })
                // The knowledge transfer happens on the receiver's side (gossip_accept onComplete).
                // Nothing extra needed on completion — the registry times out if the receiver never accepts.
                .build();
    }

    /**
     * Gossip-accept cue: mirrors the initiator's cue on the receiver's side, then writes
     * the knowledge copy into the receiver's store on completion.
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
    static SocialCueCatalogEntry gossipAccept(GossipSessionRegistry gossipSessionRegistry,
                                              ReputationQuery reputationQuery,
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
                    // Mirror the initiator's lean-in cue so both villagers perform the gesture.
                    return SocialCueScript.of(List.of(
                            new CueStep.Bubble("(listening)", ClockTicks.seconds(3)),
                            new CueStep.Wait(ClockTicks.seconds(2))
                    ));
                })
                .onComplete((receiver, sessionIdStr) -> {
                    // The knowledge transfer is the receiver's completion action.
                    // Look up the session, copy the entry into the receiver's store.
                    UUID sessionId = UUID.fromString(sessionIdStr);
                    gossipSessionRegistry.getActiveSession(receiver.getUUID()).ifPresent(session -> {
                        KnowledgeEntry sourceEntry = session.getEntryToShare();
                        long currentTick = receiver.level().getGameTime();
                        int incomingHop = sourceEntry.getHop() + 1;

                        float baseWeight = GossipWeightCalculator.compute(
                                sourceEntry.getWeight(),
                                sourceEntry.getOriginTimestampTick(),
                                currentTick,
                                receiver.getGenetics(),
                                incomingHop);

                        // Apply the sender's credibility multiplier at admission time so that
                        // tips from less-trusted sources carry less weight in the receiver's
                        // planning. Unknown senders default to 1.0 (no penalty).
                        float credibilityMultiplier = reputationQuery.getCredibilityMultiplier(
                                receiver.getUUID(), session.getInitiatorId());
                        float adjustedWeight = baseWeight * credibilityMultiplier;

                        KnowledgeEntry hearsayEntry = KnowledgeEntry.fromHearsay(
                                sourceEntry,
                                session.getInitiatorId(),
                                currentTick,
                                adjustedWeight);

                        // Knowledge copy: write to receiver's store if not already known.
                        // admit() now applies corroboration logic on the dedupe branch so a
                        // second independent source corroborating the same fact bumps confidence
                        // rather than silently no-oping. The session must close regardless of the
                        // outcome — leaving it open would permanently lock both participants out
                        // of future gossip.
                        receiver.getKnowledgeStore().admit(hearsayEntry);
                        gossipSessionRegistry.completeSession(sessionId);
                    });
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
     * Selects the single most-important shareable entry to share during a gossip exchange.
     * Prefers first-hand entries to hearsay (lower hop = more trustworthy), then falls
     * back to the highest weight for tiebreaking.
     */
    @VisibleForTesting
    protected static KnowledgeEntry selectBestEntry(List<KnowledgeEntry> shareable) {
        KnowledgeEntry best = shareable.getFirst();
        for (KnowledgeEntry candidate : shareable) {
            if (candidate.getHop() < best.getHop()) {
                best = candidate;
            } else if (candidate.getHop() == best.getHop() && candidate.getWeight() > best.getWeight()) {
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Finds the nearest villager who: (a) is not the initiator and (b) is not already in a
     * gossip session. The initiator does not check whether the candidate already knows the fact
     * — reading another villager's private knowledge store violates the Phase 4 anti-telepathy
     * rule. Gossip flows blind; the receiver dedupes on receipt via
     * {@link VillagerKnowledgeStore#admit}.
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

    /**
     * Builds a minimal ambient {@link DialogueContext} from the villager's profession and
     * knowledge store: a terse persona plus up to five knowledge seeds, AMBIENT priority, no
     * specific stimulus. Only the LIVE provider reads this; PACKS samples a pre-generated line.
     */
    private static DialogueContext buildAmbientContext(BaseVillager villager) {
        // TODO:CONFIRM & TODO:LLM; this is hard coded
        // Provisional was-cured injection — the LLM rework (docs/working/llm-rework.md) will fold
        // this into the persona token bundle rather than appending it here.
        DialogueContext.DialogueContextBuilder builder = DialogueContext.builder()
                .personaCard("Profession: %s. Personality: diligent, sociable.%s"
                        .formatted(villager.getVillagerData().getProfession().toString(),
                                VillagerWasCuredAttachment.wasCured(villager) ? " Survived being a zombie and was cured." : ""))
                .priority(DialoguePriority.AMBIENT);

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
