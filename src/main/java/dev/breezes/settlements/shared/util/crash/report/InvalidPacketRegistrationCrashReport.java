package dev.breezes.settlements.shared.util.crash.report;

import javax.annotation.Nonnull;

public class InvalidPacketRegistrationCrashReport extends CrashReport {

    private static final String CRASH_TITLE = "Invalid Packet Handler Registration";
    private static final String CRASH_MESSAGE = """
            Settlements detected invalid packet handler annotation registration.
            This usually means one or more packet handlers were bound incorrectly.
            """.strip();
    private static final String SUGGESTED_ACTION_MESSAGE = """
            Ensure each packet handler has the correct annotation:
            - @HandleClientPacket for ClientSidePacketHandler implementations
            - @HandleServerPacket for ServerSidePacketHandler implementations
            Also ensure each handler has a public no-arg constructor and no duplicate packet bindings exist per side.
            If you are using a modpack, please report this to the modpack author.
            """.strip();

    public InvalidPacketRegistrationCrashReport(@Nonnull RuntimeException reason) {
        super(reason, CRASH_TITLE, CRASH_MESSAGE, SUGGESTED_ACTION_MESSAGE, reason);
    }

}
