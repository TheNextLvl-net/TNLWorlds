package net.thenextlvl.worlds.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.suggestion.WorldSuggestionProvider;
import net.thenextlvl.worlds.model.WorldActionResult;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
@SuppressWarnings("UnstableApiUsage")
class WorldUnloadCommand {
    private final WorldsPlugin plugin;

    ArgumentBuilder<CommandSourceStack, ?> create() {
        return Commands.literal("unload")
                .requires(source -> source.getSender().hasPermission("worlds.command.unload"))
                .then(Commands.argument("world", ArgumentTypes.world())
                        .suggests(new WorldSuggestionProvider<>(plugin))
                        .then(Commands.argument("fallback", ArgumentTypes.world())
                                .suggests(new WorldSuggestionProvider<>(plugin))
                                .executes(context -> {
                                    var world = context.getArgument("world", World.class);
                                    var fallback = context.getArgument("fallback", World.class);
                                    var message = unload(world, fallback).getMessage();
                                    plugin.bundle().sendMessage(context.getSource().getSender(), message,
                                            Placeholder.parsed("world", world.key().asString()));
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .executes(context -> {
                            var world = context.getArgument("world", World.class);
                            var message = unload(world, null).getMessage();
                            plugin.bundle().sendMessage(context.getSource().getSender(), message,
                                    Placeholder.parsed("world", world.key().asString()));
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private WorldActionResult unload(World world, @Nullable World fallback) {
        if (world.getKey().toString().equals("minecraft:overworld"))
            return WorldActionResult.UNLOAD_EXEMPTED;

        var fallbackSpawn = fallback != null ? fallback.getSpawnLocation()
                : plugin.getServer().getWorlds().getFirst().getSpawnLocation();
        world.getPlayers().forEach(player -> player.teleport(fallbackSpawn));

        return plugin.getServer().unloadWorld(world, world.isAutoSave())
                ? WorldActionResult.UNLOAD_SUCCESS
                : WorldActionResult.UNLOAD_FAILED;
    }
}
