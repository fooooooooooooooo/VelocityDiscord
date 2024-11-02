package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import ooo.foooooooooooo.velocitydiscord.config.commands.ListCommandConfig;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;

import static ooo.foooooooooooo.velocitydiscord.VelocityDiscord.PluginVersion;

public class Config extends BaseConfig {
  private static final String[] splitVersion = PluginVersion.split("\\.");
  private static final String configVersion = splitVersion[0] + '.' + splitVersion[1];
  private static final String configMajorVersion = splitVersion[0];

  private Path dataDir;

  private boolean configCreatedThisRun = false;

  public final BotConfig bot;
  public final DiscordMessageConfig discord;
  public final MinecraftMessageConfig minecraft;

  public final ListCommandConfig listCommand;

  public List<String> EXCLUDED_SERVERS = new ArrayList<>();
  public boolean EXCLUDED_SERVERS_RECEIVE_MESSAGES = false;
  public int PING_INTERVAL_SECONDS = 15;

  public Map<String, String> serverDisplayNames = new HashMap<>();

  private final Logger logger;

  public Config(Path dataDir, Logger logger) {
    this.dataDir = dataDir;
    this.logger = logger;

    var config = loadFile();

    loadConfig(config);

    bot = new BotConfig(config);
    discord = new DiscordMessageConfig(config);
    minecraft = new MinecraftMessageConfig(config);

    listCommand = new ListCommandConfig(config);

    var error = checkInvalidValues();

    if (error != null) {
      logger.severe(error);
    }
  }

  private com.electronwill.nightconfig.core.Config loadFile() {
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
      configCreatedThisRun = true;

      try (var in = getClass().getResourceAsStream("/config.toml")) {
        Files.copy(Objects.requireNonNull(in), configFile);
      } catch (IOException e) {
        throw new RuntimeException("ERROR: Can't write default configuration file (permissions/filesystem error?)");
      }
    }

    var fileConfig = FileConfig.of(configFile);
    fileConfig.load();

    // make sure the config makes sense for the current plugin's version
    var version = get(fileConfig, "config_version", configVersion);

    if (!versionCompatible(version)) {
      var error = String.format("ERROR: Can't use the existing configuration file: version mismatch (mod: %s, config: %s)", configVersion, version);
      throw new RuntimeException(error);
    }

    return fileConfig;
  }

  private boolean versionCompatible(String newVersion) {
    return newVersion.split("\\.")[0].equals(configMajorVersion);
  }

  // Assume it's the first run if the config hasn't been edited or has been created this run
  public boolean isFirstRun() {
    return bot.isDefaultValues() || configCreatedThisRun;
  }

  @Override
  protected void loadConfig(com.electronwill.nightconfig.core.Config config) {
    EXCLUDED_SERVERS = get(config, "exclude_servers", EXCLUDED_SERVERS);
    EXCLUDED_SERVERS_RECEIVE_MESSAGES = get(config, "excluded_servers_receive_messages", EXCLUDED_SERVERS_RECEIVE_MESSAGES);
    PING_INTERVAL_SECONDS = get(config, "ping_interval", PING_INTERVAL_SECONDS);

    CommentedConfig server_names = config.get("server_names");

    for (var entry : server_names.entrySet()) {
      if (entry.getValue() instanceof String) {
        serverDisplayNames.put(entry.getKey(), entry.getValue());
      } else {
        var warning = String.format("Invalid server name for `%s`: `%s`", entry.getKey(), entry.getValue());
        logger.warning(warning);
      }
    }

    logger.fine("serverDisplayNames: " + serverDisplayNames.toString());
  }

  public boolean serverDisabled(String name) {
    return EXCLUDED_SERVERS.contains(name);
  }

  public String serverName(String name) {
    return serverDisplayNames.getOrDefault(name, name);
  }

  public @Nullable String reloadConfig(Path dataDirectory) {
    this.dataDir = dataDirectory;
    var config = loadFile();

    try {
      loadConfig(config);

      bot.loadConfig(config);
      discord.loadConfig(config);
      minecraft.loadConfig(config);

      listCommand.loadConfig(config);

      return checkInvalidValues();
    } catch (Exception e) {
      return MessageFormat.format("ERROR: {0}", e.getMessage());
    }
  }

  private String checkInvalidValues() {
    // check for invalid values
    if (bot.WEBHOOK_URL.isEmpty() && discord.isWebhookEnabled()) {
      return ("WARN: `discord.webhook.webhook_url` is required when using webhooks, messages will not be sent");
    }

    return null;
  }
}
