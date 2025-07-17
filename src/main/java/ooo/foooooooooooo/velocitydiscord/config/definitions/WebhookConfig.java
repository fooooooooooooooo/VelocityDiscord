package ooo.foooooooooooo.velocitydiscord.config.definitions;

import ooo.foooooooooooo.velocitydiscord.config.Config;

public class WebhookConfig {
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

  public void load(Config config) {
    if (config == null) return;

    this.url = config.getOrDefault("url", this.url);
    this.avatarUrl = config.getOrDefault("avatar_url", this.avatarUrl);
    this.username = config.getOrDefault("username", this.username);

    // todo: validate url
  }
}
