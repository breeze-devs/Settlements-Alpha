package dev.breezes.settlements.presentation.ui.stats;

import dev.breezes.settlements.application.ui.stats.model.VillagerInventorySnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerStatsSnapshot;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ServerBoundCloseVillagerStatsPacket;
import dev.breezes.settlements.presentation.ui.framework.Elements;
import dev.breezes.settlements.presentation.ui.framework.Insets;
import dev.breezes.settlements.presentation.ui.framework.LayoutScreen;
import dev.breezes.settlements.presentation.ui.framework.LinearLayout;
import dev.breezes.settlements.presentation.ui.framework.ScrollableList;
import dev.breezes.settlements.presentation.ui.framework.SizeConstraint;
import dev.breezes.settlements.presentation.ui.framework.StackLayout;
import dev.breezes.settlements.presentation.ui.framework.UIElement;
import dev.breezes.settlements.presentation.ui.framework.WidgetElement;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.StringUtil;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ClientSide
@CustomLog
public class VillagerStatsScreen extends LayoutScreen {

    private static final float PANEL_WIDTH_RATIO = 0.85F;
    private static final float PANEL_HEIGHT_RATIO = 0.80F;
    private static final int MIN_GUI_WIDTH = 420;
    private static final int MIN_GUI_HEIGHT = 280;
    private static final int MAX_GUI_WIDTH = 540;
    private static final int MAX_GUI_HEIGHT = 400;
    private static final int PANEL_MARGIN = 10;
    private static final int FOOTER_HEIGHT = 24;
    private static final int ITEM_CELL_SIZE = 20;
    private static final int LEFT_PANEL_WEIGHT = 7;
    private static final int RIGHT_PANEL_WEIGHT = 13;
    private static final int MAX_HEX_CHART_HEIGHT = 100;

    private static final String TITLE_KEY = "ui.settlements.stats.title";
    private static final String INVENTORY_KEY = "ui.settlements.stats.inventory";
    private static final String ACTIVITY_NONE_KEY = "ui.settlements.stats.activity.none";
    private static final String HOME_KEY = "ui.settlements.stats.home";
    private static final String WORK_KEY = "ui.settlements.stats.work";
    private static final String HOME_NONE_KEY = "ui.settlements.stats.home.none";
    private static final String WORK_NONE_KEY = "ui.settlements.stats.work.none";
    private static final String REPUTATION_KEY = "ui.settlements.stats.reputation";
    private static final String CONNECTION_KEY = "ui.settlements.stats.connection";
    private static final String CONNECTION_OK_KEY = "ui.settlements.stats.connection.ok";
    private static final String CONNECTION_DISCONNECTED_KEY = "ui.settlements.stats.connection.disconnected";
    private static final String CONNECTION_OK_TOOLTIP_KEY = "ui.settlements.stats.connection.ok.tooltip";
    private static final String CONNECTION_DISCONNECTED_TOOLTIP_KEY = "ui.settlements.stats.connection.disconnected.tooltip";
    private static final String ACTIVITY_KEY = "ui.settlements.stats.activity";
    private static final String CLOSE_KEY = "ui.settlements.stats.close";
    private static final String UNEMPLOYED_KEY = "ui.settlements.stats.unemployed";
    private static final String DEFAULT_VILLAGER_NAME = "Villager";

    // Static fallback icons
    private static final ItemStack FALLBACK_ACTIVITY_ICON = new ItemStack(Items.PAPER);
    private static final ItemStack BED_ICON = new ItemStack(Items.RED_BED);

    @Getter
    private final long sessionId;
    private final VillagerStatsClientState villagerStatsClientState;
    private VillagerStatsSnapshot statsSnapshot;
    @Nullable
    private VillagerInventorySnapshot inventorySnapshot;
    @Nullable
    private Component unavailableMessage;

    private final HexChartRenderer hexChartRenderer;
    @Nullable
    private ScrollableList inventoryList;
    private int inventoryColumns;

    // Cached per-snapshot render state
    private ItemStack cachedWorkstationIcon;
    private ItemStack cachedActivityIcon;
    @Nullable
    private HpBarElement hpBarElement;

    // Cached components
    private Component cachedReputationText;
    private int cachedReputationColor;
    private Component cachedActivityText;
    private int cachedActivityTextColor;
    private Component cachedHomeText;
    private int cachedHomeTextColor;
    private Component cachedWorkText;
    private int cachedWorkTextColor;

    public VillagerStatsScreen(long sessionId,
                               @Nonnull VillagerStatsSnapshot statsSnapshot,
                               @Nonnull VillagerStatsClientState villagerStatsClientState) {
        super(Component.translatable(TITLE_KEY));

        this.sessionId = sessionId;
        this.villagerStatsClientState = villagerStatsClientState;
        this.statsSnapshot = statsSnapshot;
        this.inventorySnapshot = null;
        this.unavailableMessage = null;
        this.hexChartRenderer = new HexChartRenderer();
        this.cachedWorkstationIcon = getWorkstationIcon(statsSnapshot.professionKey());
        this.cachedActivityIcon = resolveActivityIcon(statsSnapshot.activeBehaviorIconId());

        rebuildCachedComponents(statsSnapshot);
    }

    @Nonnull
    @Override
    protected UIElement buildRoot() {
        int panelW = getPanelWidth();
        int panelH = getPanelHeight();

        Button closeBtn = Button.builder(Component.translatable(CLOSE_KEY), b -> onClose())
                .bounds(0, 0, 60, 20).build();
        addRenderableWidget(closeBtn);

        UIElement panel = LinearLayout.vertical()
                .width(SizeConstraint.fixed(panelW))
                .height(SizeConstraint.fixed(panelH))
                .backgroundColor(theme.panelColor())
                .child(buildBody())
                .child(Elements.hLine(theme.borderDark()))
                .child(buildFooter(closeBtn))
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

        this.villagerStatsClientState.tickHeartbeatIfNeeded(this.sessionId);
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public void removed() {
        if (this.sessionId > 0) {
            PacketDistributor.sendToServer(new ServerBoundCloseVillagerStatsPacket(this.sessionId));
            this.villagerStatsClientState.clearSession(this.sessionId);
        }

        super.removed();
    }

    public void applyStatsSnapshot(@Nonnull VillagerStatsSnapshot snapshot) {
        this.statsSnapshot = snapshot;
        this.unavailableMessage = null;
        this.cachedWorkstationIcon = getWorkstationIcon(snapshot.professionKey());
        this.cachedActivityIcon = resolveActivityIcon(snapshot.activeBehaviorIconId());

        rebuildCachedComponents(snapshot);

        if (this.hpBarElement != null) {
            this.hpBarElement.setHp(snapshot.currentHealth(), snapshot.maxHealth());
        }
    }

    public void applyInventorySnapshot(@Nonnull VillagerInventorySnapshot inventory) {
        this.inventorySnapshot = inventory;

        if (this.inventoryList != null) {
            this.inventoryList.rebuildRows();
        }
    }

    public void markUnavailable(@Nonnull Component message) {
        this.unavailableMessage = message;
    }

    private void rebuildCachedComponents(@Nonnull VillagerStatsSnapshot snapshot) {
        // Reputation
        int rep = snapshot.reputation();
        Component titleComponent = Component.translatable(VillagerStatsUtil.getReputationTitleKey(rep));
        this.cachedReputationText = Component.translatable(REPUTATION_KEY, titleComponent);
        this.cachedReputationColor = getReputationColor(rep);

        // Activity
        String nameKey = snapshot.activeBehaviorNameKey();
        if (nameKey != null) {
            Component activityName = Component.translatable(nameKey);
            this.cachedActivityText = Component.translatable(ACTIVITY_KEY, activityName);
            this.cachedActivityTextColor = theme.successColor();
        } else {
            Component activityName = Component.translatable(ACTIVITY_NONE_KEY);
            this.cachedActivityText = Component.translatable(ACTIVITY_KEY, activityName);
            this.cachedActivityTextColor = theme.subtleTextColor();
        }

        // Home
        BlockPos homePos = snapshot.homePos();
        if (homePos != null) {
            this.cachedHomeText = Component.translatable(HOME_KEY, homePos.getX(), homePos.getY(), homePos.getZ());
            this.cachedHomeTextColor = theme.textColor();
        } else {
            this.cachedHomeText = Component.translatable(HOME_NONE_KEY);
            this.cachedHomeTextColor = theme.subtleTextColor();
        }

        // Workstation
        if (VillagerStatsUtil.isUnemployed(snapshot.professionKey())) {
            this.cachedWorkText = Component.translatable(WORK_NONE_KEY);
            this.cachedWorkTextColor = theme.subtleTextColor();
        } else {
            BlockPos workPos = snapshot.workstationPos();
            if (workPos != null) {
                this.cachedWorkText = Component.translatable(WORK_KEY, workPos.getX(), workPos.getY(), workPos.getZ());
                this.cachedWorkTextColor = theme.textColor();
            } else {
                this.cachedWorkText = Component.translatable(WORK_NONE_KEY);
                this.cachedWorkTextColor = theme.subtleTextColor();
            }
        }
    }

    // ---- Body ----

    private UIElement buildBody() {
        return LinearLayout.horizontal()
                .width(SizeConstraint.FILL)
                .height(new SizeConstraint.Weighted(1))
                .child(buildLeftPanel())
                .child(Elements.rect().color(theme.borderDark())
                        .width(SizeConstraint.fixed(1)).height(SizeConstraint.FILL).build())
                .child(buildRightPanel())
                .build();
    }

    // ---- Left Panel ----

    private UIElement buildLeftPanel() {
        return LinearLayout.vertical()
                .width(SizeConstraint.weighted(LEFT_PANEL_WEIGHT))
                .height(SizeConstraint.FILL)
                .padding(new Insets(6, 6, 6, 6))
                .gap(3)
                .child(Elements.centeredText(this::getVillagerNameComponent, theme.textColor()))
                .child(Elements.centeredText(this::getExpertiseProfessionComponent, theme.subtleTextColor()))
                .child(buildVillagerModelArea())
                .child(buildHpBar())
                .child(buildHomeRow())
                .child(buildWorkstationRow())
                .child(buildCurrentActivity())
                .child(buildHexChart())
                .child(buildReputation())
                .build();
    }

    private Component getVillagerNameComponent() {
        String name = this.statsSnapshot.villagerName();
        return Component.literal(name != null ? name : DEFAULT_VILLAGER_NAME);
    }

    private Component getExpertiseProfessionComponent() {
        String profKey = this.statsSnapshot.professionKey();
        if (VillagerStatsUtil.isUnemployed(profKey)) {
            return Component.translatable(UNEMPLOYED_KEY);
        }

        String expertise;
        try {
            expertise = Expertise.fromLevel(this.statsSnapshot.expertiseLevel()).name();
        } catch (IllegalArgumentException e) {
            expertise = "Unknown";
        }
        String profession = VillagerStatsUtil.formatProfessionName(profKey);
        return Component.literal(StringUtil.titleCase(expertise) + " " + profession);
    }

    private UIElement buildVillagerModelArea() {
        // Performance note: Entity model rendering is the most expensive element.
        // Candidate for framebuffer caching if profiling shows issues.
        return new VillagerModelElement(() -> this.statsSnapshot.villagerEntityId(), theme);
    }

    private UIElement buildHpBar() {
        this.hpBarElement = new HpBarElement(theme, statsSnapshot.currentHealth(), statsSnapshot.maxHealth());
        return this.hpBarElement;
    }

    private UIElement buildHexChart() {
        return Elements.custom(SizeConstraint.FILL, SizeConstraint.weightedMax(1, MAX_HEX_CHART_HEIGHT),
                (graphics, bounds, mouseX, mouseY, partialTick) ->
                        hexChartRenderer.render(graphics, bounds, mouseX, mouseY, this.font,
                                this.statsSnapshot.geneValues(), theme));
    }

    private UIElement buildReputation() {
        return Elements.custom(SizeConstraint.FILL, SizeConstraint.fixed(10), (graphics, bounds, mouseX, mouseY, partialTick) -> {
            graphics.drawString(this.font, this.cachedReputationText, bounds.x(), bounds.y(), this.cachedReputationColor, false);

            // Hover: show numeric value
            if (bounds.contains(mouseX, mouseY)) {
                int rep = this.statsSnapshot.reputation();
                String sign = rep >= 0 ? "+" : "";
                graphics.renderTooltip(this.font, Component.literal(sign + rep), mouseX, mouseY);
            }
        });
    }

    private UIElement buildCurrentActivity() {
        return Elements.custom(SizeConstraint.FILL, SizeConstraint.fixed(18), (graphics, bounds, mouseX, mouseY, partialTick) -> {
            graphics.renderItem(this.cachedActivityIcon, bounds.x(), bounds.y());
            graphics.drawString(this.font, this.cachedActivityText, bounds.x() + 18, bounds.y() + 4, this.cachedActivityTextColor, false);
        });
    }

    private static ItemStack resolveActivityIcon(@Nullable String iconId) {
        if (iconId == null) {
            return FALLBACK_ACTIVITY_ICON;
        }

        try {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(iconId);
            Item item = BuiltInRegistries.ITEM.get(resourceLocation);
            if (item != Items.AIR) {
                return item.getDefaultInstance();
            }
        } catch (Exception e) {
            log.error("Failed to resolve activity item icon for {}", iconId, e);
        }

        return FALLBACK_ACTIVITY_ICON;
    }

    private UIElement buildHomeRow() {
        return Elements.custom(SizeConstraint.FILL, SizeConstraint.fixed(18), (graphics, bounds, mouseX, mouseY, partialTick) -> {
            graphics.renderItem(BED_ICON, bounds.x(), bounds.y());
            graphics.drawString(this.font, this.cachedHomeText, bounds.x() + 18, bounds.y() + 4, this.cachedHomeTextColor, false);
        });
    }

    private UIElement buildWorkstationRow() {
        return Elements.custom(SizeConstraint.FILL, SizeConstraint.fixed(18), (graphics, bounds, mouseX, mouseY, partialTick) -> {
            graphics.renderItem(this.cachedWorkstationIcon, bounds.x(), bounds.y());
            graphics.drawString(this.font, this.cachedWorkText, bounds.x() + 18, bounds.y() + 4, this.cachedWorkTextColor, false);
        });
    }

    private static final Map<String, Item> WORKSTATION_ICON_BY_PROFESSION = Map.ofEntries(
            Map.entry("farmer", Items.COMPOSTER),
            Map.entry("librarian", Items.LECTERN),
            Map.entry("cleric", Items.BREWING_STAND),
            Map.entry("armorer", Items.BLAST_FURNACE),
            Map.entry("weaponsmith", Items.GRINDSTONE),
            Map.entry("toolsmith", Items.SMITHING_TABLE),
            Map.entry("butcher", Items.SMOKER),
            Map.entry("leatherworker", Items.CAULDRON),
            Map.entry("mason", Items.STONECUTTER),
            Map.entry("stone_mason", Items.STONECUTTER),
            Map.entry("shepherd", Items.LOOM),
            Map.entry("fletcher", Items.FLETCHING_TABLE),
            Map.entry("cartographer", Items.CARTOGRAPHY_TABLE),
            Map.entry("fisherman", Items.BARREL)
    );

    private static ItemStack getWorkstationIcon(@Nonnull String profKey) {
        String suffix = VillagerStatsUtil.professionKeySuffix(profKey);
        Item item = WORKSTATION_ICON_BY_PROFESSION.getOrDefault(suffix, Items.CRAFTING_TABLE);
        return new ItemStack(item);
    }

    // ---- Right Panel ----

    private UIElement buildRightPanel() {
        return LinearLayout.vertical()
                .width(SizeConstraint.weighted(RIGHT_PANEL_WEIGHT))
                .height(SizeConstraint.FILL)
                .padding(new Insets(6, 6, 6, 6))
                .child(buildInventorySection())
                .build();
    }

    private UIElement buildInventorySection() {
        int availableWidth = Math.max(100, getPanelWidth() * RIGHT_PANEL_WEIGHT / (LEFT_PANEL_WEIGHT + RIGHT_PANEL_WEIGHT) - 16);
        this.inventoryColumns = Math.max(1, availableWidth / ITEM_CELL_SIZE);

        this.inventoryList = ScrollableList.builder()
                .rowHeight(ITEM_CELL_SIZE)
                .rowFactory(this::buildInventoryRows)
                .width(SizeConstraint.FILL)
                .height(new SizeConstraint.Weighted(1))
                .padding(Insets.NONE)
                .alternatingRowColors(0, 0)
                .build();
        this.inventoryList.rebuildRows();

        return LinearLayout.vertical()
                .width(SizeConstraint.FILL)
                .height(new SizeConstraint.Weighted(1))
                .gap(2)
                .child(Elements.text(this::getInventoryHeaderComponent, theme.subtleTextColor()))
                .child(this.inventoryList)
                .build();
    }

    private List<UIElement> buildInventoryRows() {
        List<ItemStack> items = this.inventorySnapshot != null
                ? this.inventorySnapshot.nonEmptyItems()
                : List.of();

        if (items.isEmpty()) {
            return List.of();
        }

        int rowCount = (items.size() + this.inventoryColumns - 1) / this.inventoryColumns;
        List<UIElement> rows = new ArrayList<>(rowCount);

        for (int row = 0; row < rowCount; row++) {
            LinearLayout.Builder rowLayout = LinearLayout.horizontal()
                    .width(SizeConstraint.WRAP)
                    .height(SizeConstraint.fixed(ITEM_CELL_SIZE))
                    .gap(0);

            for (int col = 0; col < this.inventoryColumns; col++) {
                int index = row * this.inventoryColumns + col;
                if (index < items.size()) {
                    final ItemStack stack = items.get(index);
                    rowLayout.child(Elements.itemIcon(
                            () -> stack,
                            theme::borderLight,
                            () -> stack
                    ));
                }
            }
            rows.add(rowLayout.build());
        }

        return rows;
    }

    private Component getInventoryHeaderComponent() {
        int used = this.inventorySnapshot != null ? this.inventorySnapshot.nonEmptyItems().size() : 0;
        int total = this.inventorySnapshot != null ? this.inventorySnapshot.backpackSize() : 0;
        return Component.translatable(INVENTORY_KEY, used, total);
    }

    // ---- Footer ----

    private UIElement buildFooter(@Nonnull Button closeBtn) {
        return LinearLayout.horizontal()
                .height(SizeConstraint.fixed(FOOTER_HEIGHT))
                .width(SizeConstraint.FILL)
                .padding(Insets.symmetric(2, 8))
                .crossAxisAlignment(LinearLayout.CrossAxisAlignment.CENTER)
                .child(buildConnectionStatus())
                .child(Elements.flexSpacer())
                .child(new WidgetElement(closeBtn))
                .build();
    }

    private UIElement buildConnectionStatus() {
        Component connectionLabel = Component.translatable(CONNECTION_KEY);
        Component okStatus = Component.translatable(CONNECTION_OK_KEY);
        Component disconnectedStatus = Component.translatable(CONNECTION_DISCONNECTED_KEY);

        return Elements.custom(SizeConstraint.WRAP, SizeConstraint.fixed(12), (graphics, bounds, mouseX, mouseY, partialTick) -> {
            boolean stale = this.villagerStatsClientState.isSnapshotUpdateStale(this.sessionId)
                    || this.villagerStatsClientState.isHeartbeatAckStale(this.sessionId);

            int labelWidth = this.font.width(connectionLabel);
            graphics.drawString(this.font, connectionLabel, bounds.x(), bounds.y(), theme.textColor(), false);

            Component status;
            int statusColor;
            String tooltipKey;
            if (stale) {
                status = disconnectedStatus;
                statusColor = theme.errorColor();
                tooltipKey = CONNECTION_DISCONNECTED_TOOLTIP_KEY;
            } else {
                status = okStatus;
                statusColor = theme.successColor();
                tooltipKey = CONNECTION_OK_TOOLTIP_KEY;
            }
            int statusX = bounds.x() + labelWidth + 2;
            graphics.drawString(this.font, status, statusX, bounds.y(), statusColor, false);

            // Hover tooltip
            int totalWidth = labelWidth + 2 + this.font.width(status);
            if (mouseX >= bounds.x() && mouseX <= bounds.x() + totalWidth
                    && mouseY >= bounds.y() && mouseY <= bounds.y() + 10) {
                graphics.renderTooltip(this.font, Component.translatable(tooltipKey), mouseX, mouseY);
            }
        });
    }

    private UIElement buildUnavailableOverlay() {
        return Elements.custom(SizeConstraint.FILL, SizeConstraint.FILL, (graphics, bounds, mouseX, mouseY, partialTick) -> {
            if (this.unavailableMessage == null) {
                return;
            }
            graphics.fill(bounds.x() + 8, bounds.y() + 8, bounds.right() - 8, bounds.bottom() - FOOTER_HEIGHT - 8, theme.overlayColor());
            graphics.drawCenteredString(
                    this.font,
                    this.unavailableMessage,
                    bounds.x() + bounds.width() / 2,
                    bounds.y() + bounds.height() / 2 - 4,
                    theme.errorColor()
            );
        });
    }

    // ---- Utility ----

    private int getReputationColor(int reputation) {
        if (reputation <= -10) return theme.errorColor();
        if (reputation < 10) return theme.warningColor();
        return theme.successColor();
    }

    // ---- Panel sizing ----

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
