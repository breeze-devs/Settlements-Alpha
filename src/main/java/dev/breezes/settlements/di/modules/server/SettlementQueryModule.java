package dev.breezes.settlements.di.modules.server;

import dagger.Binds;
import dagger.Module;
import dev.breezes.settlements.domain.settlement.query.SettlementQueryService;
import dev.breezes.settlements.infrastructure.minecraft.query.StructureManagerSettlementQueryService;

@Module
public abstract class SettlementQueryModule {

    @Binds
    abstract SettlementQueryService settlementQueryService(StructureManagerSettlementQueryService implementation);

}
