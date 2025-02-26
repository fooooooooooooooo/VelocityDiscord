package ooo.foooooooooooo.velocitydiscord.config;

import ooo.foooooooooooo.config.Config;
import ooo.foooooooooooo.config.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PluginConfigTest {
  PluginConfigTest() {
    TestUtils.setLogLevel();
  }

  Logger logger = LoggerFactory.getLogger(PluginConfig.class);

  String invalidVersionTestConfig = """
    config_version = "1.0"
    
    [discord]
    token = "test_token"
    channel = "123456789012345678"
    """;

  String emptyColorTestConfig = """
    config_version = "2.0"
    
    [discord]
    token = "test_token"
    channel = "123456789012345678"
    
    [discord.chat.message]
    format = "test_format"
    type = "embed"
    embed_color = ""
    """;

  String invalidColorTestConfig = """
    config_version = "2.0"
    
    [discord]
    token = "test_token"
    channel = "123456789012345678"
    
    [discord.chat.message]
    format = "test_format"
    type = "embed"
    embed_color = "not-a-color"
    """;

  String nullColorTestConfig = """
    config_version = "2.0"
    
    [discord]
    token = "test_token"
    channel = "123456789012345678"
    
    [discord.chat.message]
    format = "test_format"
    type = "embed"
    # No embed_color specified
    """;

  String serverOverridesTestConfig = """
    config_version = "2.0"
    
    [discord]
    token = "test_token"
    channel = "123456789012345678"
    
    [discord.chat.message]
    format = "main format"
    type = "embed"
    embed_color = "#00ff00"
    
    [override.survival]
    [override.survival.discord.chat.message]
    format = "survival format"
    embed_color = "#ff0000"
    
    [override.lobby]
    [override.lobby.discord.chat.message]
    format = "lobby format"
    embed_color = ""
    
    [override.creative]
    [override.creative.discord.chat.message]
    format = "creative format"
    # No color specified, should inherit from main
    """;

  @Test
  void shouldThrowGivenNullConfig() {
    try {
      var config = new PluginConfig((com.electronwill.nightconfig.core.Config) null, this.logger);

      config.loadConfig();

      fail("Expected RuntimeException");
    } catch (RuntimeException e) {
      assertEquals("ERROR: Config is empty", e.getMessage());
    }
  }

  @Test
  void shouldThrowGivenInvalidConfigVersion(@TempDir Path tempDir) {
    try {
      var config =
        TestUtils.createConfig(this.invalidVersionTestConfig, tempDir, c -> new PluginConfig(c, this.logger));

      config.loadConfig();

      fail("Expected RuntimeException");
    } catch (RuntimeException e) {
      assertEquals(
        "ERROR: Can't use the existing configuration file: version mismatch (mod: 2.0, config: 1.0)",
        e.getMessage()
      );
    }
  }

  @Test
  void shouldBeOptionalEmptyGivenEmptyColorString(@TempDir Path tempDir) {
    var config = TestUtils.createConfig(this.emptyColorTestConfig, tempDir, c -> new PluginConfig(c, this.logger));

    config.loadConfig();

    assertEquals(Optional.empty(), config.getDiscordChatConfig().MESSAGE_EMBED_COLOR);
  }

  @Test
  void shouldThrowGivenInvalidColorFormat(@TempDir Path tempDir) {
    try {
      var config = TestUtils.createConfig(this.invalidColorTestConfig, tempDir, c -> new PluginConfig(c, this.logger));

      config.loadConfig();

      fail("Expected RuntimeException");
    } catch (RuntimeException e) {
      assertInstanceOf(NumberFormatException.class, e.getCause());
      assertTrue(e.getCause().getMessage().contains("not-a-color"));
    }
  }

  @Test
  void shouldUseDefaultColorGivenNoColorSpecified(@TempDir Path tempDir) {
    var config = TestUtils.createConfig(this.nullColorTestConfig, tempDir, c -> new PluginConfig(c, this.logger));

    config.loadConfig();

    // Default is Optional.empty() based on the field declaration in DiscordChatConfig
    assertTrue(config.getDiscordChatConfig().MESSAGE_EMBED_COLOR.isEmpty());
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  void shouldHandleServerOverrides(@TempDir Path tempDir) {
    var config = TestUtils.createConfig(this.serverOverridesTestConfig, tempDir, c -> new PluginConfig(c, this.logger));

    // Test main config
    assertEquals("main format", config.getDiscordChatConfig().MESSAGE_FORMAT.get());
    assertEquals(Color.decode("#00ff00"), config.getDiscordChatConfig().MESSAGE_EMBED_COLOR.get());

    // Test survival override (explicit values)
    var survivalConfig = config.getServerConfig("survival");
    assertEquals("survival format", survivalConfig.getDiscordChatConfig().MESSAGE_FORMAT.get());
    assertEquals(Color.decode("#ff0000"), survivalConfig.getDiscordChatConfig().MESSAGE_EMBED_COLOR.get());

    // Test lobby override (empty color string - should result in Optional.empty())
    var lobbyConfig = config.getServerConfig("lobby");
    assertEquals("lobby format", lobbyConfig.getDiscordChatConfig().MESSAGE_FORMAT.get());
    assertTrue(lobbyConfig.getDiscordChatConfig().MESSAGE_EMBED_COLOR.isEmpty());

    // Test creative override (no color specified - should inherit from main)
    var creativeConfig = config.getServerConfig("creative");
    assertEquals("creative format", creativeConfig.getDiscordChatConfig().MESSAGE_FORMAT.get());
    assertEquals(Color.decode("#00ff00"), creativeConfig.getDiscordChatConfig().MESSAGE_EMBED_COLOR.get());

    // Test non-existent server (should use main config)
    var nonExistentConfig = config.getServerConfig("non-existent");
    assertEquals("main format", nonExistentConfig.getDiscordChatConfig().MESSAGE_FORMAT.get());
    assertEquals(Color.decode("#00ff00"), nonExistentConfig.getDiscordChatConfig().MESSAGE_EMBED_COLOR.get());
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  void shouldLoadConfig(@TempDir Path tempDir) {
    var testConfig = TestUtils.readResource("/config.toml");
    var config = TestUtils.createConfig(testConfig, tempDir, c -> new PluginConfig(c, this.logger));

    config.loadConfig();

    // Test root level config
    assertEquals(PluginConfig.ConfigVersion, Config.get(config, "config_version"));
    assertEquals(List.of("survival"), config.EXCLUDED_SERVERS);
    assertTrue(config.EXCLUDED_SERVERS_RECEIVE_MESSAGES);
    assertEquals(123, config.PING_INTERVAL_SECONDS);

    // Server names
    assertEquals("lobby_test_name", config.serverName("lobby"));

    var discord = config.getDiscordConfig();

    // Bot config
    assertEquals("test_token", discord.DISCORD_TOKEN);
    assertEquals("123456789012345678", discord.MAIN_CHANNEL_ID);
    assertFalse(discord.ENABLE_MENTIONS);
    assertTrue(discord.ENABLE_EVERYONE_AND_HERE);
    assertEquals("activity_format_test", discord.ACTIVITY_FORMAT.get());
    assertEquals(123, discord.UPDATE_CHANNEL_TOPIC_INTERVAL_MINUTES);

    // Channel topic config
    assertEquals("format_test", discord.TOPIC_FORMAT.get());
    assertEquals("server_test", discord.TOPIC_SERVER_FORMAT.get());
    assertEquals("server_offline_test", discord.TOPIC_SERVER_OFFLINE_FORMAT.get());
    assertEquals("players_no_players_header_test", discord.TOPIC_PLAYER_LIST_NO_PLAYERS_HEADER.get());
    assertEquals("player_list_header_test", discord.TOPIC_PLAYER_LIST_HEADER.get());
    assertEquals("player_list_player_test", discord.TOPIC_PLAYER_LIST_FORMAT);
    assertEquals("player_list_separator_test", discord.TOPIC_PLAYER_LIST_SEPARATOR);
    assertEquals(123, discord.TOPIC_PLAYER_LIST_MAX_COUNT);

    // Webhook config
    assertEquals("webhook_url_test", discord.WEBHOOK.URL);
    assertEquals("webhook_username_test", discord.WEBHOOK.USERNAME);
    assertEquals("avatar_url_test", discord.WEBHOOK.AVATAR_URL);

    var chatConfig = config.getDiscordChatConfig();

    // Chat message config
    assertEquals("format_test", chatConfig.MESSAGE_FORMAT.get());
    assertEquals(DiscordChatConfig.UserMessageType.EMBED, chatConfig.MESSAGE_TYPE);
    var messageEmbedColor = chatConfig.MESSAGE_EMBED_COLOR.get();
    this.logger.info("chatConfig.MESSAGE_EMBED_COLOR: {}", messageEmbedColor);
    this.logger.info("chatConfig.MESSAGE_EMBED_COLOR type: {}", messageEmbedColor.getClass());

    assertEquals(Color.decode("#ff00ff"), chatConfig.MESSAGE_EMBED_COLOR.get());
    assertEquals("message_webhook_url_test", chatConfig.MESSAGE_WEBHOOK.URL);
    assertEquals("message_webhook_username_test", chatConfig.MESSAGE_WEBHOOK.USERNAME);
    assertEquals("message_webhook_avatar_url_test", chatConfig.MESSAGE_WEBHOOK.AVATAR_URL);

    assertEquals("format_test", chatConfig.JOIN_FORMAT.get());
    assertEquals(DiscordChatConfig.UserMessageType.EMBED, chatConfig.JOIN_TYPE);
    assertEquals(Color.decode("#ff00ff"), chatConfig.JOIN_EMBED_COLOR.get());
    assertEquals("join_webhook_url_test", chatConfig.JOIN_WEBHOOK.URL);
    assertEquals("join_webhook_username_test", chatConfig.JOIN_WEBHOOK.USERNAME);
    assertEquals("join_webhook_avatar_url_test", chatConfig.JOIN_WEBHOOK.AVATAR_URL);

    assertEquals("format_test", chatConfig.LEAVE_FORMAT.get());
    assertEquals(DiscordChatConfig.UserMessageType.EMBED, chatConfig.LEAVE_TYPE);
    assertEquals(Color.decode("#ff00ff"), chatConfig.LEAVE_EMBED_COLOR.get());
    assertEquals("leave_webhook_url_test", chatConfig.LEAVE_WEBHOOK.URL);
    assertEquals("leave_webhook_username_test", chatConfig.LEAVE_WEBHOOK.USERNAME);
    assertEquals("leave_webhook_avatar_url_test", chatConfig.LEAVE_WEBHOOK.AVATAR_URL);

    assertEquals("format_test", chatConfig.DISCONNECT_FORMAT.get());
    assertEquals(DiscordChatConfig.UserMessageType.EMBED, chatConfig.DISCONNECT_TYPE);
    assertEquals(Color.decode("#ff00ff"), chatConfig.DISCONNECT_EMBED_COLOR.get());
    assertEquals("disconnect_webhook_url_test", chatConfig.DISCONNECT_WEBHOOK.URL);
    assertEquals("disconnect_webhook_username_test", chatConfig.DISCONNECT_WEBHOOK.USERNAME);
    assertEquals("disconnect_webhook_avatar_url_test", chatConfig.DISCONNECT_WEBHOOK.AVATAR_URL);

    assertEquals("format_test", chatConfig.SERVER_SWITCH_FORMAT.get());
    assertEquals(DiscordChatConfig.UserMessageType.EMBED, chatConfig.SERVER_SWITCH_TYPE);
    assertEquals(Color.decode("#ff00ff"), chatConfig.SERVER_SWITCH_EMBED_COLOR.get());
    assertEquals("server_switch_webhook_url_test", chatConfig.SERVER_SWITCH_WEBHOOK.URL);
    assertEquals("server_switch_webhook_username_test", chatConfig.SERVER_SWITCH_WEBHOOK.USERNAME);
    assertEquals("server_switch_webhook_avatar_url_test", chatConfig.SERVER_SWITCH_WEBHOOK.AVATAR_URL);

    assertEquals("format_test", chatConfig.DEATH_FORMAT.get());
    assertEquals(DiscordChatConfig.UserMessageType.EMBED, chatConfig.DEATH_TYPE);
    assertEquals(Color.decode("#ff00ff"), chatConfig.DEATH_EMBED_COLOR.get());
    assertEquals("death_webhook_url_test", chatConfig.DEATH_WEBHOOK.URL);
    assertEquals("death_webhook_username_test", chatConfig.DEATH_WEBHOOK.USERNAME);
    assertEquals("death_webhook_avatar_url_test", chatConfig.DEATH_WEBHOOK.AVATAR_URL);

    assertEquals("format_test", chatConfig.ADVANCEMENT_FORMAT.get());
    assertEquals(DiscordChatConfig.UserMessageType.EMBED, chatConfig.ADVANCEMENT_TYPE);
    assertEquals(Color.decode("#ff00ff"), chatConfig.ADVANCEMENT_EMBED_COLOR.get());
    assertEquals("advancement_webhook_url_test", chatConfig.ADVANCEMENT_WEBHOOK.URL);
    assertEquals("advancement_webhook_username_test", chatConfig.ADVANCEMENT_WEBHOOK.USERNAME);
    assertEquals("advancement_webhook_avatar_url_test", chatConfig.ADVANCEMENT_WEBHOOK.AVATAR_URL);

    assertEquals("format_test", chatConfig.PROXY_START_FORMAT.get());
    assertEquals(DiscordChatConfig.ServerMessageType.EMBED, chatConfig.PROXY_START_TYPE);
    assertEquals(Color.decode("#ff00ff"), chatConfig.PROXY_START_EMBED_COLOR.get());

    assertEquals("format_test", chatConfig.PROXY_STOP_FORMAT.get());
    assertEquals(DiscordChatConfig.ServerMessageType.EMBED, chatConfig.PROXY_STOP_TYPE);
    assertEquals(Color.decode("#ff00ff"), chatConfig.PROXY_STOP_EMBED_COLOR.get());

    assertEquals("format_test", chatConfig.SERVER_START_FORMAT.get());
    assertEquals(DiscordChatConfig.ServerMessageType.EMBED, chatConfig.SERVER_START_TYPE);
    assertEquals(Color.decode("#ff00ff"), chatConfig.SERVER_START_EMBED_COLOR.get());

    assertEquals("format_test", chatConfig.SERVER_STOP_FORMAT.get());
    assertEquals(DiscordChatConfig.ServerMessageType.EMBED, chatConfig.SERVER_STOP_TYPE);
    assertEquals(Color.decode("#ff00ff"), chatConfig.SERVER_STOP_EMBED_COLOR.get());

    // Discord commands config
    assertFalse(discord.COMMANDS_LIST.DISCORD_LIST_ENABLED);
    assertFalse(discord.COMMANDS_LIST.EPHEMERAL);
    assertEquals("server_format_test", discord.COMMANDS_LIST.SERVER_FORMAT);
    assertEquals("player_format_test", discord.COMMANDS_LIST.PLAYER_FORMAT);
    assertEquals("no_players_test", discord.COMMANDS_LIST.NO_PLAYERS_FORMAT.get());
    assertEquals("server_offline_test", discord.COMMANDS_LIST.SERVER_OFFLINE_FORMAT.get());
    assertEquals("codeblock_lang_test", discord.COMMANDS_LIST.CODEBLOCK_LANG);

    // Minecraft config
    var minecraft = config.getMinecraftConfig();
    assertEquals("discord_chunk_test", minecraft.DISCORD_CHUNK_FORMAT);
    assertEquals("username_chunk_test", minecraft.USERNAME_CHUNK_FORMAT);
    assertEquals("message_test", minecraft.MESSAGE_FORMAT);
    assertEquals("attachments_test", minecraft.ATTACHMENT_FORMAT);
    assertEquals("links_test", minecraft.LINK_FORMAT.get());
    assertEquals("#ff00ff", minecraft.DISCORD_COLOR);
    assertEquals("#ff00ff", minecraft.ATTACHMENT_COLOR);
    assertEquals("#ff00ff", minecraft.LINK_COLOR);
  }

  @Test
  void shouldLoadRealConfig(@TempDir Path tempDir) {
    var testConfig = TestUtils.readResource("/real_test_config.toml");
    var config = TestUtils.createConfig(testConfig, tempDir, c -> new PluginConfig(c, this.logger));

    config.loadConfig();

    // Test root level config
    assertEquals(PluginConfig.ConfigVersion, Config.get(config, "config_version"));
  }
}
