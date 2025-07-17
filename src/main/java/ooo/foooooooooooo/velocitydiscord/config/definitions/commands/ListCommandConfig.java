package ooo.foooooooooooo.velocitydiscord.config.definitions.commands;

import ooo.foooooooooooo.velocitydiscord.config.Config;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ListCommandConfig {
  /// Placeholders available: `server_name`, `online_players`, `max_players`
  public String serverFormat = "[{server_name} {online_players}/{max_players}]";

  /// Placeholders available: `username`
  public String playerFormat = "- {username}";

  public Optional<String> noPlayersFormat = Optional.of("No players online");

  public Optional<String> serverOfflineFormat = Optional.of("Server offline");

  public void load(Config config) {
    if (config == null) return;

    this.serverFormat = config.getOrDefault("server_format", this.serverFormat);
    this.playerFormat = config.getOrDefault("player_format", this.playerFormat);
    this.noPlayersFormat = config.getDisableableStringOrDefault("no_players_format", this.noPlayersFormat);
    this.serverOfflineFormat = config.getDisableableStringOrDefault("server_offline_format", this.serverOfflineFormat);
  }
}
