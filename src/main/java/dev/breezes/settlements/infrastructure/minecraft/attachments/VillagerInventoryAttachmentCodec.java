package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class VillagerInventoryAttachmentCodec {

    private static final Codec<VillagerInventorySlotState> SLOT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("slot").forGetter(VillagerInventorySlotState::slot),
                    ItemStack.CODEC.fieldOf("stack").forGetter(VillagerInventorySlotState::stack)
            ).apply(instance, VillagerInventorySlotState::new));

    public static final Codec<VillagerInventoryAttachmentState> STATE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.optionalFieldOf("initialized", false).forGetter(VillagerInventoryAttachmentState::initialized),
                    SLOT_CODEC.listOf().optionalFieldOf("slots", List.of()).forGetter(VillagerInventoryAttachmentState::slots)
            ).apply(instance, VillagerInventoryAttachmentState::new));

}
