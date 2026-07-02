package dev.breezes.settlements.domain.economy.catalog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class RestockFacetCodec {

    public static final Codec<RestockFacet> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("below").forGetter(RestockFacet::below),
                    Codec.INT.fieldOf("buyPricePerUnit").forGetter(RestockFacet::buyPricePerUnit),
                    Codec.INT.fieldOf("priority").forGetter(RestockFacet::priority)
            ).apply(instance, RestockFacet::new));

}
