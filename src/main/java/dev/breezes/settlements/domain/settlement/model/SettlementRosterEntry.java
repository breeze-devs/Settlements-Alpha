package dev.breezes.settlements.domain.settlement.model;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import lombok.Builder;
import org.apache.commons.lang3.Validate;

import java.util.UUID;

/**
 * One member's entry on a settlement's roster.
 *
 * @param memberId                    the member villager's UUID
 * @param profession                  the member's profession at the last snapshot
 * @param adult                       whether the member was an adult at the last snapshot
 * @param housed                      whether the member had a home at the last snapshot
 * @param gatheringCountAtLastCheckIn the settlement's gathering count at the member's last check-in; non-negative
 */
@Builder
public record SettlementRosterEntry(
        UUID memberId,
        VillagerProfessionKey profession,
        boolean adult,
        boolean housed,
        int gatheringCountAtLastCheckIn
) {

    public SettlementRosterEntry {
        Validate.notNull(memberId, "Roster entry member id must not be null");
        Validate.notNull(profession, "Roster entry profession must not be null for member %s", memberId);
        Validate.isTrue(gatheringCountAtLastCheckIn >= 0,
                "Roster entry gathering count at last check-in must not be negative for member %s: %d",
                memberId, gatheringCountAtLastCheckIn);
    }

}
