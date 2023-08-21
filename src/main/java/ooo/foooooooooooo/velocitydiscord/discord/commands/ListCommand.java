package ooo.foooooooooooo.velocitydiscord.discord.commands;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import ooo.foooooooooooo.velocitydiscord.Config;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class ListCommand implements ICommand {
  private final ProxyServer server;
  private final Logger logger;
  private final Config config;

  private final HashMap<RegisteredServer, Integer> serverMaxPlayers = new HashMap<>();

  public ListCommand(ProxyServer server, Logger logger, Config config) {
    this.server = server;
    this.logger = logger;
    this.config = config;
  }

  @Override
  public void handle(SlashCommandInteraction interaction) {
    final var servers = server.getAllServers();

    final var sb = new StringBuilder();
    sb.append("```").append(config.DISCORD_LIST_CODEBLOCK_LANG).append('\n');

    // todo: cache this longer if it ever becomes an issue
    updateMaxPlayers(servers);

    for (var server : servers) {
      var name = server.getServerInfo().getName();
      var players = server.getPlayersConnected();

      var playerCount = players.size();
      var maxPlayerCount = serverMaxPlayers.get(server);

      var serverInfo = new StringTemplate(config.DISCORD_LIST_SERVER_FORMAT)
        .add("server_name", name)
        .add("online_players", playerCount)
        .add("max_players", maxPlayerCount).toString();

      sb.append(serverInfo).append('\n');

      if (maxPlayerCount == 0 && config.DISCORD_LIST_SERVER_OFFLINE.isPresent()) {
        sb.append(config.DISCORD_LIST_SERVER_OFFLINE.get()).append('\n');
      } else if (playerCount == 0 && config.DISCORD_LIST_NO_PLAYERS.isPresent()) {
        sb.append(config.DISCORD_LIST_NO_PLAYERS.get()).append('\n');
      } else {
        for (var player : players) {
          var user = new StringTemplate(config.DISCORD_LIST_PLAYER_FORMAT)
            .add("username", player.getUsername()).toString();

          sb.append(user).append('\n');
        }
      }

      sb.append('\n');
    }
    sb.append("```");

    interaction.reply(sb.toString()).setEphemeral(config.DISCORD_LIST_EPHEMERAL).queue();
  }

  private void updateMaxPlayers(Collection<RegisteredServer> servers) {
    CompletableFuture
      .allOf(servers.parallelStream().map((server) -> server.ping().handle((ping, ex) -> handlePing(server, ping, ex))).toArray(CompletableFuture[]::new))
      .join();
  }

  private CompletableFuture<Void> handlePing(RegisteredServer server, ServerPing ping, Throwable ex) {
    if (ex != null) {
      logger.warning("Could not ping server: " + ex);

      serverMaxPlayers.put(server, 0);
    } else {
      serverMaxPlayers.put(server, ping.getPlayers().map(ServerPing.Players::getMax).orElse(0));
    }

    return null;
  }
}
