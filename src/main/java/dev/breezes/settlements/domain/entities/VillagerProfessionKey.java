package dev.breezes.settlements.domain.entities;

import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;

/**
 * Domain-layer identity for a villager profession: its full registry id, namespace included, so professions from
 * different mods that share a path stay distinct.
 * <p>
 * Never import VillagerProfession in domain code.
 */
public record VillagerProfessionKey(ResourceLocation id) {

    // Vanilla professions
    public static final VillagerProfessionKey NONE = vanilla("none");
    public static final VillagerProfessionKey ARMORER = vanilla("armorer");
    public static final VillagerProfessionKey BUTCHER = vanilla("butcher");
    public static final VillagerProfessionKey CARTOGRAPHER = vanilla("cartographer");
    public static final VillagerProfessionKey CLERIC = vanilla("cleric");
    public static final VillagerProfessionKey FARMER = vanilla("farmer");
    public static final VillagerProfessionKey FISHERMAN = vanilla("fisherman");
    public static final VillagerProfessionKey FLETCHER = vanilla("fletcher");
    public static final VillagerProfessionKey LEATHERWORKER = vanilla("leatherworker");
    public static final VillagerProfessionKey LIBRARIAN = vanilla("librarian");
    public static final VillagerProfessionKey MASON = vanilla("mason");
    public static final VillagerProfessionKey NITWIT = vanilla("nitwit");
    public static final VillagerProfessionKey SHEPHERD = vanilla("shepherd");
    public static final VillagerProfessionKey TOOLSMITH = vanilla("toolsmith");
    public static final VillagerProfessionKey WEAPONSMITH = vanilla("weaponsmith");

    // Custom Settlements professions
    // Add constants here as new professions are introduced, under the settlements namespace and the name used during
    // NeoForge registry registration.

    public VillagerProfessionKey {
        Validate.notNull(id, "id must not be null");
    }

    private static VillagerProfessionKey vanilla(@Nonnull String path) {
        return new VillagerProfessionKey(ResourceLocation.withDefaultNamespace(path));
    }

}
