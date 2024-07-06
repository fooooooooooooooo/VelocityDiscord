package ooo.foooooooooooo.velocitydiscord.discord.commands;

import com.velocitypowered.api.proxy.ProxyServer;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;
import ooo.foooooooooooo.velocitydiscord.config.Config;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

public class ListCommand implements ICommand {
  private final ProxyServer server;
  private final Config config;

  public ListCommand(ProxyServer server, Config config) {
    this.server = server;
    this.config = config;
  }

  @Override
  public void handle(SlashCommandInteraction interaction) {
    final var servers = server.getAllServers();

    final var sb = new StringBuilder();
    sb.append("```").append(config.listCommand.CODEBLOCK_LANG).append('\n');

    for (var server : servers) {
      var name = server.getServerInfo().getName();

      if (config.serverDisabled(name)) {
        continue;
      }

      var players = server.getPlayersConnected();

      var state = VelocityDiscord.getListener().getServerState(server);

      var serverInfo = new StringTemplate(config.listCommand.SERVER_FORMAT)
        .add("server_name", name)
        .add("online_players", state.players)
        .add("max_players", state.maxPlayers).toString();

      sb.append(serverInfo).append('\n');

      if (!state.online && config.listCommand.SERVER_OFFLINE_FORMAT.isPresent()) {
        sb.append(config.listCommand.SERVER_OFFLINE_FORMAT.get()).append('\n');
      } else if (state.players == 0 && config.listCommand.NO_PLAYERS_FORMAT.isPresent()) {
        sb.append(config.listCommand.NO_PLAYERS_FORMAT.get()).append('\n');
      } else {
        for (var player : players) {
          var user = new StringTemplate(config.listCommand.PLAYER_FORMAT)
            .add("username", player.getUsername()).toString();

          sb.append(user).append('\n');
        }
      }

      sb.append('\n');
    }
    sb.append("```");

    interaction.reply(sb.toString()).setEphemeral(config.listCommand.EPHEMERAL).queue();
  }
}
