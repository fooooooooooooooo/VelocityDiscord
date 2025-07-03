package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class MinecraftConfig {
  // role prefixes
  @SerdeKey("role_prefixes")
  public RolePrefixConfig rolePrefixes;

  // discord
  @SerdeKey("show_bot_messages")
  public Boolean SHOW_BOT_MESSAGES = false;
  @SerdeKey("show_attachments_ingame")
  public Boolean SHOW_ATTACHMENTS = true;


  // formats
  @SerdeKey("discord_chunk")
  public String DISCORD_CHUNK_FORMAT = "<dark_gray>[<{discord_color}>Discord<dark_gray>]<reset>";
  @SerdeKey("username_chunk")
  public String USERNAME_CHUNK_FORMAT =
    "<{role_color}><insert:@{username}><hover:show_text:{display_name}>{nickname}</hover></insert><reset>";
  @SerdeKey("message")
  public String MESSAGE_FORMAT =
    "{discord_chunk} {role_prefix} {username_chunk}<dark_gray> <dark_gray>»</dark_gray> <reset><gray>{message}</gray>"
      + " {attachments}";
  @SerdeKey("attachments")
  public String ATTACHMENT_FORMAT =
    "<dark_gray><click:open_url:{url}>[<{attachment_color}>Attachment<dark_gray>]</click><reset>";
  @SerdeKey("links")
  public Optional<String> LINK_FORMAT = Optional.of("<click:open_url:\"{url}\"><hover:show_text:\"{url}\">"
    + "<dark_gray>[</dark_gray><{link_color}>Link<dark_gray>]</hover></click>");

  // colors
  @SerdeKey("discord_color")
  public String DISCORD_COLOR = "#7289da";
  @SerdeKey("attachment_color")
  public String ATTACHMENT_COLOR = "#4abdff";
  @SerdeKey("link_color")
  public String LINK_COLOR = "#4abdff";
}
