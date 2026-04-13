package dev.breezes.settlements.shared.util.crash.report;

import javax.annotation.Nonnull;

public class InvalidPacketRegistrationCrashReport extends CrashReport {

    private static final String CRASH_TITLE = "Invalid Packet Handler Registration";
    private static final String CRASH_MESSAGE = """
            Settlements detected invalid packet handler registration.
            This usually means one or more packet handlers were wired incorrectly in the Dagger graph.
            """.strip();
    private static final String SUGGESTED_ACTION_MESSAGE = """
            Verify the packet handler wiring:
            - Client-side handlers are bound in ClientNetworkModule
            - Server-side handlers are bound in ServerNetworkModule
            - Each handler has an injectable constructor
            - Each packet type has exactly one binding on its correct side
            - The packet is registered through PacketRegistry on the matching receiver side
            If you are using a modpack, please report this to the modpack author.
            """.strip();

    public InvalidPacketRegistrationCrashReport(@Nonnull RuntimeException reason) {
        super(reason, CRASH_TITLE, CRASH_MESSAGE, SUGGESTED_ACTION_MESSAGE, reason);
    }

}
