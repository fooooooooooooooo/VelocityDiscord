package ooo.foooooooooooo.velocitydiscord.config.commands;

import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;
import ooo.foooooooooooo.velocitydiscord.config.ConfigConstants;

import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public class ListCommandConfig {
  @SerdeKey("server_format")
  @SerdeDefault(provider = "defaultServerFormat")
  public String SERVER_FORMAT;
  private final transient Supplier<String> defaultServerFormat = () -> "[{server_name} {online_players}/{max_players}]";

  @SerdeKey("player_format")
  @SerdeDefault(provider = "defaultPlayerFormat")
  public String PLAYER_FORMAT;
  private final transient Supplier<String> defaultPlayerFormat = () -> "- {username}";

  @SerdeKey("no_players")
  @SerdeDefault(provider = "defaultNoPlayersFormat")
  public Optional<String> NO_PLAYERS_FORMAT;
  private final transient Supplier<Optional<String>> defaultNoPlayersFormat = () -> Optional.of("No players online");

  @SerdeKey("server_offline")
  @SerdeDefault(provider = "defaultServerOfflineFormat")
  public Optional<String> SERVER_OFFLINE_FORMAT;
  private final transient Supplier<Optional<String>> defaultServerOfflineFormat = () -> Optional.of("Server offline");

  @SerdeKey("codeblock_lang")
  @SerdeDefault(provider = "defaultCodeblockLang")
  public String CODEBLOCK_LANG;
  private final transient Supplier<String> defaultCodeblockLang = () -> "asciidoc";

  public String debug() {
    return "server_format: " + ConfigConstants.debugString(this.SERVER_FORMAT) + "\n"
      + "player_format: " + ConfigConstants.debugString(this.PLAYER_FORMAT) + "\n"
      + "no_players_format: " + this.NO_PLAYERS_FORMAT.map(ConfigConstants::debugString).orElse("null") + "\n"
      + "server_offline_format: " + this.SERVER_OFFLINE_FORMAT.map(ConfigConstants::debugString).orElse("null") + "\n"
      + "codeblock_lang: " + ConfigConstants.debugString(this.CODEBLOCK_LANG) + "\n";
  }
}
