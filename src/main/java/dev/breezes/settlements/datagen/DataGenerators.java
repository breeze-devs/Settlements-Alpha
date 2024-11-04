package dev.breezes.settlements.datagen;

import dev.breezes.settlements.datagen.item.ModItemModelProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

//        generator.addProvider(event.includeServer(), new ModRecipeProvider(output, generator));
//        generator.addProvider(event.includeServer(), ModLootTableProvider.create(output));

//        generator.addProvider(event.includeClient(), new ModBlockStateProvider(output, helper));
        generator.addProvider(event.includeClient(), new ModItemModelProvider(output, helper));

//        ModBlockTagProvider blockTagProvider = generator.addProvider(event.includeServer(), new ModBlockTagProvider(output, lookupProvider, helper));
//        generator.addProvider(event.includeClient(), new ModItemTagProvider(output, lookupProvider, blockTagProvider.contentsGetter(), helper));
    }

}
