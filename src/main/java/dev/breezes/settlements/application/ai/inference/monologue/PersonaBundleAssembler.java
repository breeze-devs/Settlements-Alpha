package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.dialogue.DialogueFacet;
import dev.breezes.settlements.application.ai.genetics.PersonalityDeriver;
import dev.breezes.settlements.application.ai.naming.VillagerNameResolver;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;

/**
 * Converts live villager state into a structured persona bundle for SIS.
 * <p>
 * Facets are derived here (alongside the persona card) — they describe who the villager is, not what they know.
 * Keeping assembly cohesive in one class also means a future persona enrichment (characterSketch,
 * settlement metadata) has one obvious place to land.
 * <p>
 * Anchor reads use vanilla brain memory. JOB_SITE and HOME are Optional: absent means the
 * villager has no job site / bed assigned, and those anchors must be omitted from the wire
 * (Gson's default null-omission behavior handles this without serializeNulls).
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class PersonaBundleAssembler {

    private final PersonalityDeriver personalityDeriver;
    private final VillagerNameResolver nameResolver;
    private final VillagerFacetDeriver facetDeriver;

    public PersonaBundle assemble(@Nonnull BaseVillager villager) {
        List<DialogueFacet> facets = this.facetDeriver.derive(villager);
        Anchors anchors = buildAnchors(villager);

        PersonaBundle.PersonaBundleBuilder builder = PersonaBundle.builder()
                .name(this.nameResolver.resolve(villager.getUUID()))
                .profession(villager.getProfession().id())
                .traits(this.personalityDeriver.derivePersonalityTraits(villager.getGenetics()))
                .anchors(anchors);

        facets.forEach(builder::facet);
        return builder.build();
    }

    /**
     * Reads the villager's current position and optional POI memories to produce anchors.
     * blockPosition() is always available; JOB_SITE and HOME are omitted when absent so the
     * Anchors object carries null for those fields (Gson omits nulls by default).
     */
    private static Anchors buildAnchors(@Nonnull BaseVillager villager) {
        BlockPos body = villager.blockPosition();

        int[] jobSitePos = villager.getBrain()
                .getMemory(MemoryModuleType.JOB_SITE)
                .map(GlobalPos::pos)
                .map(PersonaBundleAssembler::toCoords)
                .orElse(null);

        int[] homePos = villager.getBrain()
                .getMemory(MemoryModuleType.HOME)
                .map(GlobalPos::pos)
                .map(PersonaBundleAssembler::toCoords)
                .orElse(null);

        return Anchors.builder()
                .body(toCoords(body))
                .jobSite(jobSitePos)
                .home(homePos)
                .build();
    }

    private static int[] toCoords(@Nonnull BlockPos pos) {
        return new int[]{pos.getX(), pos.getY(), pos.getZ()};
    }

}
