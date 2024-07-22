package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import ooo.foooooooooooo.velocitydiscord.config.commands.ListCommandConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import ooo.foooooooooooo.velocitydiscord.config.Config;
import ooo.foooooooooooo.velocitydiscord.discord.Discord;
import ooo.foooooooooooo.velocitydiscord.yep.YepListener;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.logging.Logger;

import static ooo.foooooooooooo.velocitydiscord.VelocityDiscord.PluginVersion;

public class Config extends BaseConfig {
  private static final String[] splitVersion = PluginVersion.split("\\.");
  private static final String configVersion = splitVersion[0] + '.' + splitVersion[1];
  private static final String configMajorVersion = splitVersion[0];

  private final Path dataDir;

  private boolean configCreatedThisRun = false;

  public BotConfig bot;
  public DiscordMessageConfig discord;
  public MinecraftMessageConfig minecraft;

  public ListCommandConfig listCommand;

  public List<String> EXCLUDED_SERVERS = new ArrayList<>();
  public boolean EXCLUDED_SERVERS_RECEIVE_MESSAGES = false;

  @Inject
  public Config(@DataDirectory Path dataDir, Logger logger) {
    this.dataDir = dataDir;

    var config = loadFile();

    loadConfig(config);

    bot = new BotConfig(config, logger);
    discord = new DiscordMessageConfig(config);
    minecraft = new MinecraftMessageConfig(config);

    listCommand = new ListCommandConfig(config);
  }

  private com.electronwill.nightconfig.core.Config loadFile() {
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
  }

  public boolean serverDisabled(String name) {
    return EXCLUDED_SERVERS.contains(name);
  }
}
