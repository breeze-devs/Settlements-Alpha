package dev.breezes.settlements.infrastructure.minecraft.entities.villager.genetics;

import dev.breezes.settlements.domain.genetics.ConstitutionHealthResolver;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.Builder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

public final class VillagerGeneticAttributes {

    private static final List<Binding> BINDINGS = List.of(
            Binding.builder()
                    .gene(GeneType.CONSTITUTION)
                    .attribute(Attributes.MAX_HEALTH)
                    .modifierId(ResourceLocationUtil.mod("genetics/constitution_health"))
                    .operation(AttributeModifier.Operation.ADD_VALUE)
                    .resolver(ConstitutionHealthResolver::resolveBonus)
                    .build()
    );

    public static void apply(@Nonnull BaseVillager villager) {
        GeneticsProfile genetics = villager.getGenetics();

        for (Binding binding : BINDINGS) {
            AttributeInstance attribute = villager.getAttribute(binding.attribute());
            if (attribute == null) {
                continue;
            }

            double amount = binding.resolver().applyAsDouble(genetics.getGeneValue(binding.gene()));
            attribute.removeModifier(binding.modifierId());
            attribute.addPermanentModifier(new AttributeModifier(binding.modifierId(), amount, binding.operation()));
        }

        if (villager.getHealth() > villager.getMaxHealth()) {
            villager.setHealth(villager.getMaxHealth());
        }
    }

    @Builder
    private record Binding(GeneType gene,
                           Holder<Attribute> attribute,
                           ResourceLocation modifierId,
                           AttributeModifier.Operation operation,
                           DoubleUnaryOperator resolver) {
    }

}
