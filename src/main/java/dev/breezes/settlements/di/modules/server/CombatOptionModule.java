package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.ballista.BallistaCombatOption;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit.EggCombatOption;
import dev.breezes.settlements.application.ai.threat.CombatOption;

import java.util.List;

@Module
public abstract class CombatOptionModule {

    /**
     * Every combat option in ranked order for selection.
     * An option ahead in the list is preferred over one behind it whenever both could engage.
     */
    @Provides
    static List<CombatOption> combatOptions(BallistaCombatOption ballista, EggCombatOption egg) {
        return List.of(
                ballista,
                egg
        );
    }

}
