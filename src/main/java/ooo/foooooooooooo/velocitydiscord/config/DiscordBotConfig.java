package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;

import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public class DiscordBotConfig {
  private static final String DefaultToken = "TOKEN";

  @SerdeKey("token")
  @SerdeDefault(provider = "defaultToken")
  public String TOKEN;
  private final transient Supplier<String> defaultToken = () -> DefaultToken;

  @SerdeKey("activity_format")
  @SerdeDefault(provider = "defaultActivityFormat")
  public Optional<String> ACTIVITY_FORMAT;
  private final transient Supplier<Optional<String>> defaultActivityFormat = () -> Optional.of("with {amount} players online");

  @SerdeKey("update_channel_topic_interval")
  @SerdeDefault(provider = "defaultUpdateChannelTopicInterval")
  public int UPDATE_CHANNEL_TOPIC_INTERVAL_MINUTES;
  private final transient Supplier<Integer> defaultUpdateChannelTopicInterval = () -> 0;

  public boolean updateChannelTopicDisabled() {
    return this.UPDATE_CHANNEL_TOPIC_INTERVAL_MINUTES == 0;
  }

  public boolean isDefaultValues() {
    return this.TOKEN.equals(DefaultToken);
  }

  public String debug() {
    return "token: " + ConfigConstants.debugString(this.TOKEN) + "\n"
      + "activity_format: " + this.ACTIVITY_FORMAT.map(ConfigConstants::debugString).orElse("null") + "\n"
      + "update_channel_topic_interval: " + this.UPDATE_CHANNEL_TOPIC_INTERVAL_MINUTES + "\n";
  }
}
