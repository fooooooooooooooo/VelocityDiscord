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
    if (config.serverDisabled(event.getSource().getServer().getServerInfo().getName())) return;
    VelocityDiscord.getDiscord().onPlayerAdvancement(event.getUsername(), event.getDisplayName(), event.getTitle(), event.getDescription());
  }

  @Subscribe
  public void onYepDeath(YepDeathEvent event) {
    if (config.serverDisabled(event.getSource().getServer().getServerInfo().getName())) return;
    VelocityDiscord.getDiscord().onPlayerDeath(event.getUsername(), event.getDisplayName(), event.getMessage());
  }
}
