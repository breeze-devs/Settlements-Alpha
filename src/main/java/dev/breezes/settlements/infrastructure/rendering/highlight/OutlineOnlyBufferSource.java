package dev.breezes.settlements.infrastructure.rendering.highlight;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;

import javax.annotation.Nonnull;

/**
 * Wraps {@link OutlineBufferSource} so an entity renderer only ever writes into the outline pass.
 * <p>
 * {@link OutlineBufferSource#getBuffer} takes the non-outline branch for any render type that isn't already
 * an outline type, which writes into both the outline buffer and the wrapped main buffer. Since vanilla has
 * already drawn every on-screen entity for this frame by the time a highlight provider runs, handing an
 * entity renderer the raw {@link OutlineBufferSource} would duplicate that main-buffer draw at identical
 * coordinates — overdraw and z-fighting. This wrapper resolves {@link RenderType#outline()} first so only the
 * outline generator is ever requested.
 */
@ClientSide
@AllArgsConstructor(access = AccessLevel.PRIVATE)
final class OutlineOnlyBufferSource implements MultiBufferSource, SupplementaryEntityPass {

    private final OutlineBufferSource outlineBufferSource;

    static MultiBufferSource wrapping(@Nonnull OutlineBufferSource outlineBufferSource) {
        return new OutlineOnlyBufferSource(outlineBufferSource);
    }

    @Override
    public VertexConsumer getBuffer(@Nonnull RenderType renderType) {
        if (renderType.isOutline()) {
            return outlineBufferSource.getBuffer(renderType);
        }

        // A render type with no outline mapping contributes nothing to a silhouette
        // Dropping it keeps this wrapper outline-only instead of reintroducing the duplicate main-buffer draw.
        return renderType.outline()
                .map(outlineBufferSource::getBuffer)
                .orElse(NoOpVertexConsumer.INSTANCE);
    }

    private enum NoOpVertexConsumer implements VertexConsumer {

        INSTANCE;

        @Nonnull
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        @Nonnull
        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Nonnull
        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Nonnull
        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Nonnull
        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Nonnull
        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            return this;
        }
    }

}
