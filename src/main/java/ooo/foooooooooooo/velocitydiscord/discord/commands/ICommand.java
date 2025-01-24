package ooo.foooooooooooo.velocitydiscord.discord.commands;

import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;

public interface ICommand {
  void handle(SlashCommandInteraction interaction);

  String description();
}
