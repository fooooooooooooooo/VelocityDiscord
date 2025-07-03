package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DiscordBotConfig {
  private static final String DefaultToken = "TOKEN";
  // bot
  @SerdeKey("token")
  public String TOKEN = DefaultToken;

  // bot activity
  @SerdeKey("activity_format")
  public Optional<String> ACTIVITY_FORMAT = Optional.of("with {amount} players online");

  // update channel topic
  @SerdeKey("update_channel_topic_interval")
  public int UPDATE_CHANNEL_TOPIC_INTERVAL_MINUTES = 0;

  public boolean updateChannelTopicDisabled() {
    return this.UPDATE_CHANNEL_TOPIC_INTERVAL_MINUTES == 0;
  }

  public boolean isDefaultValues() {
    return this.TOKEN.equals(DefaultToken);
  }
}
