package dev.breezes.settlements.infrastructure.minecraft.items;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joml.Vector3f;

@AllArgsConstructor
@Getter
public enum TotemMode {

    VANILLA(0, "item.settlements.villager_totem.mode.vanilla", 0.25F, 0.85F, 0.95F),
    SETTLEMENTS(1, "item.settlements.villager_totem.mode.settlements", 0.20F, 0.85F, 0.25F),
    STASIS(2, "item.settlements.villager_totem.mode.stasis", 0.45F, 0.45F, 0.45F);

    private static final TotemMode DEFAULT_MODE = SETTLEMENTS;
    private static final TotemMode[] MODES = values();

    private final int serializedId;
    private final String translationKey;
    private final float hueRed;
    private final float hueGreen;
    private final float hueBlue;

    public static TotemMode defaultMode() {
        return DEFAULT_MODE;
    }

    public static TotemMode fromSerializedId(int serializedId) {
        for (TotemMode mode : MODES) {
            if (mode.serializedId == serializedId) {
                return mode;
            }
        }
        return DEFAULT_MODE;
    }

    public TotemMode next() {
        return MODES[(ordinal() + 1) % MODES.length];
    }

    /**
     * This mode's identity color, as RGB in [0,1]. The single authority for the mode's hue — every mode-colored
     * surface derives from this rather than restating it.
     */
    public Vector3f hue() {
        return new Vector3f(hueRed, hueGreen, hueBlue);
    }

    public boolean isAlreadyTargetType(boolean isSettlementsVillager,
                                       boolean isVanillaVillagerInStasis) {
        return switch (this) {
            case VANILLA -> !isSettlementsVillager && !isVanillaVillagerInStasis;
            case SETTLEMENTS -> isSettlementsVillager;
            case STASIS -> !isSettlementsVillager && isVanillaVillagerInStasis;
        };
    }

    public boolean convertsToVanilla() {
        return this == VANILLA || this == STASIS;
    }

    public boolean keepsVanillaVillagerInStasis() {
        return this == STASIS;
    }

}
