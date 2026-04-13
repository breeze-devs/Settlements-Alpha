package dev.breezes.settlements.domain.entities;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Domain-layer identity for a villager profession.
 * <p>
 * Convert at the infrastructure boundary: {@code new VillagerProfessionKey(profession.name())}.
 * Never import VillagerProfession in domain code.
 */
public record VillagerProfessionKey(@Nonnull String id) {

    // ---- Vanilla professions ----
    public static final VillagerProfessionKey NONE = new VillagerProfessionKey("none");
    public static final VillagerProfessionKey ARMORER = new VillagerProfessionKey("armorer");
    public static final VillagerProfessionKey BUTCHER = new VillagerProfessionKey("butcher");
    public static final VillagerProfessionKey CARTOGRAPHER = new VillagerProfessionKey("cartographer");
    public static final VillagerProfessionKey CLERIC = new VillagerProfessionKey("cleric");
    public static final VillagerProfessionKey FARMER = new VillagerProfessionKey("farmer");
    public static final VillagerProfessionKey FISHERMAN = new VillagerProfessionKey("fisherman");
    public static final VillagerProfessionKey FLETCHER = new VillagerProfessionKey("fletcher");
    public static final VillagerProfessionKey LEATHERWORKER = new VillagerProfessionKey("leatherworker");
    public static final VillagerProfessionKey LIBRARIAN = new VillagerProfessionKey("librarian");
    public static final VillagerProfessionKey MASON = new VillagerProfessionKey("mason");
    public static final VillagerProfessionKey NITWIT = new VillagerProfessionKey("nitwit");
    public static final VillagerProfessionKey SHEPHERD = new VillagerProfessionKey("shepherd");
    public static final VillagerProfessionKey TOOLSMITH = new VillagerProfessionKey("toolsmith");
    public static final VillagerProfessionKey WEAPONSMITH = new VillagerProfessionKey("weaponsmith");

    // ---- Custom Settlements professions ----
    // Add constants here as new professions are introduced.
    // Ensure the string matches the name used during NeoForge registry registration.

    public VillagerProfessionKey {
        Objects.requireNonNull(id, "id");
    }

}
