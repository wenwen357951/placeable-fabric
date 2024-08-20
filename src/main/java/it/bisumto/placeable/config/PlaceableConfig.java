package it.bisumto.placeable.config;

import com.google.gson.annotations.SerializedName;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class PlaceableConfig {
    @Setter
    @SerializedName("version")
    public int configVersion = 0;

    @SerializedName("disable_plants")
    public Set<String> disablePlants;
}
