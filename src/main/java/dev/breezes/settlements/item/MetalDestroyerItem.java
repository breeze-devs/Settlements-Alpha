package dev.breezes.settlements.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class MetalDestroyerItem extends Item {

    public MetalDestroyerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide() || context.getPlayer() == null) {
            return InteractionResult.FAIL;
        }

        BlockPos targetPos = context.getClickedPos();
        boolean isMetal = context.getLevel().getBlockState(targetPos).is(BlockTags.IRON_ORES);
        if (isMetal) {
            context.getLevel().destroyBlock(targetPos, true);
        }

        context.getItemInHand().hurtAndBreak(1, context.getPlayer(), (player) -> player.broadcastBreakEvent(player.getUsedItemHand()));
        return InteractionResult.SUCCESS;
    }
}
