package dev.breezes.settlements.application.economy;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerEmeraldAttachment;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@ServerScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class VillagerWallet {

    public int getBalance(@Nonnull BaseVillager villager) {
        return VillagerEmeraldAttachment.getEmeralds(villager);
    }

    public boolean canAfford(@Nonnull BaseVillager villager, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Wallet affordability checks require a non-negative amount");
        }
        return VillagerEmeraldAttachment.getEmeralds(villager) >= amount;
    }

    public void transfer(@Nonnull BaseVillager from, @Nonnull BaseVillager to, int amount) {
        if (from == to) {
            throw new IllegalArgumentException("Wallet transfer source and target must be different villagers");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Wallet transfers require a non-negative amount");
        }

        int sourceBalance = VillagerEmeraldAttachment.getEmeralds(from);
        if (sourceBalance < amount) {
            throw new IllegalArgumentException("Wallet transfer source has insufficient emeralds");
        }

        // Read both balances before mutating either so the pre-checked state is the basis for both writes.
        int targetBalance = VillagerEmeraldAttachment.getEmeralds(to);
        VillagerEmeraldAttachment.setEmeralds(from, sourceBalance - amount);
        VillagerEmeraldAttachment.setEmeralds(to, targetBalance + amount);
    }

}
