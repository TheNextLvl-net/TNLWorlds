package worlds.command;

import cloud.commandframework.exceptions.InvalidSyntaxException;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import core.annotation.FieldsAreNonnullByDefault;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.thenextlvl.town.util.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

import static cloud.commandframework.minecraft.extras.MinecraftExceptionHandler.ExceptionType.*;

@FieldsAreNonnullByDefault
public class CustomExceptionHandler {
    public static final MinecraftExceptionHandler<CommandSender> INSTANCE = new MinecraftExceptionHandler<CommandSender>()
            .withHandler(ExceptionType.INVALID_SYNTAX, e -> {
                var syntax = ((InvalidSyntaxException) e).getCorrectSyntax()
                        .replace("<", "\\<").replace(">", "\\>")
                        .replace("[", "<dark_gray>[<gold>").replace("]", "<dark_gray>]")
                        .replace("\\<", "<dark_gray><<gold>").replace("\\>", "<dark_gray>>")
                        .replace("|", "<dark_gray>|<red>");
                return MiniMessage.miniMessage().deserialize(Messages.PREFIX.message() + " <red>/" + syntax);
            })
            .withHandler(ExceptionType.INVALID_SENDER, (sender, exception) -> {
                var locale = sender instanceof Player player ? player.locale() : Messages.ENGLISH;
                return MiniMessage.miniMessage().deserialize(Messages.INVALID_SENDER.message(locale, sender));
            })
            .withHandler(ExceptionType.NO_PERMISSION, (sender, exception) -> {
                var locale = sender instanceof Player player ? player.locale() : Messages.ENGLISH;
                return MiniMessage.miniMessage().deserialize(Messages.NO_PERMISSION.message(locale, sender));
            })
            .withCommandExecutionHandler()
            .withArgumentParsingHandler();
    private static final Pattern SPECIAL_CHARACTERS_PATTERN = Pattern.compile("[^\\s\\w\\-]");
}
