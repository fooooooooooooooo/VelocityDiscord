package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;

import java.awt.*;
import java.util.Optional;
import java.util.function.Supplier;

import static ooo.foooooooooooo.velocitydiscord.config.ConfigConstants.GREEN;
import static ooo.foooooooooooo.velocitydiscord.config.ConfigConstants.RED;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public class ProxyDiscordChatConfig {
  @SerdeKey("start.format")
  @SerdeDefault(provider = "defaultStartFormat")
  public Optional<String> START_FORMAT;
  private final transient Supplier<Optional<String>> defaultStartFormat = () -> Optional.of("**Proxy started**");

  @SerdeKey("start.type")
  @SerdeDefault(provider = "defaultStartType")
  public DiscordChatConfig.ServerMessageType START_TYPE;
  private final transient Supplier<DiscordChatConfig.ServerMessageType> defaultStartType = () -> DiscordChatConfig.ServerMessageType.TEXT;

  @SerdeKey("start.embed_color")
  @SerdeDefault(provider = "defaultStartEmbedColor")
  public Optional<Color> START_EMBED_COLOR;
  private final transient Supplier<Optional<Color>> defaultStartEmbedColor = () -> Optional.of(GREEN);

  @SerdeKey("start.channel")
  @SerdeDefault(provider = "defaultStartChannel")
  public Optional<String> START_CHANNEL;
  private final transient Supplier<Optional<String>> defaultStartChannel = Optional::empty;

  @SerdeKey("stop.format")
  @SerdeDefault(provider = "defaultStopFormat")
  public Optional<String> STOP_FORMAT;
  private final transient Supplier<Optional<String>> defaultStopFormat = () -> Optional.of("**Proxy stopped**");

  @SerdeKey("stop.type")
  @SerdeDefault(provider = "defaultStopType")
  public DiscordChatConfig.ServerMessageType STOP_TYPE;
  private final transient Supplier<DiscordChatConfig.ServerMessageType> defaultStopType = () -> DiscordChatConfig.ServerMessageType.TEXT;

  @SerdeKey("stop.embed_color")
  @SerdeDefault(provider = "defaultStopEmbedColor")
  public Optional<Color> STOP_EMBED_COLOR;
  private final transient Supplier<Optional<Color>> defaultStopEmbedColor = () -> Optional.of(RED);

  @SerdeKey("stop.channel")
  @SerdeDefault(provider = "defaultStopChannel")
  public Optional<String> STOP_CHANNEL;
  private final transient Supplier<Optional<String>> defaultStopChannel = Optional::empty;

  public String debug() {
    return "start.format: " + this.START_FORMAT.orElse("null") + "\n"
      + "start.type: " + this.START_TYPE + "\n"
      + "start.embed_color: " + this.START_EMBED_COLOR.map(Color::toString).orElse("null") + "\n"
      + "start.channel: " + this.START_CHANNEL.orElse("null") + "\n"
      + "stop.format: " + this.STOP_FORMAT.orElse("null") + "\n"
      + "stop.type: " + this.STOP_TYPE + "\n"
      + "stop.embed_color: " + this.STOP_EMBED_COLOR.map(Color::toString).orElse("null") + "\n"
      + "stop.channel: " + this.STOP_CHANNEL.orElse("null") + "\n";
  }
}
