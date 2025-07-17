package ooo.foooooooooooo.velocitydiscord.config.definitions;

import ooo.foooooooooooo.velocitydiscord.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class MinecraftConfig {
  /// Placeholders available: `discord`
  public String discordChunkFormat = "<dark_gray>[<{discord_color}>Discord<dark_gray>]<reset>";

  /// Placeholders available: `role_color`, `display_name`, `username`, `nickname`
  ///
  /// `<insert>` tag allows you to shift right-click the username to insert `@username` in the chat
  public String usernameChunkFormat = "<{role_color}><insert:@{username}><hover:show_text:{display_name}>{nickname}</hover></insert><reset>";

  /// Placeholders available: {discord_chunk}, {username_chunk}, {attachments}, {message}
  public String messageFormat = "{discord_chunk} {role_prefix} {username_chunk}<dark_gray>: <reset>{message} {attachments}";

  /// Placeholders available: `url`, `attachment_color`
  public String attachmentFormat = "<dark_gray><click:open_url:{url}>[<{attachment_color}>Attachment<dark_gray>]</click><reset>";

  /// Placeholders available: `url`, `link_color`
  public Optional<String> linkFormat = Optional.of(
    "<click:open_url:\"{url}\"><hover:show_text:\"Click to open {url}\"><dark_gray>[</dark_gray><{link_color}>Link<dark_gray>]</hover></click>");

  public String discordColor = "#7289da";
  public String attachmentColor = "#4abdff";
  public String linkColor = "#4abdff";

  public Map<String, String> rolePrefixes = new HashMap<>();

  public void load(Config config) {
    if (config == null) return;

    this.discordChunkFormat = config.getOrDefault("discord_chunk_format", this.discordChunkFormat);
    this.usernameChunkFormat = config.getOrDefault("username_chunk_format", this.usernameChunkFormat);
    this.messageFormat = config.getOrDefault("message_format", this.messageFormat);
    this.attachmentFormat = config.getOrDefault("attachment_format", this.attachmentFormat);
    this.linkFormat = config.getDisableableStringOrDefault("link_format", this.linkFormat);

    this.discordColor = config.getOrDefault("discord_color", this.discordColor);
    this.attachmentColor = config.getOrDefault("attachment_color", this.attachmentColor);
    this.linkColor = config.getOrDefault("link_color", this.linkColor);

    this.rolePrefixes = config.getOrDefault("role_prefixes", new HashMap<>());
  }
}
