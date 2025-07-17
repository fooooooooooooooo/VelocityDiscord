package ooo.foooooooooooo.velocitydiscord.config.definitions;

import ooo.foooooooooooo.velocitydiscord.config.Config;

public enum UserMessageType {
  TEXT, EMBED, WEBHOOK;

  public static UserMessageType get(Config config, String key, UserMessageType defaultValue) {
    var type = config.getOrDefault(key, defaultValue.toString());
    return switch (type) {
      case "text" -> TEXT;
      case "embed" -> EMBED;
      case "webhook" -> WEBHOOK;
      case "" -> defaultValue;
      default -> throw new IllegalArgumentException("Unknown system message type: " + type);
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case TEXT -> "text";
      case EMBED -> "embed";
      case WEBHOOK -> "webhook";
    };
  }
}
