package ooo.foooooooooooo.velocitydiscord.yep;

import cc.unilock.yeplib.api.event.YepAdvancementEvent;
import cc.unilock.yeplib.api.event.YepDeathEvent;
import com.velocitypowered.api.event.Subscribe;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;

public class YepListener {
  public YepListener() {
    VelocityDiscord.LOGGER.info("YepListener created");
  }

  @Subscribe
  public void onYepAdvancement(YepAdvancementEvent event) {
    if (VelocityDiscord.CONFIG.serverDisabled(event.getSource().getServer().getServerInfo().getName())) return;

    var uuid = event.getPlayer().getUniqueId().toString();
    var server = event.getSource().getServer().getServerInfo().getName();

    VelocityDiscord
      .getDiscord()
      .onPlayerAdvancement(event.getUsername(),
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

    VelocityDiscord
      .getDiscord()
      .onPlayerDeath(event.getUsername(), uuid, server, event.getDisplayName(), event.getMessage());
  }
}
