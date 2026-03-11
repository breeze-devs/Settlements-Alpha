package dev.breezes.settlements.presentation.ui.behavior;

import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ServerBoundCloseBehaviorControllerPacket;
import dev.breezes.settlements.application.ui.behavior.model.BehaviorControllerSnapshot;
import dev.breezes.settlements.application.ui.behavior.model.BehaviorRowSnapshot;
import dev.breezes.settlements.application.ui.behavior.model.PreconditionSummary;
import dev.breezes.settlements.application.ui.behavior.model.SchedulePhase;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
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
import java.util.Objects;

public class BehaviorControllerScreen extends Screen {

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

    private static final int PANEL_COLOR = 0xFF1E1E1E;
    private static final int BORDER_LIGHT = 0xFF555555;
    private static final int BORDER_DARK = 0xFF0F0F0F;
    private static final int ROW_COLOR = 0xFF2B2B2B;
    private static final int ROW_ALT_COLOR = 0xFF252525;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int SUBTLE_TEXT_COLOR = 0xFFA0A0A0;
    private static final int RUNNING_COLOR = 0xFF55FF55;
    private static final int ERROR_COLOR = 0xFFFF5555;
    private static final int COOLDOWN_WAITING_COLOR = 0xFFFFA500;
    private static final int BACKGROUND_DIM_COLOR = 0x50000000;
    private static final int UNAVAILABLE_OVERLAY_COLOR = 0xCC000000;

    private static final String TITLE_KEY = "ui.settlements.behavior.title";
    private static final String VILLAGER_KEY = "ui.settlements.behavior.villager";
    private static final String STAGE_KEY = "ui.settlements.behavior.stage";
    private static final String STAGE_NA_KEY = "ui.settlements.behavior.stage_na";
    private static final String SCHEDULE_LINE_KEY = "ui.settlements.behavior.schedule";
    private static final String COOLDOWN_KEY = "ui.settlements.behavior.cooldown";
    private static final String ENABLE_ALL_KEY = "ui.settlements.behavior.enable_all";
    private static final String ENABLE_ALL_STUB_KEY = "ui.settlements.behavior.enable_all_stub";
    private static final String BACK_KEY = "ui.settlements.behavior.back";
    private static final String CONNECTION_STALE_KEY = "ui.settlements.behavior.connection_stale";

    // TODO: these should be declared in some other places
    private static final ItemStack SCHEDULE_REST_ICON = new ItemStack(Items.RED_BED);
    private static final ItemStack SCHEDULE_WORK_ICON = new ItemStack(Items.CRAFTING_TABLE);
    private static final ItemStack SCHEDULE_IDLE_ICON = new ItemStack(Items.MUSIC_DISC_13);
    private static final ItemStack SCHEDULE_MEET_ICON = new ItemStack(Items.BELL);

    @Getter
    private final long sessionId;
    private BehaviorControllerSnapshot snapshot;
    @Nullable
    private Component unavailableMessage;
    private int rowScrollOffset;
    private final Map<ResourceLocation, ItemStack> rowIconCache;
    private List<CachedRowView> cachedRows;

    public BehaviorControllerScreen(long sessionId, @Nonnull BehaviorControllerSnapshot snapshot) {
        super(Component.translatable(TITLE_KEY));

        this.sessionId = sessionId;
        this.snapshot = snapshot;

        this.unavailableMessage = null;
        this.rowScrollOffset = 0;
        this.rowIconCache = new HashMap<>();
        this.cachedRows = List.of();
        this.rebuildCachedRows(snapshot);
    }

    @Override
    protected void init() {
        int x = getPanelX();
        int y = getPanelY();
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();

        this.addRenderableWidget(Button.builder(Component.translatable(ENABLE_ALL_KEY), button -> onEnableAllClicked())
                .bounds(x + 12, y + panelHeight - FOOTER_HEIGHT + 2, 120, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable(BACK_KEY), button -> onClose())
                .bounds(x + panelWidth - 132, y + panelHeight - FOOTER_HEIGHT + 2, 120, 20)
                .build());
    }

    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the background darkened without invoking blur-heavy background helpers.
        graphics.fill(0, 0, this.width, this.height, BACKGROUND_DIM_COLOR);
    }

    @Override
    public void tick() {
        BehaviorControllerClientState.tickHeartbeatIfNeeded(this.sessionId);
        this.rowScrollOffset = Mth.clamp(this.rowScrollOffset, 0, getMaxScrollOffset());
    }

    @Override
    public void onClose() {
        if (this.sessionId > 0) {
            PacketDistributor.sendToServer(new ServerBoundCloseBehaviorControllerPacket(this.sessionId));
            BehaviorControllerClientState.clearSession(this.sessionId);
        }
        super.onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0 || this.snapshot.rows().isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int nextOffset = this.rowScrollOffset + (scrollY > 0 ? -1 : 1);
        nextOffset = Mth.clamp(nextOffset, 0, getMaxScrollOffset());
        if (nextOffset == this.rowScrollOffset) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        this.rowScrollOffset = nextOffset;
        return true;
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        int x = getPanelX();
        int y = getPanelY();

        renderPanel(graphics, x, y);
        renderHeader(graphics, x, y);
        renderRows(graphics, x, y);
        renderUnavailableOverlay(graphics, x, y);
        renderFooter(graphics, x, y);
        renderStaleConnectionWarning(graphics, x, y);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public void applySnapshot(@Nonnull BehaviorControllerSnapshot snapshot) {
        this.snapshot = snapshot;
        this.unavailableMessage = null;
        this.rebuildCachedRows(snapshot);
        this.rowScrollOffset = Mth.clamp(this.rowScrollOffset, 0, getMaxScrollOffset());
    }

    public void markUnavailable(@Nonnull Component message) {
        this.unavailableMessage = message;
    }

    private void onEnableAllClicked() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.translatable(ENABLE_ALL_STUB_KEY), true);
        }
    }

    private void renderPanel(@Nonnull GuiGraphics graphics, int x, int y) {
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        graphics.fill(x, y, x + panelWidth, y + panelHeight, PANEL_COLOR);

        graphics.hLine(x, x + panelWidth - 1, y, BORDER_LIGHT);
        graphics.vLine(x, y, y + panelHeight - 1, BORDER_LIGHT);
        graphics.hLine(x, x + panelWidth - 1, y + panelHeight - 1, BORDER_DARK);
        graphics.vLine(x + panelWidth - 1, y, y + panelHeight - 1, BORDER_DARK);

        graphics.hLine(x, x + panelWidth - 1, y + HEADER_HEIGHT, BORDER_DARK);
        graphics.hLine(x, x + panelWidth - 1, y + panelHeight - FOOTER_HEIGHT, BORDER_DARK);
    }

    private void renderHeader(@Nonnull GuiGraphics graphics, int x, int y) {
        int panelWidth = getPanelWidth();
        graphics.drawCenteredString(this.font, Component.translatable(TITLE_KEY), x + panelWidth / 2, y + 6, TEXT_COLOR);
        graphics.drawString(this.font, Component.translatable(VILLAGER_KEY, this.snapshot.villagerName()), x + 8, y + 20, SUBTLE_TEXT_COLOR, false);

        int iconBaseX = x + panelWidth - 120;
        int iconY = y + 18;

        drawScheduleIcon(graphics, iconBaseX, iconY, SchedulePhase.REST, SCHEDULE_REST_ICON);
        drawScheduleIcon(graphics, iconBaseX + 20, iconY, SchedulePhase.WORK, SCHEDULE_WORK_ICON);
        drawScheduleIcon(graphics, iconBaseX + 40, iconY, SchedulePhase.IDLE, SCHEDULE_IDLE_ICON);
        drawScheduleIcon(graphics, iconBaseX + 60, iconY, SchedulePhase.MEET, SCHEDULE_MEET_ICON);
    }

    private void drawScheduleIcon(@Nonnull GuiGraphics graphics,
                                  int x,
                                  int y,
                                  @Nonnull SchedulePhase phase,
                                  @Nonnull ItemStack iconStack) {
        int borderColor = this.snapshot.scheduleBucket() == phase ? RUNNING_COLOR : BORDER_LIGHT;
        graphics.fill(x - 2, y - 2, x + 18, y + 18, borderColor);
        graphics.fill(x - 1, y - 1, x + 17, y + 17, PANEL_COLOR);
        graphics.renderItem(iconStack, x, y);
    }

    private void renderRows(@Nonnull GuiGraphics graphics, int x, int y) {
        int panelWidth = getPanelWidth();
        int startY = y + HEADER_HEIGHT + 2;
        int listX = x + 10;
        int listWidth = panelWidth - 20;
        int maxVisibleRows = getMaxVisibleRows();

        int end = Math.min(this.cachedRows.size(), this.rowScrollOffset + maxVisibleRows);

        for (int i = this.rowScrollOffset; i < end; i++) {
            CachedRowView row = this.cachedRows.get(i);
            int rowY = startY + (i - this.rowScrollOffset) * ROW_HEIGHT;
            int background = (i % 2 == 0) ? ROW_COLOR : ROW_ALT_COLOR;

            graphics.fill(listX, rowY, listX + listWidth, rowY + ROW_HEIGHT - 1, background);
            renderRow(graphics, row, listX + 2, rowY + 1, listWidth - 4, ROW_HEIGHT - 2);
        }

        if (getMaxScrollOffset() > 0) {
            int scrollHintColor = SUBTLE_TEXT_COLOR;
            if (this.rowScrollOffset > 0) {
                graphics.drawString(this.font, "▲", x + panelWidth - 12, startY - 1, scrollHintColor, false);
            }
            if (this.rowScrollOffset < getMaxScrollOffset()) {
                graphics.drawString(this.font, "▼", x + panelWidth - 12, startY + maxVisibleRows * ROW_HEIGHT - 7, scrollHintColor, false);
            }
        }
    }

    private void renderRow(@Nonnull GuiGraphics graphics,
                           @Nonnull CachedRowView row,
                           int x,
                           int y,
                           int width,
                           int height) {
        int iconX = x + 2;
        int iconY = y + 3;

        int iconBorder = row.running() ? RUNNING_COLOR : BORDER_LIGHT;
        graphics.fill(iconX - 2, iconY - 2, iconX + 18, iconY + 18, iconBorder);
        graphics.fill(iconX - 1, iconY - 1, iconX + 17, iconY + 17, PANEL_COLOR);
        graphics.renderItem(row.icon(), iconX, iconY);

        graphics.drawString(this.font, row.behaviorName(), x + 22, y + 2, TEXT_COLOR, false);
        graphics.drawString(this.font, row.scheduleLine(), x + 22, y + 12, SUBTLE_TEXT_COLOR, false);

        int rightX = x + width - this.font.width(row.rightText()) - 4;
        graphics.drawString(this.font, row.rightText(), rightX, y + 8, row.rightColor(), false);
    }

    private void rebuildCachedRows(@Nonnull BehaviorControllerSnapshot snapshot) {
        this.cachedRows = snapshot.rows().stream()
                .map(this::toCachedRowView)
                .toList();
    }

    @Nonnull
    private CachedRowView toCachedRowView(@Nonnull BehaviorRowSnapshot row) {
        var behaviorName = Component.literal("#" + row.uiBehaviorIndex() + " ")
                .append(Component.translatable(row.displayNameKey()));
        if (row.displaySuffix() != null && !row.displaySuffix().isBlank()) {
            behaviorName = behaviorName.append(Component.literal(" " + row.displaySuffix()));
        }

        String scheduleValue = row.registeredSchedules().isEmpty()
                ? SchedulePhase.UNKNOWN.name()
                : row.registeredSchedules().stream()
                .map(SchedulePhase::name)
                .reduce((left, right) -> left + ", " + right)
                .orElse(SchedulePhase.UNKNOWN.name());
        Component scheduleLine = Component.translatable(SCHEDULE_LINE_KEY, scheduleValue);

        Component rightText;
        int rightColor;
        if (row.running()) {
            Component stageComponent = row.currentStageLabel() != null
                    ? Component.literal(row.currentStageLabel())
                    : Component.translatable(STAGE_NA_KEY);
            rightText = Component.translatable(STAGE_KEY, stageComponent);
            rightColor = RUNNING_COLOR;
        } else {
            String cooldownMinutesSeconds = formatCooldownAsMinutesSeconds(row.cooldownRemainingTicks());
            Component preconditionMarker = switch (row.preconditionSummary()) {
                case PASS -> Component.literal("✔");
                case FAIL -> Component.literal("✖");
                case UNKNOWN -> Component.literal("?");
            };
            rightText = Component.translatable(COOLDOWN_KEY, cooldownMinutesSeconds)
                    .append(Component.literal(" "))
                    .append(preconditionMarker);
            rightColor = getInactiveRowColor(row);
        }

        return new CachedRowView(
                resolveItemStack(row.iconItemId()),
                behaviorName,
                scheduleLine,
                rightText,
                rightColor,
                row.running()
        );
    }


    private int getInactiveRowColor(@Nonnull BehaviorRowSnapshot row) {
        if (row.preconditionSummary() == PreconditionSummary.PASS && row.cooldownRemainingTicks() > 0) {
            return COOLDOWN_WAITING_COLOR;
        }

        return switch (row.preconditionSummary()) {
            case PASS -> RUNNING_COLOR;
            case FAIL -> ERROR_COLOR;
            case UNKNOWN -> SUBTLE_TEXT_COLOR;
        };
    }

    @Nonnull
    private String formatCooldownAsMinutesSeconds(int cooldownRemainingTicks) {
        long totalSeconds = Math.max(cooldownRemainingTicks, 0L) / Ticks.TICKS_PER_SECOND;
        long minutes = totalSeconds / Ticks.SECONDS_PER_MINUTE;
        long seconds = totalSeconds % Ticks.SECONDS_PER_MINUTE;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    @Nonnull
    private ItemStack resolveItemStack(@Nonnull ResourceLocation itemId) {
        return this.rowIconCache.computeIfAbsent(itemId, id -> new ItemStack(resolveItem(id)));
    }

    @Nonnull
    private Item resolveItem(@Nonnull ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? Items.BARRIER : item;
    }

    private void renderFooter(@Nonnull GuiGraphics graphics, int x, int y) {
        int panelHeight = getPanelHeight();
        graphics.drawString(this.font, Component.literal(this.snapshot.rawActivityKey()), x + 8, y + panelHeight - FOOTER_HEIGHT - 9, SUBTLE_TEXT_COLOR, false);
    }

    private void renderStaleConnectionWarning(@Nonnull GuiGraphics graphics, int x, int y) {
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        boolean snapshotStale = BehaviorControllerClientState.isSnapshotUpdateStale(this.sessionId);
        boolean heartbeatAckStale = BehaviorControllerClientState.isHeartbeatAckStale(this.sessionId);
        if (!snapshotStale && !heartbeatAckStale) {
            return;
        }

        graphics.drawCenteredString(this.font, Component.translatable(CONNECTION_STALE_KEY), x + panelWidth / 2, y + panelHeight - FOOTER_HEIGHT - 18, ERROR_COLOR);
    }

    private void renderUnavailableOverlay(@Nonnull GuiGraphics graphics, int x, int y) {
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        if (this.unavailableMessage == null) {
            return;
        }

        int overlayLeft = x + 8;
        int overlayRight = x + panelWidth - 8;
        int overlayTop = y + HEADER_HEIGHT + 8;
        int overlayBottom = y + panelHeight - FOOTER_HEIGHT - 8;
        graphics.fill(overlayLeft, overlayTop, overlayRight, overlayBottom, UNAVAILABLE_OVERLAY_COLOR);
        graphics.drawCenteredString(this.font, this.unavailableMessage, x + panelWidth / 2, y + panelHeight / 2 - 4, ERROR_COLOR);
    }

    private int getMaxScrollOffset() {
        return Math.max(0, this.cachedRows.size() - getMaxVisibleRows());
    }

    private int getPanelX() {
        return (this.width - getPanelWidth()) / 2;
    }

    private int getPanelY() {
        return (this.height - getPanelHeight()) / 2;
    }

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

    private int getMaxVisibleRows() {
        int rowsAreaHeight = getPanelHeight() - HEADER_HEIGHT - FOOTER_HEIGHT - 4;
        return Math.max(1, rowsAreaHeight / ROW_HEIGHT);
    }

    /**
     * Opening this screen should not pause the game
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record CachedRowView(
            @Nonnull ItemStack icon,
            @Nonnull Component behaviorName,
            @Nonnull Component scheduleLine,
            @Nonnull Component rightText,
            int rightColor,
            boolean running
    ) {
        private CachedRowView {
            Objects.requireNonNull(icon, "icon");
            Objects.requireNonNull(behaviorName, "behaviorName");
            Objects.requireNonNull(scheduleLine, "scheduleLine");
            Objects.requireNonNull(rightText, "rightText");
        }
    }

}
