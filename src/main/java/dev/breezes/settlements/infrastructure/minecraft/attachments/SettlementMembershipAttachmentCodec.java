package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.application.settlement.membership.SettlementMembership;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementMembershipAttachmentCodec {

    /**
     * A villager's membership. Both fields are required: a membership missing either is unreadable, never read with a
     * guessed value.
     */
    public static final Codec<SettlementMembership> STATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("settlement_id").forGetter(SettlementMembership::settlementId),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(SettlementMembership::dimension)
    ).apply(instance, SettlementMembership::new));

}
