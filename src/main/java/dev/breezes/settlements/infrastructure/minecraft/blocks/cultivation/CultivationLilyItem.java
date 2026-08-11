package dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Similar to vanilla lily pad, carrying one line of lore.
 * <p>
 * The lily-pad silhouette already teaches where the item goes, and the placement preview drawn while
 * it is held shows what it would claim before the click resolves, so the tooltip carries only the part
 * neither can express: that the placed pad is what assigns villagers somewhere to farm.
 */
public class CultivationLilyItem extends PlaceOnWaterBlockItem {

    private static final String TOOLTIP_PLACE_KEY = "item.settlements.cultivation_lily.tooltip.place";

    public CultivationLilyItem(@Nonnull Block block, @Nonnull Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context,
                                @Nonnull List<Component> tooltipComponents, @Nonnull TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(TOOLTIP_PLACE_KEY).withStyle(ChatFormatting.GRAY));
    }

}
