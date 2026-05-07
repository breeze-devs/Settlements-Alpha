package dev.breezes.settlements.domain.ai.schedule;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Per-profession modifiers that shape behavior selection on rest days.
 * <p>
 * Each multiplier scales the effective priority of behaviors in that category:
 * {@code 1.0} = unchanged, {@code > 1.0} = amplified, {@code 0.0} = suppressed entirely.
 * <p>
 * Default policies are available via {@link #defaultFor(VillagerProfessionKey)}.
 */
@Builder
public record RestDayPolicy(
        VillagerProfessionKey profession,
        float heavyWorkMultiplier,
        float lightWorkMultiplier,
        float socialMultiplier,
        float selfCareMultiplier,
        float leisureMultiplier
) {

    private static final Map<VillagerProfessionKey, RestDayPolicy> DEFAULT_POLICIES = Map.ofEntries(
            Map.entry(VillagerProfessionKey.NITWIT, RestDayPolicy.builder()
                    .profession(VillagerProfessionKey.NITWIT)
                    .heavyWorkMultiplier(0.0F)
                    .lightWorkMultiplier(0.0F)
                    .socialMultiplier(1.3F)
                    .selfCareMultiplier(1.5F)
                    .leisureMultiplier(2.0F)
                    .build()),
            Map.entry(VillagerProfessionKey.NONE, RestDayPolicy.builder()
                    .profession(VillagerProfessionKey.NONE)
                    .heavyWorkMultiplier(0.0F)
                    .lightWorkMultiplier(0.0F)
                    .socialMultiplier(2.0F)
                    .selfCareMultiplier(1.3F)
                    .leisureMultiplier(1.5F)
                    .build())
    );

    private static final List<RestDayPolicy> DEFAULT_POLICY_LIST = List.copyOf(DEFAULT_POLICIES.values());

    public static RestDayPolicy defaultFor(VillagerProfessionKey profession) {
        return DEFAULT_POLICIES.getOrDefault(profession, RestDayPolicy.builder()
                .profession(profession)
                .heavyWorkMultiplier(0.1F)
                .lightWorkMultiplier(0.3F)
                .socialMultiplier(1.5F)
                .selfCareMultiplier(1.3F)
                .leisureMultiplier(1.8F)
                .build());
    }

    public static List<RestDayPolicy> defaultPolicies() {
        return DEFAULT_POLICY_LIST;
    }

}
