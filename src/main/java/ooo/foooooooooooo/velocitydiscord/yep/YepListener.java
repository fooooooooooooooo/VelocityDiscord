package ooo.foooooooooooo.velocitydiscord.yep;

import cc.unilock.yeplib.api.event.YepAdvancementEvent;
import cc.unilock.yeplib.api.event.YepDeathEvent;
import cc.unilock.yeplib.api.event.YepMessageEvent;
import com.velocitypowered.api.event.Subscribe;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YepListener {
  private static final Logger logger = LoggerFactory.getLogger(YepListener.class);

  public YepListener() {
    VelocityDiscord.LOGGER.info("YepListener created");
  }

  @Subscribe
  public void onYepMessage(YepMessageEvent event) {
    logger.debug("Received YepMessageEvent: {}", event);
  }

  @Subscribe
  public void onYepAdvancement(YepAdvancementEvent event) {
    if (VelocityDiscord.CONFIG.serverDisabled(event.getSource().getServer().getServerInfo().getName())) return;

    var uuid = event.getPlayer().getUniqueId().toString();
    var server = event.getSource().getServer().getServerInfo().getName();

    VelocityDiscord.getDiscord()
      .onPlayerAdvancement(
        event.getUsername(),
        uuid,
        server,
        event.getDisplayName(),
        event.getTitle(),
        event.getDescription()
      );
  }

  @Subscribe
  public void onYepDeath(YepDeathEvent event) {
    if (VelocityDiscord.CONFIG.serverDisabled(event.getSource().getServer().getServerInfo().getName())) return;

    var uuid = event.getPlayer().getUniqueId().toString();
    var server = event.getSource().getServer().getServerInfo().getName();

    VelocityDiscord.getDiscord()
      .onPlayerDeath(event.getUsername(), uuid, server, event.getDisplayName(), event.getMessage());
  }
}
