package net.thenextlvl.worlds.view;

import core.io.IO;
import core.io.PathIO;
import core.nbt.file.NBTFile;
import core.nbt.tag.CompoundTag;
import core.nbt.tag.ListTag;
import core.nbt.tag.StringTag;
import core.nbt.tag.Tag;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import lombok.RequiredArgsConstructor;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.api.model.Generator;
import net.thenextlvl.worlds.api.model.LevelExtras;
import net.thenextlvl.worlds.api.model.WorldPreset;
import net.thenextlvl.worlds.api.preset.Biome;
import net.thenextlvl.worlds.api.preset.Layer;
import net.thenextlvl.worlds.api.preset.Preset;
import net.thenextlvl.worlds.api.preset.Structure;
import net.thenextlvl.worlds.api.view.LevelView;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class PaperLevelView implements LevelView {
    private final WorldsPlugin plugin;

    @Override
    public Stream<File> listLevels() {
        return Optional.ofNullable(plugin.getServer().getWorldContainer()
                        .listFiles(File::isDirectory)).stream()
                .flatMap(files -> Arrays.stream(files).filter(this::isLevel));
    }

    @Override
    public boolean isLevel(File file) {
        return file.isDirectory() && (new File(file, "level.dat").isFile() || new File(file, "level.dat_old").isFile());
    }

    @Override
    public boolean hasNetherDimension(File level) {
        return new File(level, "DIM-1").isDirectory();
    }

    @Override
    public boolean hasEndDimension(File level) {
        return new File(level, "DIM1").isDirectory();
    }

    @Override
    public boolean canLoad(File level) {
        return plugin.getServer().getWorlds().stream()
                .map(World::getWorldFolder)
                .noneMatch(level::equals);
    }

    @Override
    public World.Environment getEnvironment(File level) {
        var end = hasEndDimension(level);
        var nether = hasNetherDimension(level);
        if (end && nether) return World.Environment.NORMAL;
        if (end) return World.Environment.THE_END;
        if (nether) return World.Environment.NETHER;
        return World.Environment.NORMAL;
    }

    @Override
    public Optional<LevelExtras> getExtras(CompoundTag data) {
        return data.optional("BukkitValues")
                .map(Tag::getAsCompound)
                .map(values -> {
                    var key = values.optional("worlds:world_key")
                            .map(Tag::getAsString)
                            .map(NamespacedKey::fromString);
                    var generator = values.optional("worlds:generator")
                            .map(Tag::getAsString)
                            .map(serialized -> Generator.deserialize(plugin, serialized));
                    var enabled = values.optional("worlds:enabled")
                            .map(Tag::getAsBoolean);
                    if (key.isEmpty() && generator.isEmpty() && enabled.isEmpty()) return null;
                    return new LevelExtras(key.orElse(null), generator.orElse(null), enabled.orElse(false));
                });
    }

    @Override
    public Optional<Preset> getFlatPreset(CompoundTag generator) {
        var settings = generator.<CompoundTag>optional("settings");

        if (settings.isEmpty()) return Optional.empty();

        var preset = new Preset();

        settings.flatMap(tag -> tag.<Tag>optional("biome"))
                .map(Tag::getAsString)
                .map(Biome::literal)
                .ifPresent(preset::biome);

        settings.flatMap(tag -> tag.<Tag>optional("features"))
                .map(Tag::getAsBoolean)
                .ifPresent(preset::features);

        settings.flatMap(tag -> tag.<Tag>optional("lakes"))
                .map(Tag::getAsBoolean)
                .ifPresent(preset::lakes);

        settings.flatMap(tag -> tag.<ListTag<CompoundTag>>optional("layers"))
                .map(tag -> tag.stream().map(layer -> {
                    var block = layer.optional("block").orElseThrow().getAsString();
                    var height = layer.optional("height").orElseThrow().getAsInt();
                    return new Layer(block, height);
                }).collect(Collectors.toCollection(LinkedHashSet::new)))
                .ifPresent(preset::layers);

        settings.flatMap(tag -> tag.optional("structure_overrides")
                        .filter(Tag::isList).map(Tag::getAsList))
                .map(list -> list.stream()
                        .map(structure -> new Structure(structure.getAsString()))
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .ifPresent(preset::structures);
        settings.flatMap(tag -> tag.optional("structure_overrides")
                        .filter(Tag::isString).map(Tag::getAsString))
                .map(Structure::new)
                .ifPresent(preset::addStructure);

        return Optional.of(preset);
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public Optional<String> getGenerator(World world) {
        if (world.getGenerator() == null) return Optional.empty();
        var loader = world.getGenerator().getClass().getClassLoader();
        if (!(loader instanceof ConfiguredPluginClassLoader pluginLoader)) return Optional.empty();
        if (pluginLoader.getPlugin() == null) return Optional.empty();
        return Optional.of(pluginLoader.getPlugin().getName());
    }

    @Override
    public NBTFile<CompoundTag> getLevelDataFile(File level) {
        return new NBTFile<>(Optional.of(
                IO.of(level, "level.dat")
        ).filter(PathIO::exists).orElseGet(() ->
                IO.of(level, "level.dat_old")
        ), new CompoundTag());
    }

    @Override
    public String getDimension(CompoundTag dimensions, World.Environment environment) {
        return switch (environment) {
            case NORMAL -> "minecraft:overworld";
            case NETHER -> "minecraft:the_nether";
            case THE_END -> "minecraft:the_end";
            case CUSTOM -> dimensions.keySet().stream().filter(s -> !s.startsWith("minecraft")).findAny()
                    .orElseThrow(() -> new UnsupportedOperationException("Could not find custom dimension"));
        };
    }

    @Override
    public Optional<WorldPreset> getWorldPreset(CompoundTag generator) {

        var settings = getGeneratorSettings(generator);
        if (settings.filter(s -> s.equals(WorldPreset.LARGE_BIOMES.key().asString())).isPresent())
            return Optional.of(WorldPreset.LARGE_BIOMES);
        if (settings.filter(s -> s.equals(WorldPreset.AMPLIFIED.key().asString())).isPresent())
            return Optional.of(WorldPreset.AMPLIFIED);

        var type = generator.<CompoundTag>optional("biome_source")
                .flatMap(tag -> tag.<StringTag>optional("type"))
                .map(Tag::getAsString);

        if (type.filter(s -> s.equals(WorldPreset.SINGLE_BIOME.key().asString())).isPresent())
            return Optional.of(WorldPreset.SINGLE_BIOME);
        if (type.filter(s -> s.equals(WorldPreset.CHECKERBOARD.key().asString())).isPresent())
            return Optional.of(WorldPreset.CHECKERBOARD);

        var generatorType = getGeneratorType(generator);
        if (generatorType.filter(s -> s.equals(WorldPreset.DEBUG.key().asString())).isPresent())
            return Optional.of(WorldPreset.DEBUG);
        if (generatorType.filter(s -> s.equals(WorldPreset.FLAT.key().asString())).isPresent())
            return Optional.of(WorldPreset.FLAT);
        if (generatorType.filter(s -> s.equals(WorldPreset.NORMAL.key().asString())).isPresent())
            return Optional.of(WorldPreset.NORMAL);

        return Optional.empty();
    }

    @Override
    public Optional<String> getGeneratorSettings(CompoundTag generator) {
        return generator.optional("settings").filter(Tag::isString).map(Tag::getAsString);
    }

    @Override
    public Optional<String> getGeneratorType(CompoundTag generator) {
        return generator.optional("type").map(Tag::getAsString);
    }

    @Override
    public void saveLevel(World world, boolean flush) {
        var level = ((CraftWorld) world).getHandle();
        var oldSave = level.noSave;
        level.noSave = false;
        level.save(null, flush, false);
        level.noSave = oldSave;
    }

    @Override
    public void saveLevelData(World world, boolean async) {
        var level = ((CraftWorld) world).getHandle();
        if (level.getDragonFight() != null) {
            level.serverLevelData.setEndDragonFightData(level.getDragonFight().saveData());
        }
        /*var save =*/ level.getChunkSource().getDataStorage().save(async);
        //if (!async) save.join();

        level.serverLevelData.setWorldBorder(level.getWorldBorder().createSettings());
        level.serverLevelData.setCustomBossEvents(level.getServer().getCustomBossEvents().save(level.registryAccess()));
        level.convertable.saveDataTag(level.getServer().registryAccess(), level.serverLevelData, level.getServer().getPlayerList().getSingleplayerData());
    }
}
