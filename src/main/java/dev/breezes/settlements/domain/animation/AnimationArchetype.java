package dev.breezes.settlements.domain.animation;

public enum AnimationArchetype {

    IDLE,
    HOLD_TOOL_VERTICAL,
    HOLD_TOOL_HORIZONTAL,
    SWING_HEAVY,
    SWING_LIGHT,
    STAB,
    CAST,
    REEL_IN,
    REEL_OUT,
    TILL_DOWN,
    TILL_UP,
    POINT,
    CELEBRATE,
    INTERACT,
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
