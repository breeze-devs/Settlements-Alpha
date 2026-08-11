package dev.breezes.settlements.infrastructure.rendering.zone;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * The render type the zone perimeter wall draws with: unlit, per-vertex-alpha quads that blend with
 * whatever is already on screen instead of replacing it, and read the existing depth buffer without
 * writing their own — so the wall sits among other translucent geometry as a real object standing in
 * the world, occluded by terrain, rather than an overlay painted on top of it.
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ZoneWallRenderType {

    static final RenderType WALL = RenderType.create("settlements:cultivation_zone_wall",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256,
            false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

}
