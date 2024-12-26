package net.thenextlvl.worlds.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.api.model.Generator;
import net.thenextlvl.worlds.api.model.Level;
import net.thenextlvl.worlds.api.model.LevelBuilder;
import net.thenextlvl.worlds.api.model.WorldPreset;
import net.thenextlvl.worlds.api.preset.Preset;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.io.File;

@Getter
@Setter
@RequiredArgsConstructor
@Accessors(fluent = true)
public class PaperLevelBuilder implements LevelBuilder {
    private final WorldsPlugin plugin;
    private final File level;

    private @Nullable Boolean hardcore;
    private @Nullable Boolean structures;
    private @Nullable Generator generator;
    private @Nullable Long seed;
    private @Nullable NamespacedKey key;
    private @Nullable Preset preset;
    private @Nullable String name;
    private @Nullable World.Environment environment;
    private @Nullable WorldPreset type;

    @Override
    public Level build() {
        return plugin().foliaCompatible() ? new FoliaLevel(plugin, this) : new PaperLevel(plugin, this);
    }
}
