package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nonnull;

/**
 * Persists a live attachment object by mapping it to its saved form, which a codec encodes.
 * <p>
 * Writing captures the object's state at the moment of the save.
 * Reading builds a fresh object rather than filling an existing one.
 */
abstract class StateCodecAttachmentSerializer<S, T> implements IAttachmentSerializer<Tag, T> {

    private final Codec<S> stateCodec;

    StateCodecAttachmentSerializer(@Nonnull Codec<S> stateCodec) {
        this.stateCodec = stateCodec;
    }

    abstract S toState(@Nonnull T attachment);

    abstract T fromState(@Nonnull S state);

    @Override
    public final Tag write(@Nonnull T attachment, @Nonnull HolderLookup.Provider provider) {
        // Never null, even for an empty object: a skipped key reads back as an object that was never set, not as an empty one
        return this.stateCodec.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), this.toState(attachment))
                .getOrThrow();
    }

    @Override
    public final T read(@Nonnull IAttachmentHolder holder, @Nonnull Tag tag, @Nonnull HolderLookup.Provider provider) {
        // Throws rather than returning an empty object, so an unreadable tag reads as absent, not as saved empty.
        // The holder stays unused: these attachments also load onto vanilla villagers, zombie villagers and witches.
        S state = this.stateCodec.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow();
        return this.fromState(state);
    }

}
