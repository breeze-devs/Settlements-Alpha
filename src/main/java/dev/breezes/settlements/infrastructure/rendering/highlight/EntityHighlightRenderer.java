package dev.breezes.settlements.infrastructure.rendering.highlight;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import dev.breezes.settlements.shared.util.ArgbColorUtil;
import jakarta.inject.Inject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Drives every {@link EntityHighlightProvider} from {@link RenderLevelStageEvent.Stage#AFTER_ENTITIES} and
 * draws their contributions into the outline buffer.
 * <p>
 * This stage fires after vanilla's own entity render loop but before it flushes the outline buffer and
 * decides whether to run the outline shader for the frame — see {@link OutlineOnlyBufferSource} for why the
 * buffer handed to the entity renderer is wrapped rather than passed through directly.
 */
@ClientSide
@ClientScope
public final class EntityHighlightRenderer {

    /**
     * Each highlighted entity costs a second full model render, so we bound it.
     */
    private static final int MAX_HIGHLIGHTED_ENTITIES = 12;

    private static final Comparator<EntityHighlightProvider> PRECEDENCE = Comparator
            .comparingInt(EntityHighlightProvider::priority)
            .reversed()
            .thenComparing(provider -> provider.getClass().getName());

    private final List<EntityHighlightProvider> orderedProviders;

    @Inject
    EntityHighlightRenderer(Set<EntityHighlightProvider> providers) {
        this.orderedProviders = orderedByPrecedence(providers);
    }

    public void render(@Nonnull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || orderedProviders.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        EntityHighlightFrame frame = EntityHighlightFrame.builder()
                .localPlayer(minecraft.player)
                .partialTick(event.getPartialTick().getGameTimeDeltaPartialTick(false))
                .frustum(event.getFrustum())
                .aimedAtEntity(minecraft.crosshairPickEntity)
                .build();

        Map<Entity, Integer> contributions = collectContributions(frame);
        List<Entity> toRender = selectEntitiesToRender(event, frame, contributions);

        // Short circuit when nothing is highlighted
        if (toRender.isEmpty()) {
            return;
        }

        drawOutlines(minecraft, event, toRender, contributions);
        event.getLevelRenderer().requestOutlineEffect();
    }

    /**
     * Providers run in priority order and claim an entity with {@code putIfAbsent}, so the highest-priority
     * provider to contribute a given entity always wins.
     */
    private Map<Entity, Integer> collectContributions(@Nonnull EntityHighlightFrame frame) {
        Map<Entity, Integer> contributions = new LinkedHashMap<>();
        for (EntityHighlightProvider provider : orderedProviders) {
            provider.contribute(contributions::putIfAbsent, frame);
        }

        return contributions;
    }

    @VisibleForTesting
    static List<EntityHighlightProvider> orderedByPrecedence(@Nonnull Collection<EntityHighlightProvider> providers) {
        return providers.stream().sorted(PRECEDENCE).toList();
    }

    private List<Entity> selectEntitiesToRender(@Nonnull RenderLevelStageEvent event,
                                                @Nonnull EntityHighlightFrame frame,
                                                @Nonnull Map<Entity, Integer> contributions) {
        Frustum frustum = event.getFrustum();
        List<Entity> visible = new ArrayList<>(contributions.size());
        for (Entity entity : contributions.keySet()) {
            // Frustum-only, deliberately: allow seeing a highlighted entity through a wall
            // An entity the player cannot see at gets no outline to reveal it
            if (frustum.isVisible(entity.getBoundingBox()) && !entity.isInvisibleTo(frame.localPlayer())) {
                visible.add(entity);
            }
        }

        if (visible.size() > MAX_HIGHLIGHTED_ENTITIES) {
            visible.sort(byAimThenProximity(event, frame));
            visible = visible.subList(0, MAX_HIGHLIGHTED_ENTITIES);
        }

        return visible;
    }

    /**
     * Orders the entity the player is aiming at ahead of every other contribution, then by proximity.
     */
    private static Comparator<Entity> byAimThenProximity(@Nonnull RenderLevelStageEvent event,
                                                         @Nonnull EntityHighlightFrame frame) {
        Vec3 cameraPos = event.getCamera().getPosition();
        Entity aimedAt = frame.aimedAtEntity();

        return Comparator.comparingInt((Entity entity) -> entity == aimedAt ? 0 : 1)
                .thenComparingDouble(entity -> entity.distanceToSqr(cameraPos));
    }

    private void drawOutlines(@Nonnull Minecraft minecraft,
                              @Nonnull RenderLevelStageEvent event,
                              @Nonnull List<Entity> entities,
                              @Nonnull Map<Entity, Integer> colors) {
        OutlineBufferSource outlineBufferSource = minecraft.renderBuffers().outlineBufferSource();
        MultiBufferSource outlineOnlySource = OutlineOnlyBufferSource.wrapping(outlineBufferSource);
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        PoseStack poseStack = event.getPoseStack();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cameraPos = event.getCamera().getPosition();

        for (Entity entity : entities) {
            int color = colors.get(entity);
            // OutlineBufferSource#getBuffer snapshots this color into a fresh generator at buffer-acquisition
            // time, so it must be set immediately each own render call for resting and hover colors to coexist
            // correctly within the same frame.
            outlineBufferSource.setColor(ArgbColorUtil.red(color), ArgbColorUtil.green(color), ArgbColorUtil.blue(color), ArgbColorUtil.alpha(color));
            renderOutlined(entity, cameraPos, partialTick, poseStack, outlineOnlySource, dispatcher);
        }
    }

    private static void renderOutlined(@Nonnull Entity entity,
                                       @Nonnull Vec3 cameraPos,
                                       float partialTick,
                                       @Nonnull PoseStack poseStack,
                                       @Nonnull MultiBufferSource bufferSource,
                                       @Nonnull EntityRenderDispatcher dispatcher) {
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX()) - cameraPos.x;
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY()) - cameraPos.y;
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - cameraPos.z;
        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());

        dispatcher.render(entity, x, y, z, yaw, partialTick, poseStack, bufferSource, dispatcher.getPackedLightCoords(entity, partialTick));
    }

}
