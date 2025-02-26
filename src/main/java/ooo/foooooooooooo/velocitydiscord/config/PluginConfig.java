package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import ooo.foooooooooooo.config.Config;
import ooo.foooooooooooo.config.Key;
import ooo.foooooooooooo.velocitydiscord.Constants;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PluginConfig extends Config implements ServerConfig {
  private static final String[] splitVersion = Constants.PluginVersion.split("\\.");
  public static final String ConfigVersion = splitVersion[0] + '.' + splitVersion[1];
  private static final String configMajorVersion = splitVersion[0];

  private static boolean configCreatedThisRun = false;

  @Key("exclude_servers")
  public List<String> EXCLUDED_SERVERS = new ArrayList<>();
  @Key("excluded_servers_receive_messages")
  public boolean EXCLUDED_SERVERS_RECEIVE_MESSAGES = false;
  @Key("ping_interval")
  public int PING_INTERVAL_SECONDS = 15;

  @Key("discord")
  public DiscordConfig DISCORD;
  @Key("discord.chat")
  public DiscordChatConfig DISCORD_CHAT;
  @Key("minecraft")
  public MinecraftConfig MINECRAFT;

  private final Map<String, String> serverDisplayNames = new HashMap<>();

  private Path dataDir;
  private HashMap<String, OverrideConfig> serverOverridesMap = new HashMap<>();

  private final Logger logger;

  public PluginConfig(Path dataDir, Logger logger) {
    super(loadFile(dataDir));
    this.logger = logger;
    this.dataDir = dataDir;
    this.onLoad();
  }

  public PluginConfig(com.electronwill.nightconfig.core.Config config, Logger logger) {
    super(config);
    this.logger = logger;
    this.onLoad();
  }

  private void onLoad() {
    if (this.inner == null || this.inner.isEmpty()) {
      throw new RuntimeException("ERROR: Config is empty");
    }

    checkConfig();

    loadConfig();

    var error = checkInvalidValues();

    if (error != null) {
      this.logger.error(error);
    }
  }

  private static com.electronwill.nightconfig.core.Config loadFile(Path dataDir) {
    if (Files.notExists(dataDir)) {
      try {
        Files.createDirectory(dataDir);
      } catch (IOException e) {
        throw new RuntimeException("ERROR: Could not create data directory at " + dataDir.toAbsolutePath());
      }
    }

    var configFile = dataDir.resolve("config.toml");

    // create default config if it doesn't exist
    if (Files.notExists(configFile)) {
      PluginConfig.configCreatedThisRun = true;

      try (var in = PluginConfig.class.getResourceAsStream("/config.toml")) {
        Files.copy(Objects.requireNonNull(in), configFile);
      } catch (IOException e) {
        throw new RuntimeException("ERROR: Can't write default configuration file (permissions/filesystem error?)");
      }
    }

    var fileConfig = FileConfig.of(configFile);
    fileConfig.load();

    return fileConfig;
  }

  private static boolean versionCompatible(String newVersion) {
    return newVersion != null && newVersion.split("\\.")[0].equals(configMajorVersion);
  }

  private void checkConfig() {
    // make sure the config makes sense for the current plugin's version
    String version = get(this, "config_version");

    if (!versionCompatible(version)) {
      var error = String.format(
        "ERROR: Can't use the existing configuration file: version mismatch (mod: %s, config: %s)",
        ConfigVersion,
        version
      );
      throw new RuntimeException(error);
    }
  }

  // Assume it's the first run if the config hasn't been edited or has been created this run
  public boolean isFirstRun() {
    return this.DISCORD.isDefaultValues() || configCreatedThisRun;
  }

  @Override
  public void loadConfig() {
    super.loadConfig();

    if (this.inner == null) {
      throw new RuntimeException("ERROR: Config is empty");
    }

    CommentedConfig server_names = this.inner.get("server_names");

    if (server_names != null) {

      for (var entry : server_names.entrySet()) {
        if (entry.getValue() instanceof String) {
          this.serverDisplayNames.put(entry.getKey(), entry.getValue());
        } else {
          var warning = String.format("Invalid server name for `%s`: `%s`", entry.getKey(), entry.getValue());
          this.logger.warn(warning);
        }
      }
    }

    // server overrides

    CommentedConfig serverOverrides = this.inner.get("override");

    if (serverOverrides == null) {
      this.logger.debug("No server overrides found");
      return;
    }

    this.serverOverridesMap = new HashMap<>();

    for (var entry : serverOverrides.entrySet()) {
      if (entry.getValue() instanceof com.electronwill.nightconfig.core.Config serverOverride) {
        var serverName = entry.getKey();

        // todo: maybe better than this
        if (this.EXCLUDED_SERVERS.contains(serverName) && !this.EXCLUDED_SERVERS_RECEIVE_MESSAGES) {
          this.logger.debug("Ignoring override for excluded server: {}", serverName);
          continue;
        }

        this.serverOverridesMap.put(serverName, new OverrideConfig(serverOverride, this));
      } else {
        this.logger.warn("Invalid server override for `{}`: `{}`", entry.getKey(), entry.getValue());
      }
    }
  }

  public boolean serverDisabled(String name) {
    return this.EXCLUDED_SERVERS.contains(name);
  }

  public String serverName(String name) {
    return this.serverDisplayNames.getOrDefault(name, name);
  }

  public @Nullable String reloadConfig(Path dataDirectory) {
    this.dataDir = dataDirectory;

    // reset old values
    this.serverDisplayNames.clear();
    this.serverOverridesMap.clear();
    this.EXCLUDED_SERVERS.clear();

    this.setInner(PluginConfig.loadFile(this.dataDir));

    try {
      loadConfig();

      return checkInvalidValues();
    } catch (Exception e) {
      return "ERROR: " + e.getMessage();
    }
  }

  private boolean isWebhook(DiscordChatConfig.UserMessageType messageType) {
    return messageType == DiscordChatConfig.UserMessageType.WEBHOOK;
  }

  private String checkInvalidValues() {
    if (this.DISCORD_CHAT.isWebhookUsed() && this.DISCORD.WEBHOOK.invalid()) {
      var errors = new ArrayList<String>();

      // check each message category
      if (isWebhook(this.DISCORD_CHAT.ADVANCEMENT_TYPE) && this.DISCORD_CHAT.ADVANCEMENT_WEBHOOK.invalid()) {
        errors.add("`discord.chat.advancement.webhook` is set and `discord.chat.advancement.type` is set to `webhook`");
      }
      if (isWebhook(this.DISCORD_CHAT.MESSAGE_TYPE) && this.DISCORD_CHAT.MESSAGE_WEBHOOK.invalid()) {
        errors.add("`discord.chat.message.webhook` is set and `discord.chat.message.type` is set to `webhook`");
      }
      if (isWebhook(this.DISCORD_CHAT.JOIN_TYPE) && this.DISCORD_CHAT.JOIN_WEBHOOK.invalid()) {
        errors.add("`discord.chat.join.webhook` is set and `discord.chat.join.type` is set to `webhook`");
      }
      if (isWebhook(this.DISCORD_CHAT.DEATH_TYPE) && this.DISCORD_CHAT.DEATH_WEBHOOK.invalid()) {
        errors.add("`discord.chat.death.webhook` is set and `discord.chat.death.type` is set to `webhook`");
      }
      if (isWebhook(this.DISCORD_CHAT.ADVANCEMENT_TYPE) && this.DISCORD_CHAT.ADVANCEMENT_WEBHOOK.invalid()) {
        errors.add("`discord.chat.advancement.webhook` is set and `discord.chat.advancement.type` is set to `webhook`");
      }
      if (isWebhook(this.DISCORD_CHAT.LEAVE_TYPE) && this.DISCORD_CHAT.LEAVE_WEBHOOK.invalid()) {
        errors.add("`discord.chat.leave.webhook` is set and `discord.chat.leave.type` is set to `webhook`");
      }
      if (isWebhook(this.DISCORD_CHAT.DISCONNECT_TYPE) && this.DISCORD_CHAT.DISCONNECT_WEBHOOK.invalid()) {
        errors.add("`discord.chat.disconnect.webhook` is set and `discord.chat.disconnect.type` is set to `webhook`");
      }
      if (isWebhook(this.DISCORD_CHAT.SERVER_SWITCH_TYPE) && this.DISCORD_CHAT.SERVER_SWITCH_WEBHOOK.invalid()) {
        errors.add(
          "`discord.chat.server_switch.webhook` is set and `discord.chat.server_switch.type` is set to `webhook`");
      }

      if (!errors.isEmpty()) {
        var errorStart = "ERROR: neither `discord.webhook` nor ";
        return errors.stream().map(s -> errorStart + s).reduce((a, b) -> a + "\n" + b).orElse(null);
      }
    }

    return null;
  }

  public ServerConfig getServerConfig(String serverName) {
    var config = this.serverOverridesMap.get(serverName);
    if (config != null) return config;
    return this;
  }

  @Override
  public DiscordConfig getDiscordConfig() {
    return this.DISCORD;
  }

  @Override
  public DiscordChatConfig getDiscordChatConfig() {
    return this.DISCORD_CHAT;
  }

  @Override
  public MinecraftConfig getMinecraftConfig() {
    return this.MINECRAFT;
  }

  public static class OverrideConfig extends Config implements ServerConfig {
    @Key("discord")
    public DiscordConfig DISCORD;
    @Key("discord.chat")
    public DiscordChatConfig DISCORD_CHAT;
    @Key("minecraft")
    public MinecraftConfig MINECRAFT;

    public OverrideConfig(com.electronwill.nightconfig.core.Config config, PluginConfig fallback) {
      super(config, fallback);
      // explicitly load the config here
      loadConfig();
    }

    @Override
    public DiscordConfig getDiscordConfig() {
      return this.DISCORD;
    }

    @Override
    public DiscordChatConfig getDiscordChatConfig() {
      return this.DISCORD_CHAT;
    }

    @Override
    public MinecraftConfig getMinecraftConfig() {
      return this.MINECRAFT;
    }
  }
}
