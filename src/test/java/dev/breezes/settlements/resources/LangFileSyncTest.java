package dev.breezes.settlements.resources;

import com.google.gson.stream.JsonReader;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * These tests compare every lang file to the baseline (en_us) at build time.
 */
class LangFileSyncTest {

    private static final String LANG_RESOURCE_DIRECTORY = "assets/settlements/lang";
    private static final String BASELINE_FILE_NAME = "en_us.json";

    // %% is an escaped literal percent; %s and %n$s are the argument placeholders the translation formatter consumes.
    private static final Pattern FORMAT_TOKEN = Pattern.compile("%(?:%|(?:(\\d+)\\$)?s)");

    @Test
    void discovery_finds_baseline_and_at_least_one_translation() {
        Map<String, Path> langFiles = discoverLangFiles();

        assertTrue(langFiles.containsKey(BASELINE_FILE_NAME),
                "Baseline " + BASELINE_FILE_NAME + " not found in " + LANG_RESOURCE_DIRECTORY);
        assertTrue(langFiles.size() >= 2,
                "Only the baseline was discovered; translation files are expected alongside it, so discovery is likely broken");
    }

    @TestFactory
    Stream<DynamicTest> declares_no_duplicate_keys_in_any_lang_file() {
        return discoverLangFiles().entrySet().stream()
                .map(file -> dynamicTest(file.getKey(), () -> {
                    List<String> duplicates = new ArrayList<>();

                    parseEntries(file.getValue(), duplicates);

                    assertTrue(duplicates.isEmpty(),
                            () -> file.getKey() + " declares duplicate keys (the later value silently wins): " + duplicates);
                }));
    }

    @TestFactory
    Stream<DynamicTest> key_set_matches_baseline_in_every_translation() {
        Map<String, Path> langFiles = discoverLangFiles();
        Set<String> baselineKeys = parseEntries(baselinePath(langFiles)).keySet();

        return translationFiles(langFiles)
                .map(file -> dynamicTest(file.getKey(), () -> {
                    Set<String> translationKeys = parseEntries(file.getValue()).keySet();

                    Set<String> missing = sortedDifference(baselineKeys, translationKeys);
                    Set<String> extra = sortedDifference(translationKeys, baselineKeys);

                    assertTrue(missing.isEmpty() && extra.isEmpty(),
                            () -> file.getKey() + " key set differs from " + BASELINE_FILE_NAME
                                    + "\n  missing: " + missing
                                    + "\n  extra: " + extra);
                }));
    }

    @TestFactory
    Stream<DynamicTest> placeholder_arguments_match_baseline_in_every_translation() {
        Map<String, Path> langFiles = discoverLangFiles();
        Map<String, String> baselineEntries = parseEntries(baselinePath(langFiles));

        return translationFiles(langFiles)
                .map(file -> dynamicTest(file.getKey(), () -> {
                    Map<String, String> translationEntries = parseEntries(file.getValue());

                    List<String> mismatches = new ArrayList<>();
                    for (Map.Entry<String, String> entry : translationEntries.entrySet()) {
                        String baselineValue = baselineEntries.get(entry.getKey());
                        if (baselineValue == null) {
                            // A key absent from the baseline is the key-set test's failure to report, not this one's.
                            continue;
                        }
                        Set<Integer> expected = argumentIndices(baselineValue);
                        Set<Integer> actual = argumentIndices(entry.getValue());
                        if (!expected.equals(actual)) {
                            mismatches.add(entry.getKey() + ": baseline consumes arguments " + expected
                                    + " but translation consumes " + actual);
                        }
                    }

                    assertTrue(mismatches.isEmpty(),
                            () -> file.getKey() + " placeholder arguments differ from " + BASELINE_FILE_NAME
                                    + ":\n  " + String.join("\n  ", mismatches));
                }));
    }

    /**
     * Reads from the classpath rather than the source tree so the assertions cover what resource processing actually
     * ships.
     */
    private static Map<String, Path> discoverLangFiles() {
        URL directory = LangFileSyncTest.class.getClassLoader().getResource(LANG_RESOURCE_DIRECTORY);
        assertNotNull(directory, "Lang directory " + LANG_RESOURCE_DIRECTORY + " is not on the test classpath");

        try (Stream<Path> files = Files.list(Path.of(directory.toURI()))) {
            Map<String, Path> langFiles = new TreeMap<>();
            files.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> langFiles.put(path.getFileName().toString(), path));
            return langFiles;
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Failed to list lang files in " + LANG_RESOURCE_DIRECTORY, e);
        }
    }

    private static Path baselinePath(Map<String, Path> langFiles) {
        Path baseline = langFiles.get(BASELINE_FILE_NAME);
        assertNotNull(baseline, "Baseline " + BASELINE_FILE_NAME + " not found in " + LANG_RESOURCE_DIRECTORY);
        return baseline;
    }

    private static Stream<Map.Entry<String, Path>> translationFiles(Map<String, Path> langFiles) {
        return langFiles.entrySet().stream()
                .filter(file -> !file.getKey().equals(BASELINE_FILE_NAME));
    }

    private static Map<String, String> parseEntries(Path file) {
        return parseEntries(file, new ArrayList<>());
    }

    /**
     * Parses with a streaming reader instead of {@code JsonParser} because a DOM parse collapses duplicate keys before
     * anything can observe them.
     */
    private static Map<String, String> parseEntries(Path file, List<String> duplicateKeySink) {
        Map<String, String> entries = new LinkedHashMap<>();
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(file, StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                String value = reader.nextString();
                if (entries.put(key, value) != null) {
                    duplicateKeySink.add(key);
                }
            }
            reader.endObject();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse " + file, e);
        }
        return entries;
    }

    /**
     * Resolves each token to the argument index it consumes, so a translation may reorder or repeat placeholders
     * freely and only dropping an argument or referencing a nonexistent one counts as drift. Implicit {@code %s}
     * tokens number themselves in order of appearance, matching {@link java.util.Formatter} semantics.
     */
    private static Set<Integer> argumentIndices(String value) {
        Set<Integer> indices = new TreeSet<>();
        int implicitIndex = 0;
        Matcher matcher = FORMAT_TOKEN.matcher(value);
        while (matcher.find()) {
            if (matcher.group().equals("%%")) {
                continue;
            }
            String explicit = matcher.group(1);
            if (explicit != null) {
                indices.add(Integer.parseInt(explicit));
            } else {
                implicitIndex++;
                indices.add(implicitIndex);
            }
        }
        return indices;
    }

    private static Set<String> sortedDifference(Set<String> left, Set<String> right) {
        Set<String> difference = new TreeSet<>(left);
        difference.removeAll(right);
        return difference;
    }

}
