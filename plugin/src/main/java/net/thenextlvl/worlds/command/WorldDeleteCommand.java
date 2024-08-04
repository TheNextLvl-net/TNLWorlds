package net.thenextlvl.worlds.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.command.argument.CommandFlagsArgument;
import net.thenextlvl.worlds.command.suggestion.WorldSuggestionProvider;
import net.thenextlvl.worlds.model.WorldActionResult;
import org.bukkit.World;

import java.io.File;
import java.util.Set;

@RequiredArgsConstructor
@SuppressWarnings("UnstableApiUsage")
class WorldDeleteCommand {
    private final WorldsPlugin plugin;

    ArgumentBuilder<CommandSourceStack, ?> create() {
        return Commands.literal("delete")
                .requires(source -> source.getSender().hasPermission("worlds.command.delete"))
                .then(Commands.argument("world", ArgumentTypes.world())
                        .suggests(new WorldSuggestionProvider<>(plugin))
                        .then(Commands.argument("flags", new CommandFlagsArgument(
                                Set.of("--confirm", "--schedule")
                        )).executes(this::delete))
                        .executes(this::confirmationNeeded));
    }

    private int confirmationNeeded(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        plugin.bundle().sendMessage(sender, "command.confirmation",
                Placeholder.parsed("action", "/" + context.getInput()),
                Placeholder.parsed("confirmation", "/" + context.getInput() + " --confirm"));
        return Command.SINGLE_SUCCESS;
    }

    private int delete(CommandContext<CommandSourceStack> context) {
        var flags = context.getArgument("flags", CommandFlagsArgument.Flags.class);
        if (!flags.contains("--confirm")) return confirmationNeeded(context);
        var world = context.getArgument("world", World.class);
        var result = delete(world, flags.contains("--schedule"));
        plugin.bundle().sendMessage(context.getSource().getSender(), result.getMessage(),
                Placeholder.parsed("world", world.key().asString()));
        return Command.SINGLE_SUCCESS;
    }

    private WorldActionResult delete(World world, boolean schedule) {
        return schedule ? scheduleDeletion(world) : deleteNow(world);
    }

    private WorldActionResult deleteNow(World world) {
        if (world.getKey().toString().equals("minecraft:overworld"))
            return WorldActionResult.DELETE_EXEMPTED;

        var fallback = plugin.getServer().getWorlds().getFirst().getSpawnLocation();
        world.getPlayers().forEach(player -> player.teleport(fallback));

        if (!plugin.getServer().unloadWorld(world, false))
            return WorldActionResult.UNLOAD_FAILED;

        return delete(world.getWorldFolder())
                ? WorldActionResult.DELETE_SUCCESS
                : WorldActionResult.DELETE_FAILED;
    }

    private WorldActionResult scheduleDeletion(World world) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (delete(world.getWorldFolder())) return;
            plugin.getComponentLogger().error("Failed to delete world {}", world.getName());
        }));
        return WorldActionResult.DELETE_SCHEDULED;
    }

    private boolean delete(File file) {
        if (file.isFile()) return file.delete();
        var files = file.listFiles();
        if (files == null) return false;
        for (var file1 : files) delete(file1);
        return file.delete();
    }
}
