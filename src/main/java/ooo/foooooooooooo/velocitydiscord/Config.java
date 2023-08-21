package ooo.foooooooooooo.velocitydiscord;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static ooo.foooooooooooo.velocitydiscord.VelocityDiscord.PluginVersion;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Config {
  private static final String DefaultToken = "TOKEN";
  private static final String DefaultChannelId = "000000000000000000";
  private static final String DefaultWebhookUrl = "";
  private static final String DefaultAvatarUrl = "https://crafatar.com/avatars/{uuid}?overlay";
  private static final String[] splitVersion = PluginVersion.split("\\.");
  private static final String configVersion = splitVersion[0] + '.' + splitVersion[1];
  private static final String configMajorVersion = splitVersion[0];

  private final Path dataDir;

  public String DISCORD_TOKEN = DefaultToken;
  public String CHANNEL_ID = DefaultChannelId;

  // toggles
  public Boolean SHOW_BOT_MESSAGES = false;
  public Boolean SHOW_ATTACHMENTS = true;
  public Boolean SHOW_ACTIVITY = true;
  public Boolean ENABLE_MENTIONS = true;
  public Boolean ENABLE_EVERYONE_AND_HERE = false;

  // webhooks
  public Boolean DISCORD_USE_WEBHOOK = false;
  public String WEBHOOK_URL = DefaultWebhookUrl;
  public String WEBHOOK_AVATAR_URL = DefaultAvatarUrl;
  public String WEBHOOK_USERNAME = "{username}";

  // discord formats
  public Optional<String> DISCORD_CHAT_MESSAGE = Optional.of("{username}: {message}");
  public Optional<String> JOIN_MESSAGE = Optional.of("**{username} joined the game**");
  public Optional<String> LEAVE_MESSAGE = Optional.of("**{username} left the game**");
  public Optional<String> DISCONNECT_MESSAGE = Optional.of("**{username} disconnected**");
  public Optional<String> SERVER_SWITCH_MESSAGE = Optional.of("**{username} moved to {current} from {previous}**");
  public Optional<String> DEATH_MESSAGE = Optional.of("**{username} {death_message}**");
  public Optional<String> ADVANCEMENT_MESSAGE = Optional.of("**{username} has made the advancement __{advancement_title}__**\n_{advancement_description}_");
  public String DISCORD_ACTIVITY_TEXT = "with {amount} players online";

  // discord commands
  public Boolean DISCORD_LIST_ENABLED = true;
  public Boolean DISCORD_LIST_EPHEMERAL = true;
  public String DISCORD_LIST_SERVER_FORMAT = "[{server_name} {online_players}/{max_players}]";
  public String DISCORD_LIST_PLAYER_FORMAT = "- {username}";
  public Optional<String> DISCORD_LIST_NO_PLAYERS = Optional.of("No players online");
  public Optional<String> DISCORD_LIST_SERVER_OFFLINE = Optional.of("Server offline");
  public String DISCORD_LIST_CODEBLOCK_LANG = "asciidoc";

  // minecraft formats
  public String DISCORD_CHUNK = "<dark_gray>[<{discord_color}>Discord<dark_gray>]";
  public String USERNAME_CHUNK = "<{role_color}><hover:show_text:{username}#{discriminator}>{nickname}</hover>";
  public String MC_CHAT_MESSAGE = "{discord_chunk} {username_chunk}<dark_gray>: <white>{message} {attachments}";
  public String ATTACHMENTS = "<click:open_url:{url}>[Attachment]</click>";

  // colors
  public String DISCORD_COLOR = "#7289da";
  public String ATTACHMENT_COLOR = "#4abdff";

  private FileConfig config;

  private boolean isFirstRun = false;

  @Inject
  public Config(@DataDirectory Path dataDir) {
    this.dataDir = dataDir;

    loadFile();
    loadConfigs();
  }

  private void loadFile() {
    if (Files.notExists(dataDir)) {
      try {
        Files.createDirectory(dataDir);
      } catch (IOException e) {
        throw new RuntimeException("Could not create data directory at " + dataDir.toAbsolutePath());
      }
    }

    var configFile = dataDir.resolve("config.toml");

    // create default config if it doesn't exist
    if (Files.notExists(configFile)) {
      isFirstRun = true;

      try (var in = getClass().getResourceAsStream("/config.toml")) {
        Files.copy(Objects.requireNonNull(in), configFile);
      } catch (IOException e) {
        throw new RuntimeException("ERROR: Can't write default configuration file (permissions/filesystem error?)");
      }
    }

    config = FileConfig.of(configFile);
    config.load();

    // make sure the config makes sense for the current plugin's version
    var version = get("config_version", configVersion);

    if (!versionCompatible(version)) {
      var error = String.format("ERROR: Can't use the existing configuration file: version mismatch (mod: %s, config: %s)", configVersion, version);
      throw new RuntimeException(error);
    }
  }

  private boolean versionCompatible(String newVersion) {
    return newVersion.split("\\.")[0].equals(configMajorVersion);
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public void loadConfigs() {
    DISCORD_TOKEN = get("discord.token", DISCORD_TOKEN);
    CHANNEL_ID = get("discord.channel", CHANNEL_ID);

    SHOW_BOT_MESSAGES = get("discord.show_bot_messages", SHOW_BOT_MESSAGES);
    SHOW_ATTACHMENTS = get("discord.show_attachments_ingame", SHOW_ATTACHMENTS);
    SHOW_ACTIVITY = get("discord.show_activity", SHOW_ACTIVITY);
    ENABLE_MENTIONS = get("discord.enable_mentions", ENABLE_MENTIONS);
    ENABLE_EVERYONE_AND_HERE = get("discord.enable_everyone_and_here", ENABLE_EVERYONE_AND_HERE);

    DISCORD_USE_WEBHOOK = get("discord.use_webhook", DISCORD_USE_WEBHOOK);
    WEBHOOK_URL = get("discord.webhook.webhook_url", WEBHOOK_URL);
    WEBHOOK_AVATAR_URL = get("discord.webhook.avatar_url", WEBHOOK_AVATAR_URL);
    WEBHOOK_USERNAME = get("discord.webhook.webhook_username", WEBHOOK_USERNAME);

    DISCORD_CHAT_MESSAGE = getOptional("discord.chat.message", DISCORD_CHAT_MESSAGE.get());
    JOIN_MESSAGE = getOptional("discord.chat.join_message", JOIN_MESSAGE.get());
    LEAVE_MESSAGE = getOptional("discord.chat.leave_message", LEAVE_MESSAGE.get());
    DISCONNECT_MESSAGE = getOptional("discord.chat.disconnect_message", DISCONNECT_MESSAGE.get());
    SERVER_SWITCH_MESSAGE = getOptional("discord.chat.server_switch_message", SERVER_SWITCH_MESSAGE.get());
    DEATH_MESSAGE = getOptional("discord.chat.death_message", DEATH_MESSAGE.get());
    ADVANCEMENT_MESSAGE = getOptional("discord.chat.advancement_message", ADVANCEMENT_MESSAGE.get());
    DISCORD_ACTIVITY_TEXT = get("discord.activity_text", DISCORD_ACTIVITY_TEXT);

    // todo: split these into a sub config if any more commands are added
    DISCORD_LIST_ENABLED = get("discord.commands.list.enabled", DISCORD_LIST_ENABLED);
    DISCORD_LIST_EPHEMERAL = get("discord.commands.list.ephemeral", DISCORD_LIST_EPHEMERAL);
    DISCORD_LIST_SERVER_FORMAT = get("discord.commands.list.server_format", DISCORD_LIST_SERVER_FORMAT);
    DISCORD_LIST_PLAYER_FORMAT = get("discord.commands.list.player_format", DISCORD_LIST_PLAYER_FORMAT);
    DISCORD_LIST_NO_PLAYERS = getOptional("discord.commands.list.no_players", DISCORD_LIST_NO_PLAYERS.get());
    DISCORD_LIST_SERVER_OFFLINE = getOptional("discord.commands.list.server_offline", DISCORD_LIST_SERVER_OFFLINE.get());
    DISCORD_LIST_CODEBLOCK_LANG = get("discord.commands.list.codeblock_lang", DISCORD_LIST_CODEBLOCK_LANG);

    DISCORD_CHUNK = get("minecraft.discord_chunk", DISCORD_CHUNK);
    USERNAME_CHUNK = get("minecraft.username_chunk", USERNAME_CHUNK);
    MC_CHAT_MESSAGE = get("minecraft.message", MC_CHAT_MESSAGE);
    ATTACHMENTS = get("minecraft.attachments", ATTACHMENTS);
    DISCORD_COLOR = get("minecraft.discord_color", DISCORD_COLOR);
    ATTACHMENT_COLOR = get("minecraft.attachment_color", ATTACHMENT_COLOR);
  }

  <T> T get(String key, T defaultValue) {
    return config.getOrElse(key, defaultValue);
  }

  static String invalidValueFormatString = "ERROR: `%s` is not a valid value for `%s`, acceptable values: `false`, any string";

  Optional<String> getOptional(String key, String defaultValue) {
    return getOptional(config, key, defaultValue);
  }

  public static Optional<String> getOptional(com.electronwill.nightconfig.core.Config config, String key, String defaultValue) {
    var value = config.getRaw(key);

    if (value == null) {
      return Optional.of(defaultValue);
    }

    if (value instanceof Boolean bool) {
      if (!bool) {
        return Optional.empty();

      } else {
        throw new RuntimeException(String.format(invalidValueFormatString, "true", key));
      }
    }

    if (value instanceof String str) {
      if (str.isEmpty()) {
        return Optional.empty();
      }

      return Optional.of(str);
    }

    throw new RuntimeException(String.format(invalidValueFormatString, value, key));
  }

  // Assume it's the first run if the config hasn't been edited or has been created this run
  public boolean isFirstRun() {
    return DISCORD_TOKEN.equals(DefaultToken) || CHANNEL_ID.equals(DefaultChannelId) || isFirstRun;
  }
}
