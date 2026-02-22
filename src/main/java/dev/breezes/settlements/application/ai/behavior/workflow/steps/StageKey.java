package dev.breezes.settlements.application.ai.behavior.workflow.steps;

/**
 * Marker interface for stage keys used in behaviors.
 * This allows behaviors to expose their internal stages safely.
 */
public interface StageKey {

    String name();

}
