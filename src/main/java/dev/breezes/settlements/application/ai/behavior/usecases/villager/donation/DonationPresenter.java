package dev.breezes.settlements.application.ai.behavior.usecases.villager.donation;

import dev.breezes.settlements.application.ui.bubble.BubbleChannel;
import dev.breezes.settlements.application.ui.bubble.BubbleCommand;
import dev.breezes.settlements.application.ui.bubble.BubbleMessage;
import dev.breezes.settlements.application.ui.bubble.BubbleSegment;
import dev.breezes.settlements.application.ui.bubble.VillagerBubbleService;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Cosmetic ceremony for a donation hand-off: dual bubbles (donor emerald count, recipient heart),
 * a small particle burst, and a sound. Server-authoritative — every bubble mutation goes through
 * {@link VillagerBubbleService} rather than being constructed client-side.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class DonationPresenter {

    private static final ClockTicks DONOR_BUBBLE_TTL = ClockTicks.seconds(6);
    private static final ClockTicks HEART_BUBBLE_TTL = ClockTicks.seconds(4);
    private static final String DONATION_SOURCE = "donation";

    private static final BubbleSegment HEART_GLYPH = BubbleSegment.Text.builder()
            .literal("❤")
            .color(ChatFormatting.RED)
            .bold(true)
            .scale(0.9F)
            .build();

    private final VillagerBubbleService villagerBubbleService;

    public void presentGift(@Nonnull BaseVillager donor, @Nonnull BaseVillager recipient, int amount, long gameTime) {
        BubbleSegment emeraldWithCount = BubbleSegment.Item.builder()
                .itemId(BuiltInRegistries.ITEM.getKey(Items.EMERALD))
                .count(amount)
                .build();

        this.villagerBubbleService.applyCommand(donor,
                new BubbleCommand.Upsert(BubbleChannel.BEHAVIOR, ownerKey(donor), BubbleMessage.builder()
                        .priority(10)
                        .ttl(DONOR_BUBBLE_TTL)
                        .sourceType(DONATION_SOURCE)
                        .segments(List.of(emeraldWithCount))
                        .build()),
                gameTime);

        // FLAVOR keeps this from evicting whatever BEHAVIOR bubble the recipient is already showing.
        this.villagerBubbleService.applyCommand(recipient,
                new BubbleCommand.Upsert(BubbleChannel.FLAVOR, ownerKey(donor), BubbleMessage.builder()
                        .priority(10)
                        .ttl(HEART_BUBBLE_TTL)
                        .sourceType(DONATION_SOURCE)
                        .segments(List.of(HEART_GLYPH))
                        .build()),
                gameTime);

        Location recipientLocation = Location.fromEntity(recipient, true);
        recipientLocation.displayParticles(ParticleTypes.HEART, 12, 0.5, 0.5, 0.5, 0.02);
        recipientLocation.displayParticles(ParticleTypes.HAPPY_VILLAGER, 8, 0.5, 0.5, 0.5, 0.01);
        recipientLocation.playSound(SoundEvents.VILLAGER_YES, 1.0f, 1.0f, SoundSource.NEUTRAL);
    }

    private static String ownerKey(@Nonnull BaseVillager donor) {
        return "donation-" + donor.getUUID();
    }

}
