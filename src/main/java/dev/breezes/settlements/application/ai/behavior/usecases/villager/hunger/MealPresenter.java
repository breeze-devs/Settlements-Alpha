package dev.breezes.settlements.application.ai.behavior.usecases.villager.hunger;

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
 * The one thing a meal shows a player that the eating animation cannot: that this villager sat down
 * to a meal with nothing to eat.
 * <p>
 * A meal the villager can actually eat needs no presentation of its own -- the held item, chewing,
 * and crumb particles already read as eating. Only going without has no natural silhouette, so it
 * gets an explicit one.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class MealPresenter {

    private static final ClockTicks HUNGRY_BUBBLE_TTL = ClockTicks.seconds(8);
    private static final String MEAL_SOURCE = "meal";

    private static final BubbleSegment QUERY_GLYPH = BubbleSegment.Text.builder()
            .literal("?")
            .color(ChatFormatting.RED)
            .bold(true)
            .scale(0.9F)
            .build();

    private final VillagerBubbleService villagerBubbleService;

    /**
     * Shows the villager asking after a meal it does not have: a bread icon standing for food in
     * general rather than for bread specifically, and a question mark for the part it is missing.
     */
    public void presentMissedMeal(@Nonnull BaseVillager villager, long gameTime) {
        BubbleSegment breadIcon = BubbleSegment.Item.iconOnly(BuiltInRegistries.ITEM.getKey(Items.BREAD));

        this.villagerBubbleService.applyCommand(villager,
                new BubbleCommand.Upsert(BubbleChannel.BEHAVIOR, ownerKey(villager), BubbleMessage.builder()
                        .priority(10)
                        .ttl(HUNGRY_BUBBLE_TTL)
                        .sourceType(MEAL_SOURCE)
                        .segments(List.of(breadIcon, QUERY_GLYPH))
                        .build()),
                gameTime);

        Location villagerHead = Location.fromEntity(villager, true);
        villagerHead.displayParticles(ParticleTypes.ANGRY_VILLAGER, 6, 0.3, 0.3, 0.3, 0.01);
        villagerHead.playSound(SoundEvents.VILLAGER_NO, 0.6f, 1.0f, SoundSource.NEUTRAL);
    }

    private static String ownerKey(@Nonnull BaseVillager villager) {
        return "meal-" + villager.getUUID();
    }

}
