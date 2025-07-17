package ooo.foooooooooooo.velocitydiscord.config.definitions;

import ooo.foooooooooooo.velocitydiscord.config.Config;

import java.awt.*;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class SystemMessageConfig {
  public SystemMessageType type = SystemMessageType.TEXT;
  public Optional<String> channel = Optional.empty();

  public Optional<String> format;
  public Optional<Color> embedColor;

  public SystemMessageConfig(String format, Color embedColor) {
    this.format = Optional.ofNullable(format);
    this.embedColor = Optional.ofNullable(embedColor);
  }

  public void load(Config config) {
    if (config == null) return;

    this.type = SystemMessageType.get(config, "type", this.type);
    this.channel = config.getDisableableStringOrDefault("channel", this.channel);

    this.format = config.getDisableableStringOrDefault("format", this.format);
    this.embedColor = config.getDisableableColorOrDefault("embed_color", this.embedColor);
  }
}

