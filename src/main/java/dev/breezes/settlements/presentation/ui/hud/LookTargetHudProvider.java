package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;

/**
 * A {@link HudSurfaceProvider} keyed on what the crosshair is aimed at, by way of {@link HudFrame#blockHit()}
 * and {@link HudFrame#aimedAtEntity()}. A block hit is a first-class target here, not a fallback bolted onto
 * an entity-only surface.
 * <p>
 * Absence means this provider has nothing to say for the current target — not every provider recognizes every
 * target, and the region renders nothing at all when every contributing surface returns empty.
 * <p>
 * Asked once per frame for whatever the crosshair currently rests on, with no dwell or precedence check
 * ahead of it: a provider is asked even when it cannot win, and even for targets it will never claim. An
 * implementation must therefore decline within a frame budget — a block-entity lookup at the aimed
 * position is the intended ceiling for that decision. Work proportional to anything the player controls,
 * a zone scan or a region query, belongs behind a cache the implementation owns, keyed on inputs it can
 * observe changing and bounded by a max age for the ones it cannot.
 */
@ClientSide
public interface LookTargetHudProvider extends HudSurfaceProvider {
}
