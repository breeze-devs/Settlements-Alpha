package dev.breezes.settlements.domain.animation;

public enum AnimationArchetype {

    IDLE,
    SWING_HEAVY,
    CAST,
    REEL_IN,
    REEL_OUT,
    POINT,
    INTERACT,
    SURVEY_WITH_SPYGLASS,
    WRITE_TO_MAP,
    EAT,
    ;

    private static final AnimationArchetype[] VALUES = values();

    public byte toNetworkByte() {
        return (byte) this.ordinal();
    }

    public static AnimationArchetype fromNetworkByte(byte encoded) {
        int index = Byte.toUnsignedInt(encoded);
        if (index >= VALUES.length) {
            return IDLE;
        }

        return VALUES[index];
    }

}
