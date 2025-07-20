package ooo.foooooooooooo.velocitydiscord.config.definitions;

import ooo.foooooooooooo.velocitydiscord.config.Config;
import ooo.foooooooooooo.velocitydiscord.config.definitions.commands.CommandConfig;
import ooo.foooooooooooo.velocitydiscord.discord.MessageCategory;

import java.util.Arrays;

public class DiscordConfig {
  private static final String DEFAULT_CHANNEL_ID = "000000000000000000";

  /**
   * Default channel ID to send Minecraft chat messages to
   */
  public String mainChannelId = DEFAULT_CHANNEL_ID;

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
  public int updateChannelTopicIntervalMinutes = 0;

  public ChatConfig chat = new ChatConfig();

  public CommandConfig commands = new CommandConfig();
  public ChannelTopicConfig channelTopic = new ChannelTopicConfig();

  public WebhookConfig webhook = new WebhookConfig();

  public void load(Config config) {
    if (config == null) return;

    this.mainChannelId = config.getOrDefault("channel", this.mainChannelId);
    this.showBotMessages = config.getOrDefault("show_bot_messages", this.showBotMessages);
    this.showAttachmentsIngame = config.getOrDefault("show_attachments_ingame", this.showAttachmentsIngame);
    this.enableMentions = config.getOrDefault("enable_mentions", this.enableMentions);
    this.enableEveryoneAndHere = config.getOrDefault("enable_everyone_and_here", this.enableEveryoneAndHere);
    this.updateChannelTopicIntervalMinutes =
      config.getOrDefault("update_channel_topic_interval", this.updateChannelTopicIntervalMinutes);

    this.chat.load(config.getConfig("chat"));

    this.commands.load(config.getConfig("commands"));
    this.channelTopic.load(config.getConfig("channel_topic"));

    this.webhook.load(config.getConfig("webhook"));
  }

  public boolean isWebhookUsed() {
    return Arrays.stream(new UserMessageType[]{
      this.chat.message.type,
      this.chat.death.type,
      this.chat.advancement.type,
      this.chat.join.type,
      this.chat.leave.type,
      this.chat.disconnect.type,
      this.chat.serverSwitch.type,
      }).anyMatch(t -> t == UserMessageType.WEBHOOK);
  }

  public WebhookConfig getWebhookConfig(MessageCategory type) {
    var messageSpecificWebhook = switch (type) {
      case ADVANCEMENT -> this.chat.advancement.webhook;
      case MESSAGE -> this.chat.message.webhook;
      case JOIN -> this.chat.join.webhook;
      case DEATH -> this.chat.death.webhook;
      case LEAVE -> this.chat.leave.webhook;
      case DISCONNECT -> this.chat.disconnect.webhook;
      case SERVER_SWITCH -> this.chat.serverSwitch.webhook;
    };

    return messageSpecificWebhook.orElse(this.webhook);
  }

  public boolean isDefaultChannel() {
    return this.mainChannelId.equals(DEFAULT_CHANNEL_ID);
  }
}
