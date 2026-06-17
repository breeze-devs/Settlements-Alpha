package dev.breezes.settlements.bootstrap.registry.attachments;

import com.mojang.serialization.Codec;
import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.application.economy.demand.DemandSignalCodec;
import dev.breezes.settlements.application.economy.demand.DemandSignalState;
import dev.breezes.settlements.infrastructure.minecraft.attachments.DayPlanAttachmentCodec;
import dev.breezes.settlements.infrastructure.minecraft.attachments.DayPlanAttachmentState;
import dev.breezes.settlements.infrastructure.minecraft.attachments.TeardownLedgerAttachmentCodec;
import dev.breezes.settlements.infrastructure.minecraft.attachments.TeardownLedgerAttachmentState;
import dev.breezes.settlements.infrastructure.minecraft.attachments.TotemTargetAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerBrainAttachmentCodec;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerBrainAttachmentState;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerCredibilityAttachmentCodec;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerCredibilityAttachmentState;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerEmeraldAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerGeneticsAttachmentCodec;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerGeneticsAttachmentState;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerInventoryAttachmentCodec;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerInventoryAttachmentState;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerKnowledgeAttachmentCodec;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerKnowledgeAttachmentState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class AttachmentRegistry {

    public static final DeferredRegister<AttachmentType<?>> REGISTRY =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SettlementsMod.MOD_ID);

    public static final Supplier<AttachmentType<Float>> VILLAGER_HUNGER = REGISTRY.register(
            "hunger",
            () -> AttachmentType.builder(() -> 1.0f)
                    .serialize(Codec.FLOAT)
                    .build());

    public static final Supplier<AttachmentType<Integer>> VILLAGER_EMERALDS = REGISTRY.register(
            "emeralds",
            () -> AttachmentType.builder(() -> VillagerEmeraldAttachment.INITIAL_EMERALD_COUNT)
                    .serialize(Codec.INT)
                    .build());

    public static final Supplier<AttachmentType<DemandSignalState>> VILLAGER_DEMAND_SIGNALS = REGISTRY.register(
            "demand_signals",
            () -> AttachmentType.builder(DemandSignalState::empty)
                    .serialize(DemandSignalCodec.STATE_CODEC)
                    .build());

    public static final Supplier<AttachmentType<DayPlanAttachmentState>> VILLAGER_DAY_PLAN = REGISTRY.register(
            "day_plan",
            () -> AttachmentType.builder(DayPlanAttachmentState::empty)
                    .serialize(DayPlanAttachmentCodec.STATE_CODEC)
                    .build());

    public static final Supplier<AttachmentType<VillagerInventoryAttachmentState>> VILLAGER_INVENTORY = REGISTRY.register(
            "villager_inventory",
            () -> AttachmentType.builder(VillagerInventoryAttachmentState::empty)
                    .serialize(VillagerInventoryAttachmentCodec.STATE_CODEC)
                    .build());

    public static final Supplier<AttachmentType<VillagerBrainAttachmentState>> VILLAGER_BRAIN = REGISTRY.register(
            "villager_brain",
            () -> AttachmentType.builder(VillagerBrainAttachmentState::empty)
                    .serialize(VillagerBrainAttachmentCodec.STATE_CODEC)
                    .build());

    public static final Supplier<AttachmentType<VillagerGeneticsAttachmentState>> VILLAGER_GENETICS = REGISTRY.register(
            "villager_genetics",
            () -> AttachmentType.builder(VillagerGeneticsAttachmentState::empty)
                    .serialize(VillagerGeneticsAttachmentCodec.STATE_CODEC)
                    .build());

    public static final Supplier<AttachmentType<TeardownLedgerAttachmentState>> VILLAGER_TEARDOWN_LEDGER = REGISTRY.register(
            "villager_teardown_ledger",
            () -> AttachmentType.builder(TeardownLedgerAttachmentState::empty)
                    .serialize(TeardownLedgerAttachmentCodec.STATE_CODEC)
                    .build());

    public static final Supplier<AttachmentType<VillagerKnowledgeAttachmentState>> VILLAGER_KNOWLEDGE = REGISTRY.register(
            "villager_knowledge",
            () -> AttachmentType.builder(VillagerKnowledgeAttachmentState::empty)
                    .serialize(VillagerKnowledgeAttachmentCodec.STATE_CODEC)
                    .build());

    public static final Supplier<AttachmentType<VillagerCredibilityAttachmentState>> VILLAGER_CREDIBILITY = REGISTRY.register(
            "villager_credibility",
            () -> AttachmentType.builder(VillagerCredibilityAttachmentState::empty)
                    .serialize(VillagerCredibilityAttachmentCodec.STATE_CODEC)
                    .build());

    /**
     * Transient marker for the villager a player has locked onto while channeling the villager totem. It is not
     * serialized: it is ephemeral channel state, and lives on the player rather than the totem stack, whose mutation
     * mid-use would cancel the channel.
     */
    public static final Supplier<AttachmentType<Integer>> PLAYER_TOTEM_TARGET = REGISTRY.register(
            "totem_target",
            () -> AttachmentType.builder(() -> TotemTargetAttachment.NO_TARGET)
                    .build());

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
