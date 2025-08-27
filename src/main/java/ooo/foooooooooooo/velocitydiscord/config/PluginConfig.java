package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import ooo.foooooooooooo.velocitydiscord.Constants;
import ooo.foooooooooooo.velocitydiscord.config.definitions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;

public class PluginConfig implements ServerConfig {
  private static final Logger logger = LoggerFactory.getLogger(PluginConfig.class);

  private static final String[] splitVersion = Constants.PluginVersion.split("\\.");
  public static final String ConfigVersion = splitVersion[0] + '.' + splitVersion[1];
  private static final String configMajorVersion = splitVersion[0];

  private static boolean configCreatedThisRun = false;

  private HashMap<String, ServerOverrideConfig> serverOverridesMap = new HashMap<>();

  private Config config;

  public GlobalConfig global = new GlobalConfig();
  public LocalConfig local = new LocalConfig();

  public PluginConfig(Path dataDir) {
    this.config = PluginConfig.loadFile(dataDir);
    this.loadConfig();
    this.onLoad();
  }

  public PluginConfig(Config config) {
    this.config = config;
    this.loadConfig();
    this.onLoad();
  }

  private void loadConfig() {
    if (this.config == null || this.config.isEmpty()) {
      throw new RuntimeException("ERROR: Config is empty");
    }

    this.global.load(this.config);
    this.local.load(this.config);
  }

  private void onLoad() {
    var error = checkErrors(this.config);
    if (error != null) logger.error(error);
    error = this.local.checkErrors();
    if (error != null) logger.error(error);

    this.serverOverridesMap = loadOverrides(this.global, this.config);
  }

  private static Config loadFile(Path dataDir) {
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

    return new Config(fileConfig);
  }

  private static boolean versionCompatible(String newVersion) {
    return newVersion != null && newVersion.split("\\.")[0].equals(configMajorVersion);
  }

  private static @Nullable String checkErrors(Config config) {
    if (config == null || config.isEmpty()) {
      return ("ERROR: Config is empty");
    }

    // check for compatible config version
    String version = config.get("config_version");

    if (!versionCompatible(version)) {
      return String.format(
        "ERROR: Can't use the existing configuration file: version mismatch (mod: %s, config: %s)",
        ConfigVersion,
        version
      );
    }

    return null;
  }

  // Assume it's the first run if the config hasn't been edited or has been created this run
  public boolean isConfigNotSetup() {
    return configCreatedThisRun || this.global.discord.isTokenUnset() || this.local.discord.isDefaultChannel();
  }

  public static HashMap<String, ServerOverrideConfig> loadOverrides(GlobalConfig baseGlobalConfig, Config baseConfig) {
    // server overrides

    CommentedConfig overrideConfig = baseConfig.get("override");

    if (overrideConfig == null) {
      logger.debug("No server overrides found");
      return new HashMap<>();
    }

    var overrides = new HashMap<String, ServerOverrideConfig>();

    for (var entry : overrideConfig.entrySet()) {
      if (entry.getValue() instanceof com.electronwill.nightconfig.core.Config serverOverride) {
        var serverName = entry.getKey();

        // todo: maybe better than this
        if (baseGlobalConfig.excludedServers.contains(serverName) && !baseGlobalConfig.excludedServersReceiveMessages) {
          logger.info("Ignoring override for excluded server: {}", serverName);
          continue;
        }

        var config = new Config(serverOverride);
        overrides.put(serverName, new ServerOverrideConfig(baseConfig, config));
      } else {
        logger.warn("Invalid server override for `{}`: `{}`", entry.getKey(), entry.getValue());
      }
    }

    return overrides;
  }

  public boolean serverDisabled(String name) {
    return this.global.excludedServers.contains(name);
  }

  public String serverName(String name) {
    return this.global.serverDisplayNames.getOrDefault(name, name);
  }

  public @Nullable String reloadConfig(Path dataDirectory) {
    try {
      var newConfig = PluginConfig.loadFile(dataDirectory);

      var errors = checkErrors(newConfig);
      if (errors != null && !errors.isEmpty()) return errors;

      var newGlobal = new GlobalConfig();
      var newLocal = new LocalConfig();

      newGlobal.load(newConfig);
      newLocal.load(newConfig);

      var overrides = loadOverrides(newGlobal, newConfig);

      errors = newLocal.checkErrors();
      if (errors != null && !errors.isEmpty()) return errors;

      this.serverOverridesMap = overrides;

      this.config = newConfig;

      this.global = newGlobal;
      this.local = newLocal;

      return null;
    } catch (Exception e) {
      return "ERROR: " + e.getMessage();
    }
  }

  public ServerConfig getServerConfig(String serverName) {
    var override = this.serverOverridesMap.get(serverName);
    if (override != null) return override;
    return this;
  }

  @Override
  public DiscordConfig getDiscordConfig() {
    return this.local.discord;
  }

  @Override
  public ChatConfig getChatConfig() {
    return this.local.discord.chat;
  }

  @Override
  public MinecraftConfig getMinecraftConfig() {
    return this.local.minecraft;
  }

  public static class ServerOverrideConfig implements ServerConfig {
    public final LocalConfig local = new LocalConfig();

    public ServerOverrideConfig(Config baseConfig, Config overrideConfig) {
      this.local.load(baseConfig);
      // load the override config on top of the base config
      this.local.load(overrideConfig);
    }

    @Override
    public DiscordConfig getDiscordConfig() {
      return this.local.discord;
    }

    @Override
    public ChatConfig getChatConfig() {
      return this.local.discord.chat;
    }

    @Override
    public MinecraftConfig getMinecraftConfig() {
      return this.local.minecraft;
    }
  }
}
