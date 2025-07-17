package ooo.foooooooooooo.velocitydiscord.config.definitions;

import ooo.foooooooooooo.velocitydiscord.config.Config;

public class GlobalMinecraftConfig {
  public String pluginCommand = "discord";

  public void load(Config config) {
    if (config == null) return;

    this.pluginCommand = config.getOrDefault("plugin_command", this.pluginCommand);
  }
}
