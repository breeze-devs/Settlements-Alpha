package dev.breezes.settlements.application.economy.demand;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.economy.catalog.ItemMatchCodec;

import java.util.Optional;

public final class DemandSignalCodec {

    public static final Codec<DemandSignal> DEMAND_SIGNAL_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ItemMatchCodec.CODEC.fieldOf("match").forGetter(DemandSignal::match),
                    Codec.INT.fieldOf("desiredCount").forGetter(DemandSignal::desiredCount),
                    Codec.INT.fieldOf("priorityBoost").forGetter(DemandSignal::priorityBoost),
                    Codec.INT.optionalFieldOf("pricePerUnitOverride").forGetter(signal -> Optional.ofNullable(signal.pricePerUnitOverride())),
                    Codec.STRING.fieldOf("source").forGetter(DemandSignal::source),
                    Codec.LONG.fieldOf("createdGameTime").forGetter(DemandSignal::createdGameTime),
                    Codec.LONG.fieldOf("lastTouchedGameTime").forGetter(DemandSignal::lastTouchedGameTime)
            ).apply(instance, (match, desiredCount, priorityBoost, pricePerUnitOverride, source, createdGameTime, lastTouchedGameTime) ->
                    new DemandSignal(match, desiredCount, priorityBoost, pricePerUnitOverride.orElse(null), source, createdGameTime, lastTouchedGameTime)));

    public static final Codec<DemandSignalSet> CODEC = DEMAND_SIGNAL_CODEC.listOf()
            .xmap(DemandSignalSet::new, DemandSignalSet::entries);

    public static final Codec<DemandSignalState> STATE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CODEC.fieldOf("demandSignalSet").forGetter(DemandSignalState::demandSignalSet),
                    Codec.INT.fieldOf("version").forGetter(DemandSignalState::version)
            ).apply(instance, DemandSignalState::new));

    private DemandSignalCodec() {
    }

}
