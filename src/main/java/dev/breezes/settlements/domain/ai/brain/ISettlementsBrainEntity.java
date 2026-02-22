package dev.breezes.settlements.domain.ai.brain;

import dev.breezes.settlements.domain.entities.SettlementsEntity;

public interface ISettlementsBrainEntity extends SettlementsEntity {

    IBrain getSettlementsBrain();

}
