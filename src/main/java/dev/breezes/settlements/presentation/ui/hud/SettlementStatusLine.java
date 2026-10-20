package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.infrastructure.minecraft.attachments.SettlementSummary;
import dev.breezes.settlements.presentation.ui.framework.UITheme;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nonnull;

/**
 * The line under a settlement's name that reads its status, shared by every surface that names a settlement.
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SettlementStatusLine {

    private static final String FALLING_ONE_KEY = "ui.settlements.settlement_status.falling.one";
    private static final String FALLING_MANY_KEY = "ui.settlements.settlement_status.falling.many";

    /**
     * Returns the status of a fading settlement, in the error color.
     */
    static MutableComponent of(@Nonnull SettlementSummary.Fading fading) {
        MutableComponent line = fading.daysLeft() == 1
                ? Component.translatable(FALLING_ONE_KEY)
                : Component.translatable(FALLING_MANY_KEY, fading.daysLeft());
        return line.withColor(UITheme.DEFAULT.errorColor());
    }

}
