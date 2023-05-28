package ooo.foooooooooooo.velocitydiscord.discord.commands;

import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;

public interface ICommand {
  String getDescription();

  void handle(SlashCommandInteraction interaction);
}
