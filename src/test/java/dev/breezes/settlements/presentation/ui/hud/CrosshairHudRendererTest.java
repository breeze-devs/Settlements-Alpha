package dev.breezes.settlements.presentation.ui.hud;

import net.minecraft.world.phys.HitResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrosshairHudRendererTest {

    @Test
    void isBlockHit_missType_returnsFalse() {
        // A bare `instanceof BlockHitResult` would return true here, since BlockHitResult.miss(...) is still a
        // BlockHitResult — that instanceof-only check is the defect this predicate exists to close.
        assertFalse(CrosshairHudRenderer.isBlockHit(HitResult.Type.MISS));
    }

    @Test
    void isBlockHit_blockType_returnsTrue() {
        assertTrue(CrosshairHudRenderer.isBlockHit(HitResult.Type.BLOCK));
    }

    @Test
    void withAlpha_scalesOnlyTheAlphaChannel() {
        assertEquals(0x80A0B0C0, CrosshairHudRenderer.withAlpha(0xFFA0B0C0, 128.0F / 255.0F));
    }

    @Test
    void withAlpha_tailOfAFadeNeverProducesAnAlphaTheFontRendererMisreads() {
        // The whole point of the floor: an alpha byte under 4 is read as an unset alpha channel and drawn
        // fully opaque, so these near-zero fractions are exactly the frames that used to flash the text
        // back to full brightness on its way out.
        assertEquals(4, alphaByteOf(CrosshairHudRenderer.withAlpha(0xFFA0B0C0, 0.0F)));
        assertEquals(4, alphaByteOf(CrosshairHudRenderer.withAlpha(0xFFA0B0C0, 0.001F)));
        assertEquals(4, alphaByteOf(CrosshairHudRenderer.withAlpha(0xFFA0B0C0, 0.013F)));
    }

    @Test
    void withAlpha_floorDoesNotLiftAlphasTheFontRendererAlreadyHonors() {
        // A floor applied as a minimum on the fraction rather than on the resulting byte would quietly
        // brighten a translucent base color across its whole fade, not just at the tail.
        assertEquals(51, alphaByteOf(CrosshairHudRenderer.withAlpha(0xFFA0B0C0, 0.2F)));
        assertEquals(10, alphaByteOf(CrosshairHudRenderer.withAlpha(0x33A0B0C0, 0.2F)));
    }

    private static int alphaByteOf(int argb) {
        return (argb >>> 24) & 0xFF;
    }

}
