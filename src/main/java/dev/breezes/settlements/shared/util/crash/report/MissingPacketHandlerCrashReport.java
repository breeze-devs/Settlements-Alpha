package dev.breezes.settlements.shared.util.crash.report;

import javax.annotation.Nonnull;

public class MissingPacketHandlerCrashReport extends CrashReport {

    private static final String CRASH_TITLE = "Error Handling Packets";
    private static final String CRASH_MESSAGE = """
            Settlements received a network packet without a registered handler.
            This usually means packet registration and packet handling are out of sync.
            """.strip();
    private static final String SUGGESTED_ACTION_MESSAGE = """
            Ensure the packet is registered with the correct receiver side (client/server)
            and that its handler is correctly registered.
            Verify both ends are running matching mod versions and packet protocol changes.
            If you are using a modpack, please report this to the modpack author.
            If you are developing the mod, check recent packet additions/refactors for missing bindings.
            """.strip();

    public MissingPacketHandlerCrashReport(@Nonnull RuntimeException reason) {
        super(reason, CRASH_TITLE, CRASH_MESSAGE, SUGGESTED_ACTION_MESSAGE, reason);
    }

}
