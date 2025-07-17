package ooo.foooooooooooo.velocitydiscord.config.definitions;

import ooo.foooooooooooo.velocitydiscord.config.Config;
import ooo.foooooooooooo.velocitydiscord.config.definitions.commands.CommandConfig;

public class DiscordConfig {
  /**
   * Default channel ID to send Minecraft chat messages to
   */
  public String defaultChannelId = "000000000000000000";

  /**
   * Show messages from bots in Minecraft chat
   */
  public boolean showBotMessages = false;
  /**
   * Show clickable links for attachments in Minecraft chat
   */
  public boolean showAttachmentsIngame = true;

  /**
   * Enable mentioning Discord users from Minecraft chat
   */
  public boolean enableMentions = true;
  /**
   * Enable @everyone and @here pings from Minecraft chat
   */
  public boolean enableEveryoneAndHere = false;

  /**
   * Interval (in minutes) for updating the channel topic
   */
  public boolean updateChannelTopicIntervalMinutes = false;

  public ChatConfig chat = new ChatConfig();

  public CommandConfig commands = new CommandConfig();
  public ChannelTopicConfig channelTopic = new ChannelTopicConfig();

  public WebhookConfig webhook = new WebhookConfig();

  public void load(Config config) {
    if (config == null) return;

    this.defaultChannelId = config.getOrDefault("default_channel_id", this.defaultChannelId);
    this.showBotMessages = config.getOrDefault("show_bot_messages", this.showBotMessages);
    this.showAttachmentsIngame = config.getOrDefault("show_attachments_ingame", this.showAttachmentsIngame);
    this.enableMentions = config.getOrDefault("enable_mentions", this.enableMentions);
    this.enableEveryoneAndHere = config.getOrDefault("enable_everyone_and_here", this.enableEveryoneAndHere);
    this.updateChannelTopicIntervalMinutes = config.getOrDefault("update_channel_topic_interval_minutes", this.updateChannelTopicIntervalMinutes);

    this.chat.load(config.getConfig("chat"));

    this.commands.load(config.getConfig("commands"));
    this.channelTopic.load(config.getConfig("channel_topic"));

    this.webhook.load(config.getConfig("webhook"));
  }
}
