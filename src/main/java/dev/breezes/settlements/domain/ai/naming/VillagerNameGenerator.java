package dev.breezes.settlements.domain.ai.naming;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * Deterministically derives a stable, human-readable name from a villager UUID.
 * <p>
 * Names are deterministic because they are derived from UUID bits alone with no saved state.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class VillagerNameGenerator {

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
            "Dagna", "Dagny", "Daric", "Delia", "Dirk", "Dio", "Doran", "Draven",
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
            "Jarl", "Jarvik", "Jojo", "Jorid",
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
     * Generates a deterministic name for the given UUID.
     *
     * @param uuid the villager UUID
     */
    public static String generateName(@Nonnull UUID uuid) {
        // XOR the two 64-bit halves so both halves contribute to the bucket, preventing
        // sequential UUIDs (same high word) from all mapping to adjacent names.
        long mixed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        int index = Math.floorMod(mixed, NAME_POOL.size());
        return NAME_POOL.get(index);
    }

}
