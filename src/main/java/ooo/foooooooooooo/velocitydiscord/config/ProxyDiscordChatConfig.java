package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;

import java.awt.*;
import java.util.Optional;

import static ooo.foooooooooooo.velocitydiscord.config.ConfigConstants.GREEN;
import static ooo.foooooooooooo.velocitydiscord.config.ConfigConstants.RED;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ProxyDiscordChatConfig {
  // proxy start
  @SerdeKey("start.format")
  public Optional<String> START_FORMAT = Optional.of("**Proxy started**");
  @SerdeKey("start.type")
  public DiscordChatConfig.ServerMessageType START_TYPE = DiscordChatConfig.ServerMessageType.TEXT;
  @SerdeKey("start.embed_color")
  public Optional<Color> START_EMBED_COLOR = Optional.of(GREEN);
  @SerdeKey("start.channel")
  public Optional<String> START_CHANNEL = Optional.empty();

  // proxy stop
  @SerdeKey("stop.format")
  public Optional<String> STOP_FORMAT = Optional.of("**Proxy stopped**");
  @SerdeKey("stop.type")
  public DiscordChatConfig.ServerMessageType STOP_TYPE = DiscordChatConfig.ServerMessageType.TEXT;
  @SerdeKey("stop.embed_color")
  public Optional<Color> STOP_EMBED_COLOR = Optional.of(RED);
  @SerdeKey("stop.channel")
  public Optional<String> STOP_CHANNEL = Optional.empty();
}
