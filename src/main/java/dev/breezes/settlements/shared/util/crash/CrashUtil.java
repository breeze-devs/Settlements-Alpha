package dev.breezes.settlements.shared.util.crash;

import dev.breezes.settlements.shared.util.crash.report.CrashReport;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CrashUtil {

    /**
     * Throws the report's exception without logging it. The code that catches the exception logs it, with its cause.
     */
    public static void crash(@Nonnull CrashReport report) {
        throw report.getException();
    }

}
