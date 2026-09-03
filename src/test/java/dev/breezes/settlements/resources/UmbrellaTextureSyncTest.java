package dev.breezes.settlements.resources;

import dev.breezes.settlements.domain.attachment.UmbrellaPattern;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pairs the umbrella skins against the texture files that paint them, in both directions. Neither half of
 * a mismatch announces itself: a skin with no file draws the missing-texture checkerboard on whichever
 * villagers happen to be assigned it, and a file with no skin ships without ever being drawn.
 */
class UmbrellaTextureSyncTest {

    private static final String UMBRELLA_TEXTURE_DIRECTORY = "assets/settlements/textures/entity/umbrella";
    private static final String TEXTURE_FILE_EXTENSION = ".png";

    @Test
    void everySkinHasATextureFileAndEveryTextureFileHasASkin() {
        // Arrange: naming the file after the constant is the convention the lookup relies on, restated
        // here in the test's own terms rather than read back out of the production path it checks.
        Set<String> expected = Arrays.stream(UmbrellaPattern.values())
                .map(pattern -> pattern.name().toLowerCase(Locale.ROOT) + TEXTURE_FILE_EXTENSION)
                .collect(Collectors.toCollection(TreeSet::new));

        // Act
        Set<String> shipped = discoverTextureFileNames();

        // Assert
        assertEquals(expected, shipped);
    }

    /**
     * Reads from the classpath rather than the source tree so the assertion covers what resource
     * processing actually ships.
     */
    private static Set<String> discoverTextureFileNames() {
        URL directory = UmbrellaTextureSyncTest.class.getClassLoader().getResource(UMBRELLA_TEXTURE_DIRECTORY);
        assertNotNull(directory, "Umbrella texture directory " + UMBRELLA_TEXTURE_DIRECTORY + " is not on the test classpath");

        try (Stream<Path> files = Files.list(Path.of(directory.toURI()))) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(TEXTURE_FILE_EXTENSION))
                    .collect(Collectors.toCollection(TreeSet::new));
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Failed to list umbrella textures in " + UMBRELLA_TEXTURE_DIRECTORY, e);
        }
    }

}
