package dev.breezes.settlements.infrastructure.rendering.border;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * The render types the settlement border ribbon draws with: unlit, textured quads whose per-vertex alpha blends them
 * into the scene, tested against its depth, visible from both faces, and sorted back to front before drawing so that
 * overlapping ribbons blend in order.
 * <p>
 * The ribbon draws into the particles target. Under Fabulous graphics that target is composited with translucent
 * terrain by depth, so the ribbon writes its depth there: without it, the compositor places the ribbon on the solid
 * surface behind it, beneath any water in between. Under other graphics modes the target is the main one and the ribbon
 * writes no depth, so the weather drawn after it still shows through.
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SettlementBorderRenderType {

    /**
     * White, with horizontal bars in its alpha. Loaded without clamping, so it repeats across the ribbon.
     */
    private static final ResourceLocation TEXTURE = ResourceLocationUtil.mod("textures/misc/settlement_border.png");

    private static final RenderType RIBBON = create("settlements:settlement_border", RenderStateShard.COLOR_WRITE);

    private static final RenderType FABULOUS_RIBBON = create("settlements:settlement_border_fabulous",
            RenderStateShard.COLOR_DEPTH_WRITE);

    /**
     * Returns the render type for the current graphics mode.
     */
    static RenderType ribbon() {
        return Minecraft.useShaderTransparency() ? FABULOUS_RIBBON : RIBBON;
    }

    private static RenderType create(@Nonnull String name, @Nonnull RenderStateShard.WriteMaskStateShard writeMask) {
        return RenderType.create(name, DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256,
                false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
                        // Linear filtering keeps the soft edges of the bars smooth as they scroll
                        .setTextureState(new RenderStateShard.TextureStateShard(TEXTURE, true, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(writeMask)
                        .setOutputState(RenderStateShard.PARTICLES_TARGET)
                        .createCompositeState(false));
    }

}
