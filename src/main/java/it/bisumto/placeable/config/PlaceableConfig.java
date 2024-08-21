package it.bisumto.placeable.config;

import com.google.gson.annotations.SerializedName;
import lombok.Setter;

import java.util.Set;

public class PlaceableConfig {
    @Setter
    @SerializedName("version")
    public int configVersion = 0;

    @SerializedName("disable_plants")
    public Set<String> disablePlants;
}
