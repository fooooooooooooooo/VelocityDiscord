package ooo.foooooooooooo.velocitydiscord.discord;

public enum MessageCategory {
  JOIN, SERVER_SWITCH, DISCONNECT, LEAVE, DEATH, ADVANCEMENT, MESSAGE;

  @Override
  public String toString() {
    return switch (this) {
      case JOIN -> "join";
      case SERVER_SWITCH -> "server_switch";
      case DISCONNECT -> "disconnect";
      case LEAVE -> "leave";
      case DEATH -> "death";
      case ADVANCEMENT -> "advancement";
      case MESSAGE -> "message";
    };
  }
}
