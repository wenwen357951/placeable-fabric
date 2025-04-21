package com.wennest.placeable.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.wennest.placeable.Placeable;
import com.wennest.placeable.PlaceableConfig;
import com.wennest.placeable.PlaceablePlants;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            PlaceableConfig config = AutoConfig.getConfigHolder(PlaceableConfig.class).getConfig();
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal(Placeable.MODID));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            ConfigCategory category = builder.getOrCreateCategory(Text.literal("generic"));
            for (PlaceablePlants plants : PlaceablePlants.values()) {
                boolean current = config.allowPlaceablePlants.get(plants);
                category.addEntry(entryBuilder
                        .startBooleanToggle(Text.literal(plants.getTranslationName()), current)
                        .setDefaultValue(true)
                        .setSaveConsumer(newValue -> config.allowPlaceablePlants.put(plants, newValue))
                        .build()
                );
            }

            builder.setSavingRunnable(() ->
                    AutoConfig.getConfigHolder(PlaceableConfig.class).save()
            );

            return builder.build();
        };
    }
}

