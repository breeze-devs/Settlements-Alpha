package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation.CultivationLilyBlockEntity;
import dev.breezes.settlements.infrastructure.rendering.zone.CultivationZoneStyle;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import dev.breezes.settlements.shared.util.InputIcons;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Look-target HUD content for a placed Cultivation Lily.
 */
@ClientSide
@ClientScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class CultivationLilyLookTargetHudProvider implements LookTargetHudProvider {

    private static final int PRIORITY = 0;

    private static final String ZONE_KEY = "ui.settlements.lily_hud.zone";
    private static final String ZONE_FILTERED_KEY = "ui.settlements.lily_hud.zone_filtered";

    private static final String FILTER_CHANGE_GESTURE_KEY = "ui.settlements.lily_hud.filter_change_gesture";
    private static final String FILTER_SET_GESTURE_KEY = "ui.settlements.lily_hud.filter_set_gesture";
    private static final String RESIZE_GESTURE_KEY = "ui.settlements.lily_hud.resize_gesture";

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Optional<HudContent> contentFor(@Nonnull HudFrame frame) {
        BlockHitResult hit = frame.blockHit();
        if (hit == null) {
            return Optional.empty();
        }

        BlockEntity blockEntity = frame.localPlayer().level().getBlockEntity(hit.getBlockPos());
        if (!(blockEntity instanceof CultivationLilyBlockEntity lily)) {
            return Optional.empty();
        }

        // The headline carries what the zone is; the details carry what can be done about it
        ResourceLocation filterDisplayItem = lily.getCropFilterDisplayItem();
        return Optional.of(HudContent.builder()
                .accentColor(CultivationZoneStyle.hudAccentColor(lily.isValid()))
                .headline(zoneLabel(filterDisplayItem))
                .detail(Component.translatable(filterGestureKey(filterDisplayItem), InputIcons.rightClick()))
                .detail(Component.translatable(RESIZE_GESTURE_KEY, InputIcons.sneak(), InputIcons.rightClick()))
                .build());
    }

    /**
     * Names the crop the zone is filtered to where it has one. An unfiltered zone reads as the bare name
     * rather than as a filter set to nothing: a filter narrows what may grow, so having none is the absence
     * of a decision rather than a decision worth a word.
     * <p>
     * The footprint is deliberately absent. Resizing cycles whichever axis the player faces, so a pair of
     * world-axis numbers cannot be matched to the gesture without a compass, where the zone wall states the
     * same extent in the frame the player is already standing in.
     */
    private static Component zoneLabel(@Nullable ResourceLocation filterDisplayItem) {
        if (filterDisplayItem == null) {
            return Component.translatable(ZONE_KEY);
        }

        Item displayItem = BuiltInRegistries.ITEM.get(filterDisplayItem);
        return Component.translatable(ZONE_FILTERED_KEY, Component.translatable(displayItem.getDescriptionId()));
    }

    /**
     * Which gestures are worth naming depends on the filter state: a filtered lily can be cleared or
     * replaced, an unfiltered one only set. Naming the gesture that does not apply would teach a rule the
     * player cannot act on here.
     */
    @VisibleForTesting
    static String filterGestureKey(@Nullable ResourceLocation filterDisplayItem) {
        return filterDisplayItem != null ? FILTER_CHANGE_GESTURE_KEY : FILTER_SET_GESTURE_KEY;
    }

}
