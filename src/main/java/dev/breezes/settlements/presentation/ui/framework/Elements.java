package dev.breezes.settlements.presentation.ui.framework;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Static factory for leaf UI elements.
 * Each factory method returns a small self-contained renderer extending {@link BaseElement}.
 */
@ClientSide
public final class Elements {

    // ---- Text ----

    public static UIElement text(@Nonnull Supplier<Component> textSupplier, int color) {
        return new TextElement(textSupplier, color, false);
    }

    public static UIElement centeredText(@Nonnull Supplier<Component> textSupplier, int color) {
        return new TextElement(textSupplier, color, true);
    }

    public static UIElement clickableText(@Nonnull Component text, int color, int hoverColor, @Nonnull Runnable onClick) {
        return new ClickableTextElement(text, color, hoverColor, onClick);
    }

    // ---- Item Icon ----

    public static UIElement itemIcon(@Nonnull Supplier<ItemStack> stackSupplier,
                                     @Nonnull IntSupplier borderColorSupplier,
                                     @Nullable Supplier<ItemStack> tooltipStackSupplier) {
        return new ItemIconElement(stackSupplier, borderColorSupplier, tooltipStackSupplier, null);
    }

    public static UIElement itemIcon(@Nonnull Supplier<ItemStack> stackSupplier,
                                     @Nonnull IntSupplier borderColorSupplier,
                                     @Nullable Supplier<ItemStack> tooltipStackSupplier,
                                     @Nonnull IntSupplier countSupplier) {
        return new ItemIconElement(stackSupplier, borderColorSupplier, tooltipStackSupplier, countSupplier);
    }

    // ---- Rectangle ----

    public static RectBuilder rect() {
        return new RectBuilder();
    }

    // ---- Sprite ----

    public static UIElement sprite(@Nonnull ResourceLocation texture, int width, int height) {
        return new SpriteElement(texture, width, height);
    }

    // ---- Nine Patch ----

    public static UIElement ninePatch(@Nonnull ResourceLocation texture,
                                      int cornerSize,
                                      int textureWidth,
                                      int textureHeight) {
        return new NinePatchElement(texture, cornerSize, textureWidth, textureHeight);
    }

    // ---- Custom ----

    public static UIElement custom(@Nonnull SizeConstraint widthConstraint,
                                   @Nonnull SizeConstraint heightConstraint,
                                   @Nonnull CustomRenderer renderer) {
        return new CustomElement(widthConstraint, heightConstraint, renderer);
    }

    // ---- Spacer / Flex ----

    public static UIElement spacer(int width, int height) {
        return new SpacerElement(SizeConstraint.fixed(width), SizeConstraint.fixed(height));
    }

    public static UIElement flexSpacer() {
        return new SpacerElement(SizeConstraint.FILL, SizeConstraint.FILL);
    }

    // ---- Dividers ----

    public static UIElement hLine(int color) {
        return new HLineElement(color);
    }

    // ---- Functional Interfaces ----

    @FunctionalInterface
    public interface CustomRenderer {
        void render(@Nonnull GuiGraphics graphics, @Nonnull Bounds bounds, int mouseX, int mouseY, float partialTick);
    }

    // ---- Inner Element Classes ----

    private static class TextElement extends BaseElement {

        private final Supplier<Component> textSupplier;
        private final int color;
        private final boolean centered;

        TextElement(@Nonnull Supplier<Component> textSupplier, int color, boolean centered) {
            super(centered ? SizeConstraint.FILL : SizeConstraint.WRAP,
                    SizeConstraint.WRAP,
                    Insets.NONE);
            this.textSupplier = textSupplier;
            this.color = color;
            this.centered = centered;
        }

        @Override
        public void measure(int availableWidth, int availableHeight) {
            Font font = Minecraft.getInstance().font;
            Component text = textSupplier.get();
            int textWidth = font.width(text);
            int textHeight = font.lineHeight;

            int w = resolveSize(widthConstraint, availableWidth, textWidth);
            int h = resolveSize(heightConstraint, availableHeight, textHeight);
            setMeasuredSize(w, h);
        }

        @Override
        public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            Component text = textSupplier.get();
            Bounds b = bounds();

            if (centered) {
                graphics.drawCenteredString(font, text, b.x() + b.width() / 2, b.y(), color);
            } else {
                graphics.drawString(font, text, b.x(), b.y(), color, false);
            }
        }

    }

    private static class ClickableTextElement extends BaseElement {

        private final Component text;
        private final int color;
        private final int hoverColor;
        private final Runnable onClick;

        ClickableTextElement(@Nonnull Component text, int color, int hoverColor, @Nonnull Runnable onClick) {
            super(SizeConstraint.WRAP, SizeConstraint.WRAP, Insets.NONE);
            this.text = text;
            this.color = color;
            this.hoverColor = hoverColor;
            this.onClick = onClick;
        }

        @Override
        public void measure(int availableWidth, int availableHeight) {
            Font font = Minecraft.getInstance().font;
            setMeasuredSize(
                    resolveSize(widthConstraint, availableWidth, font.width(text)),
                    resolveSize(heightConstraint, availableHeight, font.lineHeight)
            );
        }

        @Override
        public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            Bounds b = bounds();
            boolean hovered = b.contains(mouseX, mouseY);
            int renderColor = hovered ? hoverColor : color;
            graphics.drawString(font, text, b.x(), b.y(), renderColor, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && bounds().contains((int) mouseX, (int) mouseY)) {
                onClick.run();
                return true;
            }
            return false;
        }

    }

    private static class ItemIconElement extends BaseElement {

        private static final int ICON_SIZE = 16;
        private static final int BORDER_SIZE = 2;
        private static final int TOTAL_SIZE = ICON_SIZE + BORDER_SIZE * 2;
        private static final int ICON_BACKGROUND_COLOR = 0xFF1E1E1E;

        private final Supplier<ItemStack> stackSupplier;
        private final IntSupplier borderColorSupplier;
        @Nullable
        private final Supplier<ItemStack> tooltipStackSupplier;
        @Nullable
        private final IntSupplier countSupplier;

        ItemIconElement(@Nonnull Supplier<ItemStack> stackSupplier,
                        @Nonnull IntSupplier borderColorSupplier,
                        @Nullable Supplier<ItemStack> tooltipStackSupplier,
                        @Nullable IntSupplier countSupplier) {
            super(SizeConstraint.fixed(TOTAL_SIZE), SizeConstraint.fixed(TOTAL_SIZE), Insets.NONE);
            this.stackSupplier = stackSupplier;
            this.borderColorSupplier = borderColorSupplier;
            this.tooltipStackSupplier = tooltipStackSupplier;
            this.countSupplier = countSupplier;
        }

        @Override
        public void measure(int availableWidth, int availableHeight) {
            setMeasuredSize(TOTAL_SIZE, TOTAL_SIZE);
        }

        @Override
        public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Bounds b = bounds();
            int x = b.x();
            int y = b.y();

            // Border
            graphics.fill(x, y, x + TOTAL_SIZE, y + TOTAL_SIZE, borderColorSupplier.getAsInt());

            // Inner background
            graphics.fill(x + 1, y + 1, x + TOTAL_SIZE - 1, y + TOTAL_SIZE - 1, ICON_BACKGROUND_COLOR);

            // Item icon (count=1 since we don't want Minecraft to render the count natively)
            ItemStack stack = stackSupplier.get().copyWithCount(1);
            Font font = Minecraft.getInstance().font;
            graphics.renderItem(stack, x + BORDER_SIZE, y + BORDER_SIZE);
            graphics.renderItemDecorations(font, stack, x + BORDER_SIZE, y + BORDER_SIZE);

            int count = this.countSupplier == null ? stack.getCount() : this.countSupplier.getAsInt();
            if (count > 1) {
                String countText = String.valueOf(count);

                float countTextScale = 0.6F;
                float scaledTextWidth = font.width(countText) * countTextScale;
                float scaledTextHeight = 8 * countTextScale;
                float textX = x + BORDER_SIZE + ICON_SIZE - scaledTextWidth;
                float textY = y + BORDER_SIZE + ICON_SIZE - scaledTextHeight;

                graphics.pose().pushPose();
                graphics.pose().translate(textX, textY, 200.0F);
                graphics.pose().scale(countTextScale, countTextScale, 1.0F);

                // Draw at (0, 0) because we already translated to the correct position
                graphics.drawString(font, countText, 0, 0, 0xFFFFFFFF, true);
                graphics.pose().popPose();
            }
        }

        @Override
        public void renderOverlay(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (tooltipStackSupplier == null || !bounds().contains(mouseX, mouseY)) {
                return;
            }
            ItemStack tooltipStack = tooltipStackSupplier.get();
            if (!tooltipStack.isEmpty()) {
                graphics.renderTooltip(Minecraft.getInstance().font, tooltipStack, mouseX, mouseY);
            }
        }

    }

    // ---- RectElement with Builder ----

    public static class RectBuilder {

        private int fillColor = 0;
        private int borderColor = 0;
        @Nullable
        private ResourceLocation texture;
        private float u = 0;
        private float v = 0;
        private float uWidth = 0;
        private float vHeight = 0;
        private SizeConstraint width = SizeConstraint.FILL;
        private SizeConstraint height = SizeConstraint.FILL;

        private RectBuilder() {
        }

        public RectBuilder color(int fillColor) {
            this.fillColor = fillColor;
            return this;
        }

        public RectBuilder border(int borderColor) {
            this.borderColor = borderColor;
            return this;
        }

        public RectBuilder texture(@Nonnull ResourceLocation texture, float u, float v, float uWidth, float vHeight) {
            this.texture = texture;
            this.u = u;
            this.v = v;
            this.uWidth = uWidth;
            this.vHeight = vHeight;
            return this;
        }

        public RectBuilder width(@Nonnull SizeConstraint width) {
            this.width = width;
            return this;
        }

        public RectBuilder height(@Nonnull SizeConstraint height) {
            this.height = height;
            return this;
        }

        public UIElement build() {
            return new RectElement(fillColor, borderColor, texture, u, v, uWidth, vHeight, width, height);
        }

    }

    private static class RectElement extends BaseElement {

        private final int fillColor;
        private final int borderColor;
        @Nullable
        private final ResourceLocation texture;
        private final float u;
        private final float v;
        private final float uWidth;
        private final float vHeight;

        RectElement(int fillColor, int borderColor,
                    @Nullable ResourceLocation texture, float u, float v, float uWidth, float vHeight,
                    @Nonnull SizeConstraint widthConstraint, @Nonnull SizeConstraint heightConstraint) {
            super(widthConstraint, heightConstraint, Insets.NONE);
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            this.texture = texture;
            this.u = u;
            this.v = v;
            this.uWidth = uWidth;
            this.vHeight = vHeight;
        }

        @Override
        public void measure(int availableWidth, int availableHeight) {
            setMeasuredSize(
                    resolveSize(widthConstraint, availableWidth, 0),
                    resolveSize(heightConstraint, availableHeight, 0)
            );
        }

        @Override
        public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Bounds b = bounds();

            if (fillColor != 0) {
                graphics.fill(b.x(), b.y(), b.right(), b.bottom(), fillColor);
            }

            if (texture != null) {
                graphics.blit(texture, b.x(), b.y(), u, v, b.width(), b.height(),
                        (int) uWidth, (int) vHeight);
            }

            if (borderColor != 0) {
                graphics.hLine(b.x(), b.right() - 1, b.y(), borderColor);
                graphics.hLine(b.x(), b.right() - 1, b.bottom() - 1, borderColor);
                graphics.vLine(b.x(), b.y(), b.bottom() - 1, borderColor);
                graphics.vLine(b.right() - 1, b.y(), b.bottom() - 1, borderColor);
            }
        }

    }

    private static class SpriteElement extends BaseElement {

        private final ResourceLocation texture;
        private final int spriteWidth;
        private final int spriteHeight;

        SpriteElement(@Nonnull ResourceLocation texture, int width, int height) {
            super(SizeConstraint.fixed(width), SizeConstraint.fixed(height), Insets.NONE);
            this.texture = texture;
            this.spriteWidth = width;
            this.spriteHeight = height;
        }

        @Override
        public void measure(int availableWidth, int availableHeight) {
            setMeasuredSize(spriteWidth, spriteHeight);
        }

        @Override
        public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Bounds b = bounds();
            graphics.blit(texture, b.x(), b.y(), 0, 0, spriteWidth, spriteHeight, spriteWidth, spriteHeight);
        }

    }

    private static class NinePatchElement extends BaseElement {

        private final ResourceLocation texture;
        private final int cornerSize;
        private final int textureWidth;
        private final int textureHeight;

        NinePatchElement(@Nonnull ResourceLocation texture, int cornerSize, int textureWidth, int textureHeight) {
            super(SizeConstraint.FILL, SizeConstraint.FILL, Insets.NONE);
            this.texture = texture;
            this.cornerSize = cornerSize;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
        }

        @Override
        public void measure(int availableWidth, int availableHeight) {
            setMeasuredSize(
                    resolveSize(widthConstraint, availableWidth, cornerSize * 2),
                    resolveSize(heightConstraint, availableHeight, cornerSize * 2)
            );
        }

        @Override
        public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Bounds b = bounds();
            int x = b.x();
            int y = b.y();
            int w = b.width();
            int h = b.height();
            int cs = cornerSize;
            int tw = textureWidth;
            int th = textureHeight;
            int innerTexW = tw - cs * 2;
            int innerTexH = th - cs * 2;
            int innerW = w - cs * 2;
            int innerH = h - cs * 2;

            // Corners
            graphics.blit(texture, x, y, 0, 0, cs, cs, tw, th);                          // top-left
            graphics.blit(texture, x + w - cs, y, tw - cs, 0, cs, cs, tw, th);            // top-right
            graphics.blit(texture, x, y + h - cs, 0, th - cs, cs, cs, tw, th);            // bottom-left
            graphics.blit(texture, x + w - cs, y + h - cs, tw - cs, th - cs, cs, cs, tw, th); // bottom-right

            // Edges (stretched)
            if (innerW > 0) {
                graphics.blit(texture, x + cs, y, innerW, cs, cs, 0, innerTexW, cs, tw, th);                 // top
                graphics.blit(texture, x + cs, y + h - cs, innerW, cs, cs, th - cs, innerTexW, cs, tw, th);  // bottom
            }
            if (innerH > 0) {
                graphics.blit(texture, x, y + cs, cs, innerH, 0, cs, cs, innerTexH, tw, th);                 // left
                graphics.blit(texture, x + w - cs, y + cs, cs, innerH, tw - cs, cs, cs, innerTexH, tw, th);  // right
            }

            // Center (stretched)
            if (innerW > 0 && innerH > 0) {
                graphics.blit(texture, x + cs, y + cs, innerW, innerH, cs, cs, innerTexW, innerTexH, tw, th);
            }
        }

    }

    private static class SpacerElement extends BaseElement {

        SpacerElement(@Nonnull SizeConstraint widthConstraint, @Nonnull SizeConstraint heightConstraint) {
            super(widthConstraint, heightConstraint, Insets.NONE);
        }

        @Override
        public void measure(int availableWidth, int availableHeight) {
            setMeasuredSize(
                    resolveSize(widthConstraint, availableWidth, 0),
                    resolveSize(heightConstraint, availableHeight, 0)
            );
        }

        @Override
        public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // no-op
        }

    }

    private static class CustomElement extends BaseElement {

        private final CustomRenderer renderer;

        CustomElement(@Nonnull SizeConstraint widthConstraint,
                      @Nonnull SizeConstraint heightConstraint,
                      @Nonnull CustomRenderer renderer) {
            super(widthConstraint, heightConstraint, Insets.NONE);
            this.renderer = renderer;
        }

        @Override
        public void measure(int availableWidth, int availableHeight) {
            setMeasuredSize(
                    resolveSize(widthConstraint, availableWidth, 0),
                    resolveSize(heightConstraint, availableHeight, 0)
            );
        }

        @Override
        public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderer.render(graphics, bounds(), mouseX, mouseY, partialTick);
        }

    }

    private static class HLineElement extends BaseElement {

        private final int color;

        HLineElement(int color) {
            super(SizeConstraint.FILL, SizeConstraint.fixed(1), Insets.NONE);
            this.color = color;
        }

        @Override
        public void measure(int availableWidth, int availableHeight) {
            setMeasuredSize(resolveSize(widthConstraint, availableWidth, availableWidth), 1);
        }

        @Override
        public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Bounds b = bounds();
            graphics.hLine(b.x(), b.right() - 1, b.y(), color);
        }

    }

}
