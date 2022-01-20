package ooo.foooooooooooo.velocitydiscord;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

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

    private final ProxyServer server;
    private final Logger logger;

    Config config;

    @Inject
    public VelocityDiscord(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;

        logger.info("Loading " + PluginName + " v" + PluginVersion);

        this.config = new Config(dataDirectory);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        register(new Discord(this.server, this.logger, this.config));
    }

    private void register(Object x) {
        this.server.getEventManager().register(this, x);
    }
}
