package dev.breezes.settlements.domain.economy.catalog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class OfferFacetCodec {

    public static final Codec<OfferFacet> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("above").forGetter(OfferFacet::above),
                    Codec.INT.fieldOf("basePrice").forGetter(OfferFacet::basePrice),
                    Codec.INT.fieldOf("priceJitter").forGetter(OfferFacet::priceJitter),
                    Codec.INT.fieldOf("bundleSize").forGetter(OfferFacet::bundleSize)
            ).apply(instance, OfferFacet::new));

}
