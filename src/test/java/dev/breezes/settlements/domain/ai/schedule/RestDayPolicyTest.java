package dev.breezes.settlements.domain.ai.schedule;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestDayPolicyTest {

    @Test
    void defaultPolicies_existForKeyProfessions() {
        assertEquals(VillagerProfessionKey.FARMER, RestDayPolicy.defaultFor(VillagerProfessionKey.FARMER).profession());
        assertEquals(VillagerProfessionKey.BUTCHER, RestDayPolicy.defaultFor(VillagerProfessionKey.BUTCHER).profession());
        assertEquals(VillagerProfessionKey.LIBRARIAN, RestDayPolicy.defaultFor(VillagerProfessionKey.LIBRARIAN).profession());
        assertEquals(VillagerProfessionKey.FISHERMAN, RestDayPolicy.defaultFor(VillagerProfessionKey.FISHERMAN).profession());
        assertEquals(VillagerProfessionKey.MASON, RestDayPolicy.defaultFor(VillagerProfessionKey.MASON).profession());
        assertEquals(VillagerProfessionKey.SHEPHERD, RestDayPolicy.defaultFor(VillagerProfessionKey.SHEPHERD).profession());
        assertEquals(VillagerProfessionKey.NITWIT, RestDayPolicy.defaultFor(VillagerProfessionKey.NITWIT).profession());
    }

    @Test
    void nitwitHasNoHeavyWorkMultiplier() {
        RestDayPolicy policy = RestDayPolicy.defaultFor(VillagerProfessionKey.NITWIT);

        assertEquals(0.0F, policy.heavyWorkMultiplier(), 0.0001F);
    }

    @Test
    void unspecifiedProfessionUsesReasonableDefaults() {
        VillagerProfessionKey custom = VillagerProfessionKey.of("custom_job");

        RestDayPolicy policy = RestDayPolicy.defaultFor(custom);

        assertEquals(custom, policy.profession());
        assertEquals(0.1F, policy.heavyWorkMultiplier(), 0.0001F);
        assertEquals(1.5F, policy.socialMultiplier(), 0.0001F);
    }

}
