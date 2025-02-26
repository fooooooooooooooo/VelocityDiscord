package ooo.foooooooooooo.velocitydiscord.config;

import ooo.foooooooooooo.config.Config;
import ooo.foooooooooooo.config.Key;
import ooo.foooooooooooo.config.Variants;

import java.awt.*;
import java.util.Arrays;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DiscordChatConfig extends Config {
  private static final Color RED = new Color(0xbf4040);
  private static final Color GREEN = new Color(0x40bf4f);

  // chat
  @Key("message.format")
  public Optional<String> MESSAGE_FORMAT = Optional.of("{username}: {message}");
  @Key("message.type")
  public UserMessageType MESSAGE_TYPE = UserMessageType.TEXT;
  @Key("message.embed_color")
  public Optional<Color> MESSAGE_EMBED_COLOR = Optional.empty();
  @Key("message.channel")
  public Optional<String> MESSAGE_CHANNEL = Optional.empty();
  @Key("message.webhook")
  public WebhookConfig MESSAGE_WEBHOOK;

  // death
  @Key("death.format")
  public Optional<String> DEATH_FORMAT = Optional.of("**{username} {death_message}**");
  @Key("death.type")
  public UserMessageType DEATH_TYPE = UserMessageType.TEXT;
  @Key("death.embed_color")
  public Optional<Color> DEATH_EMBED_COLOR = Optional.of(RED);
  @Key("death.channel")
  public Optional<String> DEATH_CHANNEL = Optional.empty();
  @Key("death.webhook")
  public WebhookConfig DEATH_WEBHOOK;

  // advancement
  @Key("advancement.format")
  public Optional<String> ADVANCEMENT_FORMAT =
    Optional.of("**{username} has made the advancement __{advancement_title}__**\n_{advancement_description}_");
  @Key("advancement.type")
  public UserMessageType ADVANCEMENT_TYPE = UserMessageType.TEXT;
  @Key("advancement.embed_color")
  public Optional<Color> ADVANCEMENT_EMBED_COLOR = Optional.of(GREEN);
  @Key("advancement.channel")
  public Optional<String> ADVANCEMENT_CHANNEL = Optional.empty();
  @Key("advancement.webhook")
  public WebhookConfig ADVANCEMENT_WEBHOOK;

  // join
  @Key("join.format")
  public Optional<String> JOIN_FORMAT = Optional.of("**{username} joined the game**");
  @Key("join.type")
  public UserMessageType JOIN_TYPE = UserMessageType.TEXT;
  @Key("join.embed_color")
  public Optional<Color> JOIN_EMBED_COLOR = Optional.of(GREEN);
  @Key("join.channel")
  public Optional<String> JOIN_CHANNEL = Optional.empty();
  @Key("join.webhook")
  public WebhookConfig JOIN_WEBHOOK;

  // leave
  @Key("leave.format")
  public Optional<String> LEAVE_FORMAT = Optional.of("**{username} left the game**");
  @Key("leave.type")
  public UserMessageType LEAVE_TYPE = UserMessageType.TEXT;
  @Key("leave.embed_color")
  public Optional<Color> LEAVE_EMBED_COLOR = Optional.of(RED);
  @Key("leave.channel")
  public Optional<String> LEAVE_CHANNEL = Optional.empty();
  @Key("leave.webhook")
  public WebhookConfig LEAVE_WEBHOOK;

  // disconnect
  @Key("disconnect.format")
  public Optional<String> DISCONNECT_FORMAT = Optional.of("**{username} disconnected**");
  @Key("disconnect.type")
  public UserMessageType DISCONNECT_TYPE = UserMessageType.TEXT;
  @Key("disconnect.embed_color")
  public Optional<Color> DISCONNECT_EMBED_COLOR = Optional.of(RED);
  @Key("disconnect.channel")
  public Optional<String> DISCONNECT_CHANNEL = Optional.empty();
  @Key("disconnect.webhook")
  public WebhookConfig DISCONNECT_WEBHOOK;

  // server switch
  @Key("server_switch.format")
  public Optional<String> SERVER_SWITCH_FORMAT = Optional.of("**{username} moved to {current} from {previous}**");
  @Key("server_switch.type")
  public UserMessageType SERVER_SWITCH_TYPE = UserMessageType.TEXT;
  @Key("server_switch.embed_color")
  public Optional<Color> SERVER_SWITCH_EMBED_COLOR = Optional.of(GREEN);
  @Key("server_switch.channel")
  public Optional<String> SERVER_SWITCH_CHANNEL = Optional.empty();
  @Key("server_switch.webhook")
  public WebhookConfig SERVER_SWITCH_WEBHOOK;

  // proxy start
  @Key(value = "proxy_start.format", overridable = false)
  public Optional<String> PROXY_START_FORMAT = Optional.of("**Proxy started**");
  @Key(value = "proxy_start.type", overridable = false)
  public ServerMessageType PROXY_START_TYPE = ServerMessageType.TEXT;
  @Key(value = "proxy_start.embed_color", overridable = false)
  public Optional<Color> PROXY_START_EMBED_COLOR = Optional.of(GREEN);
  @Key(value = "proxy_start.channel", overridable = false)
  public Optional<String> PROXY_START_CHANNEL = Optional.empty();

  // proxy stop
  @Key(value = "proxy_stop.format", overridable = false)
  public Optional<String> PROXY_STOP_FORMAT = Optional.of("**Proxy stopped**");
  @Key(value = "proxy_stop.type", overridable = false)
  public ServerMessageType PROXY_STOP_TYPE = ServerMessageType.TEXT;
  @Key(value = "proxy_stop.embed_color", overridable = false)
  public Optional<Color> PROXY_STOP_EMBED_COLOR = Optional.of(RED);
  @Key(value = "proxy_stop.channel", overridable = false)
  public Optional<String> PROXY_STOP_CHANNEL = Optional.empty();

  // server start
  @Key("server_start.format")
  public Optional<String> SERVER_START_FORMAT = Optional.of("**{server} has started**");
  @Key("server_start.type")
  public ServerMessageType SERVER_START_TYPE = ServerMessageType.TEXT;
  @Key("server_start.embed_color")
  public Optional<Color> SERVER_START_EMBED_COLOR = Optional.of(GREEN);
  @Key("server_start.channel")
  public Optional<String> SERVER_START_CHANNEL = Optional.empty();

  // server stop
  @Key("server_stop.format")
  public Optional<String> SERVER_STOP_FORMAT = Optional.of("**{server} has stopped**");
  @Key("server_stop.type")
  public ServerMessageType SERVER_STOP_TYPE = ServerMessageType.TEXT;
  @Key("server_stop.embed_color")
  public Optional<Color> SERVER_STOP_EMBED_COLOR = Optional.of(RED);
  @Key("server_stop.channel")
  public Optional<String> SERVER_STOP_CHANNEL = Optional.empty();

  @SuppressWarnings("unused")
  public DiscordChatConfig(com.electronwill.nightconfig.core.Config config) {
    super(config);
  }

  @SuppressWarnings("unused")
  public DiscordChatConfig(com.electronwill.nightconfig.core.Config config, DiscordChatConfig main) {
    super(config, main);
  }

  public boolean isWebhookUsed() {
    return Arrays.stream(new UserMessageType[]{
      this.MESSAGE_TYPE,
      this.DEATH_TYPE,
      this.ADVANCEMENT_TYPE,
      this.JOIN_TYPE,
      this.LEAVE_TYPE,
      this.DISCONNECT_TYPE,
      this.SERVER_SWITCH_TYPE
    }).anyMatch(t -> t == UserMessageType.WEBHOOK);
  }

  public WebhookConfig getWebhookConfig(MessageCategory category) {
    return switch (category) {
      case MESSAGE -> this.MESSAGE_WEBHOOK;
      case DEATH -> this.DEATH_WEBHOOK;
      case ADVANCEMENT -> this.ADVANCEMENT_WEBHOOK;
      case JOIN -> this.JOIN_WEBHOOK;
      case LEAVE -> this.LEAVE_WEBHOOK;
      case DISCONNECT -> this.DISCONNECT_WEBHOOK;
      case SERVER_SWITCH -> this.SERVER_SWITCH_WEBHOOK;
    };
  }

  @Variants
  public enum UserMessageType {
    @Variants.Key("text")
    TEXT,
    @Variants.Key("webhook")
    WEBHOOK,
    @Variants.Key("embed")
    EMBED
  }

  @Variants
  public enum ServerMessageType {
    @Variants.Key("text")
    TEXT,
    @Variants.Key("embed")
    EMBED
  }

  public enum MessageCategory {
    MESSAGE, DEATH, ADVANCEMENT, JOIN, LEAVE, DISCONNECT, SERVER_SWITCH,
  }
}
