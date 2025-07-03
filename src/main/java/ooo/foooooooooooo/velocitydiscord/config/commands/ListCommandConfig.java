package ooo.foooooooooooo.velocitydiscord.config.commands;

import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;

import java.util.Optional;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType"})
public class ListCommandConfig {
  @SerdeKey("server_format")
  public String SERVER_FORMAT = "[{server_name} {online_players}/{max_players}]";
  @SerdeKey("player_format")
  public String PLAYER_FORMAT = "- {username}";
  @SerdeKey("no_players")
  public Optional<String> NO_PLAYERS_FORMAT = Optional.of("No players online");
  @SerdeKey("server_offline")
  public Optional<String> SERVER_OFFLINE_FORMAT = Optional.of("Server offline");
  @SerdeKey("codeblock_lang")
  public String CODEBLOCK_LANG = "asciidoc";
}
