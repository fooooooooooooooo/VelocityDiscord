package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.Config;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DiscordMessageConfig extends BaseConfig {
  public DiscordMessageConfig(Config config) {
    loadConfig(config);
  }

  // chat
  public Optional<String> MESSAGE_FORMAT = Optional.of("{username}: {message}");

  public Optional<String> DEATH_MESSAGE_FORMAT = Optional.of("**{username} {death_message}**");
  public Optional<String>
    ADVANCEMENT_MESSAGE_FORMAT
    = Optional.of("**{username} has made the advancement __{advancement_title}__**\n_{advancement_description}_");

  // join/leave
  public Optional<String> JOIN_MESSAGE_FORMAT = Optional.of("**{username} joined the game**");
  public Optional<String> LEAVE_MESSAGE_FORMAT = Optional.of("**{username} left the game**");
  public Optional<String> DISCONNECT_MESSAGE_FORMAT = Optional.of("**{username} disconnected**");
  public Optional<String> SERVER_SWITCH_MESSAGE_FORMAT = Optional.of("**{username} moved to {current} from {previous}**");

  // channel topic
  public Optional<String> TOPIC_FORMAT = Optional.of("Player count: {playerCount} | Players: {playerList} | Pings: {playerPingList} | "
    + "Server count: {serverCount} | Servers: {serverList} | Hostname: {hostname} | Port: {port} | Query MOTD: {queryMotd} | "
    + "Query map: {queryMap} | Query port: {queryPort} | Max players: {queryMaxPlayers} | "
    + "Plugin count: {pluginCount} | Plugins: {pluginList} | Version: {version}");

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Override
  protected void loadConfig(Config config) {
    MESSAGE_FORMAT = getOptional(config, "discord.chat.message", MESSAGE_FORMAT.get());

    DEATH_MESSAGE_FORMAT = getOptional(config, "discord.chat.death_message", DEATH_MESSAGE_FORMAT.get());
    ADVANCEMENT_MESSAGE_FORMAT = getOptional(config, "discord.chat.advancement_message", ADVANCEMENT_MESSAGE_FORMAT.get());

    JOIN_MESSAGE_FORMAT = getOptional(config, "discord.chat.join_message", JOIN_MESSAGE_FORMAT.get());
    LEAVE_MESSAGE_FORMAT = getOptional(config, "discord.chat.leave_message", LEAVE_MESSAGE_FORMAT.get());
    DISCONNECT_MESSAGE_FORMAT = getOptional(config, "discord.chat.disconnect_message", DISCONNECT_MESSAGE_FORMAT.get());
    SERVER_SWITCH_MESSAGE_FORMAT = getOptional(config, "discord.chat.server_switch_message", SERVER_SWITCH_MESSAGE_FORMAT.get());

    TOPIC_FORMAT = getOptional(config, "discord.topic_format", TOPIC_FORMAT.get());
  }

}
