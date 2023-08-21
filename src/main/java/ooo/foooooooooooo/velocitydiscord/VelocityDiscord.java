package ooo.foooooooooooo.velocitydiscord;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import ooo.foooooooooooo.velocitydiscord.discord.Discord;
import ooo.foooooooooooo.velocitydiscord.yep.YepListener;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(
  id = "discord",
  name = VelocityDiscord.PluginName,
  description = VelocityDiscord.PluginDescription,
  version = VelocityDiscord.PluginVersion,
  url = VelocityDiscord.PluginUrl,
  authors = {"fooooooooooooooo"}
)
public class VelocityDiscord {
  public static final String PluginName = "Velocity Discord Bridge";
  public static final String PluginDescription = "Velocity Discord Chat Bridge";
  public static final String PluginVersion = "1.7.0";
  public static final String PluginUrl = "https://github.com/fooooooooooooooo/VelocityDiscord";

  public static final MinecraftChannelIdentifier YepIdentifier = MinecraftChannelIdentifier.create("velocity", "yep");

  public static boolean pluginDisabled = false;

  private static VelocityDiscord instance;

  private final ProxyServer server;
  Config config;

  @Nullable
  private Discord discord = null;

  @Nullable
  private YepListener yep = null;

  @Inject
  public VelocityDiscord(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = server;

    logger.info("Loading " + PluginName + " v" + PluginVersion);

    this.config = new Config(dataDirectory);
    pluginDisabled = config.isFirstRun();

    if (pluginDisabled) {
      logger.severe("This is the first time you are running this plugin. Please configure it in the config.yml file. Disabling plugin.");
    } else {
      this.discord = new Discord(this.server, logger, this.config);
      this.yep = new YepListener(logger);
    }

    instance = this;
  }

  public static Discord getDiscord() {
    return instance.discord;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    if (discord != null) {
      register(discord);
    }
    if (yep != null) {
      register(yep);
    }

    this.server.getChannelRegistrar().register(YepIdentifier);
  }

  private void register(Object x) {
    this.server.getEventManager().register(this, x);
  }
}
