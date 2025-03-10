package dev.breezes.settlements.models.behaviors.stages;

import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.UUID;

@Getter
public class SimpleStage implements Stage {

    private final String name;
    private final UUID uuid;

    public SimpleStage(@Nonnull String name) {
        this.name = name;
        this.uuid = UUID.randomUUID();
    }

    @Override
    public String toString() {
        return "%s[%s]".formatted(this.name, this.uuid.toString());
    }

}
