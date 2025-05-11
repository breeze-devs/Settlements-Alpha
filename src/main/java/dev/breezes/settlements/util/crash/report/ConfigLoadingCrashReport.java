package dev.breezes.settlements.util.crash.report;

import javax.annotation.Nonnull;

public class ConfigLoadingCrashReport extends CrashReport {

    private static final String CRASH_TITLE = "Error Loading Configurations";
    private static final String CRASH_MESSAGE = """
            Settlements couldn't load its configuration files.
            This usually happens due to missing or malformed configuration files.
            """.strip();
    private static final String SUGGESTED_ACTION_MESSAGE = """
            Check your configuration files (usually located in the 'config' folder) for syntax errors.
            If you are using a modpack, please report this to the modpack author.
            Otherwise, if you've manually edited config files recently, try reverting or correcting any recent changes.
            You can also try deleting the mod's config files and restarting the game to regenerate the default configurations.
            """.strip();

    public ConfigLoadingCrashReport(@Nonnull RuntimeException reason) {
        super(reason, CRASH_TITLE, CRASH_MESSAGE, SUGGESTED_ACTION_MESSAGE, reason);
    }

}
