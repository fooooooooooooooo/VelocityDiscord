package ooo.foooooooooooo.velocitydiscord.config;

import ooo.foooooooooooo.velocitydiscord.config.definitions.ChatConfig;
import ooo.foooooooooooo.velocitydiscord.config.definitions.DiscordConfig;
import ooo.foooooooooooo.velocitydiscord.config.definitions.MinecraftConfig;

public interface ServerConfig {
  DiscordConfig getDiscordConfig();

  ChatConfig getChatConfig();

  MinecraftConfig getMinecraftConfig();
}
