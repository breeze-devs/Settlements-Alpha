package dev.breezes.settlements.infrastructure.minecraft.items;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joml.Vector3f;

@AllArgsConstructor
@Getter
public enum TotemMode {

    VANILLA(0, "item.settlements.villager_totem.mode.vanilla", 0.0F, 1.0F, 0.0F),
    SETTLEMENTS(1, "item.settlements.villager_totem.mode.settlements", 1.0F, 0.84F, 0.0F),
    STASIS(2, "item.settlements.villager_totem.mode.stasis", 0.1F, 0.1F, 0.1F);

    private static final TotemMode DEFAULT_MODE = SETTLEMENTS;
    private static final TotemMode[] MODES = values();

    private final int serializedId;
    private final String translationKey;
    private final float particleRed;
    private final float particleGreen;
    private final float particleBlue;

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

    public Vector3f particleColor() {
        return new Vector3f(particleRed, particleGreen, particleBlue);
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
