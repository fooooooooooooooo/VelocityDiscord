package ooo.foooooooooooo.velocitydiscord.config.commands;

import com.electronwill.nightconfig.core.Config;
import ooo.foooooooooooo.velocitydiscord.config.BaseConfig;

import java.util.Optional;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType"})
public class ListCommandConfig extends BaseConfig {
  public ListCommandConfig(Config config) {
    loadConfig(config);
  }

  public Boolean DISCORD_LIST_ENABLED = true;
  public Boolean EPHEMERAL = true;
  public String SERVER_FORMAT = "[{server_name} {online_players}/{max_players}]";
  public String PLAYER_FORMAT = "- {username}";
  public Optional<String> NO_PLAYERS_FORMAT = Optional.of("No players online");
  public Optional<String> SERVER_OFFLINE_FORMAT = Optional.of("Server offline");
  public String CODEBLOCK_LANG = "asciidoc";

  @Override
  protected void loadConfig(Config config) {
    DISCORD_LIST_ENABLED = get(config, "discord.commands.list.enabled", DISCORD_LIST_ENABLED);
    EPHEMERAL = get(config, "discord.commands.list.ephemeral", EPHEMERAL);
    SERVER_FORMAT = get(config, "discord.commands.list.server_format", SERVER_FORMAT);
    PLAYER_FORMAT = get(config, "discord.commands.list.player_format", PLAYER_FORMAT);
    NO_PLAYERS_FORMAT = getOptional(config, "discord.commands.list.no_players", NO_PLAYERS_FORMAT);
    SERVER_OFFLINE_FORMAT = getOptional(config, "discord.commands.list.server_offline", SERVER_OFFLINE_FORMAT);
    CODEBLOCK_LANG = get(config, "discord.commands.list.codeblock_lang", CODEBLOCK_LANG);
  }
}
