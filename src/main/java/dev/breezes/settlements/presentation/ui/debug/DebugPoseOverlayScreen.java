package dev.breezes.settlements.presentation.ui.debug;

import dev.breezes.settlements.infrastructure.rendering.animation.debug.DebugPoseOverride;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Non-pausing debug screen with sliders that drive {@link DebugPoseOverride}.
 *
 * <p>The override turns on when this screen opens and off when it closes, so the world reverts to
 * the regular animation system on ESC. Slider labels show the live numeric value in the same units
 * as the {@code *Poses.java} factory helpers ({@code arms(pitch, yaw, roll)} in degrees,
 * translation in world units), so the authoring loop is: drag, read, paste.</p>
 */
@ClientSide
public final class DebugPoseOverlayScreen extends Screen {

    private static final int PANEL_LEFT = 6;
    private static final int PANEL_TOP = 6;
    private static final int PANEL_PADDING = 6;
    private static final int SLIDER_WIDTH = 220;
    private static final int SLIDER_HEIGHT = 18;
    private static final int ROW_SPACING = 2;
    private static final int GROUP_SPACING = 6;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 18;
    private static final int TITLE_HEIGHT = 12;

    private static final float ROTATION_RANGE_DEGREES = 180.0F;
    private static final double TRANSLATION_RANGE = 2.0D;

    private final DebugPoseOverride override;

    public DebugPoseOverlayScreen(@Nonnull DebugPoseOverride override) {
        super(Component.literal("Debug pose overlay"));
        this.override = override;
    }

    @Override
    protected void init() {
        this.override.setEnabled(true);

        int x = PANEL_LEFT + PANEL_PADDING;
        int y = PANEL_TOP + PANEL_PADDING + TITLE_HEIGHT + GROUP_SPACING;

        y = this.addRotationGroup(x, y, "Arms",
                this.override::getArmsPitchDegrees, this.override::setArmsPitchDegrees,
                this.override::getArmsYawDegrees, this.override::setArmsYawDegrees,
                this.override::getArmsRollDegrees, this.override::setArmsRollDegrees);
        y = this.addRotationGroup(x, y, "Head",
                this.override::getHeadPitchDegrees, this.override::setHeadPitchDegrees,
                this.override::getHeadYawDegrees, this.override::setHeadYawDegrees,
                this.override::getHeadRollDegrees, this.override::setHeadRollDegrees);
        y = this.addRotationGroup(x, y, "Body",
                this.override::getBodyPitchDegrees, this.override::setBodyPitchDegrees,
                this.override::getBodyYawDegrees, this.override::setBodyYawDegrees,
                this.override::getBodyRollDegrees, this.override::setBodyRollDegrees);
        y = this.addTranslationGroup(x, y, "Arms reach",
                this.override::getArmsTranslationX, this.override::setArmsTranslationX,
                this.override::getArmsTranslationY, this.override::setArmsTranslationY,
                this.override::getArmsTranslationZ, this.override::setArmsTranslationZ);

        this.addRenderableWidget(Button.builder(Component.literal("Reset"), b -> this.resetAndRebuild())
                .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
                .bounds(x + BUTTON_WIDTH + 4, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void onClose() {
        this.override.setEnabled(false);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = SLIDER_WIDTH + (PANEL_PADDING * 2);
        int panelHeight = (12 * (SLIDER_HEIGHT + ROW_SPACING)) + (3 * GROUP_SPACING) + BUTTON_HEIGHT
                + TITLE_HEIGHT + (PANEL_PADDING * 2) + GROUP_SPACING;
        graphics.fill(PANEL_LEFT, PANEL_TOP, PANEL_LEFT + panelWidth, PANEL_TOP + panelHeight, 0xC0101010);
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(this.font, this.getTitle(),
                PANEL_LEFT + PANEL_PADDING, PANEL_TOP + PANEL_PADDING, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void resetAndRebuild() {
        this.override.reset();
        this.rebuildWidgets();
    }

    private int addRotationGroup(int x, int y, @Nonnull String groupName,
                                 @Nonnull DoubleSupplier pitchGet, @Nonnull DoubleConsumer pitchSet,
                                 @Nonnull DoubleSupplier yawGet, @Nonnull DoubleConsumer yawSet,
                                 @Nonnull DoubleSupplier rollGet, @Nonnull DoubleConsumer rollSet) {
        y = this.addSliderRow(x, y, groupName + " pitch", "°",
                -ROTATION_RANGE_DEGREES, ROTATION_RANGE_DEGREES, 1, pitchGet, pitchSet);
        y = this.addSliderRow(x, y, groupName + " yaw", "°",
                -ROTATION_RANGE_DEGREES, ROTATION_RANGE_DEGREES, 1, yawGet, yawSet);
        y = this.addSliderRow(x, y, groupName + " roll", "°",
                -ROTATION_RANGE_DEGREES, ROTATION_RANGE_DEGREES, 1, rollGet, rollSet);
        return y + GROUP_SPACING;
    }

    private int addTranslationGroup(int x, int y, @Nonnull String groupName,
                                    @Nonnull DoubleSupplier xGet, @Nonnull DoubleConsumer xSet,
                                    @Nonnull DoubleSupplier yGet, @Nonnull DoubleConsumer ySet,
                                    @Nonnull DoubleSupplier zGet, @Nonnull DoubleConsumer zSet) {
        y = this.addSliderRow(x, y, groupName + " X", "", -TRANSLATION_RANGE, TRANSLATION_RANGE, 2, xGet, xSet);
        y = this.addSliderRow(x, y, groupName + " Y", "", -TRANSLATION_RANGE, TRANSLATION_RANGE, 2, yGet, ySet);
        y = this.addSliderRow(x, y, groupName + " Z", "", -TRANSLATION_RANGE, TRANSLATION_RANGE, 2, zGet, zSet);
        return y + GROUP_SPACING;
    }

    private int addSliderRow(int x, int y, @Nonnull String label, @Nonnull String unit,
                             double minValue, double maxValue, int decimals,
                             @Nonnull DoubleSupplier getter, @Nonnull DoubleConsumer setter) {
        this.addRenderableWidget(new ValueSlider(x, y, SLIDER_WIDTH, SLIDER_HEIGHT,
                label, unit, minValue, maxValue, decimals, getter.getAsDouble(), setter));
        return y + SLIDER_HEIGHT + ROW_SPACING;
    }

    private static final class ValueSlider extends AbstractSliderButton {

        private final String label;
        private final String unit;
        private final double minValue;
        private final double maxValue;
        private final int decimals;
        private final DoubleConsumer onChange;

        private ValueSlider(int x, int y, int width, int height,
                            @Nonnull String label, @Nonnull String unit,
                            double minValue, double maxValue, int decimals,
                            double initialValue, @Nonnull DoubleConsumer onChange) {
            super(x, y, width, height, Component.empty(),
                    normalize(initialValue, minValue, maxValue));
            this.label = label;
            this.unit = unit;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.decimals = decimals;
            this.onChange = onChange;
            this.updateMessage();
        }

        private static double normalize(double rawValue, double minValue, double maxValue) {
            if (maxValue <= minValue) {
                return 0.0D;
            }
            return Math.max(0.0D, Math.min(1.0D, (rawValue - minValue) / (maxValue - minValue)));
        }

        private double currentValue() {
            return this.minValue + this.value * (this.maxValue - this.minValue);
        }

        @Override
        protected void updateMessage() {
            String format = "%s: %." + this.decimals + "f%s";
            this.setMessage(Component.literal(String.format(format, this.label, this.currentValue(), this.unit)));
        }

        @Override
        protected void applyValue() {
            this.onChange.accept(this.currentValue());
        }

    }

}
