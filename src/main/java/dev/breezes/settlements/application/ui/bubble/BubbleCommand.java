package dev.breezes.settlements.application.ui.bubble;

import java.util.UUID;

/**
 * Producer-facing command contract for mutating villager bubbles.
 */
public sealed interface BubbleCommand
        permits BubbleCommand.Upsert,
        BubbleCommand.Push,
        BubbleCommand.RemoveById,
        BubbleCommand.RemoveByOwner,
        BubbleCommand.ClearChannel {

    record Upsert(BubbleChannel channel,
                  String ownerKey,
                  BubbleMessage message) implements BubbleCommand {

        public Upsert {
            if (ownerKey.isBlank()) {
                throw new IllegalArgumentException("ownerKey must not be blank");
            }
        }

    }

    record Push(BubbleChannel channel,
                BubbleMessage message) implements BubbleCommand {

    }

    record RemoveById(UUID bubbleId) implements BubbleCommand {

    }

    record RemoveByOwner(BubbleChannel channel,
                         String ownerKey) implements BubbleCommand {

        public RemoveByOwner {
            if (ownerKey.isBlank()) {
                throw new IllegalArgumentException("ownerKey must not be blank");
            }
        }
    }

    record ClearChannel(BubbleChannel channel) implements BubbleCommand {

    }

}
