package ooo.foooooooooooo.velocitydiscord;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import ooo.foooooooooooo.velocitydiscord.Yep.YepListener;

import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(id = "discord",
        name = VelocityDiscord.PluginName,
        description = VelocityDiscord.PluginDescription,
        version = VelocityDiscord.PluginVersion,
        url = VelocityDiscord.PluginUrl,
        authors = {"fooooooooooooooo"}
)
public class VelocityDiscord {
    public static final String PluginName = "Velocity Discord Bridge";
    public static final String PluginDescription = "Velocity Discord Chat Bridge";
    public static final String PluginVersion = "1.0.2";
    public static final String PluginUrl = "https://github.com/fooooooooooooooo/VelocityDiscord";

    public static final MinecraftChannelIdentifier YepIdentifier =
            MinecraftChannelIdentifier.create("velocity", "yep");
    private static VelocityDiscord instance;

    private final ProxyServer server;
    private final Logger logger;
    private final Discord discord;
    private final YepListener yep;

    Config config;

    @Inject
    public VelocityDiscord(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;

        logger.info("Loading " + PluginName + " v" + PluginVersion);

        this.config = new Config(dataDirectory);

        this.discord = new Discord(this.server, this.logger, this.config);
        this.yep = new YepListener(this.logger);

        instance = this;
    }

    public static Discord getDiscord() {
        return instance.discord;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        register(discord);
        register(yep);

        this.server.getChannelRegistrar().register(YepIdentifier);
    }

    private void register(Object x) {
        this.server.getEventManager().register(this, x);
    }
}
