package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.memory.SensedSiteReader;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.SensedSites;
import dev.breezes.settlements.domain.ai.memory.SiteCoord;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.GlobalPos;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the spatial {@link Snapshot} sent to SIS from a villager's sensed site memories.
 * <p>
 * Two halves, deliberately separated so the language-bearing transform is unit-testable without
 * Minecraft:
 * <ul>
 *   <li>The Minecraft edge — {@link #assemble(BaseVillager)} — reads the live brain through
 *       {@link SensedSiteReader} (server-thread only; see that class for the threading contract).</li>
 *   <li>The pure transform — {@link #toSnapshot(SensedSites)} — maps the read result to the wire
 *       shape with no entity reference.</li>
 * </ul>
 * Coordinates are sent RAW and unordered: the store already caps each site list, and SIS owns all
 * proximity sorting, cardinal-direction, clustering, and filtering math — the mod must not pre-shape it.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SnapshotAssembler {

    /**
     * Every spatial-site memory identifier ends in this suffix, so stripping it is a uniform
     * transform across all site types (see {@link #toWireToken(String)}).
     */
    private static final String SITE_IDENTIFIER_SUFFIX = "_sites";

    private final SensedSiteReader sensedSiteReader;

    public Snapshot assemble(@Nonnull BaseVillager villager) {
        SensedSites sensedSites = this.sensedSiteReader.read(villager.getSettlementsBrain());
        return toSnapshot(sensedSites);
    }

    /**
     * Pure projection of sensed sites to the SIS wire snapshot. An empty read yields an empty
     * snapshot that still serializes as {@code {"sites":{}}}, matching the SIS contract for an empty snapshot.
     */
    static Snapshot toSnapshot(@Nonnull SensedSites sensedSites) {
        Snapshot.SnapshotBuilder builder = Snapshot.builder();
        for (Map.Entry<MemoryType<List<GlobalPos>>, List<SiteCoord>> entry : sensedSites.coordsByType().entrySet()) {
            String token = toWireToken(entry.getKey().identifier());
            List<int[]> coords = entry.getValue().stream()
                    .map(coord -> new int[]{coord.x(), coord.y(), coord.z()})
                    .toList();
            builder.site(token, coords);
        }
        return builder.build();
    }

    /**
     * Translates a memory identifier into the SIS wire token: strip the {@code _sites} suffix and
     * uppercase (e.g. {@code ripe_melon_sites → RIPE_MELON}, {@code cultivation_totem_sites →
     * CULTIVATION_TOTEM}).
     * <p>
     * This bridges two independently-chosen vocabularies: the mod stores lower-case, suffixed
     * identifiers, while SIS's spatial phrasing table recognizes upper-case, suffix-less tokens.
     * The wire vocabulary is an inference concern, so the mapping lives here rather than on the
     * domain {@code MemoryType}.
     */
    static String toWireToken(@Nonnull String identifier) {
        String base = identifier;
        if (base.endsWith(SITE_IDENTIFIER_SUFFIX)) {
            base = base.substring(0, base.length() - SITE_IDENTIFIER_SUFFIX.length());
        }
        return base.toUpperCase(Locale.ROOT);
    }

}
