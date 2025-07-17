package ooo.foooooooooooo.velocitydiscord.config.definitions.commands;

import ooo.foooooooooooo.velocitydiscord.config.Config;

public class CommandConfig {
  public ListCommandConfig list = new ListCommandConfig();

  public void load(Config config) {
    if (config == null) return;

    this.list.load(config.getConfig("list"));
  }
}
