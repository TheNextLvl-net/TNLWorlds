package net.thenextlvl.worlds.controller;

import lombok.RequiredArgsConstructor;
import net.thenextlvl.worlds.WorldsPlugin;
import net.thenextlvl.worlds.link.LinkController;
import net.thenextlvl.worlds.link.Relative;
import org.bukkit.NamespacedKey;
import org.bukkit.PortalType;
import org.bukkit.World;

import java.util.Optional;

@RequiredArgsConstructor
public class WorldLinkController implements LinkController {
    private final WorldsPlugin plugin;

    @Override
    public Optional<NamespacedKey> getChild(World world, Relative relative) {
        return Optional.empty();
    }

    @Override
    public Optional<NamespacedKey> getParent(World world) {
        return Optional.empty();
    }

    @Override
    public Optional<NamespacedKey> getTarget(World world, Relative relative) {
        return Optional.empty();
    }

    @Override
    public Optional<NamespacedKey> getTarget(World world, PortalType type) {
        return switch (type) {
            case NETHER -> switch (world.getEnvironment()) {
                case NORMAL, THE_END -> getTarget(world, Relative.NETHER);
                case NETHER -> getTarget(world, Relative.OVERWORLD);
                default -> Optional.empty();
            };
            case ENDER -> switch (world.getEnvironment()) {
                case NORMAL, NETHER -> getTarget(world, Relative.THE_END);
                case THE_END -> getTarget(world, Relative.OVERWORLD);
                default -> Optional.empty();
            };
            default -> Optional.empty();
        };
    }

    @Override
    public boolean canLink(World source, World destination) {
        return false;
    }

    @Override
    public boolean link(World source, World destination) {
        return false;
    }
}
