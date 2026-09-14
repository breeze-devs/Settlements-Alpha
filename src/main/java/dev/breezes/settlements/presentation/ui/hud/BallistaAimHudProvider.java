package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.domain.ballista.BallistaAim;
import dev.breezes.settlements.infrastructure.minecraft.blocks.ballista.BallistaAimController;
import dev.breezes.settlements.infrastructure.minecraft.blocks.ballista.BallistaBlockEntity;
import dev.breezes.settlements.presentation.ui.framework.UITheme;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import dev.breezes.settlements.shared.util.InputIcons;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Aim-mode HUD content, shown while the player aims a ballista, regardless of where they look.
 */
@ClientSide
@ClientScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class BallistaAimHudProvider implements ModeHudProvider {

    private static final int PRIORITY = 0;

    private static final int FULL_TURN_DEGREES = 360;

    /**
     * Yaw is 0 facing south.
     */
    private static final int SOUTH_BEARING_DEGREES = 180;

    private static final String ROTATION_KEY = "ui.settlements.ballista_hud.rotation";
    private static final String ELEVATION_KEY = "ui.settlements.ballista_hud.elevation";
    private static final String DEGREES_KEY = "ui.settlements.ballista_hud.degrees";

    private static final String ADJUST_KEY = "ui.settlements.ballista_hud.adjust";
    private static final String SWITCH_TO_ROTATION_KEY = "ui.settlements.ballista_hud.switch_to_rotation";
    private static final String SWITCH_TO_ELEVATION_KEY = "ui.settlements.ballista_hud.switch_to_elevation";
    private static final String DONE_KEY = "ui.settlements.ballista_hud.done";
    private static final String JOINED_KEY = "ui.settlements.ballista_hud.joined";

    private final BallistaAimController aimController;

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Optional<HudContent> contentFor(@Nonnull HudFrame frame) {
        BallistaBlockEntity ballista = this.aimController.aimedBallista();
        if (ballista == null) {
            return Optional.empty();
        }

        BallistaAim aim = ballista.getAim();
        return Optional.of(switch (this.aimController.selectedAxis()) {
            case ROTATION -> content(ROTATION_KEY, rotationText(aim.getIntentYaw()), SWITCH_TO_ELEVATION_KEY);
            case ELEVATION -> content(ELEVATION_KEY, elevationText(aim.getIntentPitch()), SWITCH_TO_ROTATION_KEY);
        });
    }

    private static HudContent content(@Nonnull String axisKey,
                                      @Nonnull Component angle,
                                      @Nonnull String switchKey) {
        return HudContent.builder()
                .accentColor(UITheme.DEFAULT.textColor())
                .headline(Component.translatable(axisKey, angle))
                .detail(Component.translatable(JOINED_KEY,
                        Component.translatable(ADJUST_KEY, InputIcons.scroll()),
                        Component.translatable(switchKey, InputIcons.rightClick())))
                .detail(Component.translatable(DONE_KEY, InputIcons.sneak()))
                .build();
    }

    private static Component rotationText(float yaw) {
        return Component.translatable(DEGREES_KEY, Integer.toString(compassBearing(yaw)));
    }

    /**
     * Signed, so a muzzle raised reads as such at a glance, and a level one reads as plain zero.
     */
    private static Component elevationText(float pitch) {
        int degrees = Math.round(pitch);
        String signed = degrees > 0 ? "+" + degrees : Integer.toString(degrees);
        return Component.translatable(DEGREES_KEY, signed);
    }

    /**
     * The whole-degree compass bearing of a yaw: 0 north, 90 east, in [0, 360).
     */
    @VisibleForTesting
    static int compassBearing(float yaw) {
        return Math.floorMod(Math.round(yaw) + SOUTH_BEARING_DEGREES, FULL_TURN_DEGREES);
    }

}
