package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.Config;

import java.util.logging.Logger;

public class ServerConfig {
    private static final String DefaultChannelId = "000000000000000000";

    public String CHANNEL_ID = DefaultChannelId;
    public String SERVER_NAME = "";

    private final Logger logger;
    public ServerConfig(Config config, String serverKey, Logger logger) {
        this.logger = logger;
        SERVER_NAME = serverKey;
        loadConfig(config);
    }

    private void loadConfig(Config config) {
        CHANNEL_ID = config.getOrElse("discord." + SERVER_NAME + ".channel", CHANNEL_ID);
        logger.info("Loaded server config for " + SERVER_NAME + " with channel " + CHANNEL_ID);
    }
}
