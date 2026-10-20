package dev.breezes.settlements.di.modules.server;

import dagger.Binds;
import dagger.Module;
import dev.breezes.settlements.application.settlement.founding.GeneratedSettlementStarts;
import dev.breezes.settlements.application.settlement.query.SettlementQueryService;
import dev.breezes.settlements.infrastructure.minecraft.query.StoreBackedSettlementQueryService;
import dev.breezes.settlements.infrastructure.minecraft.query.StructureGeneratedSettlementStarts;

@Module
public abstract class SettlementQueryModule {

    @Binds
    abstract SettlementQueryService settlementQueryService(StoreBackedSettlementQueryService implementation);

    @Binds
    abstract GeneratedSettlementStarts generatedSettlementStarts(StructureGeneratedSettlementStarts implementation);

}
