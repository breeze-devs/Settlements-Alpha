package dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultivationLilyVerbTest {

    @Test
    void resolve_sneakingEmptyHand_returnsResize() {
        assertEquals(CultivationLilyVerb.RESIZE, CultivationLilyVerb.resolve(true, true, false));
    }

    @Test
    void resolve_sneakingWithAcceptedSeedInHand_returnsDecline() {
        // A seed is not exempt from the sneak rule: resize being empty-handed-only is what makes the
        // gesture one rule instead of two. Vanilla dispatch already withholds this combination, so what
        // this pins is the answer when something forces the dispatch anyway — any mod may set the interact
        // event's use-block flag, as this one did until the verb table absorbed the rule. An implementation
        // that read sneaking as "always resize" would resize underneath someone else's block placement.
        assertEquals(CultivationLilyVerb.DECLINE, CultivationLilyVerb.resolve(true, false, true));
    }

    @Test
    void resolve_sneakingWithNonSeedItemInHand_returnsDecline() {
        // Sneak plus a full hand that is not an accepted seed (cobblestone, a torch, a bucket, ...)
        // must fall through to vanilla placement rather than resizing the zone and eating the click.
        assertEquals(CultivationLilyVerb.DECLINE, CultivationLilyVerb.resolve(true, false, false));
    }

    @Test
    void resolve_emptyHand_returnsClearFilter() {
        assertEquals(CultivationLilyVerb.CLEAR_FILTER, CultivationLilyVerb.resolve(false, true, false));
    }

    @Test
    void resolve_acceptedSeed_returnsSetFilter() {
        assertEquals(CultivationLilyVerb.SET_FILTER, CultivationLilyVerb.resolve(false, false, true));
    }

    @Test
    void resolve_nonSeedNonEmptyHand_returnsDecline() {
        assertEquals(CultivationLilyVerb.DECLINE, CultivationLilyVerb.resolve(false, false, false));
    }

    @Test
    void isEmptyHandGesture_emptyMainHand_isGesture() {
        assertTrue(CultivationLilyVerb.isEmptyHandGesture(true, true));
    }

    @Test
    void isEmptyHandGesture_emptyOffHand_isNotGesture() {
        // An empty off hand is not a deliberate gesture — it's only the absence of a second item,
        // seen whenever the main hand already declined and vanilla tries the off hand next. Losing
        // this branch is what let a declined main-hand click (e.g. a stick) fall through to an
        // off-hand pass that silently cleared the crop filter.
        assertFalse(CultivationLilyVerb.isEmptyHandGesture(true, false));
    }

    @Test
    void isEmptyHandGesture_nonEmptyMainHand_isNotGesture() {
        assertFalse(CultivationLilyVerb.isEmptyHandGesture(false, true));
    }

}
