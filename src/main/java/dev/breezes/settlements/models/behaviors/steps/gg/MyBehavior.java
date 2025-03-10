package dev.breezes.settlements.models.behaviors.steps.gg;

public class MyBehavior {

    public void start() {
        // prepare facts

        // initiate states

    }

    public void tick() {
        // blast ore behavior - list of steps
        // TODO: >> generic can continue (e.g. workstation is furnace, not dead)
        // 1. if step - condition: close enough to furnace
        // > if true - action step: navigate to furnace; continue loop
        // > if false - do nothing
        // 2. staged step - stage: behavior status;
        // 2.0. STAGE: START - default stage; transition state
        // 2.1. STAGE: PUT - timed action step - pre: hold item / tick: do nothing / post: clear held item; transition state
        // 2.2. STAGE: SMELTING - timed action step - pre: light furnace; play effects / tick: occasionally play effects / post: extinguish furnace; play effects; transition state
        // 2.3. STAGE: PICK - timed action step - pre: hold item / tick: do nothing / post: clear held item; transition state
        // 2.4. STAGE: END - default stage
        // -- no more steps, behavior will end automatically with code 'successful'
    }

    // Should also be called upon unloading or crashing to clean-up
    // probably a hook is needed
    public void end() {
        // tear down temporary states
    }

}
