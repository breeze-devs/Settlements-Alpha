package dev.breezes.settlements.genetics;

import dev.breezes.settlements.util.NbtTags;
import dev.breezes.settlements.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the complete genetic makeup of a single entity.
 */
@CustomLog
public class GeneticsProfile {

    private static final String GENETICS_NBT_TAG = NbtTags.of("genetics");

    private final Map<GeneType, Gene> genes;

    // TODO: move to configurations
    private static final double DEFAULT_MUTATION_RATE = 0.05;
    private static final double DEFAULT_WILD_MUTATION_CHANCE = 0.01;

    /**
     * Initialize this profile with random genes
     */
    public GeneticsProfile() {
        this.genes = new EnumMap<>(GeneType.class);
        for (GeneType type : GeneType.values()) {
            double baseValue = RandomUtil.randomDouble(0, 1);
            this.setGene(type, new Gene(baseValue));
        }
    }

    public GeneticsProfile(Map<GeneType, Gene> genes) {
        this.genes = new EnumMap<>(genes);
    }

    public Optional<Gene> getGene(GeneType type) {
        return Optional.ofNullable(this.genes.get(type));
    }

    public double getGeneValue(GeneType type) {
        return this.getGene(type)
                .map(Gene::value)
                .orElse(0.5);
    }

    public void setGene(GeneType type, Gene gene) {
        this.genes.put(type, gene);
    }

    public Map<GeneType, Gene> getAllGenes() {
        return Collections.unmodifiableMap(this.genes);
    }

    /**
     * Create a new child profile by crossing over this profile with the other parent.
     */
    public GeneticsProfile crossover(GeneticsProfile other, RandomSource random) {
        GeneticsProfile child = new GeneticsProfile();
        for (GeneType type : GeneType.values()) {
            double valA = this.getGeneValue(type);
            double valB = other.getGeneValue(type);

            double childValue;
            if (random.nextDouble() < DEFAULT_WILD_MUTATION_CHANCE) {
                childValue = random.nextDouble();
            } else {
                double average = (valA + valB) / 2.0;
                double mutation = (random.nextDouble() * (DEFAULT_MUTATION_RATE * 2)) - DEFAULT_MUTATION_RATE;
                childValue = Mth.clamp(average + mutation, 0.0, 1.0);
            }
            child.setGene(type, new Gene(childValue));
        }
        return child;

    }

    public void save(CompoundTag nbtTag) {
        CompoundTag geneticsTag = new CompoundTag();
        this.genes.forEach((type, gene) -> geneticsTag.putDouble(type.name(), gene.value()));
        nbtTag.put(GENETICS_NBT_TAG, geneticsTag);
        log.debug("Successfully saved genetic tags: {}", this.genes);
    }

    public void load(CompoundTag nbtTag) {
        if (!nbtTag.contains(GENETICS_NBT_TAG, Tag.TAG_COMPOUND)) {
            log.warn("Genetics NBT tag {} does not exist", GENETICS_NBT_TAG);
            return;
        }

        CompoundTag geneticsTag = nbtTag.getCompound(GENETICS_NBT_TAG);
        this.genes.clear();

        for (GeneType type : GeneType.values()) {
            if (geneticsTag.contains(type.name(), Tag.TAG_DOUBLE)) {
                this.genes.put(type, new Gene(geneticsTag.getDouble(type.name())));
            }
        }
        log.debug("Successfully loaded genetic tags: {}", this.genes);
    }

}
