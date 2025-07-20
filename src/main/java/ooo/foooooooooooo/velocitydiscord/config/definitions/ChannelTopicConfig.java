package ooo.foooooooooooo.velocitydiscord.config.definitions;

import ooo.foooooooooooo.velocitydiscord.config.Config;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ChannelTopicConfig {

  /// Template for the channel topic
  ///
  /// Placeholders available:
  /// - `players`: Total number of players online
  /// - `player_list`: List of players (format is defined below)
  /// - `servers`: Number of servers
  /// - `server_list`: List of server names
  /// - `hostname`: Server hostname
  /// - `port`: Server port
  /// - `motd`: Message of the Day (MOTD)
  /// - `query_port`: Query port
  /// - `max_players`: Maximum number of players
  /// - `plugins`: Number of plugins
  /// - `plugin_list`: List of plugin names
  /// - `version`: Server version
  /// - `software`: Software name
  /// - `average_ping`: Average ping of all players
  /// - `uptime`: Server uptime in hours and minutes
  /// - `server[SERVERNAME]`: Dynamic placeholder for each server's name and status (e.g., `server[MyServer]`, `
  /// server[AnotherServer]`, `server[Lobby]`, etc.)
  public Optional<String> format =
    Optional.of("{players}/{max_players} {player_list} {hostname}:{port} Uptime: {uptime}");

  /// Template for `server[SERVERNAME]` placeholder in the channel topic
  ///
  /// Placeholders available: `name`, `players`, `max_players`, `motd`, `version`, `protocol`
  public Optional<String> serverFormat = Optional.of("{name}: {players}/{max_players}");

  /// Template for `server[SERVERNAME]` placeholder in the channel topic when the server is offline
  ///
  /// Placeholders available: `name`
  public Optional<String> serverOfflineFormat = Optional.of("{name}: Offline");

  public Optional<String> playerListNoPlayersHeader = Optional.of("No players online");

  public Optional<String> playerListHeader = Optional.of("Players: ");

  /// Placeholders available: `username`, `ping`
  public String playerListPlayerFormat = "{username}";

  /// Separator between players in the list, \n can be used for new line
  public String playerListSeparator = ", ";

  /// Maximum number of players to show in the topic
  ///
  /// Set to 0 to show all players
  public int playerListMaxCount = 10;

  public void load(Config config) {
    if (config == null) return;

    this.format = config.getDisableableStringOrDefault("format", this.format);
    this.serverFormat = config.getDisableableStringOrDefault("server", this.serverFormat);
    this.serverOfflineFormat = config.getDisableableStringOrDefault("server_offline", this.serverOfflineFormat);
    this.playerListNoPlayersHeader =
      config.getDisableableStringOrDefault("player_list_no_players_header", this.playerListNoPlayersHeader);
    this.playerListHeader = config.getDisableableStringOrDefault("player_list_header", this.playerListHeader);
    this.playerListPlayerFormat = config.getOrDefault("player_list_player", this.playerListPlayerFormat);
    this.playerListSeparator = config.getOrDefault("player_list_separator", this.playerListSeparator);
    this.playerListMaxCount = config.getOrDefault("player_list_max_count", this.playerListMaxCount);
  }
}
