package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.serde.DeserializerContext;
import com.electronwill.nightconfig.core.serde.TypeConstraint;
import com.electronwill.nightconfig.core.serde.ValueDeserializer;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;

import java.awt.*;
import java.util.Arrays;
import java.util.Optional;

import static ooo.foooooooooooo.velocitydiscord.config.ConfigConstants.GREEN;
import static ooo.foooooooooooo.velocitydiscord.config.ConfigConstants.RED;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DiscordChatConfig {
  // chat
  @SerdeKey("message.format")
  public Optional<String> MESSAGE_FORMAT = Optional.of("{username}: {message}");
  @SerdeKey("message.type")
  public UserMessageType MESSAGE_TYPE = UserMessageType.TEXT;
  @SerdeKey("message.embed_color")
  public Optional<Color> MESSAGE_EMBED_COLOR = Optional.empty();
  @SerdeKey("message.channel")
  public Optional<String> MESSAGE_CHANNEL = Optional.empty();
  @SerdeKey("message.webhook")
  public WebhookConfig MESSAGE_WEBHOOK;

  // death
  @SerdeKey("death.format")
  public Optional<String> DEATH_FORMAT = Optional.of("**{username} {death_message}**");
  @SerdeKey("death.type")
  public UserMessageType DEATH_TYPE = UserMessageType.TEXT;
  @SerdeKey("death.embed_color")
  public Optional<Color> DEATH_EMBED_COLOR = Optional.of(RED);
  @SerdeKey("death.channel")
  public Optional<String> DEATH_CHANNEL = Optional.empty();
  @SerdeKey("death.webhook")
  public WebhookConfig DEATH_WEBHOOK;

  // advancement
  @SerdeKey("advancement.format")
  public Optional<String> ADVANCEMENT_FORMAT =
    Optional.of("**{username} has made the advancement __{advancement_title}__**\n_{advancement_description}_");
  @SerdeKey("advancement.type")
  public UserMessageType ADVANCEMENT_TYPE = UserMessageType.TEXT;
  @SerdeKey("advancement.embed_color")
  public Optional<Color> ADVANCEMENT_EMBED_COLOR = Optional.of(GREEN);
  @SerdeKey("advancement.channel")
  public Optional<String> ADVANCEMENT_CHANNEL = Optional.empty();
  @SerdeKey("advancement.webhook")
  public WebhookConfig ADVANCEMENT_WEBHOOK;

  // join
  @SerdeKey("join.format")
  public Optional<String> JOIN_FORMAT = Optional.of("**{username} joined the game**");
  @SerdeKey("join.type")
  public UserMessageType JOIN_TYPE = UserMessageType.TEXT;
  @SerdeKey("join.embed_color")
  public Optional<Color> JOIN_EMBED_COLOR = Optional.of(GREEN);
  @SerdeKey("join.channel")
  public Optional<String> JOIN_CHANNEL = Optional.empty();
  @SerdeKey("join.webhook")
  public WebhookConfig JOIN_WEBHOOK;

  // leave
  @SerdeKey("leave.format")
  public Optional<String> LEAVE_FORMAT = Optional.of("**{username} left the game**");
  @SerdeKey("leave.type")
  public UserMessageType LEAVE_TYPE = UserMessageType.TEXT;
  @SerdeKey("leave.embed_color")
  public Optional<Color> LEAVE_EMBED_COLOR = Optional.of(RED);
  @SerdeKey("leave.channel")
  public Optional<String> LEAVE_CHANNEL = Optional.empty();
  @SerdeKey("leave.webhook")
  public WebhookConfig LEAVE_WEBHOOK;

  // disconnect
  @SerdeKey("disconnect.format")
  public Optional<String> DISCONNECT_FORMAT = Optional.of("**{username} disconnected**");
  @SerdeKey("disconnect.type")
  public UserMessageType DISCONNECT_TYPE = UserMessageType.TEXT;
  @SerdeKey("disconnect.embed_color")
  public Optional<Color> DISCONNECT_EMBED_COLOR = Optional.of(RED);
  @SerdeKey("disconnect.channel")
  public Optional<String> DISCONNECT_CHANNEL = Optional.empty();
  @SerdeKey("disconnect.webhook")
  public WebhookConfig DISCONNECT_WEBHOOK;

  // server switch
  @SerdeKey("server_switch.format")
  public Optional<String> SERVER_SWITCH_FORMAT = Optional.of("**{username} moved to {current} from {previous}**");
  @SerdeKey("server_switch.type")
  public UserMessageType SERVER_SWITCH_TYPE = UserMessageType.TEXT;
  @SerdeKey("server_switch.embed_color")
  public Optional<Color> SERVER_SWITCH_EMBED_COLOR = Optional.of(GREEN);
  @SerdeKey("server_switch.channel")
  public Optional<String> SERVER_SWITCH_CHANNEL = Optional.empty();
  @SerdeKey("server_switch.webhook")
  public WebhookConfig SERVER_SWITCH_WEBHOOK;

  // server start
  @SerdeKey("server_start.format")
  public Optional<String> SERVER_START_FORMAT = Optional.of("**{server} has started**");
  @SerdeKey("server_start.type")
  public ServerMessageType SERVER_START_TYPE = ServerMessageType.TEXT;
  @SerdeKey("server_start.embed_color")
  public Optional<Color> SERVER_START_EMBED_COLOR = Optional.of(GREEN);
  @SerdeKey("server_start.channel")
  public Optional<String> SERVER_START_CHANNEL = Optional.empty();

  // server stop
  @SerdeKey("server_stop.format")
  public Optional<String> SERVER_STOP_FORMAT = Optional.of("**{server} has stopped**");
  @SerdeKey("server_stop.type")
  public ServerMessageType SERVER_STOP_TYPE = ServerMessageType.TEXT;
  @SerdeKey("server_stop.embed_color")
  public Optional<Color> SERVER_STOP_EMBED_COLOR = Optional.of(RED);
  @SerdeKey("server_stop.channel")
  public Optional<String> SERVER_STOP_CHANNEL = Optional.empty();

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

  public enum UserMessageType implements ValueDeserializer<String, UserMessageType> {
    TEXT,
    WEBHOOK,
    EMBED;

    @Override
    public UserMessageType deserialize(String value, Optional<TypeConstraint> resultType, DeserializerContext ctx) {
      return switch (value) {
        case "text" -> TEXT;
        case "webhook" -> WEBHOOK;
        case "embed" -> EMBED;
        default -> throw new IllegalArgumentException("Unknown UserMessageType: " + value + " expected one of [text, webhook, embed]");
      };
    }
  }


  public enum ServerMessageType implements ValueDeserializer<String, ServerMessageType> {
    TEXT,
    EMBED;

    @Override
    public ServerMessageType deserialize(String value, Optional<TypeConstraint> resultType, DeserializerContext ctx) {
      return switch (value) {
        case "text" -> TEXT;
        case "embed" -> EMBED;
        default -> throw new IllegalArgumentException("Unknown ServerMessageType: " + value + " expected one of [text, embed]");
      };
    }
  }

  public enum MessageCategory {
    MESSAGE, DEATH, ADVANCEMENT, JOIN, LEAVE, DISCONNECT, SERVER_SWITCH,
  }
}
