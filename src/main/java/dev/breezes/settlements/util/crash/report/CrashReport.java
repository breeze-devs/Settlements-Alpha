package dev.breezes.settlements.util.crash.report;

import dev.breezes.settlements.util.SettlementsRuntimeException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CrashReport {

    private static final String HEADER = "Oops! It looks like the Settlements mod has crashed: ";
    private static final String SUGGESTED_ACTION = "Here's what you may be able to do to fix it:\n";
    private static final String FOOTER = "If you think this is a bug, please report it to the mod author: <TODO>";
    /**
     * The actual exception that caused the crash
     */
    private final RuntimeException reason;

    private final String title;
    private final String message;
    private final String suggestedActionMessage;
    private final Throwable cause;

    public String getLogMessage() {
        return HEADER + this.title + "\n" +
                this.message + "\n\n" +
                SUGGESTED_ACTION + this.suggestedActionMessage + "\n\n" +
                FOOTER;
    }

    public SettlementsRuntimeException getException() {
        return new SettlementsRuntimeException(this.getLogMessage(), this.cause);
    }

}
