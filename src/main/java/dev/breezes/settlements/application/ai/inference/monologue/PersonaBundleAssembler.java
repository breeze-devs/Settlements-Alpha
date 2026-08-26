package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.dialogue.DialogueFacet;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.naming.VillagerNameDirectory;
import dev.breezes.settlements.domain.genetics.GeneSignal;
import dev.breezes.settlements.domain.personality.PersonalityStatus;
import dev.breezes.settlements.domain.personality.VillagerPersonality;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerPersonalityAttachment;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Converts live villager state into a structured persona bundle for SIS.
 * <p>
 * Facets are derived here (alongside the persona card) — they describe who the villager is, not what they know.
 * Keeping assembly cohesive in one class also means persona enrichment has one obvious place to land.
 * <p>
 * Anchor reads use vanilla brain memory. JOB_SITE and HOME are Optional: absent means the
 * villager has no job site / bed assigned, and those anchors must be omitted from the wire
 * (Gson's default null-omission behavior handles this without serializeNulls).
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class PersonaBundleAssembler {

    private final VillagerNameDirectory nameDirectory;
    private final VillagerFacetDeriver facetDeriver;

    public PersonaBundle assemble(@Nonnull BaseVillager villager) {
        List<DialogueFacet> facets = this.facetDeriver.derive(villager);
        Anchors anchors = buildAnchors(villager);

        PersonaBundle.PersonaBundleBuilder builder = PersonaBundle.builder()
                .name(this.nameDirectory.resolve(villager.getUUID()))
                .profession(villager.getProfession().id())
                .geneSignals(GeneSignal.allFrom(villager.getGenetics()))
                .anchors(anchors);

        facets.forEach(builder::facet);
        applyPersonaEnrichment(villager, builder);
        return builder.build();
    }

    /**
     * Only a READY persona has an authored character sketch/speech style; PENDING or FAILED villagers fall
     * back to the gene/facet grounding already in the bundle, so those fields are left unset.
     */
    private static void applyPersonaEnrichment(@Nonnull BaseVillager villager, PersonaBundle.PersonaBundleBuilder builder) {
        VillagerPersonality personality = VillagerPersonalityAttachment.read(villager);
        if (personality.status() != PersonalityStatus.READY) {
            return;
        }

        if (!personality.speechStyle().isBlank()) {
            builder.speechStyle(personality.speechStyle());
        }

        if (!personality.characterSketch().isBlank()) {
            builder.characterSketch(personality.characterSketch());
        }
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
