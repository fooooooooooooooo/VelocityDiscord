package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;
import ooo.foooooooooooo.velocitydiscord.Constants;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class PluginConfig implements ServerConfig {
  private static final String[] splitVersion = Constants.PluginVersion.split("\\.");
  public static final String ConfigVersion = splitVersion[0] + '.' + splitVersion[1];
  private static final String configMajorVersion = splitVersion[0];

  private static boolean configCreatedThisRun = false;

  @SerdeKey("exclude_servers")
  @SerdeDefault(provider = "defaultExcludedServers")
  public List<String> EXCLUDED_SERVERS;
  private final transient Supplier<List<String>> defaultExcludedServers = List::of;

  @SerdeKey("excluded_servers_receive_messages")
  @SerdeDefault(provider = "defaultExcludedServersReceiveMessages")
  public boolean EXCLUDED_SERVERS_RECEIVE_MESSAGES;
  private final transient Supplier<Boolean> defaultExcludedServersReceiveMessages = () -> false;

  @SerdeKey("ping_interval")
  @SerdeDefault(provider = "defaultPingIntervalSeconds")
  public int PING_INTERVAL_SECONDS;
  private final transient Supplier<Integer> defaultPingIntervalSeconds = () -> 15;

  public transient DiscordConfig DISCORD;
  public transient DiscordChatConfig DISCORD_CHAT;
  public transient MinecraftConfig MINECRAFT;

  public transient DiscordBotConfig BOT;
  public transient ProxyDiscordChatConfig DISCORD_CHAT_PROXY;
  public transient MinecraftGlobalConfig MINECRAFT_GLOBAL;

  private transient final Map<String, String> serverDisplayNames = new HashMap<>();

  private transient Path dataDir;
  private transient HashMap<String, OverrideConfig> serverOverridesMap = new HashMap<>();

  private transient final Logger logger;
  private transient Config config;

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

  private void logKeys(Config config, int indent) {
    var indentStr = "  ".repeat(Math.max(0, indent));
    for (var entry : config.entrySet()) {
      if (entry.getValue() instanceof Config) {
        System.out.println(indentStr + entry.getKey() + ":");
        logKeys(entry.getValue(), indent + 1);
      } else {
        System.out.println(indentStr + entry.getKey() + ": " + entry.getValue());
      }
    }
  }

  private void loadConfig() {
    if (this.config == null || this.config.isEmpty()) {
      throw new RuntimeException("ERROR: Config is empty");
    }

    this.logger.info("raw config:");
    logKeys(this.config, 0);

    Deserialization.deserializer().deserializeFields(this.config, this);

    this.BOT = Deserialization.deserialize(this.config.get("discord"), new DiscordBotConfig());
    this.DISCORD = Deserialization.deserialize(this.config.get("discord"), new DiscordConfig());

    Config discordChatConfig = this.config.get("discord.chat");
    if (discordChatConfig != null) {
      this.logger.info("raw discord.chat:");
      logKeys(discordChatConfig, 0);
    }

    this.DISCORD_CHAT = Deserialization.deserialize(discordChatConfig, new DiscordChatConfig());

    this.DISCORD_CHAT_PROXY = Deserialization.deserialize(this.config.get("discord.chat"), new ProxyDiscordChatConfig());
    this.MINECRAFT = Deserialization.deserialize(this.config.get("minecraft"), new MinecraftConfig());
    this.MINECRAFT_GLOBAL = Deserialization.deserialize(this.config.get("minecraft"), new MinecraftGlobalConfig());
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

    return fileConfig;
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
    return this.DISCORD.isDefaultValues() || configCreatedThisRun;
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
        if (this.EXCLUDED_SERVERS.contains(serverName) && !this.EXCLUDED_SERVERS_RECEIVE_MESSAGES) {
          this.logger.info("Ignoring override for excluded server: {}", serverName);
          continue;
        }

        // todo: load normal config then load override config on top of it
        // this.serverOverridesMap.put(serverName, new OverrideConfig(serverOverride, serverName, this));
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

    this.config = PluginConfig.loadFile(this.dataDir);

    try {
      loadOverrides();

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

  public Config getInner() {
    return this.config;
  }

  public String debug() {
    var sb = new StringBuilder();

    sb.append("PluginConfig:\n");
    sb.append("  config_version: ").append(ConfigVersion).append("\n");
    sb.append("  excluded_servers: ").append(this.EXCLUDED_SERVERS).append("\n");
    sb.append("  excluded_servers_receive_messages: ").append(this.EXCLUDED_SERVERS_RECEIVE_MESSAGES).append("\n");
    sb.append("  ping_interval_seconds: ").append(this.PING_INTERVAL_SECONDS).append("\n");
    sb.append("  server_names: ").append(this.serverDisplayNames).append("\n");

    sb.append("  discord: ").append("\n");
    for (var line : this.DISCORD.debug().split("\n")) {
      sb.append("    ").append(line).append("\n");
    }

    sb.append("  discord.chat: ").append("\n");
    for (var line : this.DISCORD_CHAT.debug().split("\n")) {
      sb.append("    ").append(line).append("\n");
    }

    sb.append("  minecraft: ").append("\n");
    for (var line : this.MINECRAFT.debug().split("\n")) {
      sb.append("    ").append(line).append("\n");
    }

    sb.append("  discord.bot: ").append("\n");
    for (var line : this.BOT.debug().split("\n")) {
      sb.append("    ").append(line).append("\n");
    }

    sb.append("  discord.chat.proxy: ").append("\n");
    for (var line : this.DISCORD_CHAT_PROXY.debug().split("\n")) {
      sb.append("    ").append(line).append("\n");
    }

    sb.append("  minecraft.global: ").append("\n");
    for (var line : this.MINECRAFT_GLOBAL.debug().split("\n")) {
      sb.append("    ").append(line).append("\n");
    }

    sb.append("  server_overrides: ").append("\n");
    for (var entry : this.serverOverridesMap.entrySet()) {
      sb.append("    ").append(entry.getKey()).append(": ").append("\n");

      for (var line : entry.getValue().debug().split("\n")) {
        sb.append("      ").append(line).append("\n");
      }
    }

    return sb.toString();
  }

  public static class OverrideConfig implements ServerConfig {
    @SerdeKey("discord")
    public DiscordConfig DISCORD;
    @SerdeKey("discord.chat")
    public DiscordChatConfig DISCORD_CHAT;
    @SerdeKey("minecraft")
    public MinecraftConfig MINECRAFT;

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

    public String debug() {
      var sb = new StringBuilder();

      sb.append("discord:\n");
      for (var line : this.DISCORD.debug().split("\n")) {
        sb.append("  ").append(line).append("\n");
      }

      sb.append("discord.chat:\n");
      for (var line : this.DISCORD_CHAT.debug().split("\n")) {
        sb.append("  ").append(line).append("\n");
      }

      sb.append("minecraft:\n");
      for (var line : this.MINECRAFT.debug().split("\n")) {
        sb.append("  ").append(line).append("\n");
      }

      return sb.toString();
    }
  }

}
