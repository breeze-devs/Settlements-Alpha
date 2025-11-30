package dev.breezes.settlements.models.behaviors.steps;

/**
 * Marker interface for stage keys used in behaviors.
 * This allows behaviors to expose their internal stages safely.
 */
public interface StageKey {

    String name();

}
