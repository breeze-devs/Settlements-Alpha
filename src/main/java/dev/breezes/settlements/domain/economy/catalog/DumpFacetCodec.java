package dev.breezes.settlements.domain.economy.catalog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DumpFacetCodec {

    public static final Codec<DumpFacet> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("above").forGetter(DumpFacet::above)
            ).apply(instance, DumpFacet::new));

}
