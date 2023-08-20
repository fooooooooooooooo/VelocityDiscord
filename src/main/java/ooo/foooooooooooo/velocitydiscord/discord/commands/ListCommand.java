package ooo.foooooooooooo.velocitydiscord.discord.commands;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import ooo.foooooooooooo.velocitydiscord.Config;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class ListCommand implements ICommand {
  private final ProxyServer server;
  private final Logger logger;
  private final Config config;

  public ListCommand(ProxyServer server, Logger logger, Config config) {
    this.server = server;
    this.logger = logger;
    this.config = config;
  }

  @Override
  @SuppressWarnings("null")
  public void handle(SlashCommandInteraction interaction) {
    final var servers = server.getAllServers();

    final var sb = new StringBuilder();
    sb.append("```").append(config.DISCORD_LIST_CODEBLOCK_LANG).append('\n');

    final var maxPlayersMap = new HashMap<RegisteredServer, Integer>(servers.size());
    CompletableFuture.allOf(servers.parallelStream().map(server -> server.ping().handle((ping, ex) -> {
      if (ex != null) {
        logger.warning("Could not ping server: " + ex);
        maxPlayersMap.put(server, 0);
        return null;
      }
      maxPlayersMap.put(server, ping.getPlayers().map(ServerPing.Players::getMax).orElse(0));
      return null;
    })).toArray(CompletableFuture[]::new)).join();

    for (final var server : servers) {
      final var name = server.getServerInfo().getName();
      final var players = server.getPlayersConnected();

      final var playerCount = players.size();
      final var maxPlayerCount = maxPlayersMap.get(server);

      sb
        .append(new StringTemplate(config.DISCORD_LIST_SERVER_FORMAT)
                  .add("server_name", name)
                  .add("online_players", playerCount)
                  .add("max_players", maxPlayerCount).toString())
        .append('\n');

      if (maxPlayerCount == 0) {
        if (!config.DISCORD_LIST_SERVER_OFFLINE.isEmpty()) {
          sb.append(config.DISCORD_LIST_SERVER_OFFLINE).append('\n');
        }
      } else if (playerCount == 0) {
        if (!config.DISCORD_LIST_NO_PLAYERS.isEmpty()) {
          sb.append(config.DISCORD_LIST_NO_PLAYERS).append('\n');
        }
      } else {
        for (var player : players) {
          sb
            .append(new StringTemplate(config.DISCORD_LIST_PLAYER_FORMAT)
                      .add("username", player.getUsername()).toString())
            .append('\n');
        }
      }

      sb.append('\n');
    }
    sb.append("```");

    interaction.reply(sb.toString()).setEphemeral(config.DISCORD_LIST_EPHEMERAL).queue();
  }
}
