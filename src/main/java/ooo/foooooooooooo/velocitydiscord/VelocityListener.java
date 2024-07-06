package ooo.foooooooooooo.velocitydiscord;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import ooo.foooooooooooo.velocitydiscord.config.Config;
import ooo.foooooooooooo.velocitydiscord.discord.Discord;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class VelocityListener {
  private final Config config;
  private final Discord discord;
  private final ProxyServer server;

  private final Map<String, ServerState> serverState = new HashMap<>();

  private boolean firstHealthCheck = true;

  public VelocityListener(Config config, Discord discord, ProxyServer server) {
    this.config = config;
    this.discord = discord;
    this.server = server;
  }

  @Subscribe(order = PostOrder.FIRST)
  public void onPlayerChat(PlayerChatEvent event) {
    var currentServer = event.getPlayer().getCurrentServer();

    if (currentServer.isEmpty()) {
      return;
    }

    var server = currentServer.get().getServerInfo().getName();

    if (config.serverDisabled(server)) {
      return;
    }

    var username = event.getPlayer().getUsername();
    var uuid = event.getPlayer().getUniqueId().toString();

    discord.onPlayerChat(username, uuid, server, event.getMessage());
  }

  @Subscribe
  public void onConnect(ServerConnectedEvent event) {
    updatePlayerCount();

    var server = event.getServer().getServerInfo().getName();

    if (config.serverDisabled(server)) {
      return;
    }

    setServerOnline(server);

    var username = event.getPlayer().getUsername();
    var previousServer = event.getPreviousServer();
    var previousName = previousServer.map(s -> s.getServerInfo().getName()).orElse(null);

    // if previousServer is disabled but the current server is not, treat it as a join
    if (previousServer.isPresent() && !config.serverDisabled(previousName)) {
      discord.onServerSwitch(username, server, previousName);
    } else {
      discord.onJoin(username, server);
    }
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent event) {
    updatePlayerCount();

    var currentServer = event.getPlayer().getCurrentServer();

    var username = event.getPlayer().getUsername();

    if (currentServer.isEmpty()) {
      discord.onDisconnect(username);
    } else {
      var name = currentServer.get().getServerInfo().getName();

      if (config.serverDisabled(name)) {
        return;
      }

      setServerOnline(name);

      discord.onLeave(username, currentServer.get().getServerInfo().getName());
    }
  }

  @Subscribe
  public void onProxyInitialize(ProxyInitializeEvent event) {
    discord.onProxyInitialize();
    updatePlayerCount();
    checkServerHealth();
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    discord.onProxyShutdown();
  }

  // theoretically can get notified of a server going offline by listening to
  // com.velocitypowered.api.event.player.KickedFromServerEvent and then parsing
  // the reason Component to check if its server shutting down message or something
  // but this seems like it would fail to work if literally anything in the message changes
  private void onServerOffline(String server) {
    discord.onServerStop(server);
  }

  private void onServerOnline(String server) {
    discord.onServerStart(server);
  }

  private void updatePlayerCount() {
    discord.updateActivityPlayerAmount(this.server.getPlayerCount());
  }

  /**
   * Ping all servers and update online state
   */
  public void checkServerHealth() {
    var servers = server.getAllServers();

    CompletableFuture
      .allOf(servers.parallelStream().map((server) -> server.ping().handle((ping, ex) -> handlePing(server, ping, ex))).toArray(CompletableFuture[]::new))
      .join();

    this.firstHealthCheck = false;
  }

  private CompletableFuture<Void> handlePing(RegisteredServer server, ServerPing ping, Throwable ex) {
    var name = server.getServerInfo().getName();

    if (config.serverDisabled(name)) {
      return CompletableFuture.completedFuture(null);
    }

    var state = serverState.getOrDefault(name, ServerState.empty());

    if (ex != null) {
      if (state.online) {
        if (!this.firstHealthCheck) {
          onServerOffline(name);
        }
        state.online = false;
        serverState.put(name, state);
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

    serverState.put(name, state);

    return CompletableFuture.completedFuture(null);
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

  public ServerState getServerState(RegisteredServer server) {
    var name = server.getServerInfo().getName();
    return serverState.get(name);
  }

  private void setServerOnline(String server) {
    var state = serverState.getOrDefault(server, ServerState.empty());

    if (!state.online) {
      onServerOnline(server);
      state.online = true;
      serverState.put(server, state);
    }
  }
}
