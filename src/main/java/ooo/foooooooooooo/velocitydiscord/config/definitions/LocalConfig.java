package ooo.foooooooooooo.velocitydiscord.config.definitions;

import ooo.foooooooooooo.velocitydiscord.config.Config;

import java.util.Arrays;

public class LocalConfig {
  public DiscordConfig discord = new DiscordConfig();
  public MinecraftConfig minecraft = new MinecraftConfig();

  public void load(Config config) {
    this.discord.load(config.getConfig("discord"));
  }

  public boolean isWebhookUsed() {
    var chat = this.discord.chat;
    return Arrays.stream(new UserMessageType[]{
      chat.message.type,
      chat.death.type,
      chat.advancement.type,
      chat.join.type,
      chat.leave.type,
      chat.disconnect.type,
      chat.serverSwitch.type,
    }).anyMatch(t -> t == UserMessageType.WEBHOOK);
  }
}
