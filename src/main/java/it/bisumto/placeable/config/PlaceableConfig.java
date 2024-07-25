package it.bisumto.placeable.config;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import it.bisumto.placeable.Placeable;
import it.bisumto.placeable.util.FileUtil;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.List;

@Getter
public class PlaceableConfig {
    private static final File configFile = FabricLoader.getInstance().getConfigDir().resolve("placeable.json").toFile();

    @SerializedName("disable_plants")
    private List<String> disablePlants;

    @NotNull
    public static PlaceableConfig loadOrExtract() {
        if (FileUtil.create(configFile)) {
            try (InputStream inputStream = Placeable.class.getResourceAsStream("/placeable.json")) {
                if (inputStream != null) {
                    FileUtil.copy(inputStream, configFile);
                }
            } catch (IOException exception) {
                Placeable.LOGGER.error("A problem occurred while copying the configuration file", exception);
            }
        }

        try {
            Gson gson = new Gson();
            JsonReader jsonReader = new JsonReader(new FileReader(configFile));
            return gson.fromJson(jsonReader, PlaceableConfig.class);
        } catch (FileNotFoundException exception) {
            Placeable.LOGGER.error("A problem occurred while read json by Gson.", exception);
            return new PlaceableConfig();
        }
    }
}
