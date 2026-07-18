package dev.breezes.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Works around ModDevGradle 1.0.21, whose {@code ResolvedJarJarArtifact#isObfuscated} dereferences
 * {@link JarFile#getManifest()} with no null check -- so any jarJar'd artifact lacking a
 * {@code META-INF/MANIFEST.MF} fails the {@code :jarJar} task outright.
 * <p>
 * {@code javax.inject:javax.inject:1} was published in 2009 with no manifest at all and trips this.
 * The bug is still unfixed upstream, so bumping the plugin does not help.
 * <p>
 * This lives in {@code buildSrc} rather than inline in {@code build.gradle} because Gradle 8.14
 * embeds Groovy 3, which cannot parse the JDK 25 class files the daemon runs on -- declaring any
 * class in the build script fails with "Unsupported class file major version 69".
 */
public abstract class EnsureJarManifest implements TransformAction<TransformParameters.None> {

    @InputArtifact
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public void transform(TransformOutputs outputs) {
        File input = getInputArtifact().get().getAsFile();
        try {
            if (hasManifest(input)) {
                outputs.file(input);
                return;
            }
            // The output name must match the input: JarJar derives the embedded path from it, and for
            // a manifest-less jar the JPMS automatic module name is derived from it too. Renaming here
            // would silently change the module identity that JarJar dedup relies on.
            repackageWithManifest(input, outputs.file(input.getName()));
        } catch (IOException e) {
            throw new GradleException("Failed to inspect or repair jar: " + input, e);
        }
    }

    private static boolean hasManifest(File jar) throws IOException {
        try (JarFile jarFile = new JarFile(jar)) {
            return jarFile.getManifest() != null;
        }
    }

    private static void repackageWithManifest(File input, File output) throws IOException {
        Manifest manifest = new Manifest();
        // A bare Manifest-Version is all that is needed; anything richer would diverge this copy from
        // the canonical artifact other mods embed under the same coordinate.
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarFile jarFile = new JarFile(input);
             OutputStream fileOut = Files.newOutputStream(output.toPath());
             JarOutputStream jarOut = new JarOutputStream(fileOut, manifest)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                // Re-create each entry instead of reusing it, so the copy carries no stale
                // compressed-size or CRC metadata from the source jar.
                jarOut.putNextEntry(new ZipEntry(entry.getName()));
                if (!entry.isDirectory()) {
                    try (InputStream entryIn = jarFile.getInputStream(entry)) {
                        entryIn.transferTo(jarOut);
                    }
                }
                jarOut.closeEntry();
            }
        }
    }

}
