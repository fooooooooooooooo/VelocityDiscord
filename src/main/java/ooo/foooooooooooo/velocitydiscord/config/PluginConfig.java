package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import ooo.foooooooooooo.velocitydiscord.Constants;
import ooo.foooooooooooo.velocitydiscord.config.definitions.*;
import ooo.foooooooooooo.velocitydiscord.discord.MessageCategory;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PluginConfig {
  private static final String[] splitVersion = Constants.PluginVersion.split("\\.");
  public static final String ConfigVersion = splitVersion[0] + '.' + splitVersion[1];
  private static final String configMajorVersion = splitVersion[0];

  private static boolean configCreatedThisRun = false;

  private final Map<String, String> serverDisplayNames = new HashMap<>();

  private Path dataDir;
  private HashMap<String, ServerOverrideConfig> serverOverridesMap = new HashMap<>();

  private final Logger logger;
  private Config config;

  public GlobalConfig global = new GlobalConfig();
  public LocalConfig local = new LocalConfig();

  public PluginConfig(Path dataDir, Logger logger) {
    this.logger = logger;
    this.dataDir = dataDir;
    this.config = PluginConfig.loadFile(dataDir);
    this.loadConfig();
    this.onLoad();
  }

  public PluginConfig(Config config, Logger logger) {
    this.logger = logger;
    this.dataDir = null;
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
    checkConfig();

    loadOverrides();

    var error = checkInvalidValues();

    if (error != null) {
      this.logger.error(error);
    }
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

  private void checkConfig() {
    if (this.config == null || this.config.isEmpty()) {
      throw new RuntimeException("ERROR: Config is empty");
    }

    // check for compatible config version
    String version = this.config.get("config_version");

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
    return configCreatedThisRun;
  }

  public void loadOverrides() {
    CommentedConfig server_names = this.config.get("server_names");

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

    CommentedConfig serverOverrides = this.config.get("override");

    if (serverOverrides == null) {
      this.logger.debug("No server overrides found");
      return;
    }

    this.serverOverridesMap = new HashMap<>();

    for (var entry : serverOverrides.entrySet()) {
      if (entry.getValue() instanceof Config serverOverride) {
        var serverName = entry.getKey();

        // todo: maybe better than this
        if (this.global.excludedServers.contains(serverName) && !this.global.excludedServersReceiveMessages) {
          this.logger.info("Ignoring override for excluded server: {}", serverName);
          continue;
        }

        this.serverOverridesMap.put(serverName, new ServerOverrideConfig(serverOverride, this));
      } else {
        this.logger.warn("Invalid server override for `{}`: `{}`", entry.getKey(), entry.getValue());
      }
    }
  }

  public boolean serverDisabled(String name) {
    return this.global.excludedServers.contains(name);
  }

  public String serverName(String name) {
    return this.serverDisplayNames.getOrDefault(name, name);
  }

  public @Nullable String reloadConfig(Path dataDirectory) {
    this.dataDir = dataDirectory;

    // reset old values
    this.serverDisplayNames.clear();
    this.serverOverridesMap.clear();
    this.global.excludedServers.clear();

    this.config = PluginConfig.loadFile(this.dataDir);

    try {
      loadOverrides();

      return checkInvalidValues();
    } catch (Exception e) {
      return "ERROR: " + e.getMessage();
    }
  }

  //  private boolean isWebhook(DiscordChatConfig.UserMessageType messageType) {
  //    return messageType == DiscordChatConfig.UserMessageType.WEBHOOK;
  //  }

  private String checkInvalidValues() {
    //    if (this.inner.discord.webhook.isWebhookUsed() && this.DISCORD.WEBHOOK.invalid()) {
    //      var errors = new ArrayList<String>();
    //
    //      // check each message category
    //      if (isWebhook(this.DISCORD_CHAT.ADVANCEMENT_TYPE) && this.DISCORD_CHAT.ADVANCEMENT_WEBHOOK.invalid()) {
    //        errors.add("`discord.chat.advancement.webhook` is set and `discord.chat.advancement.type` is set to `webhook`");
    //      }
    //      if (isWebhook(this.DISCORD_CHAT.MESSAGE_TYPE) && this.DISCORD_CHAT.MESSAGE_WEBHOOK.invalid()) {
    //        errors.add("`discord.chat.message.webhook` is set and `discord.chat.message.type` is set to `webhook`");
    //      }
    //      if (isWebhook(this.DISCORD_CHAT.JOIN_TYPE) && this.DISCORD_CHAT.JOIN_WEBHOOK.invalid()) {
    //        errors.add("`discord.chat.join.webhook` is set and `discord.chat.join.type` is set to `webhook`");
    //      }
    //      if (isWebhook(this.DISCORD_CHAT.DEATH_TYPE) && this.DISCORD_CHAT.DEATH_WEBHOOK.invalid()) {
    //        errors.add("`discord.chat.death.webhook` is set and `discord.chat.death.type` is set to `webhook`");
    //      }
    //      if (isWebhook(this.DISCORD_CHAT.ADVANCEMENT_TYPE) && this.DISCORD_CHAT.ADVANCEMENT_WEBHOOK.invalid()) {
    //        errors.add("`discord.chat.advancement.webhook` is set and `discord.chat.advancement.type` is set to `webhook`");
    //      }
    //      if (isWebhook(this.DISCORD_CHAT.LEAVE_TYPE) && this.DISCORD_CHAT.LEAVE_WEBHOOK.invalid()) {
    //        errors.add("`discord.chat.leave.webhook` is set and `discord.chat.leave.type` is set to `webhook`");
    //      }
    //      if (isWebhook(this.DISCORD_CHAT.DISCONNECT_TYPE) && this.DISCORD_CHAT.DISCONNECT_WEBHOOK.invalid()) {
    //        errors.add("`discord.chat.disconnect.webhook` is set and `discord.chat.disconnect.type` is set to `webhook`");
    //      }
    //      if (isWebhook(this.DISCORD_CHAT.SERVER_SWITCH_TYPE) && this.DISCORD_CHAT.SERVER_SWITCH_WEBHOOK.invalid()) {
    //        errors.add(
    //          "`discord.chat.server_switch.webhook` is set and `discord.chat.server_switch.type` is set to `webhook`");
    //      }
    //
    //      if (!errors.isEmpty()) {
    //        var errorStart = "ERROR: neither `discord.webhook` nor ";
    //        return errors.stream().map(s -> errorStart + s).reduce((a, b) -> a + "\n" + b).orElse(null);
    //      }
    //    }

    return null;
  }

  public ServerOverrideConfig getServerConfig(String serverName) {
    return this.serverOverridesMap.get(serverName);
  }

  public Config getInner() {
    return this.config;
  }

  public static class ServerOverrideConfig {
    public final LocalConfig local;

    public ServerOverrideConfig(
      Config overrideConfig,
      PluginConfig pluginConfig
    ) {
      this.local = new LocalConfig();
      this.local.load(pluginConfig.getInner());
      // load the override config on top of the base config
      this.local.load(overrideConfig);
    }

    public DiscordConfig getDiscordConfig() {
      return this.local.discord;
    }

    public ChatConfig getChatConfig() {
      return this.local.discord.chat;
    }

    public MinecraftConfig getMinecraftConfig() {
      return this.local.minecraft;
    }

    public boolean isWebhookUsed() {
      var chat = this.getChatConfig();
      return Arrays.stream(new UserMessageType[]{
        chat.message.type,
        chat.death.type,
        chat.advancement.type,
        chat.join.type,
        chat.leave.type,
        chat.disconnect.type,
        chat.serverSwitch.type,
      }).anyMatch(t -> t == UserMessageType.WEBHOOK);
    }

    public WebhookConfig getWebhookConfig(MessageCategory type) {
      var messageSpecificWebhook = switch (type) {
        case ADVANCEMENT -> this.local.discord.chat.advancement.webhook;
        case MESSAGE -> this.local.discord.chat.message.webhook;
        case JOIN -> this.local.discord.chat.join.webhook;
        case DEATH -> this.local.discord.chat.death.webhook;
        case LEAVE -> this.local.discord.chat.leave.webhook;
        case DISCONNECT -> this.local.discord.chat.disconnect.webhook;
        case SERVER_SWITCH -> this.local.discord.chat.serverSwitch.webhook;
      };

      // todo: fall back to global webhook if message specific one is not set
      return this.local.discord.webhook;
    }
  }
}
