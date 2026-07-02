package dev.breezes.settlements.domain.generation.model.building;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Optional;

/**
 * Datapack codec for {@link DisplayInfo}. {@code custom_name} is the only optional/nullable field.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DisplayInfoCodec {

    public static final Codec<DisplayInfo> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("display_name").forGetter(DisplayInfo::displayName),
                    Codec.STRING.fieldOf("description").forGetter(DisplayInfo::description),
                    Codec.STRING.optionalFieldOf("custom_name").forGetter(info -> Optional.ofNullable(info.customName())),
                    Codec.STRING.fieldOf("icon_item_id").forGetter(DisplayInfo::iconItemId)
            ).apply(instance, (displayName, description, customName, iconItemId) ->
                    new DisplayInfo(displayName, description, customName.orElse(null), iconItemId)));

}
