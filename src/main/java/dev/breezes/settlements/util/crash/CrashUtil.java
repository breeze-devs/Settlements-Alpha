package dev.breezes.settlements.util.crash;

import dev.breezes.settlements.util.crash.report.CrashReport;
import lombok.CustomLog;

import javax.annotation.Nonnull;

@CustomLog
public class CrashUtil {

    public static void crash(@Nonnull CrashReport report) {
        log.error(report.getLogMessage());
        throw report.getException();
    }

}
