package dev.breezes.settlements.bootstrap.registry.attachments;

import com.mojang.serialization.Codec;
import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.application.economy.demand.DemandSignalCodec;
import dev.breezes.settlements.application.economy.demand.DemandSignalState;
import dev.breezes.settlements.infrastructure.minecraft.attachments.DayPlanAttachmentCodec;
import dev.breezes.settlements.infrastructure.minecraft.attachments.DayPlanAttachmentState;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerEmeraldAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerBrainAttachmentCodec;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerBrainAttachmentState;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerGeneticsAttachmentCodec;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerGeneticsAttachmentState;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerInventoryAttachmentCodec;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerInventoryAttachmentState;
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

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
