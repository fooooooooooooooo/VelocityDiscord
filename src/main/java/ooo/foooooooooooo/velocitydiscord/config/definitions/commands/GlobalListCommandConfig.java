package ooo.foooooooooooo.velocitydiscord.config.definitions.commands;

import ooo.foooooooooooo.velocitydiscord.config.Config;

public class GlobalListCommandConfig {
  public boolean enabled = true;

  /**
   * Ephemeral messages are only visible to the user who sent the command
   */
  public boolean ephemeral = true;

  public String codeblockLang = "asciidoc";

  public void load(Config config) {
    if (config == null) return;

    this.enabled = config.getOrDefault("enabled", this.enabled);
    this.ephemeral = config.getOrDefault("ephemeral", this.ephemeral);
    this.codeblockLang = config.getOrDefault("codeblock_lang", this.codeblockLang);
  }
}
