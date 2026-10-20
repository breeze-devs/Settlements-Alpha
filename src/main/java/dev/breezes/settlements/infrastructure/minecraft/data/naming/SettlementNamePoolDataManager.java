package dev.breezes.settlements.infrastructure.minecraft.data.naming;

import dev.breezes.settlements.domain.settlement.naming.SettlementNamePoolRegistry;
import dev.breezes.settlements.domain.settlement.naming.SettlementNamePools;
import dev.breezes.settlements.domain.settlement.naming.SettlementNamePoolsCodec;
import dev.breezes.settlements.infrastructure.minecraft.data.framework.CodecJsonDataManager;
import jakarta.inject.Inject;
import lombok.CustomLog;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads the settlement name pools from the pool files of every namespace.
 * <p>
 * Every file contributes its words, so a pack extends the pools by adding a file and replaces a file's words by
 * overriding its id. The merged pools hold the words in file id order, then in each file's own order.
 */
@CustomLog
public class SettlementNamePoolDataManager extends CodecJsonDataManager<SettlementNamePools>
        implements SettlementNamePoolRegistry {

    private static final String DIRECTORY_PATH = "settlements/names/settlements";

    private SettlementNamePools pools = SettlementNamePools.EMPTY;

    @Inject
    public SettlementNamePoolDataManager() {
        super(DIRECTORY_PATH, SettlementNamePoolsCodec.CODEC);
    }

    @Override
    protected String label() {
        return "settlement name pool";
    }

    @Override
    protected void onReloaded(@Nonnull Map<ResourceLocation, SettlementNamePools> values) {
        List<String> prefixes = new ArrayList<>();
        List<String> suffixes = new ArrayList<>();
        for (SettlementNamePools file : values.values()) {
            prefixes.addAll(file.prefixes());
            suffixes.addAll(file.suffixes());
        }
        this.pools = new SettlementNamePools(prefixes, suffixes);

        if (this.pools.prefixes().isEmpty()) {
            log.warn("No settlement name prefixes are loaded, so settlements cannot get generated names");
        }
        if (this.pools.suffixes().isEmpty()) {
            log.warn("No settlement name suffixes are loaded, so settlements cannot get generated names");
        }
    }

    @Override
    public SettlementNamePools pools() {
        return this.pools;
    }

}
