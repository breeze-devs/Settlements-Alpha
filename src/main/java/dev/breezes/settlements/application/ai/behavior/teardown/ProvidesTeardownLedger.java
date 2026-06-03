package dev.breezes.settlements.application.ai.behavior.teardown;

/**
 * Capability interface implemented by {@code BaseVillager}.
 * <p>
 * {@code StateMachineBehavior.doStart} binds the ledger into the behavior's
 * {@link TeardownScope} via an {@code instanceof} check so that:
 * <ul>
 *   <li>Domain code ({@code ISettlementsBrainEntity}) stays MC-free.</li>
 *   <li>Test entities that do not implement this interface are unaffected
 *       (the binding simply does not happen).</li>
 * </ul>
 */
public interface ProvidesTeardownLedger {

    ITeardownLedger getTeardownLedger();

}
