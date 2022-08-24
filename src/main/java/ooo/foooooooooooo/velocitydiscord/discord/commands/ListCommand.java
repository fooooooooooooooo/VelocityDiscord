package ooo.foooooooooooo.velocitydiscord.discord.commands;

import com.velocitypowered.api.proxy.ProxyServer;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import ooo.foooooooooooo.velocitydiscord.Config;
import ooo.foooooooooooo.velocitydiscord.util.StringTemplate;

import java.util.concurrent.ExecutionException;

public class ListCommand implements ICommand {

    private final ProxyServer server;
    private final Config config;

    public ListCommand(ProxyServer server, Config config) {
        this.server = server;
        this.config = config;
    }

    @Override
    public void handle(SlashCommandInteraction interaction) {
        var servers = server.getAllServers().stream().toList();

        var sb = new StringBuilder();
        sb.append("```").append(config.DISCORD_LIST_CODEBLOCK_LANG).append("\n");

        for (var server : servers) {
            var name = server.getServerInfo().getName();
            var players = server.getPlayersConnected();
            var maxPlayers = 0;

            try {
                var b = server.ping().get().asBuilder();
                maxPlayers = b.getMaximumPlayers();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            var playerCount = players.size();

            sb.append(new StringTemplate(config.DISCORD_LIST_SERVER_FORMAT)
                .add("server_name", name)
                .add("online_players", playerCount)
                .add("max_players", maxPlayers)
                .toString()
            ).append("\n");

            if (playerCount == 0) {
                if (!config.DISCORD_LIST_NO_PLAYERS.isEmpty()) {
                    sb.append(config.DISCORD_LIST_NO_PLAYERS).append("\n");
                }
            } else {
                for (var player : players) {
                    sb.append(new StringTemplate(config.DISCORD_LIST_PLAYER_FORMAT)
                        .add("username", player.getUsername())
                        .toString()
                    ).append("\n");
                }
            }

            sb.append("\n");
        }
        sb.append("```");

        interaction.reply(sb.toString()).queue();
    }
}
