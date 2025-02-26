package ooo.foooooooooooo.velocitydiscord.config.commands;

import ooo.foooooooooooo.config.Config;
import ooo.foooooooooooo.config.Key;

import java.util.Optional;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType"})
public class ListCommandConfig extends Config {
  @Key(value = "enabled", overridable = false)
  public Boolean DISCORD_LIST_ENABLED = true;
  @Key(value = "ephemeral", overridable = false)
  public Boolean EPHEMERAL = true;
  @Key("server_format")
  public String SERVER_FORMAT = "[{server_name} {online_players}/{max_players}]";
  @Key("player_format")
  public String PLAYER_FORMAT = "- {username}";
  @Key("no_players")
  public Optional<String> NO_PLAYERS_FORMAT = Optional.of("No players online");
  @Key("server_offline")
  public Optional<String> SERVER_OFFLINE_FORMAT = Optional.of("Server offline");
  @Key(value = "codeblock_lang", overridable = false)
  public String CODEBLOCK_LANG = "asciidoc";

  public ListCommandConfig(com.electronwill.nightconfig.core.Config config) {
    super(config);
  }

  public ListCommandConfig(com.electronwill.nightconfig.core.Config config, ListCommandConfig main) {
    super(config, main);
  }
}
