package ooo.foooooooooooo.velocitydiscord.config.definitions;

import ooo.foooooooooooo.velocitydiscord.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class WebhookConfig {
  private static final Logger logger = LoggerFactory.getLogger(WebhookConfig.class);

  private static final Pattern WEBHOOK_URL_REGEX = Pattern.compile(
    "https?://(?:[^\\s.]+\\.)?discord(?:app)?\\.com/api(?:/v\\d+)?/webhooks/(?<id>\\d+)/(?<token>[^\\s/]+)",
    Pattern.CASE_INSENSITIVE
  );

  /// Full webhook URL to send chat messages to
  public String url = "";

  /// Full URL of an avatar service to get the player's avatar from
  ///
  /// Placeholders available: `uuid`, `username`
  public String avatarUrl = "https://visage.surgeplay.com/face/96/{uuid}";

  /// The format of the webhook's username
  ///
  /// Placeholders available: `username`, `server`
  public String username = "{username}";

  public boolean valid = false;
  public String id;

  public void load(Config config) {
    if (config == null) return;

    this.url = config.getOrDefault("url", this.url);
    this.avatarUrl = config.getOrDefault("avatar_url", this.avatarUrl);
    this.username = config.getOrDefault("username", this.username);

    var matcher = WEBHOOK_URL_REGEX.matcher(this.url);
    this.valid = matcher.matches();

    if (this.valid) {
      this.id = matcher.group("id");
    } else if (!this.url.isEmpty()) {
      logger.warn("Invalid webhook URL: {}", this.url);
    }
  }

  public boolean isInvalid() {
    return this.url.isEmpty() || !this.valid;
  }
}
