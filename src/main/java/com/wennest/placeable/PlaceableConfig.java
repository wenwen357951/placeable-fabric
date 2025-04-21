package com.wennest.placeable;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.EnumMap;
import java.util.Map;

@Config(name = Placeable.MODID)
public class PlaceableConfig implements ConfigData {

    @Comment("Enable or disable the mod.")
    public boolean enable = true;

    @Comment("Allow placement on blocks without a top rim.")
    public boolean placedWithoutTopRim = false;

    @Comment("Allow or disable specific plants.")
    public Map<PlaceablePlants, Boolean> allowPlaceablePlants = new EnumMap<>(PlaceablePlants.class);

    public PlaceableConfig() {
        for (PlaceablePlants e : PlaceablePlants.values()) {
            allowPlaceablePlants.put(e, true);
        }
    }
}
