package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.Config;

import java.util.logging.Logger;

public class ServerConfig {
    public static final String DefaultChannelId = "000000000000000001";

    public String CHANNEL_ID = DefaultChannelId;
    public String WEBHOOK_URL = "";
    public String SERVER_NAME;

    private final Logger logger;
    public ServerConfig(Config config, String serverKey, Logger logger) {
        this.logger = logger;
        SERVER_NAME = serverKey;
        loadConfig(config);
    }

    private void loadConfig(Config config) {
        CHANNEL_ID = config.getOrElse("discord." + SERVER_NAME + ".channel", DefaultChannelId);
        WEBHOOK_URL = config.getOrElse("discord." + SERVER_NAME + ".webhook_url", "");
        logger.info("Loaded server config for " + SERVER_NAME + " with channel ID " + CHANNEL_ID + " and webhook URL " + WEBHOOK_URL);
    }
}
