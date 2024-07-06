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
  dependencies = @Dependency(id = "yeplib", optional = true)
)
public class VelocityDiscord {
  public static final String PluginName = "Velocity Discord Bridge";
  public static final String PluginDescription = "Velocity Discord Chat Bridge";
  public static final String PluginVersion = "1.8.2";
  public static final String PluginUrl = "https://github.com/fooooooooooooooo/VelocityDiscord";

  public static final MinecraftChannelIdentifier YepIdentifier = MinecraftChannelIdentifier.create("velocity", "yep");

  public static boolean pluginDisabled = false;

  private static VelocityDiscord instance;

  private final ProxyServer server;
  private final Config config;

  @Nullable
  private VelocityListener listener = null;

  @Nullable
  private Discord discord = null;

  @Nullable
  private YepListener yep = null;

  @Inject
  public VelocityDiscord(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = server;

    logger.info("Loading " + PluginName + " v" + PluginVersion);

    this.config = new Config(dataDirectory);

    pluginDisabled = this.config.isFirstRun();

    instance = this;

    if (pluginDisabled) {
      logger.severe("This is the first time you are running this plugin. Please configure it in the config.yml file. Disabling plugin.");
      return;
    }

    this.discord = new Discord(this.server, logger, this.config);

    if (server.getPluginManager().isLoaded("yeplib")) {
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

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    if (listener != null) {
      register(listener);
    }

    if (yep != null) {
      register(yep);
    }

    this.server.getChannelRegistrar().register(YepIdentifier);

    if (this.config.PING_INTERVAL > 0) {
      server.getScheduler()
        .buildTask(this, () -> {
          if (this.listener != null) this.listener.checkServerHealth();
        })
        .repeat(this.config.PING_INTERVAL, TimeUnit.SECONDS)
        .schedule();
    }
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
}
