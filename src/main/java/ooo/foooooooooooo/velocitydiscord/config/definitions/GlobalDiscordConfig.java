package ooo.foooooooooooo.velocitydiscord.config.definitions;


import ooo.foooooooooooo.velocitydiscord.config.Config;
import ooo.foooooooooooo.velocitydiscord.config.definitions.commands.GlobalCommandConfig;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class GlobalDiscordConfig {
  public String token;

  /**
   * Activity text of the bot to show in Discord
   * <p>
   * Placeholders available: <code>{amount}</code>
   */
  public Optional<String> activityText = Optional.of("with {amount} players online");

  /**
   * Set the interval (in minutes) for updating the channel topic
   * <p>
   * Use a value of 0 to disable
   */
  public int updateChannelTopicIntervalMinutes = 0;

  public GlobalChatConfig chat = new GlobalChatConfig();
  public GlobalCommandConfig commands = new GlobalCommandConfig();

  public void load(Config config) {
    if (config == null) return;

    this.token = config.get("token");
    this.activityText = config.getDisableableStringOrDefault("activity_text", this.activityText);
    this.updateChannelTopicIntervalMinutes = config.getOrDefault("update_channel_topic_interval", this.updateChannelTopicIntervalMinutes);

    this.chat.load(config.getConfig("chat"));
    this.commands.load(config.getConfig("commands"));
  }

  public boolean updateChannelTopicEnabled() {
    return this.updateChannelTopicIntervalMinutes > 0;
  }
}
