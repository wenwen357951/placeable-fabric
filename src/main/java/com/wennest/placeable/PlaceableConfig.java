package com.wennest.placeable;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

import java.util.*;

@Config(name = Placeable.MODID)
public class PlaceableConfig implements ConfigData {

    public Map<PlaceablePlants, Boolean> allowPlaceablePlants = new EnumMap<>(PlaceablePlants.class);

    public PlaceableConfig() {
        for (PlaceablePlants e : PlaceablePlants.values()) {
            allowPlaceablePlants.put(e, true);
        }
    }
}
