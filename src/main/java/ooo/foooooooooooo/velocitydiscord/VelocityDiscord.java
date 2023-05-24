package ooo.foooooooooooo.velocitydiscord;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import ooo.foooooooooooo.velocitydiscord.discord.Discord;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VelocityDiscord implements ModInitializer {
  public static final String ModId = "velocity_discord";
  public static final String ModName = "Velocity Discord";
  public static final String ModVersion = "1.6.0";

  public static final Logger LOGGER = LoggerFactory.getLogger(ModId);

  @Nullable
  public static MinecraftServer SERVER;
  public static Config CONFIG;

  public static boolean pluginDisabled = false;

  Discord discord;

  @Override
  public void onInitialize() {

    LOGGER.info("Loading " + ModName + " v" + ModVersion);

    var configDir = FabricLoader.getInstance().getConfigDir();

    CONFIG = new Config(configDir);
    pluginDisabled = CONFIG.isFirstRun();

    if (pluginDisabled) {
      LOGGER.error(
        "This is the first time you are running this plugin. Please configure it in the config.yml file. Disabling plugin.");
    } else {
      ServerTickEvents.START_SERVER_TICK.register((server) -> {
        SERVER = server;
      });

      ServerTickEvents.END_SERVER_TICK.register((server) -> {
        SERVER = null;
      });

      this.discord = new Discord();
    }
  }
}
