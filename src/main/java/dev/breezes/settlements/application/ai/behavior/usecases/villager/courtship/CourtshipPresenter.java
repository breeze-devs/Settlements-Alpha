package dev.breezes.settlements.application.ai.behavior.usecases.villager.courtship;

import dev.breezes.settlements.application.ai.courtship.ChoreographyTimeline;
import dev.breezes.settlements.application.ai.courtship.CourtshipBeat;
import dev.breezes.settlements.application.ai.courtship.CourtshipChoreographyLibrary;
import dev.breezes.settlements.application.ai.courtship.CourtshipCloseReason;
import dev.breezes.settlements.application.ai.courtship.CourtshipRole;
import dev.breezes.settlements.application.ai.courtship.CourtshipSession;
import dev.breezes.settlements.application.ui.bubble.BubbleChannel;
import dev.breezes.settlements.application.ui.bubble.BubbleCommand;
import dev.breezes.settlements.application.ui.bubble.BubbleMessage;
import dev.breezes.settlements.application.ui.bubble.BubbleSegment;
import dev.breezes.settlements.application.ui.bubble.VillagerBubbleService;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class CourtshipPresenter {

    private static final ClockTicks APPROACH_BUBBLE_TTL = ClockTicks.seconds(10);
    private static final ClockTicks BEAT_BUBBLE_TTL = ClockTicks.seconds(4);
    private static final ClockTicks BIRTH_BUBBLE_TTL = ClockTicks.seconds(3);
    private static final ClockTicks FAILURE_BUBBLE_TTL = ClockTicks.seconds(4);
    private static final String COURTSHIP_SOURCE = "courtship";

    private static final BubbleSegment POPPY_ICON = BubbleSegment.Item.iconOnly(BuiltInRegistries.ITEM.getKey(Items.POPPY));
    private static final BubbleSegment BED_ICON = BubbleSegment.Item.iconOnly(BuiltInRegistries.ITEM.getKey(Items.RED_BED));
    private static final BubbleSegment CLOCK_ICON = BubbleSegment.Item.iconOnly(BuiltInRegistries.ITEM.getKey(Items.CLOCK));
    private static final BubbleSegment BREAD_ICON = BubbleSegment.Item.iconOnly(BuiltInRegistries.ITEM.getKey(Items.BREAD));

    // A red cross appended after the reason icon, reading as "no <icon>" / failure. Mirrors the trade walk-away marker.
    private static final BubbleSegment FAILURE_CROSS = BubbleSegment.Text.builder()
            .literal("✖")
            .color(ChatFormatting.RED)
            .bold(true)
            .scale(0.9F)
            .build();
    // Stand-in icon for "partner vanished" since no vanilla item depicts a missing villager cleanly.
    private static final BubbleSegment PARTNER_GONE_MARK = BubbleSegment.Text.builder()
            .literal("?")
            .color(ChatFormatting.WHITE)
            .bold(true)
            .scale(1.0F)
            .build();

    private final VillagerBubbleService villagerBubbleService;
    private final CourtshipChoreographyLibrary choreographyLibrary;

    public void presentApproach(@Nonnull CourtshipSession session, @Nonnull BaseVillager self) {
        self.setHeldItem(new ItemStack(Items.POPPY));
        upsert(self, session, BubbleMessage.builder()
                .priority(10)
                .ttl(APPROACH_BUBBLE_TTL)
                .sourceType(COURTSHIP_SOURCE)
                .segments(List.of(POPPY_ICON))
                .build());
    }

    /**
     * Dispatches role-specific animation and sound for a single beat.
     * Role is derived from the session so callers do not need to compute or pass it.
     * Heart particles fire on every beat regardless of role to keep the visual warm.
     */
    public void presentBeat(@Nonnull CourtshipSession session, @Nonnull BaseVillager self, int beatIndex) {
        CourtshipRole role = CourtshipRole.of(self.getUUID(), session.partnerOf(self.getUUID()));
        ChoreographyTimeline timeline = this.choreographyLibrary.get(session.getChoreographyId());
        CourtshipBeat beat = timeline.beatAt(beatIndex);

        self.triggerMotion(beat.motionFor(role));
        Location.fromEntity(self, true).displayParticles(ParticleTypes.HEART, 3, 0.3, 0.3, 0.3, 0.01);

        Optional.ofNullable(beat.sound())
                .ifPresent(soundEvent -> Location.fromEntity(self, true).playSound(soundEvent, 0.5f, 1.1f, SoundSource.NEUTRAL));

        upsert(self, session, BubbleMessage.builder()
                .priority(10)
                .ttl(BEAT_BUBBLE_TTL)
                .sourceType(COURTSHIP_SOURCE)
                .segments(List.of(POPPY_ICON))
                .build());
    }

    /**
     * Culmination: heart burst at midpoint, happy particles at each parent, celebrate sound, love puff event.
     * Called once by CourtshipInitiateBehavior immediately before spawning children.
     */
    public void presentBirth(@Nonnull CourtshipSession session,
                             @Nonnull BaseVillager presenter,
                             @Nonnull BaseVillager receiver) {
        Location midpoint = Location.of((presenter.getX() + receiver.getX()) / 2.0, (presenter.getEyeY() + receiver.getEyeY()) / 2.0,
                (presenter.getZ() + receiver.getZ()) / 2.0, presenter.level());
        midpoint.displayParticles(ParticleTypes.HEART, 12, 0.5, 0.5, 0.5, 0.02);
        midpoint.displayParticles(ParticleTypes.HAPPY_VILLAGER, 8, 0.5, 0.5, 0.5, 0.01);
        midpoint.playSound(SoundEvents.VILLAGER_CELEBRATE, 1.0f, 1.0f, SoundSource.NEUTRAL);

        upsert(presenter, session, BubbleMessage.builder()
                .priority(15)
                .ttl(BIRTH_BUBBLE_TTL)
                .sourceType(COURTSHIP_SOURCE)
                .segments(List.of(POPPY_ICON))
                .build());
        upsert(receiver, session, BubbleMessage.builder()
                .priority(15)
                .ttl(BIRTH_BUBBLE_TTL)
                .sourceType(COURTSHIP_SOURCE)
                .segments(List.of(POPPY_ICON))
                .build());
    }

    /**
     * Abort: anger puff event + NO sound, plus a bubble depicting why the courtship failed.
     * <p>
     * The reason bubble uses a distinct owner key from the in-progress poppy bubble so that the
     * subsequent {@link #presentEnd} cleanup (which targets the in-progress key) leaves it standing
     * to live out its short TTL rather than wiping it the same tick.
     */
    public void presentAbort(@Nonnull CourtshipSession session, @Nonnull BaseVillager self, @Nonnull CourtshipCloseReason reason) {
        Location location = Location.fromEntity(self, true);
        location.displayParticles(ParticleTypes.ANGRY_VILLAGER, 3, 0.2, 0.2, 0.2, 0.1);
        location.playSound(SoundEvents.VILLAGER_NO, 0.8f, 1.0f, SoundSource.NEUTRAL);

        List<BubbleSegment> reasonSegments = failureSegments(reason);
        if (reasonSegments.isEmpty()) {
            // Non-presentable reason (e.g. chunk unload): just clear the in-progress bubble, show nothing.
            removeByOwner(self, session, self.level().getGameTime());
            return;
        }

        // Upserting onto the single-slot BEHAVIOR channel evicts the in-progress poppy bubble for us.
        upsertResult(self, session, BubbleMessage.builder()
                .priority(15)
                .ttl(FAILURE_BUBBLE_TTL)
                .sourceType(COURTSHIP_SOURCE)
                .segments(reasonSegments)
                .build());
    }

    /**
     * Maps a close reason to the icons that explain it: a reason-specific icon followed by a red cross.
     * Returns an empty list for reasons that should not surface a bubble (success or silent cancellation).
     */
    private static List<BubbleSegment> failureSegments(@Nonnull CourtshipCloseReason reason) {
        return switch (reason) {
            case REJECTED_CHARISMA -> List.of(POPPY_ICON, FAILURE_CROSS);
            case ABORTED_NO_BED -> List.of(BED_ICON, FAILURE_CROSS);
            case TIMEOUT -> List.of(CLOCK_ICON, FAILURE_CROSS);
            case ABORTED_WILLINGNESS -> List.of(BREAD_ICON, FAILURE_CROSS);
            case ABORTED_PARTNER_GONE -> List.of(PARTNER_GONE_MARK, FAILURE_CROSS);
            case ABORTED_SPAWN_FAILED -> List.of(FAILURE_CROSS);
            case COMPLETED, EXTERNAL_CANCEL -> List.of();
        };
    }

    /**
     * End cleanup: clear held poppy, remove bubble. Called by both behaviors in onBehaviorStop.
     */
    public void presentEnd(@Nonnull BaseVillager self, @Nonnull UUID sessionId, long gameTime) {
        self.clearHeldItem();
        self.setMotion(AnimationArchetype.IDLE);
        this.villagerBubbleService.applyCommand(self, new BubbleCommand.RemoveByOwner(BubbleChannel.BEHAVIOR, ownerKey(sessionId)), gameTime);
    }

    private void upsert(@Nonnull BaseVillager self,
                        @Nonnull CourtshipSession session,
                        @Nonnull BubbleMessage message) {
        this.villagerBubbleService.applyCommand(self, new BubbleCommand.Upsert(BubbleChannel.BEHAVIOR, ownerKey(session.getSessionId()), message),
                self.level().getGameTime());
    }

    private void upsertResult(@Nonnull BaseVillager self,
                              @Nonnull CourtshipSession session,
                              @Nonnull BubbleMessage message) {
        this.villagerBubbleService.applyCommand(self, new BubbleCommand.Upsert(BubbleChannel.BEHAVIOR, resultOwnerKey(session.getSessionId()), message),
                self.level().getGameTime());
    }

    private void removeByOwner(@Nonnull BaseVillager self,
                               @Nonnull CourtshipSession session,
                               long gameTime) {
        this.villagerBubbleService.applyCommand(self, new BubbleCommand.RemoveByOwner(BubbleChannel.BEHAVIOR, ownerKey(session.getSessionId())), gameTime);
    }

    private static String ownerKey(@Nonnull UUID sessionId) {
        return "courtship-" + sessionId;
    }

    private static String resultOwnerKey(@Nonnull UUID sessionId) {
        return "courtship-result-" + sessionId;
    }

}
