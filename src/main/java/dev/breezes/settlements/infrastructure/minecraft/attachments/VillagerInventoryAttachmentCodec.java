package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.inventory.BackpackEntry;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class VillagerInventoryAttachmentCodec {

    private static final Codec<BackpackEntry> ENTRY_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ItemStack.SINGLE_ITEM_CODEC.fieldOf("item").forGetter(BackpackEntry::representative),
                    Codec.INT.fieldOf("count").forGetter(BackpackEntry::count)
            ).apply(instance, BackpackEntry::new));

    public static final Codec<VillagerInventoryAttachmentState> STATE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.optionalFieldOf("initialized", false).forGetter(VillagerInventoryAttachmentState::initialized),
                    ENTRY_CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(VillagerInventoryAttachmentState::entries)
            ).apply(instance, VillagerInventoryAttachmentState::new));

}
