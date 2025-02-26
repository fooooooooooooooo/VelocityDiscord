package ooo.foooooooooooo.velocitydiscord.config;

import ooo.foooooooooooo.config.Config;
import ooo.foooooooooooo.config.Key;

import java.util.regex.Pattern;

public class WebhookConfig extends Config {
  private static final Pattern WEBHOOK_URL_REGEX = Pattern.compile("https?://.*\\.?discord.com/api/webhooks/(\\d*)/.*");

  @Key("url")
  public String URL = "";
  @Key("avatar_url")
  public String AVATAR_URL = "https://visage.surgeplay.com/face/96/{uuid}";
  @Key("username")
  public String USERNAME = "{username}";

  public String webhookId = null;

  @SuppressWarnings("unused")
  public WebhookConfig(com.electronwill.nightconfig.core.Config config) {
    super(config);
  }

  @SuppressWarnings("unused")
  public WebhookConfig(com.electronwill.nightconfig.core.Config config, WebhookConfig main) {
    super(config, main);
  }

  @Override
  public void loadConfig() {
    super.loadConfig();

    var matcher = WEBHOOK_URL_REGEX.matcher(this.URL);
    if (matcher.matches()) {
      this.webhookId = matcher.group(1);
    } else {
      if (!this.URL.isEmpty()) {
        this.getLogger().warn("Invalid webhook URL: {}", this.URL);
      }

      this.webhookId = null;
    }
  }

  public boolean invalid() {
    return this.URL.isEmpty() || this.webhookId == null;
  }
}
