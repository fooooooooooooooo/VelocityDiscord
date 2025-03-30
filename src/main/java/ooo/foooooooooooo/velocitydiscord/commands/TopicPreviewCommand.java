package ooo.foooooooooooo.velocitydiscord.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;

public final class TopicPreviewCommand {
  public static LiteralArgumentBuilder<CommandSource> create() {
    return BrigadierCommand
      .literalArgumentBuilder("topic")
      .then(BrigadierCommand
        .literalArgumentBuilder("preview")
        .requires(source -> source.hasPermission("discord.topic.preview"))
        .executes(TopicPreviewCommand::execute));
  }

  private static int execute(CommandContext<CommandSource> source) {
    var discord = VelocityDiscord.getDiscord();

    if (discord == null) {
      source.getSource().sendPlainMessage("Plugin not initialized");
      return 0;
    }

    var topic = discord.generateChannelTopic();

    source.getSource().sendPlainMessage("Generated channel topic: \n\n" + topic + "\n");

    return Command.SINGLE_SUCCESS;
  }
}
