package dev.breezes.settlements.shared.logging;

import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusLogger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Cost of one Settlements logging call, made through ILogger the way mod code makes it, under the
 * default logging policy and a Log4j configuration that mirrors the NeoForge host's
 * (neoforge-host-log4j2.xml). A call the policy rejects never reaches the host.
 * <p>
 * Each benchmark fixes a level, an argument shape, and a thread count, and nothing else, so a run
 * stays comparable with any other run of the same benchmark however the logging stack beneath
 * ILogger changes. The 2-argument benchmarks share one message and one argument set, so their
 * differences come from the level alone.
 * <p>
 * Log files go to settlements-jmh-logs under java.io.tmpdir and last only until the next fork
 * starts.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
@Warmup(iterations = 3, time = 1)
// A DEBUG flood rolls debug.log over by size every few seconds. A long measurement averages over
// those rollovers instead of letting a fork's mean hinge on where in the cycle it stops.
@Measurement(iterations = 10, time = 1)
@State(Scope.Thread)
public class LoggerCallBenchmark {

    private static final String HOST_CONFIG_RESOURCE = "neoforge-host-log4j2.xml";
    private static final Path LOG_DIR = Path.of(System.getProperty("java.io.tmpdir"), "settlements-jmh-logs");

    private static final String TWO_ARG_FORMAT = "Villager {} advanced its schedule to stage {}";
    private static final String FOUR_ARG_FORMAT = "Created ui sessionId={} channel={} player={} villagerEntityId={}";

    // Static and final, as Lombok's @CustomLog declares it in mod code.
    private static final ILogger LOG;

    static {
        clearLogDir();
        System.setProperty("settlements.jmh.logDir", LOG_DIR.toString());
        System.setProperty("log4j2.configurationFile", HOST_CONFIG_RESOURCE);
        // Without a terminal, TerminalConsoleAppender's JLine probe fails and it falls back to plain
        // stdout. Disabling the probe takes that fallback directly, however the fork's streams are
        // attached.
        System.setProperty("terminal.jline", "false");

        // TerminalConsoleAppender keeps the System.out it finds when Log4j first configures it.
        // Swapping System.out only around that moment sends the console to a file while JMH keeps
        // its own stream.
        PrintStream jmhOut = System.out;
        System.setOut(newConsoleStream());
        try {
            LOG = LoggerFactory.getLogger(LoggerCallBenchmark.class.getName());
        } finally {
            System.setOut(jmhOut);
        }

        // The mod constructor installs the policy resolved from the debugging configuration, and
        // the benchmark JVM never runs it. The default policy is what a default configuration
        // resolves to.
        LoggerFactory.installPolicy(LoggingPolicy.DEFAULT);
    }

    // Instance fields rather than constants, so the JIT cannot fold the arguments: mod code logs
    // values read at runtime, and boxing and the varargs array are part of what a call costs.
    private final UUID villagerId = UUID.fromString("3f2b9c4e-8a1d-4e6f-9b2a-5c7d8e9f0a1b");
    private final UUID playerId = UUID.fromString("0c6e1d52-7f3a-4b8e-a91d-2e4f6a8b0c13");
    private final int scheduleStage = 3;
    private final long sessionId = 12L;
    private final String channel = "villager_trade";
    private final int villagerEntityId = 48_213;

    /**
     * Fails the trial unless the host configuration is active and was built completely. Log4j
     * carries on without whatever it cannot load, and every number measured after that would
     * describe a different host.
     */
    @Setup(Level.Trial)
    public void requireHostConfiguration() {
        // Only the host configuration writes debug.log to this directory.
        if (!Files.exists(LOG_DIR.resolve("debug.log"))) {
            throw new IllegalStateException(HOST_CONFIG_RESOURCE + " is not active: no debug.log in " + LOG_DIR);
        }
        // Log4j answers a configuration it cannot find with its default one, and an element it
        // cannot build, such as an appender whose plugin is missing, by leaving it out. It reports
        // both only to its status log, which nothing in a benchmark run displays.
        List<String> problems = StatusLogger.getLogger().getStatusData().stream()
                .filter(status -> status.getLevel().isMoreSpecificThan(org.apache.logging.log4j.Level.WARN))
                .map(StatusData::getFormattedStatus)
                .toList();
        if (!problems.isEmpty()) {
            throw new IllegalStateException(HOST_CONFIG_RESOURCE + " did not load cleanly:\n" + String.join("\n", problems));
        }
    }

    /**
     * Gives the measuring thread the name of the game thread most mod logging runs on. Every layout
     * prints the thread name, and JMH's own worker names are several times longer than the host's.
     */
    @Setup(Level.Trial)
    public void nameThreadAsServer() {
        Thread.currentThread().setName("Server thread");
    }

    /**
     * A TRACE call with no arguments. Its output is discarded, so this is the floor cost of a call.
     */
    @Benchmark
    public void trace0Args() {
        LOG.trace("Villager schedule unchanged");
    }

    /**
     * A discarded TRACE call carrying the 2-argument shape. Compared with trace0Args, it isolates what
     * arguments cost on a call whose output is discarded.
     */
    @Benchmark
    public void trace2Args() {
        LOG.trace(TWO_ARG_FORMAT, this.villagerId, this.scheduleStage);
    }

    /**
     * A DEBUG call, which the host formats and writes to debug.log only, when the policy forwards it.
     */
    @Benchmark
    public void debug2Args() {
        LOG.debug(TWO_ARG_FORMAT, this.villagerId, this.scheduleStage);
    }

    /**
     * An INFO call, which the host delivers to every destination: the console, the server GUI
     * queue, latest.log, and debug.log.
     */
    @Benchmark
    public void info2Args() {
        LOG.info(TWO_ARG_FORMAT, this.villagerId, this.scheduleStage);
    }

    /**
     * A DEBUG call with four mixed arguments: a cached Long, a String, a UUID, and an int outside the
     * Integer cache, whose boxing allocates on every call. Compared with debug2Args, it isolates how
     * cost grows with argument count and, when the call is forwarded, formatting work.
     */
    @Benchmark
    public void debug4Args() {
        LOG.debug(FOUR_ARG_FORMAT, this.sessionId, this.channel, this.playerId, this.villagerEntityId);
    }

    /**
     * debug2Args from four threads at once. Compared with debug2Args, the difference is contention
     * between threads on the path a DEBUG call takes.
     */
    @Benchmark
    @Threads(4)
    public void debug2ArgsContended() {
        LOG.debug(TWO_ARG_FORMAT, this.villagerId, this.scheduleStage);
    }

    /**
     * info2Args from four threads at once. Compared with info2Args, the difference is contention on
     * every destination an INFO event reaches.
     */
    @Benchmark
    @Threads(4)
    public void info2ArgsContended() {
        LOG.info(TWO_ARG_FORMAT, this.villagerId, this.scheduleStage);
    }

    private static void clearLogDir() {
        // The previous fork's files would trip OnStartupTriggeringPolicy, which compresses them in
        // the background while this fork measures. Deleting them also bounds disk use to one fork.
        try {
            Files.createDirectories(LOG_DIR);
            try (DirectoryStream<Path> files = Files.newDirectoryStream(LOG_DIR)) {
                for (Path file : files) {
                    Files.delete(file);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static PrintStream newConsoleStream() {
        try {
            // System.out's own construction: a 128-byte buffer that flushes at every line, so a
            // console line costs one write, as it does on a piped stdout.
            FileOutputStream file = new FileOutputStream(LOG_DIR.resolve("console.log").toFile());
            return new PrintStream(new BufferedOutputStream(file, 128), true, StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

}
