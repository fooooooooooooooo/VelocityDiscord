package ooo.foooooooooooo.velocitydiscord.commands;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;

public final class Commands {
  public static void RegisterCommands(CommandManager commandManager) {
    var node = BrigadierCommand
      .literalArgumentBuilder("discord")
      .then(ReloadCommand.create())
      .then(TopicPreviewCommand.create())
      .build();

    var command = new BrigadierCommand(node);

    var meta = commandManager.metaBuilder(command).plugin(VelocityDiscord.getInstance()).build();

    commandManager.register(meta, command);
  }
}
