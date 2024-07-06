package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.Config;

public class MinecraftMessageConfig extends BaseConfig {
  public MinecraftMessageConfig(Config config) {
    loadConfig(config);
  }

  // discord
  public Boolean SHOW_BOT_MESSAGES = false;
  public Boolean SHOW_ATTACHMENTS = true;

  // formats
  public String DISCORD_CHUNK_FORMAT = "<dark_gray>[<{discord_color}>Discord<dark_gray>]<reset>";
  public String USERNAME_CHUNK_FORMAT = "<{role_color}><insert:@{username}><hover:show_text:{display_name}>{nickname}</hover></insert><reset>";
  public String MESSAGE_FORMAT = "{discord_chunk} {username_chunk}<dark_gray>: <reset>{message} {attachments}";
  public String ATTACHMENT_FORMAT = "<dark_gray><click:open_url:{url}>[<{attachment_color}>Attachment<dark_gray>]</click><reset>";

  // colors
  public String DISCORD_COLOR = "#7289da";
  public String ATTACHMENT_COLOR = "#4abdff";

  @Override
  protected void loadConfig(Config config) {
    // discord
    SHOW_BOT_MESSAGES = get(config, "discord.show_bot_messages", SHOW_BOT_MESSAGES);
    SHOW_ATTACHMENTS = get(config, "discord.show_attachments_ingame", SHOW_ATTACHMENTS);

    // formats
    DISCORD_CHUNK_FORMAT = get(config, "minecraft.discord_chunk", DISCORD_CHUNK_FORMAT);
    USERNAME_CHUNK_FORMAT = get(config, "minecraft.username_chunk", USERNAME_CHUNK_FORMAT);
    MESSAGE_FORMAT = get(config, "minecraft.message", MESSAGE_FORMAT);
    ATTACHMENT_FORMAT = get(config, "minecraft.attachments", ATTACHMENT_FORMAT);

    // colors
    DISCORD_COLOR = get(config, "minecraft.discord_color", DISCORD_COLOR);
    ATTACHMENT_COLOR = get(config, "minecraft.attachment_color", ATTACHMENT_COLOR);
  }
}
