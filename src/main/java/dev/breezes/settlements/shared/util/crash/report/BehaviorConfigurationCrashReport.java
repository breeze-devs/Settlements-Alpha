package dev.breezes.settlements.shared.util.crash.report;

import javax.annotation.Nonnull;

public class BehaviorConfigurationCrashReport extends CrashReport {

    private static final String CRASH_TITLE = "Invalid Behavior Configuration";
    private static final String CRASH_MESSAGE = """
            Settlements detected an invalid behavior configuration.
            This usually means the behavior step graph or stage wiring violates required invariants.
            """.strip();
    private static final String SUGGESTED_ACTION_MESSAGE = """
            Review recent behavior configuration changes and validate stage-to-step wiring.
            Ensure each stage has a non-null step and every stage uses a unique step instance.
            Ensure onStart/onEnd are not reused by stage steps and are not the same instance.
            If you are using a modpack, please report this to the modpack author.
            """.strip();

    public BehaviorConfigurationCrashReport(@Nonnull RuntimeException reason) {
        super(reason, CRASH_TITLE, CRASH_MESSAGE, SUGGESTED_ACTION_MESSAGE, reason);
    }

}
