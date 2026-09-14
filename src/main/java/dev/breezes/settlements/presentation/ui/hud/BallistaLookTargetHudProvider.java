package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.infrastructure.minecraft.blocks.ballista.BallistaBlockEntity;
import dev.breezes.settlements.infrastructure.minecraft.blocks.ballista.BallistaHudState;
import dev.breezes.settlements.presentation.ui.framework.UITheme;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.InputIcons;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Look-target HUD content for a placed ballista.
 */
@ClientSide
@ClientScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class BallistaLookTargetHudProvider implements LookTargetHudProvider {

    private static final int PRIORITY = 0;

    private static final String UNWOUND_KEY = "ui.settlements.ballista_hud.unwound";
    private static final String WINDING_KEY = "ui.settlements.ballista_hud.winding";
    private static final String COCKED_EMPTY_KEY = "ui.settlements.ballista_hud.cocked_empty";
    private static final String LOADED_KEY = "ui.settlements.ballista_hud.loaded";
    private static final String FIRING_KEY = "ui.settlements.ballista_hud.firing";

    private static final String BOLT_NEEDED_KEY = "ui.settlements.ballista_hud.bolt_needed";

    private static final String WIND_KEY = "ui.settlements.ballista_hud.wind";
    private static final String LOAD_KEY = "ui.settlements.ballista_hud.load";
    private static final String UNLOAD_KEY = "ui.settlements.ballista_hud.unload";
    private static final String AIM_KEY = "ui.settlements.ballista_hud.aim";

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Optional<HudContent> contentFor(@Nonnull HudFrame frame) {
        BlockHitResult hit = frame.blockHit();
        if (hit == null
                || !(frame.localPlayer().level().getBlockEntity(hit.getBlockPos()) instanceof BallistaBlockEntity ballista)) {
            return Optional.empty();
        }

        BallistaHudState state = ballista.hudStateForHeldItem(frame.mainHandStack());
        HudContent.HudContentBuilder content = HudContent.builder()
                .accentColor(UITheme.DEFAULT.textColor())
                .headline(Component.translatable(statusKey(state.status())));
        if (state.boltNeeded()) {
            content.detail(Component.translatable(BOLT_NEEDED_KEY));
        }
        if (state.use() != null) {
            content.detail(Component.translatable(useKey(state.use()), InputIcons.rightClick()));
        }
        if (state.aimOffered()) {
            content.detail(Component.translatable(AIM_KEY, InputIcons.sneak(), InputIcons.rightClick()));
        }
        return Optional.of(content.build());
    }

    private static String statusKey(@Nonnull BallistaHudState.Status status) {
        return switch (status) {
            case UNWOUND -> UNWOUND_KEY;
            case WINDING -> WINDING_KEY;
            case COCKED_EMPTY -> COCKED_EMPTY_KEY;
            case COCKED_LOADED -> LOADED_KEY;
            case FIRING -> FIRING_KEY;
        };
    }

    private static String useKey(@Nonnull BallistaHudState.Gesture gesture) {
        return switch (gesture) {
            case WIND -> WIND_KEY;
            case LOAD -> LOAD_KEY;
            case UNLOAD -> UNLOAD_KEY;
        };
    }

}
