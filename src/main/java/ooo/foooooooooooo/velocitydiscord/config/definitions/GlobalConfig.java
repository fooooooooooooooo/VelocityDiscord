package ooo.foooooooooooo.velocitydiscord.config.definitions;


import ooo.foooooooooooo.velocitydiscord.config.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GlobalConfig {
  public List<String> excludedServers = new ArrayList<>();
  public boolean excludedServersReceiveMessages = false;

  /**
   * How often to ping all servers to check for online status (seconds)
   */
  public int pingIntervalSeconds = 30;

  public HashMap<String, String> serverDisplayNames = new HashMap<>();

  public GlobalDiscordConfig discord = new GlobalDiscordConfig();
  public GlobalMinecraftConfig minecraft = new GlobalMinecraftConfig();

  public void load(Config config) {
    this.excludedServers = config.getOrDefault("exclude_servers", this.excludedServers);
    this.excludedServersReceiveMessages =
      config.getOrDefault("excluded_servers_receive_messages", this.excludedServersReceiveMessages);

    this.pingIntervalSeconds = config.getOrDefault("ping_interval", this.pingIntervalSeconds);

    this.serverDisplayNames = config.getMapOrDefault("server_names", this.serverDisplayNames);

    this.discord.load(config.getConfig("discord"));
    this.minecraft.load(config.getConfig("minecraft"));
  }

  public boolean pingIntervalEnabled() {
    return this.pingIntervalSeconds > 0;
  }
}
