package ooo.foooooooooooo.velocitydiscord.config;

import ooo.foooooooooooo.velocitydiscord.config.definitions.UserMessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PluginConfigTests {
  @Test
  public void allConfigKeysLoadedCorrectly(@TempDir Path tempDir) {
    var config = TestUtils.createConfig(TestUtils.getResource("config.toml"), tempDir);
    var pluginConfig = new PluginConfig(config);

    // global config
    assertEquals(List.of("survival"), pluginConfig.global.excludedServers);
    assertTrue(pluginConfig.global.excludedServersReceiveMessages);
    assertEquals(123, pluginConfig.global.pingIntervalSeconds);
    assertEquals("lobby_test_name", pluginConfig.global.serverDisplayNames.get("lobby"));

    // global discord config
    var globalDiscord = pluginConfig.global.discord;
    assertEquals("test_token", globalDiscord.token);
    assertEquals(Optional.of("activity_text_test"), globalDiscord.activityText);
    assertEquals(123, globalDiscord.updateChannelTopicIntervalMinutes);

    // local discord config
    var discord = pluginConfig.local.discord;
    assertEquals("123456789012345678", discord.mainChannelId);
    assertTrue(discord.showBotMessages);
    assertFalse(discord.showAttachmentsIngame);
    assertFalse(discord.enableMentions);
    assertTrue(discord.enableEveryoneAndHere);

    // channel topic config
    var topic = discord.channelTopic;
    assertEquals(Optional.of("format_test"), topic.format);
    assertEquals(Optional.of("server_test"), topic.serverFormat);
    assertEquals(Optional.of("server_offline_test"), topic.serverOfflineFormat);
    assertEquals(Optional.of("players_no_players_header_test"), topic.playerListNoPlayersHeader);
    assertEquals(Optional.of("player_list_header_test"), topic.playerListHeader);
    assertEquals("player_list_player_test", topic.playerListPlayerFormat);
    assertEquals("player_list_separator_test", topic.playerListSeparator);
    assertEquals(123, topic.playerListMaxCount);

    // webhook config
    var webhook = discord.webhook;
    assertEquals("url_test", webhook.url);
    assertEquals("avatar_url_test", webhook.avatarUrl);
    assertEquals("username_test", webhook.username);

    // chat message config
    var chat = discord.chat;
    var messageConfig = chat.message;
    assertEquals(Optional.of("format_test"), messageConfig.format);
    assertEquals(UserMessageType.EMBED, messageConfig.type);
    assertEquals(Optional.of(Color.decode("#ff00ff")), messageConfig.embedColor);

    // join config
    var joinConfig = chat.join;
    assertEquals(Optional.of("format_test"), joinConfig.format);
    assertEquals(UserMessageType.EMBED, joinConfig.type);
    assertEquals(Optional.of(Color.decode("#ff00ff")), joinConfig.embedColor);

    // leave config
    var leaveConfig = chat.leave;
    assertEquals(Optional.of("format_test"), leaveConfig.format);
    assertEquals(UserMessageType.EMBED, leaveConfig.type);
    assertEquals(Optional.of(Color.decode("#ff00ff")), leaveConfig.embedColor);

    // server switch config
    var serverSwitchConfig = chat.serverSwitch;
    assertEquals(Optional.of("format_test"), serverSwitchConfig.format);
    assertEquals(UserMessageType.EMBED, serverSwitchConfig.type);
    assertEquals(Optional.of(Color.decode("#ff00ff")), serverSwitchConfig.embedColor);

    // death config
    var deathConfig = chat.death;
    assertEquals(Optional.of("format_test"), deathConfig.format);
    assertEquals(UserMessageType.EMBED, deathConfig.type);
    assertEquals(Optional.of(Color.decode("#ff00ff")), deathConfig.embedColor);

    // minecraft config
    var minecraft = pluginConfig.local.minecraft;
    assertEquals("discord_chunk_test", minecraft.discordChunkFormat);
    assertEquals("username_chunk_test", minecraft.usernameChunkFormat);
    assertEquals("message_test", minecraft.messageFormat);
    assertEquals("attachments_test", minecraft.attachmentFormat);
    assertEquals(Optional.of("links_test"), minecraft.linkFormat);
    assertEquals("#ff00ff", minecraft.discordColor);
    assertEquals("#ff00ff", minecraft.attachmentColor);
    assertEquals("#ff00ff", minecraft.linkColor);
    assertEquals("role_prefix_test_1", minecraft.rolePrefixes.get("123456789"));
    assertEquals("role_prefix_test_2", minecraft.rolePrefixes.get("987654321"));

    // list command config
    var globalListCommand = pluginConfig.global.discord.commands.list;
    assertFalse(globalListCommand.enabled);
    assertFalse(globalListCommand.ephemeral);
    assertEquals("codeblock_lang_test", globalListCommand.codeblockLang);

    var listCommand = discord.commands.list;
    assertEquals("server_format_test", listCommand.serverFormat);
    assertEquals("player_format_test", listCommand.playerFormat);
    assertEquals(Optional.of("no_players_test"), listCommand.noPlayersFormat);
    assertEquals(Optional.of("server_offline_test"), listCommand.serverOfflineFormat);
  }

  @Test
  public void serverDisplayNamesWorks(@TempDir Path tempDir) {
    var config = TestUtils.createConfig(TestUtils.getResource("real_test_config.toml"), tempDir);
    var pluginConfig = new PluginConfig(config);

    assertEquals("Server A", pluginConfig.global.serverDisplayNames.get("server_a"));
    assertEquals("Server B", pluginConfig.global.serverDisplayNames.get("server_b"));
  }
}
