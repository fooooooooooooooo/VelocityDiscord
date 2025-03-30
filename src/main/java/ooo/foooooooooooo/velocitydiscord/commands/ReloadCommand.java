package ooo.foooooooooooo.velocitydiscord.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;

import java.text.MessageFormat;

public final class ReloadCommand {
  public static LiteralArgumentBuilder<CommandSource> create() {
    return BrigadierCommand
      .literalArgumentBuilder("reload")
      .requires(source -> source.hasPermission("discord.reload"))
      .executes(ReloadCommand::execute);
  }

  private static int execute(CommandContext<CommandSource> source) {
    String error;

    try {
      error = VelocityDiscord.getInstance().reloadConfig();
    } catch (Exception e) {
      error = e.getMessage();
    }

    if (error == null) {
      source.getSource().sendPlainMessage("Config reloaded");
      return Command.SINGLE_SUCCESS;
    } else {
      source
        .getSource()
        .sendPlainMessage(MessageFormat.format(
          "Error reloading config:\n{0}\n\nFix the error and reload again",
          error
        ));
      return 0;
    }
  }
}
