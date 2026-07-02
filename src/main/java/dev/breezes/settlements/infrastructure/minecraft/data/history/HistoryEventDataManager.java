package dev.breezes.settlements.infrastructure.minecraft.data.history;

import dev.breezes.settlements.domain.generation.history.HistoryEventDefinition;
import dev.breezes.settlements.domain.generation.history.HistoryEventDefinitionCodec;
import dev.breezes.settlements.domain.generation.history.HistoryEventRegistry;
import dev.breezes.settlements.infrastructure.minecraft.data.framework.KeyedCatalogDataManager;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;

public class HistoryEventDataManager extends KeyedCatalogDataManager<String, HistoryEventDefinition> implements HistoryEventRegistry {

    private static final String DIRECTORY_PATH = "settlements/history/events";

    @Inject
    public HistoryEventDataManager() {
        super(DIRECTORY_PATH, HistoryEventDefinitionCodec.CODEC);
    }

    @Override
    protected String label() {
        return "history event";
    }

    @Override
    protected String keyOf(@Nonnull HistoryEventDefinition value) {
        return value.id();
    }

    @Override
    public List<HistoryEventDefinition> allEvents() {
        return List.copyOf(this.all().values());
    }

}
