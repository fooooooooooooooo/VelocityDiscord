package ooo.foooooooooooo.velocitydiscord.discord.commands;

import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

import static ooo.foooooooooooo.velocitydiscord.VelocityDiscord.CONFIG;
import static ooo.foooooooooooo.velocitydiscord.VelocityDiscord.SERVER;

public class ListCommand implements ICommand {

  public ListCommand() { }

  public String getDescription() {
    return "List players";
  }

  @Override
  public void handle(SlashCommandInteraction interaction) {
    if (SERVER == null) {
      interaction.reply("Server is not running").setEphemeral(CONFIG.DISCORD_LIST_EPHEMERAL).queue();

      return;
    }

    final var sb = new StringBuilder();

    sb.append("```").append(CONFIG.DISCORD_LIST_CODEBLOCK_LANG).append('\n');

    final var playerNames = SERVER.getPlayerNames();

    final var playerCount = playerNames.length;
    final var maxPlayerCount = SERVER.getMaxPlayerCount();

    sb
      .append(new StringTemplate(CONFIG.DISCORD_LIST_SERVER_FORMAT)
        .add("online_players", playerCount)
        .add("max_players", maxPlayerCount))
      .append('\n');

    if (maxPlayerCount == 0) {
      if (!CONFIG.DISCORD_LIST_SERVER_OFFLINE.isEmpty()) {
        sb.append(CONFIG.DISCORD_LIST_SERVER_OFFLINE).append('\n');
      }
    } else if (playerCount == 0) {
      if (!CONFIG.DISCORD_LIST_NO_PLAYERS.isEmpty()) {
        sb.append(CONFIG.DISCORD_LIST_NO_PLAYERS).append('\n');
      }
    } else {
      for (var player : playerNames) {
        sb.append(new StringTemplate(CONFIG.DISCORD_LIST_PLAYER_FORMAT).add("username", player)).append('\n');
      }
    }

    sb.append("\n```");

    interaction.reply(sb.toString()).setEphemeral(CONFIG.DISCORD_LIST_EPHEMERAL).queue();
  }
}
