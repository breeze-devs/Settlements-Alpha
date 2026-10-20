package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.application.settlement.bell.SettlementBellRules;
import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.domain.settlement.naming.SettlementRename;
import dev.breezes.settlements.infrastructure.minecraft.attachments.SettlementClientView;
import dev.breezes.settlements.infrastructure.minecraft.attachments.SettlementDirectory;
import dev.breezes.settlements.infrastructure.minecraft.attachments.SettlementSummary;
import dev.breezes.settlements.presentation.ui.framework.UITheme;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.InputIcons;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Look-target HUD content for a bell: the settlement it lets the player into, or what founding one there would take,
 * and the effect of the named name tag the player holds.
 */
@ClientSide
@ClientScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SettlementBellLookTargetHudProvider implements LookTargetHudProvider {

    private static final int PRIORITY = 0;

    private static final String NEEDS_ONE_KEY = "ui.settlements.bell_hud.needs_residents.one";
    private static final String NEEDS_MANY_KEY = "ui.settlements.bell_hud.needs_residents.many";
    private static final String RECOVERING_KEY = "ui.settlements.bell_hud.recovering";
    private static final String RENAME_HINT_KEY = "ui.settlements.bell_hud.rename_hint";
    private static final String RENAME_KEY = "ui.settlements.bell_hud.rename";
    private static final String NAME_TAKEN_KEY = "ui.settlements.bell_hud.name_taken";
    private static final String ALREADY_NAMED_KEY = "ui.settlements.bell_hud.already_named";

    private static final String NO_SETTLEMENT_KEY = "ui.settlements.bell_hud.no_settlement";
    private static final String FOUNDING_CUE_KEY = "ui.settlements.bell_hud.founding_cue";
    private static final String FOUNDING_EXCLUDED_KEY = "ui.settlements.bell_hud.founding_excluded";

    /**
     * The rename last previewed, or null before the first.
     * The hover is asked for its content every frame, and a preview reads every settlement name in the dimension.
     */
    @Nullable
    private RenamePreview lastRenamePreview;

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

        Level level = frame.localPlayer().level();
        BlockPos bell = hit.getBlockPos();
        if (!SettlementBellRules.isBell(level.getBlockState(bell))) {
            return Optional.empty();
        }

        // Before the server's first sync there is nothing to show
        Optional<SettlementDirectory> directory = SettlementClientView.directory(level);
        if (directory.isEmpty()) {
            return Optional.empty();
        }

        // A player who may not build cannot use a name tag on a bell, so is not taught to rename
        boolean mayName = frame.localPlayer().mayBuild();
        String tagName = mayName ? SettlementBellRules.nameTagName(frame.mainHandStack()).orElse(null) : null;
        Optional<UUID> claimant = SettlementClientView.claimAt(level, bell);
        if (claimant.isPresent()) {
            // A claim whose summary has not arrived yet shows nothing, as a missing directory does
            return directory.get().find(claimant.get())
                    .map(summary -> inside(summary, directory.get(), mayName, tagName));
        }

        if (!directory.get().foundingAllowed()) {
            // The single line and nothing else, since nothing else about the bell applies there
            return Optional.of(HudContent.builder()
                    .accentColor(UITheme.DEFAULT.subtleTextColor())
                    .headline(Component.translatable(FOUNDING_EXCLUDED_KEY))
                    .build());
        }

        return Optional.of(HudContent.builder()
                .accentColor(UITheme.DEFAULT.textColor())
                .headline(Component.translatable(NO_SETTLEMENT_KEY))
                .detail(Component.translatable(FOUNDING_CUE_KEY))
                .build());
    }

    private HudContent inside(SettlementSummary summary,
                              SettlementDirectory directory,
                              boolean mayName,
                              @Nullable String tagName) {
        HudContent.HudContentBuilder content = HudContent.builder()
                .accentColor(UITheme.DEFAULT.textColor())
                .headline(Component.literal(summary.name()));
        SettlementSummary.Fading fading = summary.fading();
        if (fading != null) {
            content.detail(SettlementStatusLine.of(fading));
            // The bell under the crosshair meets the bell condition, so residents are all a revival can still lack, and
            // with enough of them the next health visit revives the settlement
            int needed = fading.minimumResidents() - fading.livingMembers();
            content.detail(needed <= 0
                    ? Component.translatable(RECOVERING_KEY).withColor(UITheme.DEFAULT.progressColor())
                    : needed == 1
                    ? Component.translatable(NEEDS_ONE_KEY)
                    : Component.translatable(NEEDS_MANY_KEY, needed));
        }

        if (!mayName) {
            return content.build();
        }
        if (tagName == null) {
            return content.detail(Component.translatable(RENAME_HINT_KEY)).build();
        }

        switch (this.renamePreview(directory, summary.name(), tagName)) {
            case SettlementRename.Renamed renamed -> {
                content.detail(Component.translatable(RENAME_KEY, InputIcons.rightClick(), renamed.previousName(),
                        renamed.name()));
                if (!renamed.name().equals(tagName)) {
                    content.detail(Component.translatable(NAME_TAKEN_KEY, tagName));
                }
            }
            // The click rings the bell, so no gesture is offered
            case SettlementRename.Unchanged unchanged ->
                    content.detail(Component.translatable(ALREADY_NAMED_KEY, unchanged.name()));
        }
        return content.build();
    }

    private SettlementRename renamePreview(SettlementDirectory directory, String currentName, String tagName) {
        RenamePreview last = this.lastRenamePreview;
        // The directory is compared by identity: a client's copy is replaced on each sync and never changed in place
        if (last == null
                || last.directory() != directory
                || !last.currentName().equals(currentName)
                || !last.tagName().equals(tagName)) {
            last = new RenamePreview(directory, currentName, tagName,
                    SettlementRename.of(currentName, tagName, SettlementClientView.names(directory)));
            this.lastRenamePreview = last;
        }
        return last.rename();
    }

    /**
     * A previewed rename and the inputs it was computed from.
     */
    private record RenamePreview(SettlementDirectory directory,
                                 String currentName,
                                 String tagName,
                                 SettlementRename rename) {
    }

}
