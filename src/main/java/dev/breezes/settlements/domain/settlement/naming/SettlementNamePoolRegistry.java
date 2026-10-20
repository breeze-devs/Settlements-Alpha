package dev.breezes.settlements.domain.settlement.naming;

/**
 * The settlement name pools of the loaded datapacks.
 */
public interface SettlementNamePoolRegistry {

    /**
     * Returns the pools merged from every loaded pool file. Either pool may be empty.
     */
    SettlementNamePools pools();

}
