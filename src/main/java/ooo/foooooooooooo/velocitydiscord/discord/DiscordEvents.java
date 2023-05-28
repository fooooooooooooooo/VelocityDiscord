package ooo.foooooooooooo.velocitydiscord.discord;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static ooo.foooooooooooo.velocitydiscord.VelocityDiscord.CONFIG;
import static ooo.foooooooooooo.velocitydiscord.VelocityDiscord.Logger;

public class DiscordEvents extends ListenerAdapter {
  private final Discord discord;

  public DiscordEvents(Discord discord) {
    this.discord = discord;
  }

  @Override
  public void onReady(@NotNull ReadyEvent event) {
    Logger.info("Bot ready, Guilds: {} ({} available)", event.getGuildTotalCount(), event.getGuildAvailableCount());

    var channel = event.getJDA().getTextChannelById(Objects.requireNonNull(CONFIG.CHANNEL_ID));

    if (channel == null) {
      Logger.error("Could not load channel with id: {}", CONFIG.CHANNEL_ID);
      throw new RuntimeException("Could not load channel id: " + CONFIG.CHANNEL_ID);
    }

    Logger.info("Loaded channel: {}", channel.getName());

    if (!channel.canTalk()) {
      Logger.error("Cannot talk in configured channel");
      throw new RuntimeException("Cannot talk in configured channel");
    }

    discord.setActiveChannel(channel);
    discord.setupCommands();
    discord.updateActivityPlayerAmount();
  }

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    discord.handleSlashCommand(event);
  }
}
