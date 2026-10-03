package dev.breezes.settlements.domain.ai.threat;

import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.Builder;

import javax.annotation.Nullable;

/**
 * One villager's threat situation for a single assessment, reduced to the scores and facts the response
 * rules decide on.
 * <p>
 * Continuation and entry each carry the nerve of the one option that rule would authorize, so an option
 * ruled out for one rule cannot stand in for a different option the other rule would pick.
 *
 * @param previousVerdict       the verdict this villager had before, or null before its first assessment
 * @param danger                the sum of every aware hostile's {@link ThreatResponseRules#dangerContribution}
 * @param awareHostilePresent   whether any hostile is within awareness
 * @param liveHitPresent        whether a qualifying hit is still live
 * @param continuingNerve       the previously selected option's nerve, or null when there is no selection or its
 *                              option can no longer continue
 * @param sinceEngageableTarget how long the previously selected option has gone without an engageable target; zero
 *                              when this assessment found one
 * @param enteringNerve         the nerve of the first option in precedence order that can engage a sighted hostile,
 *                              or null when none can
 * @param sinceAlarmed          how long since an assessment last decided {@link ThreatDecision#PANIC} for this
 *                              villager; meaningful whenever the previous verdict is PANIC
 */
@Builder
public record ThreatSituation(@Nullable ThreatVerdict previousVerdict,
                              double danger,
                              boolean awareHostilePresent,
                              boolean liveHitPresent,
                              @Nullable Double continuingNerve,
                              ClockTicks sinceEngageableTarget,
                              @Nullable Double enteringNerve,
                              ClockTicks sinceAlarmed) {

}

