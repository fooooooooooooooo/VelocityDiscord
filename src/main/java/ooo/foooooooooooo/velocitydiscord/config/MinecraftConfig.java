package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;

import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public class MinecraftConfig {
  // role prefixes
  @SerdeKey("role_prefixes")
  @SerdeDefault(provider = "defaultRolePrefixes")
  public RolePrefixConfig rolePrefixes;
  private final transient Supplier<RolePrefixConfig> defaultRolePrefixes = RolePrefixConfig::new;

  // discord
  @SerdeKey("show_bot_messages")
  @SerdeDefault(provider = "defaultShowBotMessages")
  public Boolean SHOW_BOT_MESSAGES;
  private final transient Supplier<Boolean> defaultShowBotMessages = () -> false;
  @SerdeKey("show_attachments_ingame")
  @SerdeDefault(provider = "defaultShowAttachments")
  public Boolean SHOW_ATTACHMENTS;
  private final transient Supplier<Boolean> defaultShowAttachments = () -> true;

  // formats
  @SerdeKey("discord_chunk")
  @SerdeDefault(provider = "defaultDiscordChunkFormat")
  public String DISCORD_CHUNK_FORMAT;
  private final transient Supplier<String> defaultDiscordChunkFormat = () -> "<dark_gray>[<{discord_color}>Discord<dark_gray>]<reset>";
  @SerdeKey("username_chunk")
  @SerdeDefault(provider = "defaultUsernameChunkFormat")
  public String USERNAME_CHUNK_FORMAT;
  private final transient Supplier<String> defaultUsernameChunkFormat = () -> "<{role_color}><insert:@{username}><hover:show_text:{display_name}>{nickname}</hover></insert"
    + "><reset>";
  @SerdeKey("message")
  @SerdeDefault(provider = "defaultMessageFormat")
  public String MESSAGE_FORMAT;
  private final transient Supplier<String> defaultMessageFormat = () ->
    "{discord_chunk} {role_prefix} {username_chunk}<dark_gray> <dark_gray>»</dark_gray> <reset><gray>{message}</gray>"
      + " {attachments}";
  @SerdeKey("attachments")
  @SerdeDefault(provider = "defaultAttachmentFormat")
  public String ATTACHMENT_FORMAT;
  private final transient Supplier<String> defaultAttachmentFormat = () -> "<dark_gray><click:open_url:{url}>[<{attachment_color}>Attachment<dark_gray>]</click><reset>";
  @SerdeKey("links")
  @SerdeDefault(provider = "defaultLinkFormat")
  public Optional<String> LINK_FORMAT;
  private final transient Supplier<Optional<String>> defaultLinkFormat = () -> Optional.of("<click:open_url:\"{url}\"><hover:show_text:\"{url}\">"
    + "<dark_gray>[</dark_gray><{link_color}>Link<dark_gray>]</hover></click>");

  // colors
  @SerdeKey("discord_color")
  @SerdeDefault(provider = "defaultDiscordColor")
  public String DISCORD_COLOR;
  private final transient Supplier<String> defaultDiscordColor = () -> "#7289da";
  @SerdeKey("attachment_color")
  @SerdeDefault(provider = "defaultAttachmentColor")
  public String ATTACHMENT_COLOR;
  private final transient Supplier<String> defaultAttachmentColor = () -> "#4abdff";
  @SerdeKey("link_color")
  @SerdeDefault(provider = "defaultLinkColor")
  public String LINK_COLOR;
  private final transient Supplier<String> defaultLinkColor = () -> "#4abdff";

  public String debug() {
    return "role_prefixes: " + this.rolePrefixes.debug() + "\n"
      + "show_bot_messages: " + this.SHOW_BOT_MESSAGES + "\n"
      + "show_attachments_ingame: " + this.SHOW_ATTACHMENTS + "\n"
      + "discord_chunk_format: " + ConfigConstants.debugString(this.DISCORD_CHUNK_FORMAT) + "\n"
      + "username_chunk_format: " + (this.USERNAME_CHUNK_FORMAT) + "\n"
      + "message_format: " + ConfigConstants.debugString(this.MESSAGE_FORMAT) + "\n"
      + "attachment_format: " + ConfigConstants.debugString(this.ATTACHMENT_FORMAT) + "\n"
      + "link_format: " + this.LINK_FORMAT.map(ConfigConstants::debugString).orElse("null") + "\n"
      + "discord_color: " + ConfigConstants.debugString(this.DISCORD_COLOR) + "\n"
      + "attachment_color: " + ConfigConstants.debugString(this.ATTACHMENT_COLOR) + "\n"
      + "link_color: " + ConfigConstants.debugString(this.LINK_COLOR);
  }
}
