package dev.breezes.settlements.bootstrap.registry.attachments;

import com.mojang.serialization.Codec;
import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.application.economy.demand.DemandSignalCodec;
import dev.breezes.settlements.application.economy.demand.DemandSignalState;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import dev.breezes.settlements.domain.ai.memory.SettlementsMemoryStore;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.personality.PersonaLineageSnapshot;
import dev.breezes.settlements.domain.personality.VillagerPersonality;
import dev.breezes.settlements.infrastructure.minecraft.attachments.TeardownLedgerAttachmentSerializer;
import dev.breezes.settlements.infrastructure.minecraft.attachments.TotemTargetAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerEmeraldAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerGeneticsAttachmentSerializer;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerInventoryAttachmentSerializer;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerKnowledgeAttachmentSerializer;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerOriginAttachmentCodec;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerOriginAttachmentState;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerPersonalityAttachmentCodec;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerTeardownLedger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
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

    /**
     * Absent until the villager is set up, and reading it before then throws.
     */
    public static final Supplier<AttachmentType<VillagerInventory>> VILLAGER_INVENTORY = REGISTRY.register(
            "villager_inventory",
            // Never created on demand: an inventory that appeared on a read would mark a villager that was never set
            // up as set up, and its save would make a villager recreated from the tag skip its starting food
            () -> AttachmentType.<VillagerInventory>builder(holder -> {
                        throw new IllegalStateException("Villager inventory read before the villager was set up");
                    })
                    .serialize(new VillagerInventoryAttachmentSerializer())
                    .build());

    /**
     * Rolled fresh on the first read when none was saved.
     */
    public static final Supplier<AttachmentType<GeneticsProfile>> VILLAGER_GENETICS = REGISTRY.register(
            "villager_genetics",
            () -> AttachmentType.builder(() -> new GeneticsProfile())
                    .serialize(new VillagerGeneticsAttachmentSerializer())
                    .copyOnDeath() // carry over when converted to other entities like zombie villagers
                    .build());

    public static final Supplier<AttachmentType<VillagerTeardownLedger>> VILLAGER_TEARDOWN_LEDGER = REGISTRY.register(
            "villager_teardown_ledger",
            () -> AttachmentType.builder(() -> new VillagerTeardownLedger(List.of()))
                    .serialize(new TeardownLedgerAttachmentSerializer())
                    .build());

    public static final Supplier<AttachmentType<VillagerKnowledgeStore>> VILLAGER_KNOWLEDGE = REGISTRY.register(
            "villager_knowledge",
            () -> AttachmentType.builder(VillagerKnowledgeAttachmentSerializer::emptyStore)
                    .serialize(new VillagerKnowledgeAttachmentSerializer())
                    .build());

    /**
     * Transient per-villager store for decaying Settlements memories
     */
    public static final Supplier<AttachmentType<SettlementsMemoryStore>> SETTLEMENTS_MEMORY_STORE = REGISTRY.register(
            "settlements_memory_store",
            () -> AttachmentType.builder(SettlementsMemoryStore::new)
                    .build());

    /**
     * Serialized birth-provenance record for a villager: how it came into existence (e.g. bred,
     * world-gen-replaced, cured from a zombie, or unknown/uncapturable). Stamped once at the
     * earliest server-side genesis seam that can observe it and never re-stamped afterward.
     */
    public static final Supplier<AttachmentType<VillagerOriginAttachmentState>> VILLAGER_ORIGIN = REGISTRY.register(
            "villager_origin",
            () -> AttachmentType.builder(VillagerOriginAttachmentState::empty)
                    .serialize(VillagerOriginAttachmentCodec.STATE_CODEC)
                    .build());

    /**
     * Serialized persona card for a villager: lifecycle status plus adjectives,
     * characterSketch, and speech style once generated. Defaults to {@link VillagerPersonality#pending()} so
     * every old save and freshly spawned villager starts at PENDING — the state a later retro-sweep
     * wave scans for and drives generation from.
     */
    public static final Supplier<AttachmentType<VillagerPersonality>> VILLAGER_PERSONALITY = REGISTRY.register(
            "villager_personality",
            () -> AttachmentType.builder(VillagerPersonality::pending)
                    .serialize(VillagerPersonalityAttachmentCodec.STATE_CODEC)
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

    /**
     * Transient birth-only snapshot of both parents' persona, captured at breed time for a BRED child.
     * <p>
     * Not serialized: if the server stops before that sweep runs, the child simply falls back to a null lineage
     * on its next sweep -- an accepted graceful degradation, chosen over persisting a parent snapshot forever
     * just to cover that narrow window.
     */
    public static final Supplier<AttachmentType<PersonaLineageSnapshot>> VILLAGER_PERSONA_LINEAGE = REGISTRY.register(
            "villager_persona_lineage",
            () -> AttachmentType.builder(PersonaLineageSnapshot::empty)
                    .build());

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
