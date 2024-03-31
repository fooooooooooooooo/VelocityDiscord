package ooo.foooooooooooo.velocitydiscord.yep;

import cc.unilock.yeplib.api.event.YepAdvancementEvent;
import cc.unilock.yeplib.api.event.YepDeathEvent;
import com.velocitypowered.api.event.Subscribe;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;

import java.util.logging.Logger;

public class YepListener {
  public YepListener(Logger logger) {
    logger.info("YepListener created");
  }

  @Subscribe
  public void onYepAdvancement(YepAdvancementEvent event) {
    VelocityDiscord.getDiscord().sendPlayerAdvancement(event.getUsername(), event.getDisplayName(), event.getTitle(), event.getDescription());
  }

  @Subscribe
  public void onYepDeath(YepDeathEvent event) {
    VelocityDiscord.getDiscord().sendPlayerDeath(event.getUsername(), event.getDisplayName(), event.getMessage());
  }
}
