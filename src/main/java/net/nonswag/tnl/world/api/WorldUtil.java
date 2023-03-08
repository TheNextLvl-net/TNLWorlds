package net.nonswag.tnl.world.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.nonswag.core.api.annotation.FieldsAreNonnullByDefault;
import net.nonswag.core.api.annotation.MethodsReturnNonnullByDefault;
import net.nonswag.core.api.file.formats.JsonFile;
import net.nonswag.core.api.file.helper.FileHelper;
import net.nonswag.core.utils.LinuxUtil;
import net.nonswag.tnl.listener.api.plugin.PluginManager;
import net.nonswag.tnl.listener.api.world.Dimension;
import net.nonswag.tnl.listener.api.world.WorldHelper;
import net.nonswag.tnl.world.api.events.WorldDeleteEvent;
import net.nonswag.tnl.world.api.world.Environment;
import net.nonswag.tnl.world.api.world.TNLWorld;
import net.nonswag.tnl.world.api.world.WorldType;
import net.nonswag.tnl.world.generators.CustomGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WorldUtil {

    @Getter
    private static final JsonFile saves = new JsonFile("plugins/Worlds/", "saves.json");
    private static final Logger logger = LoggerFactory.getLogger(WorldUtil.class);

    public static List<String> getWorlds() {
        List<String> strings = new ArrayList<>();
        JsonObject root = getSaves().getRoot().getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) strings.add(entry.getKey());
        return strings;
    }

    public static void export(TNLWorld world) {
        JsonObject jsonObject = getSaves().getRoot().getAsJsonObject();
        JsonObject object = new JsonObject();
        object.addProperty("type", world.type().name());
        object.addProperty("environment", world.environment().name());
        object.addProperty("seed", world.bukkit().getSeed());
        object.addProperty("full-bright", world.fullBright());
        if (world.generator() != null) object.addProperty("generator", world.generator());
        jsonObject.add(world.bukkit().getName(), object);
    }

    public static void exportAll() {
        TNLWorld.cast(Bukkit.getWorlds()).forEach(WorldUtil::export);
        getSaves().save();
    }

    public static boolean unloadWorld(TNLWorld world, boolean save) {
        Dimension dimension = WorldHelper.getInstance().getDimension(world.bukkit());
        if (!WorldHelper.getInstance().isRegistered(dimension)) return false;
        if (dimension.equals(Dimension.OVERWORLD)) return false;
        if (WorldHelper.getInstance().hasPlayers(world.bukkit())) {
            List<World> worlds = Bukkit.getWorlds();
            worlds.remove(world.bukkit());
            World to = worlds.isEmpty() ? null : worlds.get(0);
            if (to == null) return false;
            world.bukkit().getPlayers().forEach(all -> all.teleport(to.getSpawnLocation()));
            WorldHelper.getInstance().removePlayers(world.bukkit());
        }
        return Bukkit.unloadWorld(world.unregister().bukkit(), save);
    }

    public static boolean deleteWorld(TNLWorld world) {
        if (new WorldDeleteEvent(world).call() && unloadWorld(world, false)) {
            File file = new File(Bukkit.getWorldContainer(), world.bukkit().getName());
            FileHelper.delete(file);
            JsonObject root = getSaves().getRoot().getAsJsonObject();
            root.remove(world.bukkit().getName());
            return !file.exists();
        } else return false;
    }

    public static void loadWorlds() {
        try {
            getWorlds().forEach(WorldUtil::loadWorld);
        } catch (Exception e) {
            LinuxUtil.Suppressed.runShellCommand("cp %s broken-%s".formatted(getSaves().getFile().getName(), getSaves().getFile().getName()), getSaves().getFile().getAbsoluteFile().getParentFile());
        }
    }

    @Nullable
    public static TNLWorld loadWorld(String name) {
        JsonObject root = getSaves().getRoot().getAsJsonObject();
        if (!root.has(name) || !root.get(name).isJsonObject()) return null;
        File sessionLock = new File(new File(Bukkit.getWorldContainer(), name), "session.lock");
        if (sessionLock.exists()) FileHelper.delete(sessionLock);
        JsonObject object = root.getAsJsonObject(name);
        WorldCreator worldCreator = new WorldCreator(name);
        WorldType worldType;
        Environment environment;
        String generator = null;
        boolean fullBright = object.has("full-bright") && object.get("full-bright").getAsBoolean();
        if (object.has("generator") && !(generator = object.get("generator").getAsString()).isEmpty()) {
            Plugin plugin = PluginManager.getPlugin(generator);
            if (plugin == null) plugin = CustomGenerator.getGenerator(generator);
            if (plugin == null || (!plugin.isEnabled() && !(plugin instanceof CustomGenerator))) {
                worldCreator.generator(((ChunkGenerator) null));
            } else worldCreator.generator(plugin.getDefaultWorldGenerator(name, null));
            if (plugin == null) logger.error("Generator <'{}'> not found", generator);
        } else worldCreator.generator(((ChunkGenerator) null));
        if (object.has("type") && (worldType = WorldType.getByName(object.get("type").getAsString())) != null) {
            worldCreator.type(worldType.getWorldType());
        } else worldCreator.type((worldType = WorldType.NORMAL).getWorldType());
        if (object.has("environment")) {
            environment = Environment.getByName(object.get("environment").getAsString());
            if (environment != null) worldCreator.environment(environment.getEnvironment());
            else environment = Environment.NORMAL;
        } else worldCreator.environment((environment = Environment.NORMAL).getEnvironment());
        if (!object.has("full-bright")) fullBright = environment.equals(Environment.NETHER);
        if (object.has("seed")) worldCreator.seed(object.get("seed").getAsLong());
        World created = worldCreator.createWorld();
        if (created == null) return null;
        return new TNLWorld(created, environment, worldType, generator, fullBright).register();
    }
}
