package dev.breezes.settlements.domain.ai.observation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ObservationContentRenderer}. Pure string formatting — no Minecraft types.
 */
class ObservationContentRendererTest {

    @Test
    void render_withActorAndParenthetical_producesExpectedFormat() {
        // Arrange, Act
        String content = ObservationContentRenderer.render("LEATHER_WASHED", "dd79d2ec6f", "wash_leather");

        // Assert
        assertEquals("leather washed by dd79d2ec6f (wash_leather)", content);
    }

    @Test
    void render_underscoresReplacedWithSpacesAndLowercased() {
        // Arrange, Act
        String content = ObservationContentRenderer.render("RESOURCE_HARVESTED", "actor-id", null);

        // Assert
        assertEquals("resource harvested by actor-id", content);
    }

    @Test
    void render_nullActor_fallsBackToUnknown() {
        // Arrange, Act
        String content = ObservationContentRenderer.render("TRADE_COMPLETED", null, null);

        // Assert
        assertEquals("trade completed by unknown", content);
    }

    @Test
    void render_blankActor_fallsBackToUnknown() {
        // Arrange, Act
        String content = ObservationContentRenderer.render("TRADE_COMPLETED", "  ", null);

        // Assert
        assertEquals("trade completed by unknown", content);
    }

    @Test
    void render_nullParenthetical_omitsParenthetical() {
        // Arrange, Act
        String content = ObservationContentRenderer.render("SHEEP_SHEARED", "actor-id", null);

        // Assert
        assertEquals("sheep sheared by actor-id", content);
    }

    @Test
    void render_blankParenthetical_omitsParenthetical() {
        // Arrange, Act
        String content = ObservationContentRenderer.render("SHEEP_SHEARED", "actor-id", "   ");

        // Assert
        assertEquals("sheep sheared by actor-id", content);
    }

    @Test
    void render_parentheticalPresent_wrapsInParentheses() {
        // Arrange, Act
        String content = ObservationContentRenderer.render("COURTSHIP_COMPLETED", "actor-id", "no one answered");

        // Assert
        assertEquals("courtship completed by actor-id (no one answered)", content);
    }

}
