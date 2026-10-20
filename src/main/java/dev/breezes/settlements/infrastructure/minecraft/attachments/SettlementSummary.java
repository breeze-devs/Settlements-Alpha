package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.settlement.model.SettlementFading;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.TimeOfDay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * What a client is shown of one settlement: the projection of its record that a {@link SettlementDirectory} carries.
 * <p>
 * A summary holds only what a client shows, so two summaries are equal when a client would show the same for both.
 *
 * @param name   the settlement's name
 * @param fading its fading status, or null when it is not fading
 */
public record SettlementSummary(String name, @Nullable Fading fading) {

    public static final StreamCodec<ByteBuf, SettlementSummary> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SettlementSummary::name,
            ByteBufCodecs.optional(Fading.STREAM_CODEC), summary -> Optional.ofNullable(summary.fading()),
            (name, fading) -> new SettlementSummary(name, fading.orElse(null)));

    /**
     * Returns the summary of the settlement as stored.
     *
     * @param fadingDuration   the fading time after which a settlement disbands, or null when decay is disabled
     * @param minimumResidents the fewest living members a healthy settlement has
     */
    public static SettlementSummary of(@Nonnull Settlement settlement, @Nullable ClockTicks fadingDuration,
                                       int minimumResidents) {
        SettlementFading fading = settlement.fading();
        // A settlement left fading when decay was disabled never disbands, so it is not shown as falling into ruin
        if (fading == null || fadingDuration == null) {
            return new SettlementSummary(settlement.name(), null);
        }

        // Rounded up, so a settlement never disbands sooner than its summary says. Floored at zero because a fading can
        // outlast a duration shortened since it began, until its next health visit disbands it.
        long ticksLeft = Math.max(0L, fadingDuration.getTicks() - fading.elapsed().getTicks());
        int daysLeft = Math.toIntExact(Math.ceilDiv(ticksLeft, TimeOfDay.TICKS_PER_DAY));
        return new SettlementSummary(settlement.name(),
                new Fading(daysLeft, settlement.roster().entries().size(), minimumResidents));
    }

    /**
     * How a fading settlement stands.
     *
     * @param daysLeft         the whole loaded days it has left before it disbands, rounded up
     * @param livingMembers    the members its roster lists
     * @param minimumResidents the fewest living members a healthy settlement has
     */
    public record Fading(int daysLeft, int livingMembers, int minimumResidents) {

        public static final StreamCodec<ByteBuf, Fading> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Fading::daysLeft,
                ByteBufCodecs.VAR_INT, Fading::livingMembers,
                ByteBufCodecs.VAR_INT, Fading::minimumResidents,
                Fading::new);

    }

}
