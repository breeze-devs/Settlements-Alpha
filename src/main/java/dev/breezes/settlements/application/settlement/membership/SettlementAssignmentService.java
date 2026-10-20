package dev.breezes.settlements.application.settlement.membership;

import dev.breezes.settlements.application.settlement.founding.SettlementFoundingConfig;
import dev.breezes.settlements.application.settlement.founding.SettlementFoundingService;
import dev.breezes.settlements.application.settlement.query.SettlementQueryService;
import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

/**
 * Service finding a settlement for villagers that belong to none.
 * <p>
 * Must be called on the server thread.
 */
@ServerSide
@ServerScope
@RequiredArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SettlementAssignmentService {

    private final SettlementQueryService settlementQueryService;
    private final SettlementMembershipService membershipService;
    private final SettlementFoundingService foundingService;
    private final SettlementFoundingConfig foundingConfig;

    /**
     * Attempts to assign an unassigned villager to a settlement using the villager's entity or meeting point location.
     * If there are no existing settlements, attempts to found one.
     * If founding fails, the villager stays unassigned.
     *
     * @param villager a villager in a server level
     */
    public void assign(@Nonnull BaseVillager villager) {
        if (villager.hasData(AttachmentRegistry.SETTLEMENT_MEMBERSHIP)) {
            return;
        }

        Validate.validState(villager.level() instanceof ServerLevel, "Villager %s is not in a server level", villager.getUUID());
        ServerLevel level = (ServerLevel) villager.level();

        // A villager that traveled may remember a bell in the dimension it left, which says nothing about this one
        Optional<BlockPos> bell = villager.getBrain().getMemory(MemoryModuleType.MEETING_POINT)
                .filter(meetingPoint -> meetingPoint.dimension().equals(level.dimension()))
                .map(GlobalPos::pos);

        Optional<Settlement> settlement = bell
                .flatMap(position -> this.settlementQueryService.getSettlementAt(level, position))
                .or(() -> this.settlementQueryService.getSettlementAt(level, villager.blockPosition()));
        if (settlement.isPresent()) {
            this.membershipService.join(level, settlement.get().id(), List.of(villager));
            return;
        }

        // The outcome is left alone: a refusal leaves the villager to try again on its next call, and a founding
        // makes its founders as members
        bell.ifPresent(position -> this.foundingService.found(level, position, this.foundingConfig.minimumResidents()));
    }

}
