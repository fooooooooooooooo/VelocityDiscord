package ooo.foooooooooooo.velocitydiscord.config.definitions.commands;

import ooo.foooooooooooo.velocitydiscord.config.Config;

public class GlobalCommandConfig {
  public GlobalListCommandConfig list = new GlobalListCommandConfig();

  public void load(Config config) {
    if (config == null) return;

    this.list.load(config.getConfig("list"));
  }
}
