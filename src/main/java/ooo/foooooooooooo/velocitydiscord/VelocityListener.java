package ooo.foooooooooooo.velocitydiscord;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import ooo.foooooooooooo.velocitydiscord.discord.Discord;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VelocityListener {
  private final Discord discord;

  private final Map<String, ServerState> serverState = new HashMap<>();

  private boolean firstHealthCheck = true;

  public VelocityListener(Discord discord) {
    this.discord = discord;
  }

  @Subscribe
  public void onPlayerChat(PlayerChatEvent event) {
    var currentServer = event.getPlayer().getCurrentServer();

    if (currentServer.isEmpty()) {
      return;
    }

    var server = currentServer.get().getServerInfo().getName();

    if (VelocityDiscord.CONFIG.serverDisabled(server)) {
      return;
    }

    var username = event.getPlayer().getUsername();
    var uuid = event.getPlayer().getUniqueId();

    var prefix = getPrefix(uuid);

    this.discord.onPlayerChat(username, uuid.toString(), prefix, server, event.getMessage());
  }

  @Subscribe
  public void onConnect(ServerConnectedEvent event) {
    updatePlayerCount();

    var server = event.getServer().getServerInfo().getName();

    if (VelocityDiscord.CONFIG.serverDisabled(server)) {
      return;
    }

    setServerOnline(server);

    var username = event.getPlayer().getUsername();
    var previousServer = event.getPreviousServer();
    var previousName = previousServer.map(s -> s.getServerInfo().getName()).orElse(null);

    var uuid = event.getPlayer().getUniqueId();

    var prefix = getPrefix(uuid);

    // if previousServer is disabled but the current server is not, treat it as a join
    if (previousServer.isPresent() && !VelocityDiscord.CONFIG.serverDisabled(previousName)) {
      this.discord.onServerSwitch(username, uuid.toString(), prefix, server, previousName);
    } else {
      this.discord.onJoin(event.getPlayer(), prefix, server);
    }
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent event) {
    updatePlayerCount();

    var currentServer = event.getPlayer().getCurrentServer();

    var username = event.getPlayer().getUsername();
    var uuid = event.getPlayer().getUniqueId();
    var prefix = getPrefix(uuid);

    if (currentServer.isEmpty()) {
      this.discord.onDisconnect(username, uuid.toString(), prefix, "");
    } else {
      var name = currentServer.get().getServerInfo().getName();

      if (VelocityDiscord.CONFIG.serverDisabled(name)) {
        return;
      }

      setServerOnline(name);

      this.discord.onLeave(username, uuid.toString(), prefix, name);
    }
  }

  @Subscribe
  public void onProxyInitialize(ProxyInitializeEvent event) {
    this.discord.onProxyInitialize();
    updatePlayerCount();
    checkServerHealth();
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    this.discord.onProxyShutdown();
  }

  // theoretically can get notified of a server going offline by listening to
  // com.velocitypowered.api.event.player.KickedFromServerEvent and then parsing
  // the reason Component to check if its server shutting down message or something
  // but this seems like it would fail to work if literally anything in the message changes
  private void onServerOffline(String server) {
    this.discord.onServerStop(server);
  }

  private void onServerOnline(String server) {
    this.discord.onServerStart(server);
  }

  private void updatePlayerCount() {
    this.discord.updateActivityPlayerAmount(VelocityDiscord.SERVER.getPlayerCount());
  }

  private Optional<String> getPrefix(UUID uuid) {
    var luckPerms = VelocityDiscord.getLuckPerms();
    if (luckPerms == null) return Optional.empty();

    var user = luckPerms.getUserManager().getUser(uuid);
    if (user != null) {
      return Optional.ofNullable(user.getCachedData().getMetaData().getPrefix());
    }

    return Optional.empty();
  }

  /**
   * Ping all servers and update online state
   */
  public void checkServerHealth() {
    var servers = VelocityDiscord.SERVER.getAllServers();

    CompletableFuture
      .allOf(servers
        .parallelStream()
        .map((server) -> server.ping().handle((ping, ex) -> handlePing(server, ping, ex)))
        .toArray(CompletableFuture[]::new))
      .join();

    this.firstHealthCheck = false;
  }

  private CompletableFuture<Void> handlePing(RegisteredServer server, ServerPing ping, Throwable ex) {
    var name = server.getServerInfo().getName();

    if (VelocityDiscord.CONFIG.serverDisabled(name)) {
      return CompletableFuture.completedFuture(null);
    }

    var state = this.serverState.getOrDefault(name, ServerState.empty());

    if (ex != null) {
      if (state.online) {
        if (!this.firstHealthCheck) {
          onServerOffline(name);
        }
        state.online = false;
        this.serverState.put(name, state);
      }

      return CompletableFuture.completedFuture(null);
    }

    if (!state.online && !this.firstHealthCheck) {
      onServerOnline(name);
    }

    var players = 0;
    var maxPlayers = 0;

    if (ping.getPlayers().isPresent()) {
      players = ping.getPlayers().get().getOnline();
      maxPlayers = ping.getPlayers().get().getMax();
    }

    state.online = true;
    state.players = players;
    state.maxPlayers = maxPlayers;

    this.serverState.put(name, state);

    return CompletableFuture.completedFuture(null);
  }

  public ServerState getServerState(RegisteredServer server) {
    var name = server.getServerInfo().getName();
    return this.serverState.getOrDefault(name, ServerState.empty());
  }

  private void setServerOnline(String server) {
    var state = this.serverState.getOrDefault(server, ServerState.empty());

    if (!state.online) {
      onServerOnline(server);
      state.online = true;
      this.serverState.put(server, state);
    }
  }

  public static class ServerState {
    public boolean online;
    public int players;
    public int maxPlayers;

    public ServerState(boolean online, int players, int maxPlayers) {
      this.online = online;
      this.players = players;
      this.maxPlayers = maxPlayers;
    }

    public static ServerState empty() {
      return new ServerState(false, 0, 0);
    }
  }
}
