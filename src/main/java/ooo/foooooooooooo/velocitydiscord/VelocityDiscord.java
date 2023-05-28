package ooo.foooooooooooo.velocitydiscord;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.Advancement;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import ooo.foooooooooooo.velocitydiscord.discord.Discord;
import ooo.foooooooooooo.velocitydiscord.events.AdvancementCallback;
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

    discord = new Discord();

    setupListeners();

    discord.start();
  }

  void setupListeners() {
    ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
    ServerLifecycleEvents.SERVER_STOPPED.register(this::onServerStopped);

    ServerPlayConnectionEvents.JOIN.register(this::onPlayerJoin);
    ServerPlayConnectionEvents.DISCONNECT.register(this::onPlayerDisconnect);

    ServerMessageEvents.CHAT_MESSAGE.register(this::onChatMessage);

    ServerLivingEntityEvents.AFTER_DEATH.register(this::onDeath);

    AdvancementCallback.EVENT.register(this::onAdvancement);
  }

  private static String getComponentText(Text component) {
    return component.getString();
  }

  private void onServerStarted(MinecraftServer server) { SERVER = server; }

  private void onServerStopped(MinecraftServer server) {
    SERVER = null;

    discord.shutdown();
  }

  private void onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
    discord.onPlayerConnect(handler.getPlayer().getName().getString());
  }

  private void onPlayerDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
    discord.onPlayerDisconnect(handler.getPlayer().getName().getString());
  }

  private void onChatMessage(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params) {
    if (sender.isPlayer()) {
      var username = sender.getName().getString();
      var content = message.getContent().getString();

      discord.onPlayerChat(username, content, sender.getUuid().toString());
    }
  }

  private void onDeath(LivingEntity entity, DamageSource damageSource) {
    if (entity instanceof ServerPlayerEntity player) {
      var name = player.getDisplayName().getString();
      var message = getComponentText(damageSource.getDeathMessage(player)).replace(name + " ", "");

      discord.onPlayerDeath(name, message);
    }
  }

  private void onAdvancement(ServerPlayerEntity player, Advancement advancement) {
    var display = advancement.getDisplay();

    if (display == null || display.isHidden()) {
      Logger.trace("Ignoring unsent display");
      return;
    }

    var title = getComponentText(display.getTitle());
    var description = getComponentText(display.getDescription());

    discord.onPlayerAdvancement(player.getName().getString(), title, description);
  }
}
