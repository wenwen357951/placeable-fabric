package it.bisumto.placeable.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.bisumto.placeable.Placeable;
import it.bisumto.placeable.util.FileUtil;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;

@Slf4j
public class ConfigManager {

    public static final int LATEST_VERSION = 1;
    private final File placeableConfigFile;

    @Getter
    private PlaceableConfig placeableConfig;

    public ConfigManager() {
        this.placeableConfigFile = FabricLoader
                .getInstance()
                .getConfigDir()
                .resolve("placeable.json")
                .toFile();
    }

    public void setup() {
        this.loadOrExtract(false);
        if (this.migrationConfig()) {
            this.reload();
        }
    }

    public void loadOrExtract(boolean force) {
        if (FileUtil.create(this.placeableConfigFile) || force) {
            try (InputStream inputStream = Placeable.class.getResourceAsStream("/placeable.json")) {
                if (inputStream != null) {
                    FileUtil.copy(inputStream, this.placeableConfigFile);
                }
            } catch (IOException exception) {
                Placeable.LOGGER.error("A problem occurred while copying the configuration file", exception);
            }
        }
        this.reload();
    }

    public boolean migrationConfig() {
        int currentConfigVersion = this.placeableConfig.configVersion;
        if (currentConfigVersion >= LATEST_VERSION) {
            return false;
        }

        log.info("Placeable configuration version is older than the latest version.");
        // Migrate to version: 1
        this.placeableConfig.disablePlants.add("//minecraft:carrots");
        this.placeableConfig.disablePlants.add("//minecraft:potatoes");
        this.placeableConfig.disablePlants.add("//minecraft:red_mushroom");
        this.placeableConfig.disablePlants.add("//minecraft:brown_mushroom");

        // Saved Upgraded Config
        this.placeableConfig.configVersion = LATEST_VERSION;
        this.save();
        return true;
    }

    public void reload() {
        try {
            Gson gson = new Gson();
            JsonReader jsonReader = new JsonReader(new FileReader(this.placeableConfigFile));
            this.placeableConfig = gson.fromJson(jsonReader, PlaceableConfig.class);
        } catch (FileNotFoundException exception) {
            Placeable.LOGGER.error("A problem occurred while read json by Gson.", exception);
            this.placeableConfig = new PlaceableConfig();
        }

        System.out.println("Config: " + this.placeableConfig);
        if (this.placeableConfig == null) {
            this.loadOrExtract(true);
        }
    }

    @SneakyThrows
    public void save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonWriter jsonWriter = new JsonWriter(
                new FileWriter(placeableConfigFile)
        );
        jsonWriter.setIndent("  ");
        String jsonString = gson.toJson(this.placeableConfig.disablePlants);
        jsonWriter.beginObject()
                .name("_comment").value("DO NOT MODIFY THE VERSION VALUE!")
                .name("version").value(this.placeableConfig.configVersion)
                .name("HOW_TO_DISABLE_PLANTS!!!").beginArray()
                .value("Please remove the '//' at the beginning of the string.")
                .value("For example, to disable the ability to place cactus anywhere,")
                .value("change '//minecraft:cactus' to 'minecraft:cactus'")
                .endArray()
                .name("disable_plants").jsonValue(jsonString)
                .endObject();
        jsonWriter.close();
    }
}
