package ooo.foooooooooooo.velocitydiscord.discord.commands;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import ooo.foooooooooooo.velocitydiscord.Config;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

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

        for (final var server : servers) {
            final var name = server.getServerInfo().getName();
            final var players = server.getPlayersConnected();
            final var maxPlayers = server.ping().handle((ping, ex) -> {
                if (ex != null) {
                    logger.warning("Could not ping server: " + ex);
                    return 0;
                }
                return ping.getPlayers()
                        .map(ServerPing.Players::getMax)
                        .orElse(0);
            }).join();

            final var playerCount = players.size();

            sb.append(new StringTemplate(config.DISCORD_LIST_SERVER_FORMAT)
                    .add("server_name", name)
                    .add("online_players", playerCount)
                    .add("max_players", maxPlayers)
                    .toString()
            ).append('\n');

            if (maxPlayers == 0) {
                if (!config.DISCORD_LIST_SERVER_OFFLINE.isEmpty()) {
                    sb.append(config.DISCORD_LIST_SERVER_OFFLINE).append('\n');
                }
            } else if (playerCount == 0) {
                if (!config.DISCORD_LIST_NO_PLAYERS.isEmpty()) {
                    sb.append(config.DISCORD_LIST_NO_PLAYERS).append('\n');
                }
            } else {
                for (var player : players) {
                    sb.append(new StringTemplate(config.DISCORD_LIST_PLAYER_FORMAT)
                            .add("username", player.getUsername())
                            .toString()
                    ).append('\n');
                }
            }

            sb.append('\n');
        }
        sb.append("```");

        interaction.reply(sb.toString())
                .setEphemeral(config.DISCORD_LIST_EPHEMERAL)
                .queue();
    }
}
