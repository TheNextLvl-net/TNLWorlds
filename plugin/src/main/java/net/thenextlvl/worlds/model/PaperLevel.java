package net.thenextlvl.worlds.model;

import com.google.gson.JsonObject;
import core.nbt.file.NBTFile;
import core.nbt.tag.ByteTag;
import core.nbt.tag.CompoundTag;
import core.nbt.tag.LongTag;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.api.model.*;
import net.thenextlvl.worlds.api.preset.Preset;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Accessors(fluent = true)
public class PaperLevel implements Level {
    private final NBTFile<CompoundTag> levelData;
    private final WorldsPlugin plugin;

    private final NamespacedKey key;
    private final String name;
    private final World.Environment environment;
    private final WorldPreset type;

    private final @Nullable Generator generator;
    private final @Nullable Preset preset;

    private final boolean enabled;
    private final boolean hardcore;
    private final boolean importedBefore;
    private final boolean structures;
    private final long seed;

    public PaperLevel(WorldsPlugin plugin, LevelBuilder builder) {
        this.levelData = plugin.levelView().getLevelDataFile(builder.level());
        this.plugin = plugin;

        this.name = Optional.ofNullable(builder.name())
                .orElseGet(() -> builder.level().getName());

        var data = levelData().getRoot().<CompoundTag>optional("Data");
        var extras = data.flatMap(plugin.levelView()::getExtras);

        this.importedBefore = extras.isPresent();
        this.enabled = extras.filter(LevelExtras::enabled).isPresent();

        this.environment = Optional.ofNullable(builder.environment())
                .orElseGet(() -> plugin.levelView().getEnvironment(builder.level()));

        this.key = Optional.ofNullable(builder.key())
                .or(() -> extras.map(LevelExtras::key))
                .orElseGet(() -> {
                    var namespace = builder.level().getName().toLowerCase()
                            .replace("(", "").replace(")", "")
                            .replace(" ", "_");
                    return new NamespacedKey("worlds", namespace);
                });

        var settings = data.flatMap(tag -> tag.<CompoundTag>optional("WorldGenSettings"));
        var dimensions = settings.flatMap(tag -> tag.<CompoundTag>optional("dimensions"));
        var dimension = dimensions.flatMap(tag -> tag.<CompoundTag>optional(
                plugin.levelView().getDimension(tag, environment)));
        var generator = dimension.flatMap(tag -> tag.<CompoundTag>optional("generator"));


        this.hardcore = Optional.ofNullable(builder.hardcore())
                .or(() -> settings.flatMap(tag -> tag.<ByteTag>optional("hardcore"))
                        .map(ByteTag::getAsBoolean))
                .orElse(true);
        this.seed = Optional.ofNullable(builder.seed())
                .or(() -> settings.flatMap(tag -> tag.<LongTag>optional("seed"))
                        .map(LongTag::getAsLong))
                .orElse(ThreadLocalRandom.current().nextLong());
        this.structures = Optional.ofNullable(builder.structures())
                .or(() -> settings.flatMap(tag -> tag.<ByteTag>optional("generate_features"))
                        .map(ByteTag::getAsBoolean))
                .orElse(true);


        var worldPreset = generator.flatMap(plugin.levelView()::getWorldPreset);

        this.preset = Optional.ofNullable(builder.preset())
                .or(() -> worldPreset.filter(preset -> preset.equals(WorldPreset.FLAT))
                        .flatMap(worldType -> generator.flatMap(plugin.levelView()::getFlatPreset)))
                .orElse(null);

        this.type = Optional.ofNullable(builder.type()).orElseGet(() ->
                worldPreset.orElse(WorldPreset.NORMAL));

        this.generator = Optional.ofNullable(builder.generator())
                .or(() -> extras.map(LevelExtras::generator))
                .orElse(null);
    }

    private WorldType typeOf(WorldPreset worldPreset) {
        if (worldPreset.equals(WorldPreset.AMPLIFIED)) return WorldType.AMPLIFIED;
        if (worldPreset.equals(WorldPreset.FLAT)) return WorldType.FLAT;
        if (worldPreset.equals(WorldPreset.LARGE_BIOMES)) return WorldType.LARGE_BIOMES;
        if (worldPreset.equals(WorldPreset.NORMAL)) return WorldType.NORMAL;
        plugin.getComponentLogger().warn("Custom world presets do not work yet, defaulting to normal");
        return WorldType.NORMAL;
    }

    @Override
    public Optional<World> create() {
        var generatorSettings = Optional.ofNullable(preset())
                .map(Preset::serialize)
                .map(JsonObject::toString)
                .orElse("");

        var creator = new WorldCreator(name(), key())
                .environment(environment())
                .generateStructures(structures())
                .generatorSettings(generatorSettings)
                .hardcore(hardcore())
                .seed(seed())
                .type(typeOf(type()));

        if (generator() != null) creator.generator(generator().generator(creator.name()));
        if (generator() != null) creator.biomeProvider(generator().biomeProvider(creator.name()));

        return Optional.ofNullable(createWorld(creator));
    }

    protected @Nullable World createWorld(WorldCreator creator) {
        return creator.createWorld();
    }
}
