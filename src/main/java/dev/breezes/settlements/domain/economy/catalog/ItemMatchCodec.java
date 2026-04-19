package dev.breezes.settlements.domain.economy.catalog;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import javax.annotation.Nonnull;

public final class ItemMatchCodec {

    private static final Codec<ItemMatch.ItemRef> ITEM_REF_CODEC = ResourceLocation.CODEC.fieldOf("item")
            .codec()
            .xmap(ItemMatch.ItemRef::new, ItemMatch.ItemRef::id);

    private static final Codec<ItemMatch.TagRef> TAG_REF_CODEC = ResourceLocation.CODEC.fieldOf("tag")
            .codec()
            .xmap(id -> new ItemMatch.TagRef(TagKey.create(Registries.ITEM, id)),
                    key -> key.tag().location());

    public static final Codec<ItemMatch> CODEC = Codec.xor(ITEM_REF_CODEC, TAG_REF_CODEC)
            .xmap(either -> either.map(item -> item, tag -> tag),
                    match -> switch (match) {
                        case ItemMatch.ItemRef itemRef -> Either.left(itemRef);
                        case ItemMatch.TagRef tagRef -> Either.right(tagRef);
                    });

    public static final StreamCodec<FriendlyByteBuf, ItemMatch> STREAM_CODEC = StreamCodec.of(
            ItemMatchCodec::writeToNetwork,
            ItemMatchCodec::readFromNetwork
    );

    private static void writeToNetwork(@Nonnull FriendlyByteBuf buffer, @Nonnull ItemMatch match) {
        switch (match) {
            case ItemMatch.ItemRef itemRef -> {
                buffer.writeBoolean(false);
                buffer.writeResourceLocation(itemRef.id());
            }
            case ItemMatch.TagRef tagRef -> {
                buffer.writeBoolean(true);
                buffer.writeResourceLocation(tagRef.tag().location());
            }
        }
    }

    private static ItemMatch readFromNetwork(@Nonnull FriendlyByteBuf buffer) {
        boolean isTag = buffer.readBoolean();
        ResourceLocation id = buffer.readResourceLocation();
        if (isTag) {
            return new ItemMatch.TagRef(TagKey.create(Registries.ITEM, id));
        }
        return new ItemMatch.ItemRef(id);
    }

    private ItemMatchCodec() {
    }

}
