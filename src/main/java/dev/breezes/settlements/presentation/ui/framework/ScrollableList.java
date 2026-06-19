package dev.breezes.settlements.presentation.ui.framework;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Supplier;

/**
 * A vertically scrolling list of uniform-height rows.
 * <p>
 * Uniform height enables O(1) visible-range calculation
 * and avoids measuring off-screen rows.
 * <p>
 * Rows are provided via a {@link Supplier} so the list can be rebuilt
 * when data changes without reconstructing the container.
 */
@ClientSide
public class ScrollableList extends BaseElement {

    private static final int SCROLL_HINT_COLOR = 0xFFA0A0A0;

    private final int rowHeight;
    private final Supplier<List<UIElement>> rowFactory;
    private final int alternatingRowColor1;
    private final int alternatingRowColor2;

    @Getter
    private List<UIElement> rows = List.of();
    private int scrollOffset = 0;

    private ScrollableList(int rowHeight,
                           @Nonnull Supplier<List<UIElement>> rowFactory,
                           @Nonnull SizeConstraint widthConstraint,
                           @Nonnull SizeConstraint heightConstraint,
                           @Nonnull Insets padding,
                           int alternatingRowColor1,
                           int alternatingRowColor2) {
        super(widthConstraint, heightConstraint, padding);
        this.rowHeight = rowHeight;
        this.rowFactory = rowFactory;
        this.alternatingRowColor1 = alternatingRowColor1;
        this.alternatingRowColor2 = alternatingRowColor2;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void rebuildRows() {
        // Dispose old widget elements before replacing
        for (UIElement row : this.rows) {
            disposeWidgets(row);
        }

        this.rows = this.rowFactory.get();
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, getMaxScrollOffset());

        // Re-layout visible rows if bounds are already set
        if (!bounds().equals(Bounds.ZERO)) {
            layoutVisibleRows();
        }
    }

    @Override
    public void measure(int availableWidth, int availableHeight) {
        setMeasuredSize(resolveSize(this.widthConstraint, availableWidth, availableWidth),
                resolveSize(this.heightConstraint, availableHeight, availableHeight));
    }

    @Override
    public void layout(@Nonnull Bounds bounds) {
        super.layout(bounds);
        layoutVisibleRows();
    }

    private void layoutVisibleRows() {
        Bounds bounds = bounds();
        int innerW = bounds.width() - this.padding.horizontalTotal();
        int x = bounds.x() + this.padding.left();
        int y = bounds.y() + this.padding.top();

        int maxVisible = getMaxVisibleRows();
        int end = Math.min(this.rows.size(), this.scrollOffset + maxVisible);
        for (int i = this.scrollOffset; i < end; i++) {
            UIElement row = this.rows.get(i);
            row.measure(innerW, this.rowHeight);
            int rowY = y + (i - this.scrollOffset) * this.rowHeight;
            row.layout(new Bounds(x, rowY, innerW, this.rowHeight));
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Bounds b = bounds();
        int innerW = b.width() - this.padding.horizontalTotal();
        int x = b.x() + this.padding.left();
        int y = b.y() + this.padding.top();

        int maxVisible = getMaxVisibleRows();
        int end = Math.min(this.rows.size(), this.scrollOffset + maxVisible);
        for (int i = this.scrollOffset; i < end; i++) {
            int rowY = y + (i - this.scrollOffset) * this.rowHeight;

            // Alternating row backgrounds
            if (this.alternatingRowColor1 != 0 || this.alternatingRowColor2 != 0) {
                int bgColor = (i % 2 == 0) ? this.alternatingRowColor1 : this.alternatingRowColor2;
                if (bgColor != 0) {
                    graphics.fill(x, rowY, x + innerW, rowY + this.rowHeight - 1, bgColor);
                }
            }

            rows.get(i).render(graphics, mouseX, mouseY, partialTick);
        }

        renderScrollIndicators(graphics);
    }

    @Override
    public void renderOverlay(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int maxVisible = getMaxVisibleRows();
        int end = Math.min(this.rows.size(), this.scrollOffset + maxVisible);
        for (int i = this.scrollOffset; i < end; i++) {
            this.rows.get(i).renderOverlay(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void tick() {
        int maxVisible = getMaxVisibleRows();
        int end = Math.min(this.rows.size(), this.scrollOffset + maxVisible);
        for (int i = this.scrollOffset; i < end; i++) {
            this.rows.get(i).tick();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!bounds().contains((int) mouseX, (int) mouseY) || this.rows.isEmpty()) {
            return false;
        }

        int next = Mth.clamp(this.scrollOffset + (scrollY > 0 ? -1 : 1), 0, getMaxScrollOffset());
        if (next == this.scrollOffset) {
            return false;
        }

        this.scrollOffset = next;
        layoutVisibleRows();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int maxVisible = getMaxVisibleRows();
        int end = Math.min(this.rows.size(), this.scrollOffset + maxVisible);
        for (int i = this.scrollOffset; i < end; i++) {
            UIElement row = this.rows.get(i);
            if (row.bounds().contains((int) mouseX, (int) mouseY) && row.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    public int getMaxVisibleRows() {
        int areaHeight = bounds().height() - this.padding.verticalTotal();
        return Math.max(1, areaHeight / this.rowHeight);
    }

    public int getMaxScrollOffset() {
        return Math.max(0, this.rows.size() - getMaxVisibleRows());
    }

    private void renderScrollIndicators(@Nonnull GuiGraphics graphics) {
        if (getMaxScrollOffset() <= 0) {
            return;
        }

        Bounds b = bounds();
        int indicatorX = b.right() - 12;
        int topY = b.y() + this.padding.top() - 1;
        int bottomY = b.y() + this.padding.top() + getMaxVisibleRows() * this.rowHeight - 7;

        Font font = Minecraft.getInstance().font;
        if (this.scrollOffset > 0) {
            graphics.drawString(font, "▲", indicatorX, topY, SCROLL_HINT_COLOR, false);
        }
        if (this.scrollOffset < getMaxScrollOffset()) {
            graphics.drawString(font, "▼", indicatorX, bottomY, SCROLL_HINT_COLOR, false);
        }
    }

    private void disposeWidgets(@Nonnull UIElement element) {
        if (element instanceof WidgetElement widget) {
            widget.dispose();
        }
        for (UIElement child : element.children()) {
            disposeWidgets(child);
        }
    }

    // Package-private for testing
    int getScrollOffset() {
        return this.scrollOffset;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Builder {

        private int rowHeight = 24;
        private Supplier<List<UIElement>> rowFactory = List::of;
        private SizeConstraint width = SizeConstraint.FILL;
        private SizeConstraint height = SizeConstraint.WRAP;
        private Insets padding = Insets.NONE;
        private int alternatingRowColor1 = UITheme.DEFAULT.rowColor();
        private int alternatingRowColor2 = UITheme.DEFAULT.rowAltColor();

        public Builder rowHeight(int rowHeight) {
            this.rowHeight = rowHeight;
            return this;
        }

        public Builder rowFactory(@Nonnull Supplier<List<UIElement>> rowFactory) {
            this.rowFactory = rowFactory;
            return this;
        }

        public Builder width(@Nonnull SizeConstraint width) {
            this.width = width;
            return this;
        }

        public Builder height(@Nonnull SizeConstraint height) {
            this.height = height;
            return this;
        }

        public Builder padding(@Nonnull Insets padding) {
            this.padding = padding;
            return this;
        }

        public Builder alternatingRowColors(int color1, int color2) {
            this.alternatingRowColor1 = color1;
            this.alternatingRowColor2 = color2;
            return this;
        }

        public ScrollableList build() {
            return new ScrollableList(rowHeight, rowFactory, width, height, padding,
                    alternatingRowColor1, alternatingRowColor2);
        }

    }

}
