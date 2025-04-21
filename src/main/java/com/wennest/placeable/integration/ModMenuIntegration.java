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

            // General Category
            ConfigCategory genericCategory = builder.getOrCreateCategory(
                    Text.translatable("config.placeable.category.general")
            );
            genericCategory.addEntry(entryBuilder
                    .startBooleanToggle(
                            Text.translatable("config.placeable.option.enable"),
                            config.enable
                    )
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> config.enable = newValue)
                    .build()
            );
            genericCategory.addEntry(entryBuilder
                    .startBooleanToggle(
                            Text.translatable("config.placeable.option.placed_without_top_rim"),
                            config.placedWithoutTopRim
                    )
                    .setTooltip(
                            Text.translatable("config.placeable.option.placed_without_top_rim.tooltip")
                    )
                    .setDefaultValue(false)
                    .setSaveConsumer(newValue -> config.placedWithoutTopRim = newValue)
                    .build()
            );

            // Allowed Plants Category
            ConfigCategory allowedPlantsCategory = builder.getOrCreateCategory(
                    Text.translatable("config.placeable.category.allowed_plants")
            );
            for (PlaceablePlants plants : PlaceablePlants.values()) {
                boolean current = config.allowPlaceablePlants.get(plants);
                allowedPlantsCategory.addEntry(entryBuilder
                        .startBooleanToggle(
                                Text.literal(plants.getTranslationName()),
                                current
                        )
                        .setDefaultValue(true)
                        .setSaveConsumer(newValue -> config.allowPlaceablePlants.put(plants, newValue))
                        .build()
                );
            }

            // Saving
            builder.setSavingRunnable(() ->
                    AutoConfig.getConfigHolder(PlaceableConfig.class).save()
            );

            return builder.build();
        };
    }
}

