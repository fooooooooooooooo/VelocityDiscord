package ooo.foooooooooooo.velocitydiscord.config.definitions;

import ooo.foooooooooooo.velocitydiscord.config.Config;

public class ChatConfig {
  public UserMessageConfig message = new UserMessageConfig("{username}: {message}", null);
  public UserMessageConfig join = new UserMessageConfig("**{username} joined the game**", Config.GREEN);
  public UserMessageConfig leave = new UserMessageConfig("**{username} left the game**", Config.RED);
  public UserMessageConfig disconnect = new UserMessageConfig("**{username} disconnected**", Config.RED);
  public UserMessageConfig death = new UserMessageConfig("**{death_message}**", Config.RED);
  public UserMessageConfig advancement = new UserMessageConfig(
    "**{username} has made the advancement __{advancement_title}__**\n_{advancement_description}_",
    Config.GREEN
  );

  public UserMessageConfig serverSwitch =
    new UserMessageConfig("**{username} moved to {current} from {previous}**", Config.GREEN);

  public SystemMessageConfig serverStart = new SystemMessageConfig("**{server} has started**", Config.GREEN);
  public SystemMessageConfig serverStop = new SystemMessageConfig("**{server} has stopped**", Config.RED);

  public void load(Config config) {
    if (config == null) return;

    this.message.load(config.getConfig("message"));
    this.join.load(config.getConfig("join"));
    this.leave.load(config.getConfig("leave"));
    this.disconnect.load(config.getConfig("disconnect"));
    this.death.load(config.getConfig("death"));
    this.advancement.load(config.getConfig("advancement"));

    this.serverSwitch.load(config.getConfig("server_switch"));

    this.serverStart.load(config.getConfig("server_start"));
    this.serverStop.load(config.getConfig("server_stop"));
  }
}
