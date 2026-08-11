package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.infrastructure.minecraft.items.TotemMode;
import dev.breezes.settlements.infrastructure.minecraft.items.VillagerTotemItem;
import dev.breezes.settlements.presentation.ui.framework.UITheme;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.InputIcons;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Held-item HUD content for the villager totem: current mode with no target, a quiet "already that type" for
 * an ineligible one, or the conversion outcome, gesture, and inventory-loss warning for an eligible one.
 */
@ClientSide
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class TotemHeldItemHudProvider implements HeldItemHudProvider {

    private static final int PRIORITY = 0;

    private static final String MODE_KEY = "ui.settlements.totem_hud.mode";
    private static final String CYCLE_KEY = "ui.settlements.totem_hud.cycle";
    private static final String ALREADY_KEY = "ui.settlements.totem_hud.already";
    private static final String CONVERT_TO_KEY = "ui.settlements.totem_hud.convert_to";
    private static final String GESTURE_KEY = "ui.settlements.totem_hud.gesture";
    private static final String ITEMS_LOST_KEY = "ui.settlements.totem_hud.items_lost";

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Optional<HudContent> contentFor(@Nonnull HudFrame frame) {
        ItemStack heldStack = frame.mainHandStack();
        if (!(heldStack.getItem() instanceof VillagerTotemItem)) {
            return Optional.empty();
        }

        TotemMode mode = VillagerTotemItem.getMode(heldStack);
        Optional<Villager> target = crosshairVillager(frame);
        boolean isEligible = target.map(villager -> VillagerTotemItem.isEligibleForConversion(villager, mode)).orElse(false);
        TotemHudState state = TotemHudState.resolve(target.isPresent(), isEligible);

        return Optional.of(buildContent(state, mode));
    }

    private static HudContent buildContent(@Nonnull TotemHudState state, @Nonnull TotemMode mode) {
        var builder = HudContent.builder().accentColor(UITheme.DEFAULT.successColor());

        switch (state) {
            case NO_TARGET -> builder.headline(namedLine(MODE_KEY, mode))
                    .detail(Component.translatable(CYCLE_KEY, InputIcons.rightClick()));
            case INELIGIBLE_TARGET -> builder.headline(namedLine(ALREADY_KEY, mode));
            case ELIGIBLE_TARGET -> builder.headline(namedLine(CONVERT_TO_KEY, mode))
                    .detail(Component.translatable(GESTURE_KEY, InputIcons.sneak(), InputIcons.rightClick()))
                    .detail(Component.translatable(ITEMS_LOST_KEY));
        }

        return builder.build();
    }

    private static Component namedLine(@Nonnull String translationKey, @Nonnull TotemMode mode) {
        return Component.translatable(translationKey, Component.translatable(mode.getTranslationKey()));
    }

    private static Optional<Villager> crosshairVillager(@Nonnull HudFrame frame) {
        Entity target = frame.aimedAtEntity();
        return target instanceof Villager villager ? Optional.of(villager) : Optional.empty();
    }

}
