package dev.breezes.settlements.application.ui.bubble;

/**
 * Identifier for a bubble sprite asset.
 * <p>
 * Closed by design: every entry here is bound to a mod-owned texture in the client-side
 * {@code SpriteCatalog}. Adding a sprite is intentionally cheap because these assets are still
 * owned by the mod and should remain exhaustively known at compile time.
 * <p>
 * If addon-driven sprite contribution ever becomes a real use case, migrate this enum to a
 * string-keyed registry interface. Encoding by identifier name keeps the wire format stable
 * across that client-side type change.
 */
public enum SpriteRef {

    SHEARS,
    SHEEP,

}
