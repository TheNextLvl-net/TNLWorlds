package net.thenextlvl.worlds;

import core.annotation.FieldsAreNotNullByDefault;
import core.annotation.ParametersAreNotNullByDefault;
import core.i18n.file.ComponentBundle;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.thenextlvl.worlds.api.WorldsProvider;
import net.thenextlvl.worlds.api.link.LinkController;
import net.thenextlvl.worlds.api.model.Generator;
import net.thenextlvl.worlds.api.model.LevelBuilder;
import net.thenextlvl.worlds.api.preset.Presets;
import net.thenextlvl.worlds.api.view.GeneratorView;
import net.thenextlvl.worlds.api.view.LevelView;
import net.thenextlvl.worlds.command.WorldCommand;
import net.thenextlvl.worlds.controller.WorldLinkController;
import net.thenextlvl.worlds.listener.PortalListener;
import net.thenextlvl.worlds.listener.ServerListener;
import net.thenextlvl.worlds.model.PaperLevelBuilder;
import net.thenextlvl.worlds.version.PluginVersionChecker;
import net.thenextlvl.worlds.view.PaperLevelView;
import net.thenextlvl.worlds.view.PluginGeneratorView;
import org.bstats.bukkit.Metrics;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Locale;

import static org.bukkit.persistence.PersistentDataType.BOOLEAN;
import static org.bukkit.persistence.PersistentDataType.STRING;

@Getter
@Accessors(fluent = true)
@FieldsAreNotNullByDefault
@ParametersAreNotNullByDefault
public class WorldsPlugin extends JavaPlugin implements WorldsProvider {
    private final GeneratorView generatorView = new PluginGeneratorView();
    private final LevelView levelView = new PaperLevelView(this);

    private final LinkController linkController = new WorldLinkController(this);

    private final File presetsFolder = new File(getDataFolder(), "presets");
    private final File translations = new File(getDataFolder(), "translations");

    private final ComponentBundle bundle = new ComponentBundle(translations, audience ->
            audience instanceof Player player ? player.locale() : Locale.US)
            .register("worlds", Locale.US)
            .register("worlds_german", Locale.GERMANY)
            .miniMessage(bundle -> MiniMessage.builder().tags(TagResolver.resolver(
                    TagResolver.standard(),
                    Placeholder.component("prefix", bundle.component(Locale.US, "prefix"))
            )).build());

    private final PluginVersionChecker versionChecker = new PluginVersionChecker(this);
    private final Metrics metrics = new Metrics(this, 19652);

    private final boolean foliaCompatible = folia();

    @Override
    public void onLoad() {
        if (!presetsFolder().isDirectory()) saveDefaultPresets();
        versionChecker().checkVersion();
        registerServices();
    }

    @Override
    public void onEnable() {
        registerListeners();
        registerCommands();
    }

    @Override
    public void onDisable() {
        metrics().shutdown();
        unloadWorlds();
    }

    @Override
    public LevelBuilder levelBuilder(File level) {
        return new PaperLevelBuilder(this, level);
    }

    private void unloadWorlds() {
        getServer().getWorlds().stream().filter(world -> !world.isAutoSave()).forEach(world -> {
            world.getPlayers().forEach(player -> player.kick(getServer().shutdownMessage()));
            getServer().unloadWorld(world, false);
        });
    }

    public void persistWorld(World world, boolean enabled) {
        var worldKey = new NamespacedKey("worlds", "world_key");
        world.getPersistentDataContainer().set(worldKey, STRING, world.getKey().asString());
        persistStatus(world, enabled, true);
    }

    public void persistStatus(World world, boolean enabled, boolean force) {
        var enabledKey = new NamespacedKey("worlds", "enabled");
        if (!force && !world.getPersistentDataContainer().has(enabledKey)) return;
        world.getPersistentDataContainer().set(enabledKey, BOOLEAN, enabled);
    }

    public void persistGenerator(World world, Generator generator) {
        var generatorKey = new NamespacedKey("worlds", "generator");
        world.getPersistentDataContainer().set(generatorKey, STRING, generator.serialize());
    }

    private void registerServices() {
        getServer().getServicesManager().register(WorldsProvider.class, this, this, ServicePriority.Highest);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PortalListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerListener(this), this);
    }

    private void registerCommands() {
        new WorldCommand(this).register();
    }

    private void saveDefaultPresets() {
        Presets.BOTTOMLESS_PIT.saveToFile(new File(presetsFolder(), "bottomless-pit.json"), true);
        Presets.CLASSIC_FLAT.saveToFile(new File(presetsFolder(), "classic-flat.json"), true);
        Presets.DESERT.saveToFile(new File(presetsFolder(), "desert.json"), true);
        Presets.OVERWORLD.saveToFile(new File(presetsFolder(), "overworld.json"), true);
        Presets.REDSTONE_READY.saveToFile(new File(presetsFolder(), "redstone-ready.json"), true);
        Presets.SNOWY_KINGDOM.saveToFile(new File(presetsFolder(), "snowy-kingdom.json"), true);
        Presets.THE_VOID.saveToFile(new File(presetsFolder(), "the-void.json"), true);
        Presets.TUNNELERS_DREAM.saveToFile(new File(presetsFolder(), "tunnelers-dream.json"), true);
        Presets.WATER_WORLD.saveToFile(new File(presetsFolder(), "water-world.json"), true);
    }

    private boolean folia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
