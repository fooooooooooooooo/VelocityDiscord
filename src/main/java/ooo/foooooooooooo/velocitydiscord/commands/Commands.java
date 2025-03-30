package ooo.foooooooooooo.velocitydiscord.commands;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;
import ooo.foooooooooooo.velocitydiscord.config.PluginConfig;

public final class Commands {
  private static CommandMeta COMMAND;

  public static void registerCommands(CommandManager commandManager, PluginConfig config) {
    var node = BrigadierCommand
      .literalArgumentBuilder(config.getMinecraftConfig().PLUGIN_COMMAND)
      .then(ReloadCommand.create())
      .then(TopicPreviewCommand.create())
      .build();

    var command = new BrigadierCommand(node);

    var meta = commandManager.metaBuilder(command).plugin(VelocityDiscord.getInstance()).build();

    COMMAND = meta;

    commandManager.register(meta, command);
  }

  public static void unregisterCommands(CommandManager commandManager) {
    commandManager.unregister(COMMAND);
  }
}
