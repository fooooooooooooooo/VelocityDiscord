package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.Config;
import ooo.foooooooooooo.velocitydiscord.config.commands.ListCommandConfig;

public class BotConfig extends BaseConfig {
  public final ListCommandConfig listCommand;

  public BotConfig(Config config) {
    super(config);
    this.listCommand = new ListCommandConfig(config);
    loadConfig();
  }

  public BotConfig(Config config, BotConfig main) {
    super(config, main);
    this.listCommand = new ListCommandConfig(config, main.listCommand);
    loadConfig();
  }

  @Override
  protected void loadConfig() {
    super.loadConfig();
    this.listCommand.loadConfig();
  }

  private static final String DefaultToken = "TOKEN";
  private static final String DefaultChannelId = "000000000000000000";
  private static final String DefaultWebhookUrl = "";
  private static final String DefaultAvatarUrl = "https://crafatar.com/avatars/{uuid}?overlay";

  // bot
  @Key(value = "discord.token", overridable = false)
  public String DISCORD_TOKEN = DefaultToken;
  @Key("discord.channel")
  public String MAIN_CHANNEL_ID = DefaultChannelId;

  // webhooks
  @Key("discord.webhook.webhook_url")
  public String WEBHOOK_URL = DefaultWebhookUrl;
  @Key("discord.webhook.avatar_url")
  public String WEBHOOK_AVATAR_URL = DefaultAvatarUrl;
  @Key("discord.webhook.webhook_username")
  public String WEBHOOK_USERNAME = "{username}";

  // pings
  @Key("discord.enable_mentions")
  public Boolean ENABLE_MENTIONS = true;
  @Key("discord.enable_everyone_and_here")
  public Boolean ENABLE_EVERYONE_AND_HERE = false;

  // bot activity
  @Key("discord.show_activity")
  public Boolean SHOW_ACTIVITY = true;
  @Key("discord.activity_text")
  public String ACTIVITY_FORMAT = "with {amount} players online";

  // update channel topic
  @Key("discord.update_channel_topic_interval")
  public int UPDATE_CHANNEL_TOPIC_INTERVAL_MINUTES = -1;

  public boolean isDefaultValues() {
    return this.DISCORD_TOKEN.equals(DefaultToken) || this.MAIN_CHANNEL_ID.equals(DefaultChannelId);
  }
}
