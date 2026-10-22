package dev.breezes.settlements.infrastructure.rendering.border;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.di.ClientSessionResettable;
import dev.breezes.settlements.domain.tags.SettlementsItemTags;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.attachments.SettlementClientView;
import dev.breezes.settlements.infrastructure.minecraft.attachments.SettlementDirectory;
import dev.breezes.settlements.presentation.ui.ClientSurfaceSuppression;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.ArgbColorUtil;
import dev.breezes.settlements.shared.util.ClientMonotonicClock;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Draws the outlines of the settlement claims near the local player as a translucent ribbon standing on the ground,
 * while the player holds an item tagged to show settlement borders. The ribbon fades in and out, and fades with
 * distance toward the edge of its range. Nothing draws while {@link ClientSurfaceSuppression} suppresses settlement
 * borders.
 * <p>
 * The outline and its ground are read again when the ribbon appears, when the player enters another chunk or level, and
 * at a fixed interval while the ribbon shows, which picks up placed and broken blocks and changed claims without
 * listening for them. Each frame re-sends the geometry read last, with the current scroll, fade, and distance falloff.
 */
@ClientSide
@ClientScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SettlementBorderRenderer implements ClientSessionResettable {

    /**
     * How far from the player's chunk claimed chunks are outlined, in chunks.
     */
    private static final int RANGE_CHUNKS = 8;

    /**
     * How many chunks at the far end of the range the ribbon fades out over.
     */
    private static final int RANGE_FADE_CHUNKS = 2;

    private static final double RANGE_BLOCKS = RANGE_CHUNKS * ClaimOutline.CHUNK_WIDTH_BLOCKS;
    private static final double RANGE_FADE_START_BLOCKS =
            (RANGE_CHUNKS - RANGE_FADE_CHUNKS) * ClaimOutline.CHUNK_WIDTH_BLOCKS;

    /**
     * How far the ribbon reaches above flat ground.
     */
    private static final double RIBBON_HEIGHT_BLOCKS = 3.5D;

    /**
     * How far the ribbon reaches out from the face of a step in the ground.
     */
    private static final double RISER_REACH_BLOCKS = 0.5D;

    /**
     * How far below the top of a canopy the ground is looked for, before the ribbon settles for the canopy.
     */
    private static final int MAX_CANOPY_DEPTH_BLOCKS = 32;

    /**
     * How many blocks one repeat of the texture covers, both along the ribbon and up it. It must divide the chunk
     * width: positions along an edge start over at every chunk corner, and the texture only lines up across the corner
     * when the chunk width is a whole number of repeats.
     */
    private static final double TEXTURE_SPAN_BLOCKS = 1.0D;

    /**
     * How long the bars take to rise by one texture span.
     */
    private static final long SCROLL_PERIOD_MILLIS = ClientMonotonicClock.millisIn(ClockTicks.seconds(2));

    /**
     * How long the ribbon takes to fade fully in, and to fade back out.
     */
    private static final long FADE_MILLIS = ClientMonotonicClock.millisIn(ClockTicks.seconds(0.25));

    /**
     * How long the outline read last is drawn while the ribbon shows before it is read again.
     */
    private static final long REREAD_INTERVAL_MILLIS = ClientMonotonicClock.millisIn(ClockTicks.seconds(1));

    /**
     * Opacity on the ground line of the settlement the player stands in.
     */
    private static final float GROUND_ALPHA = 0.85F;

    /**
     * The share of that opacity every other settlement's ribbon is drawn with.
     */
    private static final float OTHER_SETTLEMENT_ALPHA_SHARE = 0.55F;

    /**
     * The color of the ribbon of the settlement the player stands in, unless that settlement is fading.
     */
    private static final int STANDING_IN_RGB = 0x55FF55;

    /**
     * The color of a fading settlement's ribbon, whether or not the player stands in it.
     */
    private static final int FADING_RGB = 0xFF5555;

    /**
     * The color of every other settlement's ribbon.
     */
    private static final int OTHER_RGB = 0xFFFFFF;

    private static final RibbonProfile PROFILE = new RibbonProfile(RIBBON_HEIGHT_BLOCKS, RISER_REACH_BLOCKS);

    /**
     * Floats per vertex in the geometry read last: the position relative to the origin, texture U, color, and the
     * alpha before the per-frame fades.
     */
    private static final int VERTEX_STRIDE = 8;
    private static final int QUAD_STRIDE = 4 * VERTEX_STRIDE;

    private static final long NEVER = Long.MIN_VALUE;

    private final FloatArrayList vertices = new FloatArrayList();
    private final double[] columnGround = new double[ClaimOutline.CHUNK_WIDTH_BLOCKS];
    private final float[] cornerFalloff = new float[4];
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    /**
     * The level the geometry was read from, or null when none is held.
     */
    @Nullable
    private Level readLevel;
    private int readChunkX;
    private int readChunkZ;
    private long readAtMillis;

    // The block corner of the player's chunk when the geometry was read. Vertices are stored relative to it, so their
    // coordinates stay small enough for float precision anywhere in the world.
    private int originX;
    private int originZ;

    private float visibility;
    private long lastFrameMillis = NEVER;

    @Override
    public void onClientSessionEnded() {
        this.forget();
        this.vertices.trim();
        this.visibility = 0.0F;
        this.lastFrameMillis = NEVER;
    }

    /**
     * Draws the ribbon during the stage it belongs to, and does nothing during any other.
     */
    public void render(@Nonnull RenderLevelStageEvent event) {
        // After particles is the stage NeoForge provides for translucent geometry that must sort against translucent
        // terrain under Fabulous graphics; under the others, it draws over the water already on screen
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (ClientSurfaceSuppression.isSettlementBordersSuppressed(minecraft)) {
            // Hidden at once rather than faded, and holding no geometry, so no level outlives a dimension change made
            // while hidden; the first frame shown again reads the outline afresh
            this.forget();
            return;
        }
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level != this.readLevel) {
            this.forget();
        }

        long nowMillis = ClientMonotonicClock.nowMillis();
        boolean shown = holdsBorderItem(player);
        this.advanceVisibility(shown, nowMillis);
        if (shown && this.isStale(level, player, nowMillis)) {
            this.read(level, player, nowMillis);
        }

        if (this.visibility <= 0.0F) {
            if (!shown) {
                this.forget();
            }
            return;
        }
        this.draw(event, player, nowMillis);
    }

    private static boolean holdsBorderItem(@Nonnull LocalPlayer player) {
        return player.getMainHandItem().is(SettlementsItemTags.SHOWS_SETTLEMENT_BORDERS)
                || player.getOffhandItem().is(SettlementsItemTags.SHOWS_SETTLEMENT_BORDERS);
    }

    private void advanceVisibility(boolean shown, long nowMillis) {
        long elapsed = this.lastFrameMillis == NEVER ? 0L : nowMillis - this.lastFrameMillis;
        this.lastFrameMillis = nowMillis;
        float step = (float) elapsed / FADE_MILLIS;
        this.visibility = shown
                ? Math.min(1.0F, this.visibility + step)
                : Math.max(0.0F, this.visibility - step);
    }

    private boolean isStale(@Nonnull Level level, @Nonnull LocalPlayer player, long nowMillis) {
        // The age term covers what no key can see changing: blocks placed or broken, and claims synced since
        ChunkPos chunk = player.chunkPosition();
        return level != this.readLevel
                || chunk.x != this.readChunkX
                || chunk.z != this.readChunkZ
                || nowMillis - this.readAtMillis >= REREAD_INTERVAL_MILLIS;
    }

    private void forget() {
        this.vertices.clear();
        this.readLevel = null;
    }

    private void read(@Nonnull ClientLevel level, @Nonnull LocalPlayer player, long nowMillis) {
        ChunkPos center = player.chunkPosition();
        this.vertices.clear();
        this.readLevel = level;
        this.readChunkX = center.x;
        this.readChunkZ = center.z;
        this.readAtMillis = nowMillis;
        this.originX = center.getMinBlockX();
        this.originZ = center.getMinBlockZ();

        UUID standingIn = SettlementClientView.claimAt(level, player.blockPosition()).orElse(null);
        SettlementDirectory directory = SettlementClientView.directory(level).orElse(null);
        for (ClaimOutline.Edge edge : ClaimOutline.extract(new LevelClaims(level), center.x, center.z, RANGE_CHUNKS)) {
            this.readEdge(level, edge, standingIn, directory);
        }
    }

    private void readEdge(@Nonnull Level level, @Nonnull ClaimOutline.Edge edge, @Nullable UUID standingIn,
                          @Nullable SettlementDirectory directory) {
        for (int column = 0; column < this.columnGround.length; column++) {
            this.columnGround[column] = this.groundAt(level, edge.columnX(column), edge.columnZ(column));
        }
        int after = ClaimOutline.CHUNK_WIDTH_BLOCKS;
        // The ribbon stands on the column past an end it reaches into, which at an inner corner is the column the
        // turning edge reaches into too, so both meet on the same ground. It climbs to the column past an end it goes
        // on along. Past any other end the outline turns away or is unknown, and that ground is not the ribbon's
        double groundBefore = edge.start() < 0.0D || edge.continuesPastStart()
                ? this.groundAt(level, edge.columnX(-1), edge.columnZ(-1))
                : Double.NaN;
        double groundAfter = edge.end() > after || edge.continuesPastEnd()
                ? this.groundAt(level, edge.columnX(after), edge.columnZ(after))
                : Double.NaN;

        // A settlement missing from the directory, as before its first sync, is drawn as healthy
        boolean fading = directory != null
                && directory.find(edge.owner()).map(summary -> summary.fading() != null).orElse(false);
        boolean ownsStandingChunk = edge.owner().equals(standingIn);
        // Fading outranks standing in it: the warning matters most to the player inside the settlement
        int rgb = fading ? FADING_RGB : ownsStandingChunk ? STANDING_IN_RGB : OTHER_RGB;
        float red = ArgbColorUtil.red(rgb) / 255.0F;
        float green = ArgbColorUtil.green(rgb) / 255.0F;
        float blue = ArgbColorUtil.blue(rgb) / 255.0F;
        float alpha = ownsStandingChunk ? GROUND_ALPHA : GROUND_ALPHA * OTHER_SETTLEMENT_ALPHA_SHARE;

        boolean runsAlongX = edge.runsAlongX();
        float plane = (float) (edge.plane() - (runsAlongX ? this.originZ : this.originX));
        double alongOrigin = edge.worldAlong(0.0D) - (runsAlongX ? this.originX : this.originZ);
        PROFILE.emit(this.columnGround, edge.start(), edge.end(), groundBefore, groundAfter, (along, y, weight) -> {
            float relativeAlong = (float) (alongOrigin + along);
            this.vertices.add(runsAlongX ? relativeAlong : plane);
            this.vertices.add((float) y);
            this.vertices.add(runsAlongX ? plane : relativeAlong);
            this.vertices.add((float) (along / TEXTURE_SPAN_BLOCKS));
            this.vertices.add(red);
            this.vertices.add(green);
            this.vertices.add(blue);
            this.vertices.add(alpha * weight);
        });
    }

    /**
     * Returns the height of the ground surface in the column, or NaN when this client does not hold its chunk.
     * <p>
     * The ground is the highest block that stops motion or holds a fluid, looking past leaves and anything under them
     * that does neither, so the ribbon stands on the ground under a tree rather than on its canopy. A roof is ground.
     */
    private double groundAt(@Nonnull Level level, int x, int z) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(x),
                SectionPos.blockToSectionCoord(z));
        if (chunk == null) {
            return Double.NaN;
        }

        int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        int lowest = Math.max(level.getMinBuildHeight(), top - MAX_CANOPY_DEPTH_BLOCKS);
        for (int y = top; y >= lowest; y--) {
            BlockState state = chunk.getBlockState(this.cursor.set(x, y, z));
            if (!state.is(BlockTags.LEAVES) && Heightmap.Types.MOTION_BLOCKING.isOpaque().test(state)) {
                return y + surfaceHeight(level, state, this.cursor);
            }
        }
        return top + 1.0D;
    }

    /**
     * Returns how far above its block position a ground block's top surface stands: the top of its outline, or of its
     * fluid where that is higher, so the ribbon stands on a slab, a path, or a water surface rather than on top of the
     * block space they occupy.
     */
    private static double surfaceHeight(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockPos pos) {
        FluidState fluid = state.getFluidState();
        double fluidTop = fluid.isEmpty() ? 0.0D : fluid.getHeight(level, pos);
        VoxelShape outline = state.getShape(level, pos);
        // An empty outline, such as a moving piston's, has no top. A block without one counts as ground only by holding a
        // fluid, whose surface is then its top, or else by stopping motion, which it does through its whole block space
        double outlineTop = outline.isEmpty()
                ? (fluid.isEmpty() ? 1.0D : 0.0D)
                : Mth.clamp(outline.max(Direction.Axis.Y), 0.0D, 1.0D);
        return Math.max(fluidTop, outlineTop);
    }

    private void draw(@Nonnull RenderLevelStageEvent event, @Nonnull LocalPlayer player, long nowMillis) {
        if (this.vertices.isEmpty()) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float playerX = (float) (Mth.lerp(partialTick, player.xo, player.getX()) - this.originX);
        float playerZ = (float) (Mth.lerp(partialTick, player.zo, player.getZ()) - this.originZ);
        // V falls as height rises, so a growing offset carries the bars upward
        float scroll = (float) Math.floorMod(nowMillis, SCROLL_PERIOD_MILLIS) / SCROLL_PERIOD_MILLIS;

        RenderType renderType = SettlementBorderRenderType.ribbon();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(renderType);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        // Subtract in double precision before the pose narrows to floats
        poseStack.translate(this.originX - camera.x, -camera.y, this.originZ - camera.z);
        Matrix4f pose = poseStack.last().pose();

        float[] data = this.vertices.elements();
        int size = this.vertices.size();
        for (int quad = 0; quad < size; quad += QUAD_STRIDE) {
            if (!this.measureFalloff(data, quad, playerX, playerZ)) {
                continue;
            }
            for (int corner = 0; corner < 4; corner++) {
                int vertex = quad + corner * VERTEX_STRIDE;
                float y = data[vertex + 1];
                buffer.addVertex(pose, data[vertex], y, data[vertex + 2])
                        .setUv(data[vertex + 3], (float) (scroll - y / TEXTURE_SPAN_BLOCKS))
                        .setColor(data[vertex + 4], data[vertex + 5], data[vertex + 6],
                                data[vertex + 7] * this.visibility * this.cornerFalloff[corner]);
            }
        }

        poseStack.popPose();
        bufferSource.endBatch(renderType);
    }

    /**
     * Fills cornerFalloff with the distance falloff at each corner of the quad, returning whether any corner is within
     * range.
     */
    private boolean measureFalloff(@Nonnull float[] data, int quad, float playerX, float playerZ) {
        boolean inRange = false;
        for (int corner = 0; corner < 4; corner++) {
            int vertex = quad + corner * VERTEX_STRIDE;
            float falloff = distanceFalloff(data[vertex] - playerX, data[vertex + 2] - playerZ);
            this.cornerFalloff[corner] = falloff;
            inRange |= falloff > 0.0F;
        }
        return inRange;
    }

    private static float distanceFalloff(float offsetX, float offsetZ) {
        double distance = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
        if (distance <= RANGE_FADE_START_BLOCKS) {
            return 1.0F;
        }
        if (distance >= RANGE_BLOCKS) {
            return 0.0F;
        }
        return (float) ((RANGE_BLOCKS - distance) / (RANGE_BLOCKS - RANGE_FADE_START_BLOCKS));
    }

    /**
     * The claims of a level as this client holds them.
     */
    private record LevelClaims(Level level) implements ClaimOutline.ChunkClaims {

        @Override
        public boolean holds(int chunkX, int chunkZ) {
            return this.level.getChunkSource().getChunkNow(chunkX, chunkZ) != null;
        }

        @Override
        public Optional<UUID> claimant(int chunkX, int chunkZ) {
            LevelChunk chunk = this.level.getChunkSource().getChunkNow(chunkX, chunkZ);
            return chunk == null ? Optional.empty() : SettlementClientView.claimOf(chunk);
        }

    }

}
