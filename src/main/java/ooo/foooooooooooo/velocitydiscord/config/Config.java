package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;

import static ooo.foooooooooooo.velocitydiscord.VelocityDiscord.PluginVersion;

public class Config extends BaseConfig implements ServerConfig {
  private static final String[] splitVersion = PluginVersion.split("\\.");
  private static final String configVersion = splitVersion[0] + '.' + splitVersion[1];
  private static final String configMajorVersion = splitVersion[0];

  private Path dataDir;

  private static boolean configCreatedThisRun = false;

  public final BotConfig bot;
  public final DiscordMessageConfig discord;
  private final MinecraftMessageConfig minecraft;

  @Key("exclude_servers")
  public List<String> EXCLUDED_SERVERS = new ArrayList<>();
  @Key("excluded_servers_receive_messages")
  public boolean EXCLUDED_SERVERS_RECEIVE_MESSAGES = false;
  @Key("ping_interval")
  public int PING_INTERVAL_SECONDS = 15;

  public Map<String, String> serverDisplayNames = new HashMap<>();
  private HashMap<String, OverrideConfig> serverOverridesMap = new HashMap<>();

  public Config(Path dataDir) {
    super(loadFile(dataDir));

    this.dataDir = dataDir;

    checkConfig();

    this.bot = new BotConfig(this.inner);
    this.discord = new DiscordMessageConfig(this.inner);
    this.minecraft = new MinecraftMessageConfig(this.inner);

    loadConfig();

    var error = checkInvalidValues();

    if (error != null) {
      VelocityDiscord.LOGGER.error(error);
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
      Config.configCreatedThisRun = true;

      try (var in = Config.class.getResourceAsStream("/config.toml")) {
        Files.copy(Objects.requireNonNull(in), configFile);
      } catch (IOException e) {
        throw new RuntimeException("ERROR: Can't write default configuration file (permissions/filesystem error?)");
      }
    }

    var fileConfig = FileConfig.of(configFile);
    fileConfig.load();

    return fileConfig;
  }

  private void checkConfig() {
    // make sure the config makes sense for the current plugin's version
    var version = get(this, "config_version", configVersion);

    if (!versionCompatible(version)) {
      var error = String.format(
        "ERROR: Can't use the existing configuration file: version mismatch (mod: %s, config: %s)",
        configVersion,
        version
      );
      throw new RuntimeException(error);
    }
  }

  private static boolean versionCompatible(String newVersion) {
    return newVersion.split("\\.")[0].equals(configMajorVersion);
  }

  // Assume it's the first run if the config hasn't been edited or has been created this run
  public boolean isFirstRun() {
    return this.bot.isDefaultValues() || configCreatedThisRun;
  }

  @Override
  protected void loadConfig() {
    super.loadConfig();

    this.bot.loadConfig();
    this.discord.loadConfig();
    this.minecraft.loadConfig();

    CommentedConfig server_names = this.inner.get("server_names");

    if (server_names == null) return;

    for (var entry : server_names.entrySet()) {
      if (entry.getValue() instanceof String) {
        this.serverDisplayNames.put(entry.getKey(), entry.getValue());
      } else {
        var warning = String.format("Invalid server name for `%s`: `%s`", entry.getKey(), entry.getValue());
        VelocityDiscord.LOGGER.warn(warning);
      }
    }

    VelocityDiscord.LOGGER.info("serverDisplayNames: {}", this.serverDisplayNames.toString());

    // server overrides

    CommentedConfig serverOverrides = this.inner.get("override");

    if (serverOverrides == null) {
      VelocityDiscord.LOGGER.info("No server overrides found");
      return;
    }

    // dump to logger

    VelocityDiscord.LOGGER.info("Server overrides found ({}):", serverOverrides.size());

    this.serverOverridesMap = new HashMap<>();

    for (var entry : serverOverrides.entrySet()) {
      if (entry.getValue() instanceof com.electronwill.nightconfig.core.Config serverOverride) {
        var serverName = entry.getKey();

        // todo: maybe better than this
        if (this.EXCLUDED_SERVERS.contains(serverName) && !this.EXCLUDED_SERVERS_RECEIVE_MESSAGES) {
          VelocityDiscord.LOGGER.info("Ignoring override for excluded server: {}", serverName);
          continue;
        }

        VelocityDiscord.LOGGER.info("serverOverride: {} -> {}", serverName, serverOverride);

        this.serverOverridesMap.put(serverName, new OverrideConfig(serverOverride, this));
      } else {
        VelocityDiscord.LOGGER.warn("Invalid server override for `{}`: `{}`", entry.getKey(), entry.getValue());
      }
    }

    VelocityDiscord.LOGGER.info("serverOverridesMap: {}", this.serverOverridesMap);
  }

  public boolean serverDisabled(String name) {
    return this.EXCLUDED_SERVERS.contains(name);
  }

  public String serverName(String name) {
    return this.serverDisplayNames.getOrDefault(name, name);
  }

  public @Nullable String reloadConfig(Path dataDirectory) {
    this.dataDir = dataDirectory;
    //    VelocityDiscord.LOGGER.info("prev inner:");
    //    VelocityDiscord.LOGGER.info("---");
    //    logInner();
    //    VelocityDiscord.LOGGER.info("---");
    setInner(Config.loadFile(this.dataDir));
    //    VelocityDiscord.LOGGER.info("new inner:");
    //    VelocityDiscord.LOGGER.info("---");
    //    logInner();
    //    VelocityDiscord.LOGGER.info("---");

    try {
      loadConfig();

      for (var overrideConfigEntry : this.serverOverridesMap.entrySet()) {
        var overrideConfig = overrideConfigEntry.getValue();
        overrideConfig.setInner(this.inner);
        overrideConfig.loadConfig();
      }

      return checkInvalidValues();
    } catch (Exception e) {
      return MessageFormat.format("ERROR: {0}", e.getMessage());
    }
  }

  private String checkInvalidValues() {
    // check for invalid values
    if (this.bot.WEBHOOK_URL.isEmpty() && this.discord.isWebhookEnabled()) {
      return ("WARN: `discord.webhook.webhook_url` is required when using webhooks, messages will not be sent");
    }

    return null;
  }

  public ServerConfig getServerConfig(String serverName) {
    var config = this.serverOverridesMap.get(serverName);

    if (config != null) {
      return config;
    }

    return this;
  }

  @Override
  public BotConfig getBotConfig() {
    return this.bot;
  }

  @Override
  public DiscordMessageConfig getDiscordMessageConfig() {
    return this.discord;
  }

  @Override
  public MinecraftMessageConfig getMinecraftMessageConfig() {
    return this.minecraft;
  }

  public boolean isAnyWebhookEnabled() {
    return this.discord.isWebhookEnabled() || this.serverOverridesMap
      .values()
      .stream()
      .anyMatch(OverrideConfig::isWebhookEnabled);
  }

  public static class OverrideConfig implements ServerConfig {
    public final BotConfig bot;
    public final DiscordMessageConfig discord;
    public final MinecraftMessageConfig minecraft;

    public OverrideConfig(com.electronwill.nightconfig.core.Config config, Config main) {
      this.bot = new BotConfig(config, main.bot);
      this.discord = new DiscordMessageConfig(config, main.discord);
      this.minecraft = new MinecraftMessageConfig(config, main.minecraft);
    }

    public void loadConfig() {
      this.bot.loadConfig();
      this.discord.loadConfig();
      this.minecraft.loadConfig();
    }

    public void setInner(com.electronwill.nightconfig.core.Config config) {
      this.bot.setInner(config);
      this.discord.setInner(config);
      this.minecraft.setInner(config);
    }

    @Override
    public BotConfig getBotConfig() {
      return this.bot;
    }

    @Override
    public DiscordMessageConfig getDiscordMessageConfig() {
      return this.discord;
    }

    @Override
    public MinecraftMessageConfig getMinecraftMessageConfig() {
      return this.minecraft;
    }

    public boolean isWebhookEnabled() {
      return this.discord.isWebhookEnabled();
    }
  }
}
