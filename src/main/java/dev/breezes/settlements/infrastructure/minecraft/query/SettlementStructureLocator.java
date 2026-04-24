package dev.breezes.settlements.infrastructure.minecraft.query;

import dev.breezes.settlements.SettlementsMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class SettlementStructureLocator {

    private static final ResourceLocation SETTLEMENT_STRUCTURE_ID =
            ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "settlement");

    public Optional<StructureStart> locate(@Nonnull ServerLevel level, @Nonnull BlockPos position) {
        Structure settlementStructure = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .get(SETTLEMENT_STRUCTURE_ID);
        if (settlementStructure == null) {
            return Optional.empty();
        }

        StructureStart structureStart = level.structureManager().getStructureAt(position, settlementStructure);
        if (!structureStart.isValid()) {
            return Optional.empty();
        }

        return Optional.of(structureStart);
    }

}
