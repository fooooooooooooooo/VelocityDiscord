package ooo.foooooooooooo.velocitydiscord.config;

import ooo.foooooooooooo.config.Config;
import ooo.foooooooooooo.config.Key;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class MinecraftConfig extends Config {
  // role prefixes
  public final RolePrefixConfig rolePrefixes;

  // discord
  @Key("show_bot_messages")
  public Boolean SHOW_BOT_MESSAGES = false;
  @Key("show_attachments_ingame")
  public Boolean SHOW_ATTACHMENTS = true;

  @Key(value = "plugin_command", overridable = false)
  public String PLUGIN_COMMAND = "discord";

  // formats
  @Key("discord_chunk")
  public String DISCORD_CHUNK_FORMAT = "<dark_gray>[<{discord_color}>Discord<dark_gray>]<reset>";
  @Key("username_chunk")
  public String USERNAME_CHUNK_FORMAT =
    "<{role_color}><insert:@{username}><hover:show_text:{display_name}>{nickname}</hover></insert><reset>";
  @Key("message")
  public String MESSAGE_FORMAT =
    "{discord_chunk} {role_prefix} {username_chunk}<dark_gray> <dark_gray>»</dark_gray> <reset><gray>{message}</gray>"
      + " {attachments}";
  @Key("attachments")
  public String ATTACHMENT_FORMAT =
    "<dark_gray><click:open_url:{url}>[<{attachment_color}>Attachment<dark_gray>]</click><reset>";
  @Key("links")
  public Optional<String> LINK_FORMAT = Optional.of("<click:open_url:\"{url}\"><hover:show_text:\"{url}\">"
    + "<dark_gray>[</dark_gray><{link_color}>Link<dark_gray>]</hover></click>");

  // colors
  @Key("discord_color")
  public String DISCORD_COLOR = "#7289da";
  @Key("attachment_color")
  public String ATTACHMENT_COLOR = "#4abdff";
  @Key("link_color")
  public String LINK_COLOR = "#4abdff";

  @SuppressWarnings("unused")
  public MinecraftConfig(com.electronwill.nightconfig.core.Config config) {
    super(config);
    this.rolePrefixes = new RolePrefixConfig(config);
    loadConfig();
  }

  @SuppressWarnings("unused")
  public MinecraftConfig(com.electronwill.nightconfig.core.Config config, MinecraftConfig main) {
    super(config, main);
    this.rolePrefixes = new RolePrefixConfig(config, main.rolePrefixes);
    loadConfig();
  }

  @Override
  public void loadConfig() {
    super.loadConfig();

    // Reload role prefixes
    this.rolePrefixes.loadConfig();
  }
}
