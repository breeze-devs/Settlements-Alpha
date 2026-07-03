package dev.breezes.settlements.di.modules.server;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import dev.breezes.settlements.application.ai.speech.BubbleSpeechSink;
import dev.breezes.settlements.application.ai.speech.ChatSpeechSink;
import dev.breezes.settlements.application.ai.speech.VillagerSpeechSink;

import java.util.Set;

/**
 * Registers every {@link VillagerSpeechSink} as a Dagger multibinding, consumed by
 * {@code VillagerSpeechService}. Adding a new sink later is one more {@code @Binds} method here.
 */
@Module
public abstract class SpeechModule {

    @Multibinds
    abstract Set<VillagerSpeechSink> villagerSpeechSinks();

    @Binds
    @IntoSet
    abstract VillagerSpeechSink bubbleSpeechSink(BubbleSpeechSink impl);

    @Binds
    @IntoSet
    abstract VillagerSpeechSink chatSpeechSink(ChatSpeechSink impl);

}
