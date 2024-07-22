package ooo.foooooooooooo.velocitydiscord.yep;

import cc.unilock.yeplib.api.event.YepAdvancementEvent;
import cc.unilock.yeplib.api.event.YepDeathEvent;
import com.velocitypowered.api.event.Subscribe;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;
import ooo.foooooooooooo.velocitydiscord.config.Config;

import java.util.logging.Logger;

public class YepListener {
  private final Config config;

  public YepListener(Logger logger, Config config) {
    this.config = config;
    logger.info("YepListener created");
  }

  @Subscribe
  public void onYepAdvancement(YepAdvancementEvent event) {
    String serverName = event.getSource().getServer().getServerInfo().getName();
    if (config.serverDisabled(serverName)) return;
    VelocityDiscord.getDiscord().sendPlayerAdvancement(event.getUsername(), event.getDisplayName(), event.getTitle(), event.getDescription(), serverName);
  }

  @Subscribe
  public void onYepDeath(YepDeathEvent event) {
    String serverName = event.getSource().getServer().getServerInfo().getName();
    if (config.serverDisabled(serverName)) return;
    VelocityDiscord.getDiscord().sendPlayerDeath(event.getUsername(), event.getDisplayName(), event.getMessage(), serverName);
  }
}
