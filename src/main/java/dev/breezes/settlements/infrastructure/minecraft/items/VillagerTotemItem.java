package dev.breezes.settlements.infrastructure.minecraft.items;

import dev.breezes.settlements.bootstrap.registry.components.DataComponentRegistry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.attachments.TotemTargetAttachment;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.conversion.VillagerConversionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.OptionalInt;

public class VillagerTotemItem extends Item {

    private static final String ALREADY_TARGET_TYPE_KEY = "item.settlements.villager_totem.already_target_type";
    private static final double MAX_DISTANCE_SQUARED = 16.0;

    public VillagerTotemItem(Properties properties) {
        super(properties);
    }

    public static TotemMode getMode(ItemStack stack) {
        return TotemMode.fromSerializedId(stack.getOrDefault(DataComponentRegistry.VILLAGER_TOTEM_MODE.get(),
                TotemMode.defaultMode().getSerializedId()));
    }

    public static void setMode(ItemStack stack, TotemMode mode) {
        stack.set(DataComponentRegistry.VILLAGER_TOTEM_MODE.get(), mode.getSerializedId());
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, @Nonnull List<Component> tooltipComponents, @Nonnull TooltipFlag tooltipFlag) {
        TotemMode mode = getMode(stack);
        tooltipComponents.add(Component.translatable("item.settlements.villager_totem.tooltip.mode",
                Component.translatable(mode.getTranslationKey())).withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.translatable("item.settlements.villager_totem.tooltip.use").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.settlements.villager_totem.tooltip.items_lost").withStyle(ChatFormatting.RED));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                setMode(stack, getMode(stack).next());
                level.playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS);
            }
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(@Nonnull ItemStack stack, @Nonnull Player player, @Nonnull LivingEntity target, @Nonnull InteractionHand hand) {
        if (target instanceof Villager villager && player.isShiftKeyDown()) {
            TotemTargetAttachment.setTarget(player, villager.getId());
            player.startUsingItem(hand);

            if (!player.level().isClientSide()) {
                villager.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ClockTicks.seconds(3).getTicksAsInt(), 10, false, false));
            }

            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public int getUseDuration(@Nonnull ItemStack stack, @Nonnull LivingEntity entity) {
        return ClockTicks.seconds(2.5).getTicksAsInt();
    }

    @Nonnull
    @Override
    public UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(@Nonnull Level level, @Nonnull LivingEntity playerEntity, @Nonnull ItemStack stack, int remainingUseDuration) {
        if (playerEntity instanceof Player player) {
            OptionalInt targetId = TotemTargetAttachment.getTarget(player);
            if (targetId.isEmpty()) {
                player.stopUsingItem();
                return;
            }

            if (level.getEntity(targetId.getAsInt()) instanceof Villager villager) {
                if (villager.distanceToSqr(player) > MAX_DISTANCE_SQUARED) {
                    player.stopUsingItem();
                    return;
                }

                if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                    TotemMode mode = getMode(stack);

                    int ticksUsed = this.getUseDuration(stack, player) - remainingUseDuration;
                    double height = (villager.getBbHeight() * ticksUsed) / 50.0;
                    double angle = ticksUsed * 0.5;
                    double radius = 0.8;

                    double x = villager.getX() + radius * Math.cos(angle);
                    double y = villager.getY() + height;
                    double z = villager.getZ() + radius * Math.sin(angle);

                    serverLevel.sendParticles(new DustParticleOptions(mode.particleColor(), 1.0f), x, y, z, 2, 0, 0, 0, 0);

                    if (remainingUseDuration % 10 == 0) {
                        level.playSound(null, villager.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.PLAYERS, 0.5F, 0.5F + (ticksUsed / 50.0F));
                    }
                }
            } else {
                player.stopUsingItem();
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            OptionalInt targetId = TotemTargetAttachment.getTarget(player);
            if (targetId.isPresent() && !level.isClientSide() && level instanceof ServerLevel serverLevel) {
                if (serverLevel.getEntity(targetId.getAsInt()) instanceof Villager villager
                        && villager.distanceToSqr(livingEntity) <= MAX_DISTANCE_SQUARED) {
                    TotemMode mode = getMode(stack);

                    if (isAlreadyTargetType(villager, mode)) {
                        failConversion(serverLevel, player, villager);
                        return stack;
                    }

                    serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 2.0F),
                            villager.getX(), villager.getY() + 1.0, villager.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
                    level.playSound(null, villager.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);

                    convertVillager(serverLevel, villager, mode);
                }
            }
        }
        return stack;
    }

    private boolean isAlreadyTargetType(@Nonnull Villager villager, TotemMode mode) {
        return mode.isAlreadyTargetType(villager instanceof BaseVillager, villager.isNoAi());
    }

    private void failConversion(@Nonnull ServerLevel level,
                                @Nonnull Player player,
                                @Nonnull Villager villager) {
        player.displayClientMessage(Component.translatable(ALREADY_TARGET_TYPE_KEY).withStyle(ChatFormatting.RED), true);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.85F, 0.0F, 0.0F), 1.5F),
                villager.getX(), villager.getY() + 1.0, villager.getZ(), 20, 0.35, 0.35, 0.35, 0.02);
        level.sendParticles(ParticleTypes.SMOKE,
                villager.getX(), villager.getY() + 1.0, villager.getZ(), 12, 0.25, 0.25, 0.25, 0.01);
        level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    @Override
    public void releaseUsing(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity livingEntity, int timeCharged) {
        // Single teardown point for the locked-on target: both aborting (stopUsingItem) and completing the channel
        // (completeUsingItem -> stopUsingItem) route through here, so the transient attachment never lingers.
        if (livingEntity instanceof Player player) {
            TotemTargetAttachment.clearTarget(player);
        }
    }

    private void convertVillager(ServerLevel level, Villager oldVillager, TotemMode mode) {
        if (mode.convertsToVanilla()) {
            VillagerConversionUtil.convertToVanilla(level, oldVillager, mode.keepsVanillaVillagerInStasis());
        } else {
            VillagerConversionUtil.convertToSettlements(level, oldVillager);
        }
    }

}
