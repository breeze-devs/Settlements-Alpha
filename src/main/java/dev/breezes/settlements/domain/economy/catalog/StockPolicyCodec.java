package dev.breezes.settlements.domain.economy.catalog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Optional;

/**
 * Datapack codec for {@link StockPolicy}. A policy must define at least one of restock/offer/dump,
 * and rung ordering across the facets must hold; both invariants are enforced by the record's
 * compact constructor, so a violation simply fails decoding of that entry.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class StockPolicyCodec {

    public static final Codec<StockPolicy> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(StockPolicy::id),
                    ItemMatchCodec.CODEC.fieldOf("match").forGetter(StockPolicy::match),
                    RestockFacetCodec.CODEC.optionalFieldOf("restock").forGetter(policy -> Optional.ofNullable(policy.restock())),
                    OfferFacetCodec.CODEC.optionalFieldOf("offer").forGetter(policy -> Optional.ofNullable(policy.offer())),
                    DumpFacetCodec.CODEC.optionalFieldOf("dump").forGetter(policy -> Optional.ofNullable(policy.dump()))
            ).apply(instance, (id, match, restock, offer, dump) -> StockPolicy.builder()
                    .id(id)
                    .match(match)
                    .restock(restock.orElse(null))
                    .offer(offer.orElse(null))
                    .dump(dump.orElse(null))
                    .build()));

}
