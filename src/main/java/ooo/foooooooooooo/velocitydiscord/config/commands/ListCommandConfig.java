package ooo.foooooooooooo.velocitydiscord.config.commands;

import com.electronwill.nightconfig.core.Config;
import ooo.foooooooooooo.velocitydiscord.config.BaseConfig;
import ooo.foooooooooooo.velocitydiscord.config.Key;

import java.util.Optional;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType"})
public class ListCommandConfig extends BaseConfig {
  public ListCommandConfig(Config config) {
    super(config);
    loadConfig();
  }

  public ListCommandConfig(Config config, ListCommandConfig main) {
    super(config, main);
    loadConfig();
  }

  @Key(value = "discord.commands.list.enabled", overridable = false)
  public Boolean DISCORD_LIST_ENABLED = true;
  @Key(value = "discord.commands.list.ephemeral", overridable = false)
  public Boolean EPHEMERAL = true;
  @Key("discord.commands.list.server_format")
  public String SERVER_FORMAT = "[{server_name} {online_players}/{max_players}]";
  @Key("discord.commands.list.player_format")
  public String PLAYER_FORMAT = "- {username}";
  @Key("discord.commands.list.no_players")
  public Optional<String> NO_PLAYERS_FORMAT = Optional.of("No players online");
  @Key("discord.commands.list.server_offline")
  public Optional<String> SERVER_OFFLINE_FORMAT = Optional.of("Server offline");
  @Key(value = "discord.commands.list.codeblock_lang", overridable = false)
  public String CODEBLOCK_LANG = "asciidoc";
}
