package ooo.foooooooooooo.velocitydiscord.discord.commands;

import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

public class ListCommand implements ICommand {
  public static final String COMMAND_NAME = "list";

  public ListCommand() {}

  @Override
  public void handle(SlashCommandInteraction interaction) {
    final var servers = VelocityDiscord.SERVER.getAllServers();

    final var sb = new StringBuilder();
    sb.append("```").append(VelocityDiscord.CONFIG.global.discord.commands.list.codeblockLang).append('\n');

    for (var server : servers) {
      var name = server.getServerInfo().getName();

      if (VelocityDiscord.CONFIG.serverDisabled(name)) {
        continue;
      }

      var serverDiscordConfig = VelocityDiscord.CONFIG.getServerConfig(name).getDiscordConfig();

      var players = server.getPlayersConnected();

      var state = VelocityDiscord.getListener().getServerState(server);

      var serverInfo = new StringTemplate(serverDiscordConfig.commands.list.serverFormat)
        .add("server_name", VelocityDiscord.CONFIG.serverName(name))
        .add("online_players", state.players)
        .add("max_players", state.maxPlayers)
        .toString();

      sb.append(serverInfo).append('\n');

      if (!state.online && serverDiscordConfig.commands.list.serverOfflineFormat.isPresent()) {
        sb.append(serverDiscordConfig.commands.list.serverOfflineFormat.get()).append('\n');
      } else if (state.players == 0 && serverDiscordConfig.commands.list.noPlayersFormat.isPresent()) {
        sb.append(serverDiscordConfig.commands.list.noPlayersFormat.get()).append('\n');
      } else {
        for (var player : players) {
          var user = new StringTemplate(serverDiscordConfig.commands.list.playerFormat)
            .add("username", player.getUsername())
            .toString();

          sb.append(user).append('\n');
        }
      }

      sb.append('\n');
    }
    sb.append("```");

    interaction
      .reply(sb.toString())
      .setEphemeral(VelocityDiscord.CONFIG.global.discord.commands.list.ephemeral)
      .queue();
  }

  @Override
  public String description() {
    return "List all servers and their players";
  }
}
