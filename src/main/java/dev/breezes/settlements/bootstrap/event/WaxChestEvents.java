package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.infrastructure.minecraft.chest.ChestWaxService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Handles wax-on / wax-off interactions for the wax-chest mechanic
 * <p>
 * Sneak + honeycomb on a chest waxes it (red border, HONEYCOMB_WAX_ON sound).
 * Sneak + any axe on a chest unwaxes it (green border, AXE_WAX_OFF sound).
 * The axe never loses durability — this is purely a marking action, not a use.
 * <p>
 * Normal (non-sneak) right-click passes through unchanged so players can still open the chest. A
 * redundant interaction (waxing an already-waxed chest, or scraping an unwaxed one) also passes
 * through untouched — it neither cancels the open nor emits feedback.
 */
@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class WaxChestEvents {

    private static final String KEY_WAXED = "action.settlements.chest.waxed";
    private static final String KEY_UNWAXED = "action.settlements.chest.unwaxed";

    /**
     * Number of particles to emit per AABB edge segment. Kept low to produce a clean visible
     * outline rather than a dense cloud.
     */
    private static final int PARTICLES_PER_EDGE = 6;

    private static final Vector3f COLOR_WAX_RED = new Vector3f(1.0f, 0.0f, 0.0f);
    private static final Vector3f COLOR_WAX_GREEN = new Vector3f(0.0f, 1.0f, 0.0f);

    @SubscribeEvent
    public static void onRightClickBlock(@Nonnull PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(Tags.Blocks.CHESTS) || level.getBlockState(pos).is(Blocks.ENDER_CHEST)) {
            return;
        }

        boolean holdingHoneycomb = event.getItemStack().is(Items.HONEYCOMB);
        boolean holdingAxe = event.getItemStack().getItem() instanceof AxeItem;
        if (!holdingHoneycomb && !holdingAxe) {
            return;
        }

        // The wax flag is server-authoritative
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean currentlyWaxed = ChestWaxService.isWaxed(serverLevel, pos);
        if (currentlyWaxed == holdingHoneycomb) {
            // Redundant interaction: behave as if this handler did not exist and emit no feedback
            return;
        }

        // State will change: claim the interaction so the chest does not open, then apply the flag.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (holdingHoneycomb) {
            waxChest(serverLevel, player, pos);
        } else {
            unwaxChest(serverLevel, player, pos);
        }
    }

    private static void waxChest(@Nonnull ServerLevel level, @Nonnull Player player, @Nonnull BlockPos pos) {
        ChestWaxService.setWaxed(level, pos, true);

        spawnBorderParticles(level, pos, COLOR_WAX_RED);
        level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.displayClientMessage(Component.translatable(KEY_WAXED).withStyle(ChatFormatting.RED), true);
    }

    private static void unwaxChest(@Nonnull ServerLevel level, @Nonnull Player player, @Nonnull BlockPos pos) {
        ChestWaxService.setWaxed(level, pos, false);

        spawnBorderParticles(level, pos, COLOR_WAX_GREEN);
        level.playSound(null, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.displayClientMessage(Component.translatable(KEY_UNWAXED).withStyle(ChatFormatting.GREEN), true);
    }

    /**
     * Emits dust particles along all edges of the chest block's bounding box to produce a visible 3D border flash
     */
    private static void spawnBorderParticles(@Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull Vector3f color) {
        DustParticleOptions dust = new DustParticleOptions(color, 1.0f);

        // Outline the chest as a single unit; for a double-chest, span both halves
        AABB box = new AABB(pos);
        Optional<BlockPos> partner = ChestWaxService.getPartnerPos(level, pos);
        if (partner.isPresent()) {
            box = box.minmax(new AABB(partner.get()));
        }
        box = box.deflate(0.05);

        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        // Emit along all 12 edges. Bottom face (4 edges), top face (4 edges), vertical pillars (4 edges).
        spawnEdgeParticles(level, dust, minX, minY, minZ, maxX, minY, minZ);
        spawnEdgeParticles(level, dust, minX, minY, maxZ, maxX, minY, maxZ);
        spawnEdgeParticles(level, dust, minX, minY, minZ, minX, minY, maxZ);
        spawnEdgeParticles(level, dust, maxX, minY, minZ, maxX, minY, maxZ);

        spawnEdgeParticles(level, dust, minX, maxY, minZ, maxX, maxY, minZ);
        spawnEdgeParticles(level, dust, minX, maxY, maxZ, maxX, maxY, maxZ);
        spawnEdgeParticles(level, dust, minX, maxY, minZ, minX, maxY, maxZ);
        spawnEdgeParticles(level, dust, maxX, maxY, minZ, maxX, maxY, maxZ);

        spawnEdgeParticles(level, dust, minX, minY, minZ, minX, maxY, minZ);
        spawnEdgeParticles(level, dust, maxX, minY, minZ, maxX, maxY, minZ);
        spawnEdgeParticles(level, dust, minX, minY, maxZ, minX, maxY, maxZ);
        spawnEdgeParticles(level, dust, maxX, minY, maxZ, maxX, maxY, maxZ);
    }

    /**
     * Places {@link #PARTICLES_PER_EDGE} evenly-spaced particles along the line from (x1,y1,z1)
     * to (x2,y2,z2). Using sendParticles with count=1 and no spread delta gives crisp placement
     * rather than a randomised cloud.
     */
    private static void spawnEdgeParticles(@Nonnull ServerLevel level, @Nonnull DustParticleOptions dust,
                                           double x1, double y1, double z1,
                                           double x2, double y2, double z2) {
        for (int i = 0; i <= PARTICLES_PER_EDGE; i++) {
            double t = (double) i / PARTICLES_PER_EDGE;
            double x = x1 + (x2 - x1) * t;
            double y = y1 + (y2 - y1) * t;
            double z = z1 + (z2 - z1) * t;
            level.sendParticles(dust, x, y, z, 1, 0, 0, 0, 0);
        }
    }

}
