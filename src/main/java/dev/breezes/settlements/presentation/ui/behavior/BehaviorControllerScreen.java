package dev.breezes.settlements.presentation.ui.behavior;

import dev.breezes.settlements.application.ui.behavior.model.BehaviorControllerSnapshot;
import dev.breezes.settlements.application.ui.behavior.model.BehaviorRowSnapshot;
import dev.breezes.settlements.application.ui.behavior.model.PreconditionSummary;
import dev.breezes.settlements.application.ui.behavior.model.SchedulePhase;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ServerBoundCloseBehaviorControllerPacket;
import dev.breezes.settlements.presentation.ui.framework.Elements;
import dev.breezes.settlements.presentation.ui.framework.Insets;
import dev.breezes.settlements.presentation.ui.framework.LayoutScreen;
import dev.breezes.settlements.presentation.ui.framework.LinearLayout;
import dev.breezes.settlements.presentation.ui.framework.ScrollableList;
import dev.breezes.settlements.presentation.ui.framework.SizeConstraint;
import dev.breezes.settlements.presentation.ui.framework.StackLayout;
import dev.breezes.settlements.presentation.ui.framework.UIElement;
import dev.breezes.settlements.presentation.ui.framework.WidgetElement;
import lombok.Getter;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BehaviorControllerScreen extends LayoutScreen {

    private static final float PANEL_WIDTH_RATIO = 0.85F;
    private static final float PANEL_HEIGHT_RATIO = 0.80F;
    private static final int MIN_GUI_WIDTH = 360;
    private static final int MIN_GUI_HEIGHT = 240;
    private static final int MAX_GUI_WIDTH = 512;
    private static final int MAX_GUI_HEIGHT = 360;
    private static final int PANEL_MARGIN = 10;
    private static final int HEADER_HEIGHT = 42;
    private static final int FOOTER_HEIGHT = 24;
    private static final int ROW_HEIGHT = 24;

    private static final String TITLE_KEY = "ui.settlements.behavior.title";
    private static final String VILLAGER_KEY = "ui.settlements.behavior.villager";
    private static final String STAGE_KEY = "ui.settlements.behavior.stage";
    private static final String STAGE_NA_KEY = "ui.settlements.behavior.stage_na";
    private static final String SCHEDULE_LINE_KEY = "ui.settlements.behavior.schedule";
    private static final String COOLDOWN_KEY = "ui.settlements.behavior.cooldown";
    private static final String ENABLE_ALL_STUB_KEY = "ui.settlements.behavior.enable_all_stub";
    private static final String BACK_KEY = "ui.settlements.behavior.back";
    private static final String CONNECTION_STALE_KEY = "ui.settlements.behavior.connection_stale";

    private static final ItemStack SCHEDULE_REST_ICON = new ItemStack(Items.RED_BED);
    private static final ItemStack SCHEDULE_WORK_ICON = new ItemStack(Items.CRAFTING_TABLE);
    private static final ItemStack SCHEDULE_IDLE_ICON = new ItemStack(Items.MUSIC_DISC_13);
    private static final ItemStack SCHEDULE_MEET_ICON = new ItemStack(Items.BELL);

    @Getter
    private final long sessionId;
    private final BehaviorControllerClientState behaviorControllerClientState;
    private BehaviorControllerSnapshot snapshot;
    @Nullable
    private Component unavailableMessage;
    private final Map<ResourceLocation, ItemStack> rowIconCache;

    @Nullable
    private ScrollableList behaviorList;

    public BehaviorControllerScreen(long sessionId,
                                    @Nonnull BehaviorControllerSnapshot snapshot,
                                    @Nonnull BehaviorControllerClientState behaviorControllerClientState) {
        super(Component.translatable(TITLE_KEY));
        this.sessionId = sessionId;
        this.behaviorControllerClientState = behaviorControllerClientState;
        this.snapshot = snapshot;
        this.unavailableMessage = null;
        this.rowIconCache = new HashMap<>();
    }

    @Nonnull
    @Override
    protected UIElement buildRoot() {
        int panelW = getPanelWidth();
        int panelH = getPanelHeight();

        // Scrollable behavior list
        this.behaviorList = ScrollableList.builder()
                .rowHeight(ROW_HEIGHT)
                .rowFactory(this::buildBehaviorRows)
                .height(new SizeConstraint.Weighted(1))
                .width(SizeConstraint.FILL)
                .padding(Insets.symmetric(2, 10))
                .alternatingRowColors(theme.rowColor(), theme.rowAltColor())
                .build();
        behaviorList.rebuildRows();

        // Buttons
        Button backBtn = Button.builder(Component.translatable(BACK_KEY), b -> onClose())
                .bounds(0, 0, 60, 20).build();
        addRenderableWidget(backBtn);

        // Panel structure
        UIElement panel = LinearLayout.vertical()
                .width(SizeConstraint.fixed(panelW))
                .height(SizeConstraint.fixed(panelH))
                .backgroundColor(theme.panelColor())
                .child(buildHeader())
                .child(Elements.hLine(theme.borderDark()))
                .child(behaviorList)
                .child(buildStaleConnectionWarning())
                .child(buildActivityFooter())
                .child(Elements.hLine(theme.borderDark()))
                .child(LinearLayout.horizontal()
                        .height(SizeConstraint.fixed(FOOTER_HEIGHT))
                        .width(SizeConstraint.FILL)
                        .padding(Insets.symmetric(2, 12))
                        .child(new WidgetElement(backBtn))
                        .build())
                .build();

        // Stack for overlay
        return StackLayout.builder()
                .width(SizeConstraint.fixed(panelW))
                .height(SizeConstraint.fixed(panelH))
                .child(panel)
                .child(buildUnavailableOverlay())
                .build();
    }

    @Override
    public void tick() {
        super.tick();
        this.behaviorControllerClientState.tickHeartbeatIfNeeded(this.sessionId);
    }

    @Override
    public void removed() {
        if (this.sessionId > 0) {
            PacketDistributor.sendToServer(new ServerBoundCloseBehaviorControllerPacket(this.sessionId));
            this.behaviorControllerClientState.clearSession(this.sessionId);
        }
        super.removed();
    }

    public void applySnapshot(@Nonnull BehaviorControllerSnapshot snapshot) {
        this.snapshot = snapshot;
        this.unavailableMessage = null;
        if (this.behaviorList != null) {
            this.behaviorList.rebuildRows();
        }
        invalidateLayout();
    }

    public void markUnavailable(@Nonnull Component message) {
        this.unavailableMessage = message;
    }

    // ---- Section builders ----

    private UIElement buildHeader() {
        return LinearLayout.vertical()
                .height(SizeConstraint.fixed(HEADER_HEIGHT))
                .width(SizeConstraint.FILL)
                .padding(new Insets(6, 8, 0, 8))
                .child(Elements.centeredText(() -> Component.translatable(TITLE_KEY), theme.textColor()))
                .child(LinearLayout.horizontal()
                        .width(SizeConstraint.FILL)
                        .height(SizeConstraint.WRAP)
                        .padding(new Insets(4, 0, 0, 0))
                        .child(Elements.text(
                                () -> Component.translatable(VILLAGER_KEY, this.snapshot.villagerName()),
                                theme.subtleTextColor()))
                        .child(Elements.flexSpacer())
                        .child(buildScheduleIcons())
                        .build())
                .build();
    }

    private UIElement buildScheduleIcons() {
        return LinearLayout.horizontal()
                .gap(4)
                .width(SizeConstraint.WRAP)
                .height(SizeConstraint.WRAP)
                .child(scheduleIcon(SchedulePhase.REST, SCHEDULE_REST_ICON))
                .child(scheduleIcon(SchedulePhase.WORK, SCHEDULE_WORK_ICON))
                .child(scheduleIcon(SchedulePhase.IDLE, SCHEDULE_IDLE_ICON))
                .child(scheduleIcon(SchedulePhase.MEET, SCHEDULE_MEET_ICON))
                .build();
    }

    private UIElement scheduleIcon(@Nonnull SchedulePhase phase, @Nonnull ItemStack iconStack) {
        return Elements.itemIcon(
                () -> iconStack,
                () -> this.snapshot.scheduleBucket() == phase ? theme.successColor() : theme.borderLight(),
                null
        );
    }

    private List<UIElement> buildBehaviorRows() {
        return this.snapshot.rows().stream()
                .map(this::buildSingleRow)
                .toList();
    }

    private UIElement buildSingleRow(@Nonnull BehaviorRowSnapshot row) {
        Component behaviorName = buildBehaviorName(row);
        Component scheduleLine = buildScheduleLine(row);
        Component rightText = buildRightText(row);
        int rightColor = getRightColor(row);
        int borderColor = row.running() ? theme.successColor() : theme.borderLight();

        return LinearLayout.horizontal()
                .width(SizeConstraint.FILL)
                .height(SizeConstraint.fixed(ROW_HEIGHT - 1))
                .crossAxisAlignment(LinearLayout.CrossAxisAlignment.CENTER)
                .padding(Insets.symmetric(1, 2))
                .child(Elements.itemIcon(() -> resolveItemStack(row.iconItemId()), () -> borderColor, null))
                .child(LinearLayout.vertical()
                        .width(SizeConstraint.WRAP)
                        .height(SizeConstraint.WRAP)
                        .padding(new Insets(0, 0, 0, 2))
                        .child(Elements.text(() -> behaviorName, theme.textColor()))
                        .child(Elements.text(() -> scheduleLine, theme.subtleTextColor()))
                        .build())
                .child(Elements.flexSpacer())
                .child(Elements.text(() -> rightText, rightColor))
                .build();
    }

    private UIElement buildStaleConnectionWarning() {
        return Elements.custom(SizeConstraint.FILL, SizeConstraint.fixed(10), (graphics, bounds, mouseX, mouseY, partialTick) -> {
            boolean snapshotStale = this.behaviorControllerClientState.isSnapshotUpdateStale(this.sessionId);
            boolean heartbeatAckStale = this.behaviorControllerClientState.isHeartbeatAckStale(this.sessionId);
            if (!snapshotStale && !heartbeatAckStale) {
                return;
            }
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable(CONNECTION_STALE_KEY),
                    bounds.x() + bounds.width() / 2,
                    bounds.y(),
                    theme.errorColor()
            );
        });
    }

    private UIElement buildActivityFooter() {
        return Elements.text(() -> Component.literal(this.snapshot.rawActivityKey()), theme.subtleTextColor());
    }

    private UIElement buildUnavailableOverlay() {
        return Elements.custom(SizeConstraint.FILL, SizeConstraint.FILL, (graphics, bounds, mouseX, mouseY, partialTick) -> {
            if (this.unavailableMessage == null) {
                return;
            }
            int overlayLeft = bounds.x() + 8;
            int overlayRight = bounds.right() - 8;
            int overlayTop = bounds.y() + HEADER_HEIGHT + 8;
            int overlayBottom = bounds.bottom() - FOOTER_HEIGHT - 8;
            graphics.fill(overlayLeft, overlayTop, overlayRight, overlayBottom, theme.overlayColor());
            graphics.drawCenteredString(
                    this.font,
                    this.unavailableMessage,
                    bounds.x() + bounds.width() / 2,
                    bounds.y() + bounds.height() / 2 - 4,
                    theme.errorColor()
            );
        });
    }

    // ---- Data transformation (unchanged from original) ----

    private Component buildBehaviorName(@Nonnull BehaviorRowSnapshot row) {
        var name = Component.literal("#" + row.uiBehaviorIndex() + " ")
                .append(Component.translatable(row.displayNameKey()));
        if (row.displaySuffix() != null && !row.displaySuffix().isBlank()) {
            name = name.append(Component.literal(" " + row.displaySuffix()));
        }
        return name;
    }

    private Component buildScheduleLine(@Nonnull BehaviorRowSnapshot row) {
        String scheduleValue = row.registeredSchedules().isEmpty()
                ? SchedulePhase.UNKNOWN.name()
                : row.registeredSchedules().stream()
                  .map(SchedulePhase::name)
                  .reduce((left, right) -> left + ", " + right)
                  .orElse(SchedulePhase.UNKNOWN.name());
        return Component.translatable(SCHEDULE_LINE_KEY, scheduleValue);
    }

    private Component buildRightText(@Nonnull BehaviorRowSnapshot row) {
        if (row.running()) {
            Component stageComponent = row.currentStageLabel() != null
                    ? Component.literal(row.currentStageLabel())
                    : Component.translatable(STAGE_NA_KEY);
            return Component.translatable(STAGE_KEY, stageComponent);
        }

        String cooldown = formatCooldownAsMinutesSeconds(row.cooldownRemainingTicks());
        Component marker = switch (row.preconditionSummary()) {
            case PASS -> Component.literal("✔");
            case FAIL -> Component.literal("✖");
            case UNKNOWN -> Component.literal("?");
        };
        return Component.translatable(COOLDOWN_KEY, cooldown)
                .append(Component.literal(" "))
                .append(marker);
    }

    private int getRightColor(@Nonnull BehaviorRowSnapshot row) {
        if (row.running()) {
            return theme.successColor();
        }
        if (row.preconditionSummary() == PreconditionSummary.PASS && row.cooldownRemainingTicks() > 0) {
            return theme.warningColor();
        }
        return switch (row.preconditionSummary()) {
            case PASS -> theme.successColor();
            case FAIL -> theme.errorColor();
            case UNKNOWN -> theme.subtleTextColor();
        };
    }

    private String formatCooldownAsMinutesSeconds(int cooldownRemainingTicks) {
        long totalSeconds = Math.max(cooldownRemainingTicks, 0L) / Ticks.TICKS_PER_SECOND;
        long minutes = totalSeconds / Ticks.SECONDS_PER_MINUTE;
        long seconds = totalSeconds % Ticks.SECONDS_PER_MINUTE;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private ItemStack resolveItemStack(@Nonnull ResourceLocation itemId) {
        return this.rowIconCache.computeIfAbsent(itemId, id -> new ItemStack(resolveItem(id)));
    }

    private Item resolveItem(@Nonnull ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? Items.BARRIER : item;
    }

    // ---- Panel sizing (responsive clamp) ----

    private int getPanelWidth() {
        int viewportMaxWidth = Math.max(240, this.width - PANEL_MARGIN * 2);
        int maxWidth = Math.min(MAX_GUI_WIDTH, viewportMaxWidth);
        int minWidth = Math.min(MIN_GUI_WIDTH, maxWidth);
        int preferredWidth = Math.round(this.width * PANEL_WIDTH_RATIO);
        return Mth.clamp(preferredWidth, minWidth, maxWidth);
    }

    private int getPanelHeight() {
        int viewportMaxHeight = Math.max(180, this.height - PANEL_MARGIN * 2);
        int maxHeight = Math.min(MAX_GUI_HEIGHT, viewportMaxHeight);
        int minHeight = Math.min(MIN_GUI_HEIGHT, maxHeight);
        int preferredHeight = Math.round(this.height * PANEL_HEIGHT_RATIO);
        return Mth.clamp(preferredHeight, minHeight, maxHeight);
    }

}
