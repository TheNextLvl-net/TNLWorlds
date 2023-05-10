package net.thenextlvl.worlds.preset;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import core.annotation.MethodsReturnNonnullByDefault;
import core.annotation.ParametersAreNonnullByDefault;
import core.api.file.format.GsonFile;

import java.io.File;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record Preset(JsonObject settings) {

    public static Preset of(File file) {
        var gson = new GsonFile<JsonObject>(file, JsonObject.class) {
            @Override
            public GsonBuilder load(GsonBuilder builder) {
                return new GsonBuilder();
            }
        };
        if (!gson.getRoot().has("settings"))
            return new Preset(gson.getRoot());
        return new Preset(gson.getRoot().getAsJsonObject("settings"));
    }

    public static List<File> findPresets(File dataFolder) {
        File[] files = dataFolder.listFiles((file, name) -> name.endsWith(".json"));
        return files != null ? List.of(files) : Collections.emptyList();
    }
}
