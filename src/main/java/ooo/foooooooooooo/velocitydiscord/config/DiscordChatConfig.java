package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.serde.DeserializerContext;
import com.electronwill.nightconfig.core.serde.TypeConstraint;
import com.electronwill.nightconfig.core.serde.ValueDeserializer;
import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;

import java.awt.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

import static ooo.foooooooooooo.velocitydiscord.config.ConfigConstants.GREEN;
import static ooo.foooooooooooo.velocitydiscord.config.ConfigConstants.RED;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public class DiscordChatConfig {
  // chat
  @SerdeKey("message.format")
  @SerdeDefault(provider = "defaultMessageFormat")
  public Optional<String> MESSAGE_FORMAT;
  private final transient Supplier<Optional<String>> defaultMessageFormat = () -> Optional.of("{username}: {message}");

  @SerdeKey("message.type")
  @SerdeDefault(provider = "defaultMessageType")
  public UserMessageType MESSAGE_TYPE;
  private final transient Supplier<UserMessageType> defaultMessageType = () -> UserMessageType.TEXT;

  @SerdeKey("message.embed_color")
  @SerdeDefault(provider = "defaultMessageEmbedColor")
  public Optional<Color> MESSAGE_EMBED_COLOR;
  private final transient Supplier<Optional<Color>> defaultMessageEmbedColor = Optional::empty;

  @SerdeKey("message.channel")
  @SerdeDefault(provider = "defaultMessageChannel")
  public Optional<String> MESSAGE_CHANNEL;
  private final transient Supplier<Optional<String>> defaultMessageChannel = Optional::empty;

  @SerdeKey("message.webhook")
  @SerdeDefault(provider = "defaultMessageWebhook")
  public WebhookConfig MESSAGE_WEBHOOK;

  private final transient Supplier<WebhookConfig> defaultMessageWebhook = () -> EmptyConfig.deserialize(new WebhookConfig());

  // death
  @SerdeKey("death.format")
  @SerdeDefault(provider = "defaultDeathFormat")
  public Optional<String> DEATH_FORMAT;
  private final transient Supplier<Optional<String>> defaultDeathFormat = () -> Optional.of("**{username} {death_message}**");

  @SerdeKey("death.type")
  @SerdeDefault(provider = "defaultDeathType")
  public UserMessageType DEATH_TYPE;
  private final transient Supplier<UserMessageType> defaultDeathType = () -> UserMessageType.TEXT;

  @SerdeKey("death.embed_color")
  @SerdeDefault(provider = "defaultDeathEmbedColor")
  public Optional<Color> DEATH_EMBED_COLOR;
  private final transient Supplier<Optional<Color>> defaultDeathEmbedColor = () -> Optional.of(RED);

  @SerdeKey("death.channel")
  @SerdeDefault(provider = "defaultDeathChannel")
  public Optional<String> DEATH_CHANNEL;
  private final transient Supplier<Optional<String>> defaultDeathChannel = Optional::empty;

  @SerdeKey("death.webhook")
  @SerdeDefault(provider = "defaultDeathWebhook")
  public WebhookConfig DEATH_WEBHOOK;
  private final transient Supplier<WebhookConfig> defaultDeathWebhook = () -> EmptyConfig.deserialize(new WebhookConfig());

  // advancement
  @SerdeKey("advancement.format")
  @SerdeDefault(provider = "defaultAdvancementFormat")
  public Optional<String> ADVANCEMENT_FORMAT;
  private final transient Supplier<Optional<String>> defaultAdvancementFormat = () ->
    Optional.of("**{username} has made the advancement __{advancement_title}__**\n_{advancement_description}_");

  @SerdeKey("advancement.type")
  @SerdeDefault(provider = "defaultAdvancementType")
  public UserMessageType ADVANCEMENT_TYPE;
  private final transient Supplier<UserMessageType> defaultAdvancementType = () -> UserMessageType.TEXT;

  @SerdeKey("advancement.embed_color")
  @SerdeDefault(provider = "defaultAdvancementEmbedColor")
  public Optional<Color> ADVANCEMENT_EMBED_COLOR;
  private final transient Supplier<Optional<Color>> defaultAdvancementEmbedColor = () -> Optional.of(GREEN);

  @SerdeKey("advancement.channel")
  @SerdeDefault(provider = "defaultAdvancementChannel")
  public Optional<String> ADVANCEMENT_CHANNEL;
  private final transient Supplier<Optional<String>> defaultAdvancementChannel = Optional::empty;

  @SerdeKey("advancement.webhook")
  @SerdeDefault(provider = "defaultAdvancementWebhook")
  public WebhookConfig ADVANCEMENT_WEBHOOK;
  private final transient Supplier<WebhookConfig> defaultAdvancementWebhook = () -> EmptyConfig.deserialize(new WebhookConfig());

  // join
  @SerdeKey("join.format")
  @SerdeDefault(provider = "defaultJoinFormat")
  public Optional<String> JOIN_FORMAT;
  private final transient Supplier<Optional<String>> defaultJoinFormat = () -> Optional.of("**{username} joined the game**");

  @SerdeKey("join.type")
  @SerdeDefault(provider = "defaultJoinType")
  public UserMessageType JOIN_TYPE;
  private final transient Supplier<UserMessageType> defaultJoinType = () -> UserMessageType.TEXT;

  @SerdeKey("join.embed_color")
  @SerdeDefault(provider = "defaultJoinEmbedColor")
  public Optional<Color> JOIN_EMBED_COLOR;
  private final transient Supplier<Optional<Color>> defaultJoinEmbedColor = () -> Optional.of(GREEN);

  @SerdeKey("join.channel")
  @SerdeDefault(provider = "defaultJoinChannel")
  public Optional<String> JOIN_CHANNEL;
  private final transient Supplier<Optional<String>> defaultJoinChannel = Optional::empty;

  @SerdeKey("join.webhook")
  @SerdeDefault(provider = "defaultJoinWebhook")
  public WebhookConfig JOIN_WEBHOOK;
  private final transient Supplier<WebhookConfig> defaultJoinWebhook = () -> EmptyConfig.deserialize(new WebhookConfig());

  // leave
  @SerdeKey("leave.format")
  @SerdeDefault(provider = "defaultLeaveFormat")
  public Optional<String> LEAVE_FORMAT;
  private final transient Supplier<Optional<String>> defaultLeaveFormat = () -> Optional.of("**{username} left the game**");

  @SerdeKey("leave.type")
  @SerdeDefault(provider = "defaultLeaveType")
  public UserMessageType LEAVE_TYPE;
  private final transient Supplier<UserMessageType> defaultLeaveType = () -> UserMessageType.TEXT;

  @SerdeKey("leave.embed_color")
  @SerdeDefault(provider = "defaultLeaveEmbedColor")
  public Optional<Color> LEAVE_EMBED_COLOR;
  private final transient Supplier<Optional<Color>> defaultLeaveEmbedColor = () -> Optional.of(RED);

  @SerdeKey("leave.channel")
  @SerdeDefault(provider = "defaultLeaveChannel")
  public Optional<String> LEAVE_CHANNEL;
  private final transient Supplier<Optional<String>> defaultLeaveChannel = Optional::empty;

  @SerdeKey("leave.webhook")
  @SerdeDefault(provider = "defaultLeaveWebhook")
  public WebhookConfig LEAVE_WEBHOOK;
  private final transient Supplier<WebhookConfig> defaultLeaveWebhook = () -> EmptyConfig.deserialize(new WebhookConfig());

  // disconnect
  @SerdeKey("disconnect.format")
  @SerdeDefault(provider = "defaultDisconnectFormat")
  public Optional<String> DISCONNECT_FORMAT;
  private final transient Supplier<Optional<String>> defaultDisconnectFormat = () -> Optional.of("**{username} disconnected**");

  @SerdeKey("disconnect.type")
  @SerdeDefault(provider = "defaultDisconnectType")
  public UserMessageType DISCONNECT_TYPE;
  private final transient Supplier<UserMessageType> defaultDisconnectType = () -> UserMessageType.TEXT;

  @SerdeKey("disconnect.embed_color")
  @SerdeDefault(provider = "defaultDisconnectEmbedColor")
  public Optional<Color> DISCONNECT_EMBED_COLOR;
  private final transient Supplier<Optional<Color>> defaultDisconnectEmbedColor = () -> Optional.of(RED);

  @SerdeKey("disconnect.channel")
  @SerdeDefault(provider = "defaultDisconnectChannel")
  public Optional<String> DISCONNECT_CHANNEL;
  private final transient Supplier<Optional<String>> defaultDisconnectChannel = Optional::empty;

  @SerdeKey("disconnect.webhook")
  @SerdeDefault(provider = "defaultDisconnectWebhook")
  public WebhookConfig DISCONNECT_WEBHOOK;
  private final transient Supplier<WebhookConfig> defaultDisconnectWebhook = () -> EmptyConfig.deserialize(new WebhookConfig());

  // server switch
  @SerdeKey("server_switch.format")
  @SerdeDefault(provider = "defaultServerSwitchFormat")
  public Optional<String> SERVER_SWITCH_FORMAT;
  private final transient Supplier<Optional<String>> defaultServerSwitchFormat = () -> Optional.of("**{username} moved to {current} from {previous}**");

  @SerdeKey("server_switch.type")
  @SerdeDefault(provider = "defaultServerSwitchType")
  public UserMessageType SERVER_SWITCH_TYPE;
  private final transient Supplier<UserMessageType> defaultServerSwitchType = () -> UserMessageType.TEXT;

  @SerdeKey("server_switch.embed_color")
  @SerdeDefault(provider = "defaultServerSwitchEmbedColor")
  public Optional<Color> SERVER_SWITCH_EMBED_COLOR;
  private final transient Supplier<Optional<Color>> defaultServerSwitchEmbedColor = () -> Optional.of(GREEN);

  @SerdeKey("server_switch.channel")
  @SerdeDefault(provider = "defaultServerSwitchChannel")
  public Optional<String> SERVER_SWITCH_CHANNEL;
  private final transient Supplier<Optional<String>> defaultServerSwitchChannel = Optional::empty;

  @SerdeKey("server_switch.webhook")
  @SerdeDefault(provider = "defaultServerSwitchWebhook")
  public WebhookConfig SERVER_SWITCH_WEBHOOK;
  private final transient Supplier<WebhookConfig> defaultServerSwitchWebhook = () -> EmptyConfig.deserialize(new WebhookConfig());

  // server start
  @SerdeKey("server_start.format")
  @SerdeDefault(provider = "defaultServerStartFormat")
  public Optional<String> SERVER_START_FORMAT;
  private final transient Supplier<Optional<String>> defaultServerStartFormat = () -> Optional.of("**{server} has started**");

  @SerdeKey("server_start.type")
  @SerdeDefault(provider = "defaultServerStartType")
  public ServerMessageType SERVER_START_TYPE;
  private final transient Supplier<ServerMessageType> defaultServerStartType = () -> ServerMessageType.TEXT;

  @SerdeKey("server_start.embed_color")
  @SerdeDefault(provider = "defaultServerStartEmbedColor")
  public Optional<Color> SERVER_START_EMBED_COLOR;
  private final transient Supplier<Optional<Color>> defaultServerStartEmbedColor = () -> Optional.of(GREEN);

  @SerdeKey("server_start.channel")
  @SerdeDefault(provider = "defaultServerStartChannel")
  public Optional<String> SERVER_START_CHANNEL;
  private final transient Supplier<Optional<String>> defaultServerStartChannel = Optional::empty;

  // server stop
  @SerdeKey("server_stop.format")
  @SerdeDefault(provider = "defaultServerStopFormat")
  public Optional<String> SERVER_STOP_FORMAT;
  private final transient Supplier<Optional<String>> defaultServerStopFormat = () -> Optional.of("**{server} has stopped**");

  @SerdeKey("server_stop.type")
  @SerdeDefault(provider = "defaultServerStopType")
  public ServerMessageType SERVER_STOP_TYPE;
  private final transient Supplier<ServerMessageType> defaultServerStopType = () -> ServerMessageType.TEXT;

  @SerdeKey("server_stop.embed_color")
  @SerdeDefault(provider = "defaultServerStopEmbedColor")
  public Optional<Color> SERVER_STOP_EMBED_COLOR;
  private final transient Supplier<Optional<Color>> defaultServerStopEmbedColor = () -> Optional.of(RED);

  @SerdeKey("server_stop.channel")
  @SerdeDefault(provider = "defaultServerStopChannel")
  public Optional<String> SERVER_STOP_CHANNEL;
  private final transient Supplier<Optional<String>> defaultServerStopChannel = Optional::empty;

  public String debug() {
    var sb = new StringBuilder();

    sb.append("message:\n");
    sb.append("  format: ").append(this.MESSAGE_FORMAT.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  type: ").append(ConfigConstants.debugString(this.MESSAGE_TYPE.toString())).append("\n");
    sb.append("  embed_color: ").append(this.MESSAGE_EMBED_COLOR.map(Color::toString).map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  channel: ").append(this.MESSAGE_CHANNEL.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  webhook:\n");
    for (var line : this.MESSAGE_WEBHOOK.debug().split("\n")) {
      sb.append("    ").append(line).append("\n");
    }

    sb.append("death:\n");
    sb.append("  format: ").append(this.DEATH_FORMAT.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  type: ").append(ConfigConstants.debugString(this.DEATH_TYPE.toString())).append("\n");
    sb.append("  embed_color: ").append(this.DEATH_EMBED_COLOR.map(Color::toString).map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  channel: ").append(this.DEATH_CHANNEL.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  webhook:\n");
    for (var line : this.DEATH_WEBHOOK.debug().split("\n")) {
      sb.append("    ").append(line).append("\n");
    }

    sb.append("advancement:\n");
    sb.append("  format: ").append(this.ADVANCEMENT_FORMAT.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  type: ").append(ConfigConstants.debugString(this.ADVANCEMENT_TYPE.toString())).append("\n");
    sb.append("  embed_color: ").append(this.ADVANCEMENT_EMBED_COLOR.map(Color::toString).map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  channel: ").append(this.ADVANCEMENT_CHANNEL.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  webhook:\n");
    for (var line : this.ADVANCEMENT_WEBHOOK.debug().split("\n")) {
      sb.append("    ").append(line).append("\n");
    }

    sb.append("join:\n");
    sb.append("  format: ").append(this.JOIN_FORMAT.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  type: ").append(ConfigConstants.debugString(this.JOIN_TYPE.toString())).append("\n");
    sb.append("  embed_color: ").append(this.JOIN_EMBED_COLOR.map(Color::toString).map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  channel: ").append(this.JOIN_CHANNEL.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  webhook:\n");
    for (var line : this.JOIN_WEBHOOK.debug().split("\n")) {
      sb.append("    ").append(line).append("\n");
    }

    sb.append("leave:\n");
    sb.append("  format: ").append(this.LEAVE_FORMAT.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  type: ").append(ConfigConstants.debugString(this.LEAVE_TYPE.toString())).append("\n");
    sb.append("  embed_color: ").append(this.LEAVE_EMBED_COLOR.map(Color::toString).map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  channel: ").append(this.LEAVE_CHANNEL.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  webhook:\n");
    for (var line : this.LEAVE_WEBHOOK.debug().split("\n")) {
      sb.append("    ").append(line).append("\n");
    }

    sb.append("disconnect:\n");
    sb.append("  format: ").append(this.DISCONNECT_FORMAT.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  type: ").append(ConfigConstants.debugString(this.DISCONNECT_TYPE.toString())).append("\n");
    sb.append("  embed_color: ").append(this.DISCONNECT_EMBED_COLOR.map(Color::toString).map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  channel: ").append(this.DISCONNECT_CHANNEL.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  webhook:\n");
    for (var line : this.DISCONNECT_WEBHOOK.debug().split("\n")) {
      sb.append("    ").append(line).append("\n");
    }

    sb.append("server_switch:\n");
    sb.append("  format: ").append(this.SERVER_SWITCH_FORMAT.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  type: ").append(ConfigConstants.debugString(this.SERVER_SWITCH_TYPE.toString())).append("\n");
    sb.append("  embed_color: ").append(this.SERVER_SWITCH_EMBED_COLOR.map(Color::toString).map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  channel: ").append(this.SERVER_SWITCH_CHANNEL.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  webhook:\n");
    for (var line : this.SERVER_SWITCH_WEBHOOK.debug().split("\n")) {
      sb.append("    ").append(line).append("\n");
    }

    sb.append("server_start:\n");
    sb.append("  format: ").append(this.SERVER_START_FORMAT.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  type: ").append(ConfigConstants.debugString(this.SERVER_START_TYPE.toString())).append("\n");
    sb.append("  embed_color: ").append(this.SERVER_START_EMBED_COLOR.map(Color::toString).map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  channel: ").append(this.SERVER_START_CHANNEL.map(ConfigConstants::debugString).orElse("null")).append("\n");

    sb.append("server_stop:\n");
    sb.append("  format: ").append(this.SERVER_STOP_FORMAT.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  type: ").append(ConfigConstants.debugString(this.SERVER_STOP_TYPE.toString())).append("\n");
    sb.append("  embed_color: ").append(this.SERVER_STOP_EMBED_COLOR.map(Color::toString).map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("  channel: ").append(this.SERVER_STOP_CHANNEL.map(ConfigConstants::debugString).orElse("null")).append("\n");
    sb.append("\n");

    return sb.toString();
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

  public enum UserMessageType {
    TEXT,
    WEBHOOK,
    EMBED;

    @Override
    public String toString() {
      return switch (this) {
        case TEXT -> "text";
        case WEBHOOK -> "webhook";
        case EMBED -> "embed";
      };
    }
  }

  public static class UserMessageTypeDeserializer implements ValueDeserializer<String, UserMessageType> {
    @Override
    public UserMessageType deserialize(String value, Optional<TypeConstraint> resultType, DeserializerContext ctx) {
      return switch (value) {
        case "text" -> UserMessageType.TEXT;
        case "webhook" -> UserMessageType.WEBHOOK;
        case "embed" -> UserMessageType.EMBED;
        default -> throw new IllegalArgumentException("Unknown UserMessageType: " + value + " expected one of [text, webhook, embed]");
      };
    }
  }

  public enum ServerMessageType {
    TEXT,
    EMBED;

    @Override
    public String toString() {
      return switch (this) {
        case TEXT -> "text";
        case EMBED -> "embed";
      };
    }
  }

  public static class ServerMessageTypeDeserializer implements ValueDeserializer<String, ServerMessageType> {
    @Override
    public ServerMessageType deserialize(String value, Optional<TypeConstraint> resultType, DeserializerContext ctx) {
      return switch (value) {
        case "text" -> ServerMessageType.TEXT;
        case "embed" -> ServerMessageType.EMBED;
        default -> throw new IllegalArgumentException("Unknown ServerMessageType: " + value + " expected one of [text, embed]");
      };
    }
  }

  public enum MessageCategory {
    MESSAGE, DEATH, ADVANCEMENT, JOIN, LEAVE, DISCONNECT, SERVER_SWITCH,
  }
}
