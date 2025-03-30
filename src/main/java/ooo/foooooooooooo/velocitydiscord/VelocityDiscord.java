package ooo.foooooooooooo.velocitydiscord;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.scheduler.ScheduledTask;
import ooo.foooooooooooo.velocitydiscord.commands.Commands;
import ooo.foooooooooooo.velocitydiscord.compat.LuckPerms;
import ooo.foooooooooooo.velocitydiscord.config.PluginConfig;
import ooo.foooooooooooo.velocitydiscord.discord.Discord;
import ooo.foooooooooooo.velocitydiscord.yep.YepListener;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Plugin(
  id = "discord",
  name = Constants.PluginName,
  description = Constants.PluginDescription,
  version = Constants.PluginVersion,
  url = Constants.PluginUrl,
  authors = {"fooooooooooooooo"},
  dependencies = {
    @Dependency(id = Constants.YeplibId, optional = true), @Dependency(id = Constants.LuckPermsId, optional = true)
  }
)
public class VelocityDiscord {
  public static final MinecraftChannelIdentifier YepIdentifier = MinecraftChannelIdentifier.create("velocity", "yep");

  public static Logger LOGGER;
  public static PluginConfig CONFIG;
  public static ProxyServer SERVER;

  public static boolean pluginDisabled = false;

  private static VelocityDiscord instance;

  private final Path dataDirectory;

  @Nullable
  private VelocityListener listener = null;

  @Nullable
  private Discord discord = null;

  @Nullable
  private YepListener yep = null;

  @Nullable
  private LuckPerms luckPerms = null;

  private ScheduledTask pingScheduler = null;
  private ScheduledTask topicScheduler = null;

  @Inject
  public VelocityDiscord(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    SERVER = server;
    LOGGER = logger;

    this.dataDirectory = dataDirectory;

    LOGGER.info("Loading {} v{}", Constants.PluginName, Constants.PluginVersion);

    reloadConfig();

    VelocityDiscord.instance = this;

    if (pluginDisabled || CONFIG == null) {
      return;
    }

    this.discord = new Discord();

    if (server.getPluginManager().isLoaded(Constants.YeplibId)) {
      this.yep = new YepListener();
    }

    this.listener = new VelocityListener(this.discord);
  }

  public static Discord getDiscord() {
    return instance.discord;
  }

  public static VelocityListener getListener() {
    return instance.listener;
  }

  public static VelocityDiscord getInstance() {
    return instance;
  }

  public static LuckPerms getLuckPerms() {
    return instance.luckPerms;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    if (this.listener != null) {
      register(this.listener);
    }

    if (this.yep != null) {
      register(this.yep);
    }

    SERVER.getChannelRegistrar().register(YepIdentifier);

    if (CONFIG != null) {
      tryStartPingScheduler();
      tryStartTopicScheduler();
    }

    Commands.registerCommands(SERVER.getCommandManager(), CONFIG);

    try {
      if (SERVER.getPluginManager().getPlugin("luckperms").isPresent()) {
        this.luckPerms = new LuckPerms();
        LOGGER.info("LuckPerms found, prefix can be displayed");
      }
    } catch (Exception e) {
      LOGGER.warn("Error getting LuckPerms instance: {}", e.getMessage());
    } finally {
      if (this.luckPerms == null) {
        LOGGER.info("LuckPerms not found, prefix will not be displayed");
      }
    }
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    if (this.discord != null) {
      this.discord.shutdown();
    }
  }

  private void register(Object listener) {
    SERVER.getEventManager().register(this, listener);
  }

  public String reloadConfig() {
    String error = null;

    if (CONFIG == null) {
      CONFIG = new PluginConfig(this.dataDirectory, VelocityDiscord.LOGGER);
    } else {
      LOGGER.info("Reloading config");

      error = CONFIG.reloadConfig(this.dataDirectory);

      // disable server ping scheduler if it was disabled
      if (CONFIG.PING_INTERVAL_SECONDS == 0 && this.pingScheduler != null) {
        this.pingScheduler.cancel();
        this.pingScheduler = null;
      }

      tryStartPingScheduler();

      // disable channel topic scheduler if it was disabled
      if (CONFIG.getDiscordConfig().UPDATE_CHANNEL_TOPIC_INTERVAL_MINUTES == 0 && this.topicScheduler != null) {
        this.topicScheduler.cancel();
        this.topicScheduler = null;
      }

      tryStartTopicScheduler();

      if (this.discord != null) {
        this.discord.onConfigReload();
      }

      // unregister and re register commands
      Commands.unregisterCommands(SERVER.getCommandManager());
      Commands.registerCommands(SERVER.getCommandManager(), CONFIG);

      if (error != null) {
        LOGGER.error("Error reloading config:");
        for (var line : error.split("\n")) {
          LOGGER.error(line);
        }
      } else {
        LOGGER.info("Config reloaded");
      }
    }

    pluginDisabled = CONFIG.isFirstRun();

    if (pluginDisabled) {
      LOGGER.error("This is the first time you are running this plugin."
        + " Please configure it in the config.toml file. Disabling plugin.");
    }

    return error;
  }

  private void tryStartPingScheduler() {
    if (CONFIG.PING_INTERVAL_SECONDS > 0 || this.pingScheduler != null) {
      this.pingScheduler = SERVER.getScheduler().buildTask(
        this, () -> {
          if (this.listener != null) this.listener.checkServerHealth();
        }
      ).repeat(CONFIG.PING_INTERVAL_SECONDS, TimeUnit.SECONDS).schedule();
    }
  }

  private void tryStartTopicScheduler() {
    if (CONFIG.getDiscordConfig().updateChannelTopicDisabled()) return;

    var interval = CONFIG.getDiscordConfig().UPDATE_CHANNEL_TOPIC_INTERVAL_MINUTES;
    if (interval < 10) {
      LOGGER.warn("Invalid update_channel_topic_interval value: {}. Must be between > 10, setting to 10", interval);
      interval = 10;
    }

    this.topicScheduler = SERVER.getScheduler().buildTask(
      this, () -> {
        LOGGER.debug("Updating channel topic");
        if (this.discord != null) this.discord.updateChannelTopic();
      }
    ).repeat(interval, TimeUnit.MINUTES).schedule();

    LOGGER.info("Scheduled task to update channel topic every {} minutes", interval);
  }
}
