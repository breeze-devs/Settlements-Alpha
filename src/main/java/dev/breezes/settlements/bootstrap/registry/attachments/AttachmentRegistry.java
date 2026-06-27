package dev.breezes.settlements.bootstrap.registry.attachments;

import com.mojang.serialization.Codec;
import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.application.economy.demand.DemandSignalCodec;
import dev.breezes.settlements.application.economy.demand.DemandSignalState;
import dev.breezes.settlements.domain.ai.memory.SettlementsMemoryStore;
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
     * Transient per-villager store for decaying Settlements memories
     */
    public static final Supplier<AttachmentType<SettlementsMemoryStore>> SETTLEMENTS_MEMORY_STORE = REGISTRY.register(
            "settlements_memory_store",
            () -> AttachmentType.builder(SettlementsMemoryStore::new)
                    .build());

    /**
     * Serialized boolean stamped on a ZombieVillager that originated from a BaseVillager conversion.
     * Survives save/unload so the cure handler still recognizes the zombie after a server restart.
     */
    public static final Supplier<AttachmentType<Boolean>> ZOMBIE_SETTLEMENTS_ORIGIN = REGISTRY.register(
            "zombie_settlements_origin",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .build());

    /**
     * Serialized boolean flag on a BaseVillager that was restored via zombie-villager curing.
     * Acts as a permanent core-memory fact (brain was eaten, survived, was cured).
     */
    public static final Supplier<AttachmentType<Boolean>> VILLAGER_WAS_CURED = REGISTRY.register(
            "villager_was_cured",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .build());

    /**
     * Serialized boolean stamped on vanilla villagers selected during village world generation.
     * The decision can happen in a WorldGenRegion long before the villager joins a live ServerLevel,
     * so the marker must survive save/unload cycles until the deferred replacement can run safely.
     */
    public static final Supplier<AttachmentType<Boolean>> VILLAGER_PENDING_SETTLEMENTS_REPLACEMENT = REGISTRY.register(
            "villager_pending_settlements_replacement",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .build());

    /**
     * Serialized boolean stamped on a chest BlockEntity to opt it out of villager logistics.
     * When true, villagers will not discover or target this chest — persists across restarts so
     * players can permanently reserve personal storage inside a village.
     * Both halves of a double-chest carry the flag so that isWaxed only needs to probe one side.
     */
    public static final Supplier<AttachmentType<Boolean>> CHEST_WAXED = REGISTRY.register(
            "chest_waxed",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
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

    /**
     * Transient global greet cooldown: the absolute game-tick after which any villager may next
     * greet this player. Stops an entire crowd from waving the instant a player walks in — the first
     * villager to greet stamps it, the rest read it and stay quiet. Not serialized: ephemeral social
     * state that should reset on relog.
     */
    public static final Supplier<AttachmentType<Long>> PLAYER_GREET_COOLDOWN = REGISTRY.register(
            "greet_cooldown",
            () -> AttachmentType.builder(() -> 0L)
                    .build());

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
