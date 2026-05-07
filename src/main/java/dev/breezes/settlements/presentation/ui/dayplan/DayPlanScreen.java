package dev.breezes.settlements.presentation.ui.dayplan;

import dev.breezes.settlements.application.ui.dayplan.model.DayPlanSlotSnapshot;
import dev.breezes.settlements.application.ui.dayplan.model.DayPlanSlotVisualStatus;
import dev.breezes.settlements.application.ui.dayplan.model.DayPlanSnapshot;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ServerBoundCloseUiPacket;
import dev.breezes.settlements.presentation.ui.framework.Elements;
import dev.breezes.settlements.presentation.ui.framework.Insets;
import dev.breezes.settlements.presentation.ui.framework.LayoutScreen;
import dev.breezes.settlements.presentation.ui.framework.LinearLayout;
import dev.breezes.settlements.presentation.ui.framework.ScrollableList;
import dev.breezes.settlements.presentation.ui.framework.SizeConstraint;
import dev.breezes.settlements.presentation.ui.framework.StackLayout;
import dev.breezes.settlements.presentation.ui.framework.UIElement;
import dev.breezes.settlements.presentation.ui.framework.WidgetElement;
import dev.breezes.settlements.presentation.ui.sync.UiClientState;
import lombok.Getter;
import net.minecraft.client.Minecraft;
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
import java.util.Map;

public class DayPlanScreen extends LayoutScreen {

    private static final float PANEL_WIDTH_RATIO = 0.82F;
    private static final float PANEL_HEIGHT_RATIO = 0.78F;
    private static final int MIN_GUI_WIDTH = 400;
    private static final int MIN_GUI_HEIGHT = 260;
    private static final int MAX_GUI_WIDTH = 560;
    private static final int MAX_GUI_HEIGHT = 380;
    private static final int PANEL_MARGIN = 10;
    private static final int HEADER_HEIGHT = 46;
    private static final int FOOTER_HEIGHT = 24;
    private static final int ROW_HEIGHT = 32;
    private static final int DESCRIPTION_MIN_WIDTH = 54;
    private static final int DESCRIPTION_RESERVED_ROW_WIDTH = 122;

    private static final int FUTURE_ACCENT_COLOR = 0xFF66AAFF;
    private static final int COMPLETED_TEXT_COLOR = 0xFF777777;
    private static final int COMPLETED_BORDER_COLOR = 0xFF444444;
    private static final int ACTIVE_ROW_BACKGROUND_COLOR = 0x3316A34A;

    private static final String TITLE_KEY = "ui.settlements.dayplan.title";
    private static final String LOADING_KEY = "ui.settlements.dayplan.loading";
    private static final String DAY_LABEL_KEY = "ui.settlements.dayplan.day";
    private static final String WORK_DAY_KEY = "ui.settlements.dayplan.day_type.work";
    private static final String REST_DAY_KEY = "ui.settlements.dayplan.day_type.rest";
    private static final String TIME_LABEL_KEY = "ui.settlements.dayplan.time";
    private static final String CLOSE_KEY = "ui.settlements.dayplan.close";
    private static final String CONNECTION_STALE_KEY = "ui.settlements.dayplan.connection_stale";
    private static final String STATUS_COMPLETED_KEY = "ui.settlements.dayplan.status.completed";
    private static final String STATUS_ACTIVE_KEY = "ui.settlements.dayplan.status.active";
    private static final String STATUS_COMING_KEY = "ui.settlements.dayplan.status.upcoming";
    private static final String STATUS_SKIPPED_KEY = "ui.settlements.dayplan.status.skipped";
    private static final String STATUS_INTERRUPTED_KEY = "ui.settlements.dayplan.status.interrupted";
    private static final String NO_DESCRIPTION_KEY = "ui.settlements.dayplan.no_description";

    @Getter
    private final long sessionId;
    private final UiClientState uiClientState;
    private final Map<ResourceLocation, ItemStack> iconCache;
    @Nullable
    private DayPlanSnapshot snapshot;
    @Nullable
    private ScrollableList slotList;

    public DayPlanScreen(long sessionId, @Nonnull UiClientState uiClientState) {
        super(Component.translatable(TITLE_KEY));
        this.sessionId = sessionId;
        this.uiClientState = uiClientState;
        this.iconCache = new HashMap<>();
    }

    @Nonnull
    @Override
    protected UIElement buildRoot() {
        int panelW = getPanelWidth();
        int panelH = getPanelHeight();

        this.slotList = ScrollableList.builder()
                .rowHeight(ROW_HEIGHT)
                .rowFactory(this::buildSlotRows)
                .height(new SizeConstraint.Weighted(1))
                .width(SizeConstraint.FILL)
                .padding(Insets.symmetric(4, 10))
                .alternatingRowColors(theme.rowColor(), theme.rowAltColor())
                .build();
        this.slotList.rebuildRows();

        Button closeButton = Button.builder(Component.translatable(CLOSE_KEY), b -> onClose())
                .bounds(0, 0, 64, 20)
                .build();
        addRenderableWidget(closeButton);

        UIElement panel = LinearLayout.vertical()
                .width(SizeConstraint.fixed(panelW))
                .height(SizeConstraint.fixed(panelH))
                .backgroundColor(theme.panelColor())
                .child(buildHeader())
                .child(Elements.hLine(theme.borderDark()))
                .child(this.slotList)
                .child(buildStaleConnectionWarning())
                .child(Elements.hLine(theme.borderDark()))
                .child(LinearLayout.horizontal()
                        .height(SizeConstraint.fixed(FOOTER_HEIGHT))
                        .width(SizeConstraint.FILL)
                        .padding(Insets.symmetric(2, 12))
                        .child(new WidgetElement(closeButton))
                        .build())
                .build();

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
        this.uiClientState.tickHeartbeatIfNeeded(this.sessionId);
    }

    @Override
    public void removed() {
        if (this.sessionId > 0) {
            PacketDistributor.sendToServer(new ServerBoundCloseUiPacket(UiChannel.DAY_PLAN, this.sessionId));
            this.uiClientState.clearSession(this.sessionId);
        }
        super.removed();
    }

    public void applySnapshot(@Nonnull DayPlanSnapshot snapshot) {
        this.snapshot = snapshot;
        if (this.slotList != null) {
            this.slotList.rebuildRows();
        }
        invalidateLayout();
    }

    private UIElement buildHeader() {
        return LinearLayout.vertical()
                .height(SizeConstraint.fixed(HEADER_HEIGHT))
                .width(SizeConstraint.FILL)
                .padding(new Insets(6, 8, 0, 8))
                .child(Elements.centeredText(() -> Component.translatable(TITLE_KEY), theme.textColor()))
                .child(LinearLayout.horizontal()
                        .width(SizeConstraint.FILL)
                        .height(SizeConstraint.WRAP)
                        .padding(new Insets(5, 0, 0, 0))
                        .child(Elements.text(this::buildDayLabel, theme.subtleTextColor()))
                        .child(Elements.flexSpacer())
                        .child(Elements.text(this::buildDayTypeLabel, theme.textColor()))
                        .child(Elements.flexSpacer())
                        .child(Elements.text(this::buildTimeLabel, theme.subtleTextColor()))
                        .build())
                .build();
    }

    private List<UIElement> buildSlotRows() {
        if (this.snapshot == null) {
            return List.of(Elements.centeredText(() -> Component.translatable(LOADING_KEY), theme.subtleTextColor()));
        }
        return this.snapshot.slots().stream()
                .map(this::buildSlotRow)
                .toList();
    }

    private UIElement buildSlotRow(@Nonnull DayPlanSlotSnapshot slot) {
        int borderColor = borderColor(slot.status());
        int titleColor = titleColor(slot.status());
        int subtitleColor = subtitleColor(slot.status());
        int statusColor = statusColor(slot.status());
        int backgroundColor = slot.status() == DayPlanSlotVisualStatus.ACTIVE ? ACTIVE_ROW_BACKGROUND_COLOR : 0;
        Component description = truncate(Component.literal(resolveDescription(slot)), getDescriptionMaxWidthPixels());

        return LinearLayout.horizontal()
                .width(SizeConstraint.FILL)
                .height(SizeConstraint.fixed(ROW_HEIGHT - 1))
                .crossAxisAlignment(LinearLayout.CrossAxisAlignment.CENTER)
                .padding(Insets.symmetric(2, 3))
                .backgroundColor(backgroundColor)
                .child(Elements.itemIcon(() -> resolveItemStack(slot.iconItemId()), () -> borderColor, null))
                .child(LinearLayout.vertical()
                        .width(SizeConstraint.WRAP)
                        .height(SizeConstraint.WRAP)
                        .padding(new Insets(0, 0, 0, 4))
                        .child(Elements.text(() -> Component.translatable(slot.displayNameKey()), titleColor))
                        .child(Elements.text(() -> description, subtitleColor))
                        .build())
                .child(Elements.flexSpacer())
                .child(LinearLayout.vertical()
                        .width(SizeConstraint.WRAP)
                        .height(SizeConstraint.WRAP)
                        .crossAxisAlignment(LinearLayout.CrossAxisAlignment.END)
                        .child(Elements.text(() -> Component.literal(slot.formattedTime()), subtitleColor))
                        .child(Elements.text(() -> statusLabel(slot.status()), statusColor))
                        .build())
                .build();
    }

    private UIElement buildStaleConnectionWarning() {
        return Elements.custom(SizeConstraint.FILL, SizeConstraint.fixed(10), (graphics, bounds, mouseX, mouseY, partialTick) -> {
            boolean stale = this.uiClientState.isSnapshotUpdateStale(this.sessionId)
                    || this.uiClientState.isHeartbeatAckStale(this.sessionId);
            if (!stale) {
                return;
            }
            graphics.drawCenteredString(this.font, Component.translatable(CONNECTION_STALE_KEY),
                    bounds.x() + bounds.width() / 2, bounds.y(), theme.errorColor());
        });
    }

    private UIElement buildUnavailableOverlay() {
        return Elements.custom(SizeConstraint.FILL, SizeConstraint.FILL, (graphics, bounds, mouseX, mouseY, partialTick) -> {
            if (!this.uiClientState.isSessionTerminalUnavailable()) {
                return;
            }
            Component message = this.uiClientState.unavailableReasonKey()
                    .map(Component::translatable)
                    .orElseGet(() -> Component.translatable("ui.settlements.dayplan.unavailable"));
            graphics.fill(bounds.x() + 8, bounds.y() + HEADER_HEIGHT + 8,
                    bounds.right() - 8, bounds.bottom() - FOOTER_HEIGHT - 8, theme.overlayColor());
            graphics.drawCenteredString(this.font, message, bounds.x() + bounds.width() / 2,
                    bounds.y() + bounds.height() / 2 - 4, theme.errorColor());
        });
    }

    private Component buildDayLabel() {
        if (this.snapshot == null) {
            return Component.literal("--");
        }
        return Component.translatable(DAY_LABEL_KEY, this.snapshot.dayNumber());
    }

    private Component buildDayTypeLabel() {
        if (this.snapshot == null) {
            return Component.literal("--");
        }
        return Component.translatable(this.snapshot.dayType() == PlanDayType.WORK_DAY ? WORK_DAY_KEY : REST_DAY_KEY);
    }

    private Component buildTimeLabel() {
        if (this.snapshot == null) {
            return Component.literal("--:--");
        }
        return Component.translatable(TIME_LABEL_KEY, this.snapshot.currentTime());
    }

    private String resolveDescription(@Nonnull DayPlanSlotSnapshot slot) {
        if (slot.description() != null && !slot.description().isBlank()) {
            return slot.description();
        }
        return Component.translatable(NO_DESCRIPTION_KEY).getString();
    }

    private static Component statusLabel(@Nonnull DayPlanSlotVisualStatus status) {
        return Component.translatable(switch (status) {
            case COMPLETED -> STATUS_COMPLETED_KEY;
            case ACTIVE -> STATUS_ACTIVE_KEY;
            case UPCOMING -> STATUS_COMING_KEY;
            case SKIPPED -> STATUS_SKIPPED_KEY;
            case INTERRUPTED -> STATUS_INTERRUPTED_KEY;
        });
    }

    private int borderColor(@Nonnull DayPlanSlotVisualStatus status) {
        return switch (status) {
            case ACTIVE -> theme.successColor();
            case UPCOMING -> FUTURE_ACCENT_COLOR;
            case COMPLETED -> COMPLETED_BORDER_COLOR;
            case SKIPPED, INTERRUPTED -> theme.warningColor();
        };
    }

    private int titleColor(@Nonnull DayPlanSlotVisualStatus status) {
        return switch (status) {
            case COMPLETED -> COMPLETED_TEXT_COLOR;
            case ACTIVE -> theme.successColor();
            case UPCOMING -> theme.textColor();
            case SKIPPED, INTERRUPTED -> theme.warningColor();
        };
    }

    private int subtitleColor(@Nonnull DayPlanSlotVisualStatus status) {
        return status == DayPlanSlotVisualStatus.COMPLETED ? COMPLETED_TEXT_COLOR : theme.subtleTextColor();
    }

    private int statusColor(@Nonnull DayPlanSlotVisualStatus status) {
        return switch (status) {
            case ACTIVE -> theme.successColor();
            case UPCOMING -> FUTURE_ACCENT_COLOR;
            case COMPLETED -> COMPLETED_TEXT_COLOR;
            case SKIPPED, INTERRUPTED -> theme.warningColor();
        };
    }

    private Component truncate(@Nonnull Component component, int maxWidthPixels) {
        String value = component.getString();
        String ellipsis = "...";
        if (Minecraft.getInstance().font.width(value) <= maxWidthPixels) {
            return component;
        }

        while (!value.isEmpty() && Minecraft.getInstance().font.width(value + ellipsis) > maxWidthPixels) {
            value = value.substring(0, value.length() - 1);
        }
        return Component.literal(value + ellipsis);
    }

    private int getDescriptionMaxWidthPixels() {
        // Reserve the fixed-width icon, row padding, and right status column so description text can use the
        // actual remaining row width instead of being clipped to a stale narrow value.
        return Math.max(DESCRIPTION_MIN_WIDTH, getPanelWidth() - DESCRIPTION_RESERVED_ROW_WIDTH);
    }

    private ItemStack resolveItemStack(@Nonnull ResourceLocation itemId) {
        return this.iconCache.computeIfAbsent(itemId, id -> new ItemStack(resolveItem(id)));
    }

    private static Item resolveItem(@Nonnull ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? Items.BARRIER : item;
    }

    private int getPanelWidth() {
        int viewportMaxWidth = Math.max(260, this.width - PANEL_MARGIN * 2);
        int maxWidth = Math.min(MAX_GUI_WIDTH, viewportMaxWidth);
        int minWidth = Math.min(MIN_GUI_WIDTH, maxWidth);
        int preferredWidth = Math.round(this.width * PANEL_WIDTH_RATIO);
        return Mth.clamp(preferredWidth, minWidth, maxWidth);
    }

    private int getPanelHeight() {
        int viewportMaxHeight = Math.max(190, this.height - PANEL_MARGIN * 2);
        int maxHeight = Math.min(MAX_GUI_HEIGHT, viewportMaxHeight);
        int minHeight = Math.min(MIN_GUI_HEIGHT, maxHeight);
        int preferredHeight = Math.round(this.height * PANEL_HEIGHT_RATIO);
        return Mth.clamp(preferredHeight, minHeight, maxHeight);
    }

}
