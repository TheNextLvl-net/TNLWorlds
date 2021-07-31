package net.nonswag.tnl.world;

import net.nonswag.tnl.listener.api.command.CommandManager;
import net.nonswag.tnl.listener.api.event.EventManager;
import net.nonswag.tnl.listener.api.plugin.PluginUpdate;
import net.nonswag.tnl.listener.api.settings.Settings;
import net.nonswag.tnl.world.api.WorldUtil;
import net.nonswag.tnl.world.api.generator.BuildWorldGenerator;
import net.nonswag.tnl.world.commands.WorldCommand;
import net.nonswag.tnl.world.listeners.WorldListener;
import net.nonswag.tnl.world.completer.WorldCommandTabCompleter;
import org.bukkit.*;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Worlds extends JavaPlugin {

    @Nullable
    private static Worlds instance = null;

    @Override
    public void onEnable() {
        setInstance(this);
        for (World world : Bukkit.getWorlds()) WorldUtil.getInstance().save(world);
        WorldUtil.getInstance().loadWorlds();
        CommandManager commandManager = new CommandManager(this);
        commandManager.registerCommand("world", "tnl.world", new WorldCommand(), new WorldCommandTabCompleter());
        EventManager eventManager = EventManager.cast(this);
        eventManager.registerListener(new WorldListener());
        if (Settings.AUTO_UPDATER.getValue()) new PluginUpdate(this).downloadUpdate();
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@Nonnull String worldName, @Nullable String id) {
        return new BuildWorldGenerator();
    }

    @Nonnull
    public static Worlds getInstance() {
        assert instance != null;
        return instance;
    }

    private static void setInstance(@Nonnull Worlds instance) {
        Worlds.instance = instance;
    }
}
