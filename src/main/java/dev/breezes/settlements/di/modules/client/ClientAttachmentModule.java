package dev.breezes.settlements.di.modules.client;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.domain.attachment.AttachmentProvider;
import dev.breezes.settlements.domain.presentation.AttachmentDisplayProfileRegistry;
import dev.breezes.settlements.domain.presentation.InMemoryAttachmentDisplayProfileRegistry;
import dev.breezes.settlements.domain.presentation.InMemorySlotAnchorRegistry;
import dev.breezes.settlements.domain.presentation.InMemorySocketRegistry;
import dev.breezes.settlements.domain.presentation.SlotAnchorRegistry;
import dev.breezes.settlements.domain.presentation.SocketRegistry;
import dev.breezes.settlements.infrastructure.rendering.attachment.EquipmentAttachmentProvider;

@Module
public abstract class ClientAttachmentModule {

    @Binds
    @IntoSet
    abstract AttachmentProvider equipmentAttachmentProvider(EquipmentAttachmentProvider implementation);

    @Provides
    @ClientScope
    static SlotAnchorRegistry slotAnchorRegistry() {
        return InMemorySlotAnchorRegistry.defaults();
    }

    @Provides
    @ClientScope
    static SocketRegistry socketRegistry() {
        return InMemorySocketRegistry.defaults();
    }

    @Provides
    @ClientScope
    static AttachmentDisplayProfileRegistry attachmentDisplayProfileRegistry() {
        return InMemoryAttachmentDisplayProfileRegistry.defaults();
    }

}
