package dev.breezes.settlements.domain.settlement.naming;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Datapack codec for one {@link SettlementNamePools} file. Either list may be absent, which reads as empty.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementNamePoolsCodec {

    public static final Codec<SettlementNamePools> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.listOf().optionalFieldOf("prefixes", List.of()).forGetter(SettlementNamePools::prefixes),
                    Codec.STRING.listOf().optionalFieldOf("suffixes", List.of()).forGetter(SettlementNamePools::suffixes)
            ).apply(instance, SettlementNamePools::new));

}
