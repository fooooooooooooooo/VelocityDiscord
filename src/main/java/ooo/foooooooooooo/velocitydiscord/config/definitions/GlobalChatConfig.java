package ooo.foooooooooooo.velocitydiscord.config.definitions;

import ooo.foooooooooooo.velocitydiscord.config.Config;

public class GlobalChatConfig {
  public SystemMessageConfig proxyStart = new SystemMessageConfig("**Proxy started**", Config.GREEN);
  public SystemMessageConfig proxyStop = new SystemMessageConfig("**Proxy stopped**", Config.RED);

  public void load(Config config) {
    if (config == null) return;

    this.proxyStart.load(config.getConfig("proxy_start"));
    this.proxyStop.load(config.getConfig("proxy_stop"));
  }
}
