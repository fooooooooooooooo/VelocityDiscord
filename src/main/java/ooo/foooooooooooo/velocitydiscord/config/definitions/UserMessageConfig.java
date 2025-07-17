package ooo.foooooooooooo.velocitydiscord.config.definitions;

import ooo.foooooooooooo.velocitydiscord.config.Config;

import java.awt.*;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class UserMessageConfig {
  public UserMessageType type = UserMessageType.TEXT;
  public Optional<String> channelId = Optional.empty();

  public Optional<String> format;
  public Optional<Color> embedColor;

  public Optional<WebhookConfig> webhook = Optional.empty();

  public UserMessageConfig(String format, Color embedColor) {
    this.format = Optional.ofNullable(format);
    this.embedColor = Optional.ofNullable(embedColor);
  }

  public void load(Config config) {
    if (config == null) return;

    this.type = UserMessageType.get(config, "type", this.type);
    this.channelId = config.getDisableableStringOrDefault("channel", this.channelId);

    this.format = config.getDisableableStringOrDefault("format", this.format);
    this.embedColor = config.getDisableableColorOrDefault("embed_color", this.embedColor);

    var webhookConfig = config.getConfig("webhook");

    if (webhookConfig == null) {
      this.webhook = Optional.empty();
    } else {
      this.webhook = Optional.of(new WebhookConfig());
      this.webhook.get().load(webhookConfig);
    }
  }
}
