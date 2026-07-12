package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;

/**
 * A single fixed-time placement request the composer's anchor stage tries to honor
 *
 * @param key       the behavior to place
 * @param civilTick the requested civil-space tick; global on the timeline — the band it was
 *                  listed under is advisory only
 * @param meal      true for an occasion {@code eat_food} pin, which places at meal
 *                  priority/rigidity and suppresses a colliding default meal anchor;
 *                  false for every other pin, which places flexible-but-fixed at a
 *                  priority just under meals
 */
public record PinnedSelection(BehaviorKey key, int civilTick, boolean meal) {
}
