package ooo.foooooooooooo.velocitydiscord.Yep;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class YepListener {
    private final Logger logger;

    public YepListener(Logger logger) {
        this.logger = logger;
        logger.info("YepListener created");
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(VelocityDiscord.YepIdentifier)) {
            return;
        }

        String data = new String(event.getData(), StandardCharsets.UTF_8);

        // type:message
        var parts = data.split(":");

        if (parts.length != 2) {
            logger.warning("Invalid yep message: " + data);
            return;
        }

        var type = MessageType.INVALID;
        try {
            type = MessageType.valueOf(parts[0]);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid yep message type: " + parts[0]);
        }

        var message = parts[1];
        var discord = VelocityDiscord.getDiscord();

        switch (type) {
            case DEATH -> discord.playerDeath(message);
            case ADVANCEMENT -> discord.playerAdvancement(message);
            default -> logger.warning("Invalid yep message type: " + parts[0]);
        }
    }
}
