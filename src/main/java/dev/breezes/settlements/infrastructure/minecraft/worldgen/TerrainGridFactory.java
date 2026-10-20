package dev.breezes.settlements.infrastructure.minecraft.worldgen;

import dev.breezes.settlements.domain.common.BiomeId;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.survey.SurveyBounds;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;
import dev.breezes.settlements.domain.generation.model.survey.TerrainSample;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.Nonnull;

/**
 * Samples a survey area's terrain from a chunk generator's noise.
 * Each column's biome is the one at its surface height.
 * <p>
 * Sampling reads no chunk, loaded or not, so it may run off the server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TerrainGridFactory {

    /**
     * The distance between samples, in blocks. Every survey samples at this spacing, so every site is scored at the
     * same resolution.
     */
    private static final int SAMPLE_INTERVAL = 4;

    public static TerrainGrid fromGenerationContext(@Nonnull Structure.GenerationContext context,
                                                    @Nonnull SurveyBounds bounds) {
        return fromNoise(context.chunkGenerator(), context.randomState(), context.heightAccessor(), bounds);
    }

    /**
     * @param heightAccessor the build height of the dimension the generator generates
     */
    public static TerrainGrid fromNoise(@Nonnull ChunkGenerator generator,
                                        @Nonnull RandomState randomState,
                                        @Nonnull LevelHeightAccessor heightAccessor,
                                        @Nonnull SurveyBounds bounds) {
        BoundingRegion sampleArea = bounds.sampleArea();

        int gridWidth = (sampleArea.widthX() / SAMPLE_INTERVAL) + 1;
        int gridDepth = (sampleArea.widthZ() / SAMPLE_INTERVAL) + 1;
        TerrainSample[][] samples = new TerrainSample[gridDepth][gridWidth];

        for (int gz = 0; gz < gridDepth; gz++) {
            int worldZ = sampleArea.min().z() + gz * SAMPLE_INTERVAL;
            for (int gx = 0; gx < gridWidth; gx++) {
                int worldX = sampleArea.min().x() + gx * SAMPLE_INTERVAL;

                int height = generator.getBaseHeight(worldX, worldZ, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor,
                        randomState);

                Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(worldX),
                        QuartPos.fromBlock(height), QuartPos.fromBlock(worldZ), randomState.sampler());
                BiomeId biomeId = BiomeId.of(biome.unwrapKey().map(key -> key.location().toString()).orElse("minecraft:plains"));

                float temperature = biome.value().getBaseTemperature();
                samples[gz][gx] = new TerrainSample(height, biomeId, temperature);
            }
        }

        return TerrainGrid.of(sampleArea.min().x(), sampleArea.min().z(), SAMPLE_INTERVAL, samples);
    }

}
