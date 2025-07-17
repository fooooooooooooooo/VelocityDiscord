package ooo.foooooooooooo.velocitydiscord.config.definitions;


import ooo.foooooooooooo.velocitydiscord.config.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalConfig {
  public List<String> excludedServers = new ArrayList<>();
  public boolean excludedServersReceiveMessages = false;

  /**
   * How often to ping all servers to check for online status (seconds)
   */
  public int pingIntervalSeconds = 30;

  public Map<String, String> serverDisplayNames = new HashMap<>();

  public GlobalDiscordConfig discord = new GlobalDiscordConfig();
  public GlobalMinecraftConfig minecraft = new GlobalMinecraftConfig();

  public void load(Config config) {
    this.excludedServers = config.getOrDefault("excludeServers", this.excludedServers);
    this.excludedServersReceiveMessages = config.getOrDefault("excludedServersReceiveMessages", this.excludedServersReceiveMessages);

    this.pingIntervalSeconds = config.getOrDefault("pingIntervalSeconds", this.pingIntervalSeconds);

    this.serverDisplayNames = config.getOrDefault("serverDisplayNames", this.serverDisplayNames);

    this.discord.load(config.getConfig("discord"));
    this.minecraft.load(config.getConfig("minecraft"));
  }

  public boolean pingIntervalEnabled() {
    return this.pingIntervalSeconds > 0;
  }
}
