package dev.breezes.settlements.application.ai.behavior.usecases.villager.donation;

import dev.breezes.settlements.application.economy.VillagerWallet;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Optional;

@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class DonationScanner {

    private final VillagerWallet villagerWallet;

    /**
     * Finds the neediest nearby villager eligible to receive a donation from {@code donor}.
     * <p>
     * Empty when the donor cannot afford to give at their current comfort cushion (solvency
     * early-out) or when no sensed villager both sits below the floor and clears the safe cap.
     * "Neediest" is the lowest current balance, so a donor helps the most destitute neighbor first.
     */
    public Optional<BaseVillager> findNeediest(@Nonnull BaseVillager donor, int floor, int minDonation, int comfort) {
        int donorBalance = this.villagerWallet.getBalance(donor);
        if (donorBalance <= comfort) {
            return Optional.empty();
        }

        return this.getPerceivedEntities(donor)
                .ofType(BaseVillager.class, candidate -> this.isEligible(candidate, donor, floor, minDonation, donorBalance, comfort))
                .min(Comparator.comparingInt(this.villagerWallet::getBalance));
    }

    private boolean isEligible(@Nonnull BaseVillager candidate,
                               @Nonnull BaseVillager donor,
                               int floor,
                               int minDonation,
                               int donorBalance,
                               int comfort) {
        if (candidate == donor) {
            return false;
        }

        int recipientBalance = this.villagerWallet.getBalance(candidate);
        if (recipientBalance >= floor) {
            return false;
        }

        return DonationAmountCalculator.cap(floor, recipientBalance, donorBalance, comfort) >= minDonation;
    }

    private PerceivedEntities getPerceivedEntities(@Nonnull BaseVillager villager) {
        return villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                .orElse(PerceivedEntities.empty());
    }

}
