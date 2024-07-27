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
import ooo.foooooooooooo.velocitydiscord.commands.ReloadCommand;
import ooo.foooooooooooo.velocitydiscord.config.Config;
import ooo.foooooooooooo.velocitydiscord.discord.Discord;
import ooo.foooooooooooo.velocitydiscord.yep.YepListener;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


@Plugin(
  id = "discord",
  name = VelocityDiscord.PluginName,
  description = VelocityDiscord.PluginDescription,
  version = VelocityDiscord.PluginVersion,
  url = VelocityDiscord.PluginUrl,
  authors = {"fooooooooooooooo"},
  dependencies = @Dependency(id = VelocityDiscord.YeplibId, optional = true)
)
public class VelocityDiscord {
  public static final String PluginName = "Velocity Discord Bridge";
  public static final String PluginDescription = "Velocity Discord Chat Bridge";
  public static final String PluginVersion = "1.8.2";
  public static final String PluginUrl = "https://github.com/fooooooooooooooo/VelocityDiscord";
  public static final String YeplibId = "yeplib";

  public static final MinecraftChannelIdentifier YepIdentifier = MinecraftChannelIdentifier.create("velocity", "yep");

  public static boolean pluginDisabled = false;

  private static VelocityDiscord instance;

  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDirectory;
  private Config config = null;

  @Nullable
  private VelocityListener listener = null;

  @Nullable
  private Discord discord = null;

  @Nullable
  private YepListener yep = null;
  private ScheduledTask pingScheduler = null;
  private ScheduledTask topicScheduler = null;

  @Inject
  public VelocityDiscord(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;

    logger.info("Loading %s v%s".formatted(PluginName, PluginVersion));

    reloadConfig();

    instance = this;

    if (pluginDisabled || this.config == null) {
      return;
    }

    this.discord = new Discord(this.server, logger, this.config);

    if (server.getPluginManager().isLoaded(VelocityDiscord.YeplibId)) {
      this.yep = new YepListener(logger, this.config);
    }

    this.listener = new VelocityListener(config, discord, server);
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

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    if (listener != null) {
      register(listener);
    }

    if (yep != null) {
      register(yep);
    }

    this.server.getChannelRegistrar().register(YepIdentifier);

    if (this.config != null) {
      tryStartPingScheduler();
    }

    var commandManager = server.getCommandManager();

    var reload = ReloadCommand.create();
    var reloadMeta = commandManager.metaBuilder(reload).plugin(this).build();

    commandManager.register(reloadMeta, reload);
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    if (discord != null) {
      discord.shutdown();
    }
  }

  private void register(Object x) {
    this.server.getEventManager().register(this, x);
  }

  public String reloadConfig() {
    String error = null;

    if (this.config == null) {
      this.config = new Config(this.dataDirectory, this.logger);
    } else {
      this.logger.info("Reloading config");

      error = this.config.reloadConfig(this.dataDirectory);

      // disable server ping scheduler if it was disabled
      if (this.config.PING_INTERVAL_SECONDS == 0 && this.pingScheduler != null) {
        this.pingScheduler.cancel();
        this.pingScheduler = null;
      }

      tryStartPingScheduler();

      // disable channel topic scheduler if it was disabled
      if (this.config.bot.UPDATE_CHANNEL_TOPIC_INTERVAL_MINUTES == 0 && this.topicScheduler != null) {
        this.topicScheduler.cancel();
        this.topicScheduler = null;
      }

      tryStartTopicScheduler();

      if (this.discord != null) {
        this.discord.configReloaded();
      }

      if (error != null) {
        this.logger.severe("Error reloading config: " + error);
      }

      this.logger.info("Config reloaded");
    }

    pluginDisabled = this.config.isFirstRun();

    if (pluginDisabled) {
      this.logger.severe("This is the first time you are running this plugin. Please configure it in the config.toml " +
        "file. Disabling plugin.");
    }

    return error;
  }

  private void tryStartPingScheduler() {
    if (this.config.PING_INTERVAL_SECONDS > 0 || this.pingScheduler != null) {
      this.pingScheduler = server.getScheduler()
        .buildTask(this, () -> {
          if (this.listener != null) this.listener.checkServerHealth();
        })
        .repeat(this.config.PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
        .schedule();
    }
  }

  private void tryStartTopicScheduler() {
    if (config.bot.UPDATE_CHANNEL_TOPIC_INTERVAL_MINUTES < 10)
      // Schedule the task to update the channel topic at the specified interval
      this.topicScheduler = server.getScheduler().buildTask(this, () -> {
          if (this.discord != null) this.discord.updateChannelTopic();
        })
        .repeat(config.bot.UPDATE_CHANNEL_TOPIC_INTERVAL_MINUTES, TimeUnit.MINUTES)
        .schedule();

    logger.info("Scheduled task to update channel topic every %d minutes".formatted(config.bot.UPDATE_CHANNEL_TOPIC_INTERVAL_MINUTES));
  }
}
