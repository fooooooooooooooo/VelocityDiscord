package ooo.foooooooooooo.velocitydiscord.config.definitions;

import ooo.foooooooooooo.velocitydiscord.config.Config;
import ooo.foooooooooooo.velocitydiscord.discord.MessageCategory;

import java.util.ArrayList;

public class LocalConfig {
  public DiscordConfig discord = new DiscordConfig();
  public MinecraftConfig minecraft = new MinecraftConfig();

  public void load(Config config) {
    this.discord.load(config.getConfig("discord"));
    this.minecraft.load(config.getConfig("minecraft"));
  }


  public String checkErrors() {
    if (this.discord.isWebhookUsed() && this.discord.webhook.isInvalid()) {
      var invalidCategories = new ArrayList<MessageCategory>();

      var chat = this.discord.chat;

      // check each message category
      if (chat.message.isInvalidWebhook()) invalidCategories.add(MessageCategory.MESSAGE);
      if (chat.join.isInvalidWebhook()) invalidCategories.add(MessageCategory.JOIN);
      if (chat.leave.isInvalidWebhook()) invalidCategories.add(MessageCategory.LEAVE);
      if (chat.disconnect.isInvalidWebhook()) invalidCategories.add(MessageCategory.DISCONNECT);
      if (chat.serverSwitch.isInvalidWebhook()) invalidCategories.add(MessageCategory.SERVER_SWITCH);
      if (chat.advancement.isInvalidWebhook()) invalidCategories.add(MessageCategory.ADVANCEMENT);
      if (chat.death.isInvalidWebhook()) invalidCategories.add(MessageCategory.DEATH);

      if (!invalidCategories.isEmpty()) {
        var errorFormat = """
          ERROR: `discord.webhook` and `discord.chat.%s.webhook` are unset or invalid, but `discord.chat.%s.type` is set to `webhook`
          """;
        var error = new StringBuilder();

        for (var category : invalidCategories) {
          error.append(String.format(errorFormat, category.toString(), category)).append("\n");
        }

        return error.toString();
      }
    }

    return null;
  }
}
