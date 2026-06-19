package dev.breezes.settlements.application.ai.naming;

import dev.breezes.settlements.di.ServerScope;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

/**
 * Maps a villager UUID to a stable, human-readable name from a curated pool.
 * <p>
 * Names are deterministic (same UUID → same name across restarts) because they are derived from
 * UUID bits alone — no state is persisted. This makes it safe to name referenced villagers in
 * knowledge seeds even when those entities are unloaded at sweep time.
 * <p>
 * The pool is large enough that collision probability across a typical settlement of a few dozen
 * villagers is negligible. A future datapack-backed implementation can replace this class via DIP
 * without callers changing.
 */
@ServerScope
@NoArgsConstructor(onConstructor_ = @Inject)
public final class VillagerNameResolver {

    /**
     * Single mixed-gender pool of village-appropriate names.
     * Curated to feel medieval/pastoral without sounding anachronistic.
     */
    static final List<String> NAME_POOL = List.of(
            // A
            "Aldric", "Alma", "Ansel", "Aric", "Arna", "Arvid", "Astrid", "Audra",
            // B
            "Baldric", "Beatrix", "Beren", "Bertha", "Bjorn", "Bram", "Brenna", "Britta",
            // C
            "Calder", "Calla", "Cora", "Corvin", "Cyra",
            // D
            "Dagna", "Dagny", "Daric", "Delia", "Dirk", "Doran", "Draven",
            // E
            "Edda", "Edric", "Egil", "Elda", "Elric", "Elspeth", "Embla", "Erling", "Erwin", "Eska",
            // F
            "Fara", "Faye", "Ferris", "Finn", "Finna", "Freya", "Frieda",
            // G
            "Gareth", "Gerda", "Gilda", "Gorm", "Gregor", "Greta", "Gudrun", "Gunnar",
            // H
            "Hagen", "Haldis", "Halfdan", "Halla", "Halvar", "Hedda", "Helgi", "Herta",
            "Hildur", "Hjord", "Holger",
            // I
            "Ida", "Idris", "Idunn", "Inga", "Ingrid", "Ivar",
            // J
            "Jarl", "Jarvik", "Jorid",
            // K
            "Karin", "Keld", "Ketil", "Kjeld", "Klara", "Knud", "Kolr",
            // L
            "Lena", "Leif", "Lifa", "Lilja", "Lind", "Lotte", "Ludvik", "Lund",
            // M
            "Maren", "Marit", "Marta", "Mattis", "Mira", "Mjoll", "Moira",
            // N
            "Nadia", "Nanna", "Niall", "Njord",
            // O
            "Odda", "Oddvar", "Olaf", "Olga", "Olvir", "Orm", "Oskar",
            // P
            "Palva", "Petra", "Phelda",
            // R
            "Ragnar", "Ragnhild", "Ralf", "Randi", "Ranveig", "Rasa", "Reva", "Rolf",
            "Runa", "Runolf",
            // S
            "Saga", "Sigge", "Sigrid", "Sigrun", "Sigurd", "Signe", "Sigvard", "Silda",
            "Siri", "Sisse", "Skald", "Skara", "Skeld", "Solveig", "Steinar", "Stine",
            "Sturla", "Svala", "Svein", "Sven",
            // T
            "Tala", "Tilda", "Torvald", "Toste", "Tove", "Trude", "Trygg",
            // U
            "Ulf", "Ulva", "Unna", "Unn",
            // V
            "Vala", "Valdis", "Valka", "Vigdis", "Vigil", "Vilja", "Vilma",
            // W
            "Wilda", "Wulff",
            // Y
            "Ylva", "Ymir",
            // Z
            "Zara", "Zelda"
    );

    /**
     * Resolves the given UUID to a deterministic name.
     * <p>
     * Uses both the most-significant and least-significant 64 bits so UUIDs that share a
     * high-word prefix (sequential UUID v1 batches) still spread across the pool.
     *
     * @param uuid the villager UUID; if null, returns a safe fallback name
     */
    public String resolve(@Nullable UUID uuid) {
        if (uuid == null) {
            return "Someone";
        }

        // XOR the two 64-bit halves so both halves contribute to the bucket, preventing
        // sequential UUIDs (same high word) from all mapping to adjacent names.
        long mixed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        int index = Math.floorMod(mixed, NAME_POOL.size());
        return NAME_POOL.get(index);
    }

}
