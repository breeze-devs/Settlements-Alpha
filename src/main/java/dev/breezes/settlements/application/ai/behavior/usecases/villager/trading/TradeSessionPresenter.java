package dev.breezes.settlements.application.ai.behavior.usecases.villager.trading;

import dev.breezes.settlements.application.ai.trading.TradeSession;
import dev.breezes.settlements.application.ui.bubble.BubbleChannel;
import dev.breezes.settlements.application.ui.bubble.BubbleCommand;
import dev.breezes.settlements.application.ui.bubble.BubbleMessage;
import dev.breezes.settlements.application.ui.bubble.BubbleSegment;
import dev.breezes.settlements.application.ui.bubble.TradeMarker;
import dev.breezes.settlements.application.ui.bubble.VillagerBubbleService;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class TradeSessionPresenter {

    private static final Ticks BUBBLE_TTL = Ticks.seconds(2);
    private static final String TRADE_SOURCE = "trade";

    private final VillagerBubbleService villagerBubbleService;

    public void presentOpeningOffer(@Nonnull TradeSession session, @Nonnull BaseVillager self) {
        TradeRole role = this.roleOf(session, self);
        TradeMarker marker = role == TradeRole.SELLER ? TradeMarker.UP : null;
        this.upsert(self, session, this.createMessage(session, role, marker));
        Location.fromEntity(self, true).playSound(SoundEvents.VILLAGER_TRADE, 0.8f, 1.0f, SoundSource.NEUTRAL);
    }

    public void presentNegotiationUpdate(@Nonnull TradeSession session, @Nonnull BaseVillager self) {
        TradeRole role = this.roleOf(session, self);
        TradeMarker marker = switch (role) {
            case BUYER -> TradeMarker.UP;
            case SELLER -> TradeMarker.DOWN;
        };

        this.upsert(self, session, this.createMessage(session, role, marker));

        Location.fromEntity(self, true).playSound(SoundEvents.VILLAGER_TRADE, 0.8f, 1.0f, SoundSource.NEUTRAL);
    }

    public void presentDeal(@Nonnull TradeSession session, @Nonnull BaseVillager self) {
        List<BubbleSegment> segments = this.createBaseSegments(session, session.getBuyerOffer());
        segments.add(TradeMarker.CHECK.asSegment());
        this.upsert(self, session, this.createMessage(segments));
        Location selfHead = Location.fromEntity(self, true);
        selfHead.displayParticles(ParticleTypes.HAPPY_VILLAGER, 6, 0.2, 0.2, 0.2, 0.01);
        selfHead.playSound(SoundEvents.VILLAGER_CELEBRATE, 0.8f, 1.0f, SoundSource.NEUTRAL);
    }

    public void presentWalkAway(@Nonnull TradeSession session, @Nonnull BaseVillager self) {
        TradeRole role = this.roleOf(session, self);
        this.upsert(self, session, this.createMessage(session, role, TradeMarker.CROSS));
        Location selfHead = Location.fromEntity(self, true);
        selfHead.displayParticles(ParticleTypes.ANGRY_VILLAGER, 6, 0.2, 0.2, 0.2, 0.01);
        selfHead.playSound(SoundEvents.VILLAGER_NO, 0.8f, 1.0f, SoundSource.NEUTRAL);
    }

    private void upsert(@Nonnull BaseVillager self, @Nonnull TradeSession session, @Nonnull BubbleMessage message) {
        this.villagerBubbleService.applyCommand(
                self,
                new BubbleCommand.Upsert(BubbleChannel.BEHAVIOR, ownerKey(session), message),
                self.level().getGameTime());
    }

    private BubbleMessage createMessage(@Nonnull TradeSession session,
                                        @Nonnull TradeRole role,
                                        @Nullable TradeMarker marker) {
        int emeraldCount = switch (role) {
            case BUYER -> session.getBuyerOffer();
            case SELLER -> session.getSellerAsk();
        };

        List<BubbleSegment> segments = this.createBaseSegments(session, emeraldCount);
        if (marker != null) {
            segments.add(marker.asSegment());
        }

        return this.createMessage(segments);
    }

    private List<BubbleSegment> createBaseSegments(@Nonnull TradeSession session, int emeraldCount) {
        List<BubbleSegment> segments = new ArrayList<>();
        segments.add(BubbleSegment.Item.builder()
                .itemId(BuiltInRegistries.ITEM.getKey(session.getMatchedItem()))
                .count(session.getBundleSize())
                .build());
        segments.add(BubbleSegment.Item.builder()
                .itemId(BuiltInRegistries.ITEM.getKey(Items.EMERALD))
                .count(emeraldCount)
                .build());

        return segments;
    }

    private BubbleMessage createMessage(@Nonnull List<BubbleSegment> segments) {
        return BubbleMessage.builder()
                .priority(10)
                .ttl(BUBBLE_TTL)
                .sourceType(TRADE_SOURCE)
                .segments(segments)
                .build();
    }

    private TradeRole roleOf(@Nonnull TradeSession session, @Nonnull BaseVillager self) {
        if (self.getUUID().equals(session.getBuyerId())) {
            return TradeRole.BUYER;
        }
        if (self.getUUID().equals(session.getSellerId())) {
            return TradeRole.SELLER;
        }
        throw new IllegalArgumentException("Villager is not a participant in trade session " + session.getSessionId());
    }

    private static String ownerKey(@Nonnull TradeSession session) {
        return "trade-session-" + session.getSessionId();
    }

    private enum TradeRole {
        BUYER,
        SELLER,
    }

}
