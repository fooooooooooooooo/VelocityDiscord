package ooo.foooooooooooo.velocitydiscord;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import ooo.foooooooooooo.velocitydiscord.discord.Discord;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VelocityDiscord implements ModInitializer {
  public static final String ModId = "velocity_discord";
  public static final String ModName = "Velocity Discord";
  public static final String ModVersion = "1.6.1";
  public static final Logger Logger = LoggerFactory.getLogger(ModId);

  @Nullable
  public static MinecraftServer SERVER;
  public static Config CONFIG;

  public static boolean pluginDisabled = false;

  Discord discord;

  @Override
  public void onInitialize() {
    Logger.info("Loading " + ModName + " v" + ModVersion);

    var configDir = FabricLoader.getInstance().getConfigDir();

    CONFIG = new Config(configDir.resolve("velocity_discord"));
    pluginDisabled = CONFIG.isFirstRun();

    if (pluginDisabled) {
      Logger.error(
        "This is the first time you are running this mod. Please configure it in the config.toml file. Disabling mod.");
      return;
    }

    ServerLifecycleEvents.SERVER_STARTED.register((server) -> SERVER = server);

    ServerLifecycleEvents.SERVER_STOPPED.register((server) -> SERVER = null);

    this.discord = new Discord();
  }
}
