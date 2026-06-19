package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Localized SCRIPTED floor for ambient dialogue.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class ScriptedDialogueProvider implements DialogueProvider {

    private final DialogueLineIndex lineIndex;
    private final DialogueConfig config;

    @Override
    public Optional<DialogueLine> sampleAmbientLine(@Nonnull UUID villagerUuid, @Nonnull DialogueContext context) {
        if (!this.config.scriptedChatter()) {
            return Optional.empty();
        }

        return this.lineIndex.resolveKeys(context.getProfession(), context.getOccasion(), context.getFacets())
                .map(ScriptedDialogueProvider::chooseKey)
                .map(DialogueLine::translatable);
    }

    @Override
    public void runEveningPackSweep() {
        // SCRIPTED is static and localized through lang files, so there is nothing to precompute.
    }

    @Override
    public boolean isEnabled() {
        return this.config.scriptedChatter();
    }

    private static String chooseKey(List<String> keys) {
        return RandomUtil.choice(keys);
    }

}
