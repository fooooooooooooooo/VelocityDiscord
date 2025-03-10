package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.Config;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class MinecraftMessageConfig extends BaseConfig {
  // role prefixes
  public final RolePrefixConfig rolePrefixes;

  // discord
  @Key("minecraft.show_bot_messages")
  public Boolean SHOW_BOT_MESSAGES = false;
  @Key("minecraft.show_attachments_ingame")
  public Boolean SHOW_ATTACHMENTS = true;

  // formats
  @Key("minecraft.discord_chunk")
  public String DISCORD_CHUNK_FORMAT = "<dark_gray>[<{discord_color}>Discord<dark_gray>]<reset>";
  @Key("minecraft.username_chunk")
  public String USERNAME_CHUNK_FORMAT =
    "<{role_color}><insert:@{username}><hover:show_text:{display_name}>{nickname}</hover></insert><reset>";
  @Key("minecraft.message")
  public String MESSAGE_FORMAT =
    "{discord_chunk} {role_prefix} {username_chunk}<dark_gray> <dark_gray>»</dark_gray> <reset><gray>{message}</gray>"
      + " {attachments}";
  @Key("minecraft.attachments")
  public String ATTACHMENT_FORMAT =
    "<dark_gray><click:open_url:{url}>[<{attachment_color}>Attachment<dark_gray>]</click><reset>";
  @Key("minecraft.links")
  public Optional<String> LINK_FORMAT = Optional.of("<click:open_url:\"{url}\"><hover:show_text:\"{url}\">"
    + "<dark_gray>[</dark_gray><{link_color}>Link<dark_gray>]</hover></click>");

  // colors
  @Key("minecraft.discord_color")
  public String DISCORD_COLOR = "#7289da";
  @Key("minecraft.attachment_color")
  public String ATTACHMENT_COLOR = "#4abdff";
  @Key("minecraft.link_color")
  public String LINK_COLOR = "#4abdff";

  public MinecraftMessageConfig(Config config) {
    super(config);
    this.rolePrefixes = new RolePrefixConfig(config);
    loadConfig();
  }

  public MinecraftMessageConfig(Config config, MinecraftMessageConfig main) {
    super(config, main);
    this.rolePrefixes = new RolePrefixConfig(config, main.rolePrefixes);
    loadConfig();
  }

  @Override
  protected void loadConfig() {
    super.loadConfig();

    // Reload role prefixes
    this.rolePrefixes.loadConfig();
  }
}
