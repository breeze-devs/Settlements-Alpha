package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.List;

public class VillagerBrainAttachmentCodec {

    public static final Codec<VillagerBrainAttachmentState> STATE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUIDUtil.CODEC.listOf().optionalFieldOf("owned_wolves", List.of()).forGetter(VillagerBrainAttachmentState::ownedWolves)
            ).apply(instance, VillagerBrainAttachmentState::new));

}
