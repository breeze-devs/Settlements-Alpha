package dev.breezes.settlements.entities;

import dev.breezes.settlements.models.brain.IBrain;
import dev.breezes.settlements.models.entities.SettlementsEntity;

public interface ISettlementsBrainEntity extends SettlementsEntity {

    IBrain getSettlementsBrain();

}
