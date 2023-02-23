package ooo.foooooooooooo.velocitydiscord;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.plugin.annotation.DataDirectory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import static ooo.foooooooooooo.velocitydiscord.VelocityDiscord.PluginVersion;

public class Config {
    private static final String DefaultToken = "TOKEN";

    private static final String DefaultChannelId = "000000000000000000";
    private static final String StatusChannelId = "000000000000000000";
    private static final String ChatChannelId = "000000000000000000";
    private static final String ConnectionChannelId = "000000000000000000";
    private static final String DeathChannelId = "000000000000000000";
    private static final String AchievementChannelId = "000000000000000000";

    private static final String DefaultWebhookUrl = "";
    private static final String DefaultAvatarUrl = "https://crafatar.com/avatars/{uuid}?overlay";

    public String DISCORD_TOKEN = DefaultToken;
    public String DEFAULT_CHANNEL_ID = DefaultChannelId;
    public String STATUS_CHANNEL_ID = StatusChannelId;
    public String CHAT_CHANNEL_ID = ChatChannelId;
    public String CONNECTION_CHANNEL_ID = ConnectionChannelId;
    public String DEATH_CHANNEL_ID = DeathChannelId;
    public String ACHIEVEMENT_CHANNEL_ID = AchievementChannelId;

    // toggles
    public Boolean SHOW_BOT_MESSAGES = false;
    public Boolean SHOW_ATTACHMENTS = true;
    public Boolean SHOW_ACTIVITY = true;
    public Boolean ENABLE_MENTIONS = true;
    public Boolean ENABLE_EVERYONE_AND_HERE = false;

    // webhooks
    public Boolean DISCORD_USE_WEBHOOK = false;
    public String WEBHOOK_URL = DefaultWebhookUrl;
    public String WEBHOOK_AVATAR_URL = DefaultAvatarUrl;
    public String WEBHOOK_USERNAME = "{username}";

    // discord formats
    public String DISCORD_CHAT_MESSAGE = "{username}: {message}";
    public String JOIN_MESSAGE = "**{username} joined the game**";
    public String LEAVE_MESSAGE = "**{username} left the game**";
    public String DISCONNECT_MESSAGE = "**{username} disconnected**";
    public String SERVER_SWITCH_MESSAGE = "**{username} moved to {current} from {previous}**";
    public String DEATH_MESSAGE = "**{username} {death_message}**";
    public String ADVANCEMENT_MESSAGE = "**{username} has made the advancement __{advancement_title}__**\n_{advancement_description}_";
    public String DISCORD_ACTIVITY_TEXT = "with {amount} players online";

    // discord commands
    public Boolean DISCORD_LIST_ENABLED = true;
    public Boolean DISCORD_LIST_EPHEMERAL = true;
    public String DISCORD_LIST_SERVER_FORMAT = "[{server_name} {online_players}/{max_players}]";
    public String DISCORD_LIST_PLAYER_FORMAT = "- {username}";
    public String DISCORD_LIST_NO_PLAYERS = "No players online";
    public String DISCORD_LIST_SERVER_OFFLINE = "Server offline";
    public String DISCORD_LIST_CODEBLOCK_LANG = "asciidoc";

    // minecraft formats
    public String DISCORD_CHUNK = "<dark_gray>[<{discord_color}>Discord<dark_gray>]";
    public String USERNAME_CHUNK = "<{role_color}><hover:show_text:{username}#{discriminator}>{nickname}</hover>";
    public String MC_CHAT_MESSAGE = "{discord_chunk} {username_chunk}<dark_gray>: <white>{message} {attachments}";
    public String ATTACHMENTS = "<click:open_url:{url}>[Attachment]</click>";

    // colors
    public String DISCORD_COLOR = "#7289da";
    public String ATTACHMENT_COLOR = "#4abdff";

    private final Path dataDir;

    private static final String[] splitVersion = PluginVersion.split("\\.");
    private static final String configVersion = splitVersion[0] + '.' + splitVersion[1];
    private static final String configMajorVersion = splitVersion[0];

    private Toml toml;

    private boolean isFirstRun = false;

    @Inject
    public Config(@DataDirectory Path dataDir) {
        this.dataDir = dataDir;

        loadFile();
        loadConfigs();
    }

    private void loadFile() {
        if (Files.notExists(dataDir)) {
            try {
                Files.createDirectory(dataDir);
            } catch (IOException e) {
                throw new RuntimeException("Could not create data directory at " + dataDir.toAbsolutePath());
            }
        }

        Path dataFile = dataDir.resolve("config.toml");

        // create default config if it doesn't exist
        if (Files.notExists(dataFile)) {
            isFirstRun = true;

            try (InputStream in = getClass().getResourceAsStream("/config.toml")) {
                Files.copy(Objects.requireNonNull(in), dataFile);
            } catch (IOException e) {
                throw new RuntimeException("ERROR: Can't write default configuration file (permissions/filesystem error?)");
            }
        }

        toml = new Toml().read(dataFile.toFile());

        // make sure the config makes sense for the current plugin's version
        String version = toml.getString("config_version", configVersion);

        if (!versionCompatible(version)) {
            throw new RuntimeException(String.format("ERROR: Can't use the existing configuration file: version mismatch (mod: %s, config: %s)", configVersion, version));
        }
    }

    private boolean versionCompatible(String newVersion) {
        return newVersion.split("\\.")[0].equals(configMajorVersion);
    }

    public void loadConfigs() {
        DISCORD_TOKEN = toml.getString("discord.token", DISCORD_TOKEN);
        DEFAULT_CHANNEL_ID = toml.getString("discord.default_channel", DEFAULT_CHANNEL_ID);
        STATUS_CHANNEL_ID = toml.getString("discord.status_channel", STATUS_CHANNEL_ID);
        CHAT_CHANNEL_ID = toml.getString("discord.chat_channel", CHAT_CHANNEL_ID);
        CONNECTION_CHANNEL_ID = toml.getString("discord.connection_channel", CONNECTION_CHANNEL_ID);
        DEATH_CHANNEL_ID = toml.getString("discord.death_channel", DEATH_CHANNEL_ID);
        ACHIEVEMENT_CHANNEL_ID = toml.getString("discord.achievement_channel", ACHIEVEMENT_CHANNEL_ID);

        SHOW_BOT_MESSAGES = toml.getBoolean("discord.show_bot_messages", SHOW_BOT_MESSAGES);
        SHOW_ATTACHMENTS = toml.getBoolean("discord.show_attachments_ingame", SHOW_ATTACHMENTS);
        SHOW_ACTIVITY = toml.getBoolean("discord.show_activity", SHOW_ACTIVITY);
        ENABLE_MENTIONS = toml.getBoolean("discord.enable_mentions", ENABLE_MENTIONS);
        ENABLE_EVERYONE_AND_HERE = toml.getBoolean("discord.enable_everyone_and_here", ENABLE_EVERYONE_AND_HERE);

        DISCORD_USE_WEBHOOK = toml.getBoolean("discord.use_webhook", DISCORD_USE_WEBHOOK);
        WEBHOOK_URL = toml.getString("discord.webhook.webhook_url", WEBHOOK_URL);
        WEBHOOK_AVATAR_URL = toml.getString("discord.webhook.avatar_url", WEBHOOK_AVATAR_URL);
        WEBHOOK_USERNAME = toml.getString("discord.webhook.webhook_username", WEBHOOK_USERNAME);

        DISCORD_CHAT_MESSAGE = toml.getString("discord.chat.message", DISCORD_CHAT_MESSAGE);
        JOIN_MESSAGE = toml.getString("discord.chat.join_message", JOIN_MESSAGE);
        LEAVE_MESSAGE = toml.getString("discord.chat.leave_message", LEAVE_MESSAGE);
        DISCONNECT_MESSAGE = toml.getString("discord.chat.disconnect_message", DISCONNECT_MESSAGE);
        SERVER_SWITCH_MESSAGE = toml.getString("discord.chat.server_switch_message", SERVER_SWITCH_MESSAGE);
        DEATH_MESSAGE = toml.getString("discord.chat.death_message", DEATH_MESSAGE);
        ADVANCEMENT_MESSAGE = toml.getString("discord.chat.advancement_message", ADVANCEMENT_MESSAGE);
        DISCORD_ACTIVITY_TEXT = toml.getString("discord.activity_text", DISCORD_ACTIVITY_TEXT);

        DISCORD_LIST_ENABLED = toml.getBoolean("discord.commands.list.enabled", DISCORD_LIST_ENABLED);
        DISCORD_LIST_EPHEMERAL = toml.getBoolean("discord.commands.list.ephemeral", DISCORD_LIST_EPHEMERAL);
        DISCORD_LIST_SERVER_FORMAT = toml.getString("discord.commands.list.server_format", DISCORD_LIST_SERVER_FORMAT);
        DISCORD_LIST_PLAYER_FORMAT = toml.getString("discord.commands.list.player_format", DISCORD_LIST_PLAYER_FORMAT);
        DISCORD_LIST_NO_PLAYERS = toml.getString("discord.commands.list.no_players", DISCORD_LIST_NO_PLAYERS);
        DISCORD_LIST_SERVER_OFFLINE = toml.getString("discord.commands.list.server_offline", DISCORD_LIST_SERVER_OFFLINE);
        DISCORD_LIST_CODEBLOCK_LANG = toml.getString("discord.commands.list.codeblock_lang", DISCORD_LIST_CODEBLOCK_LANG);

        DISCORD_CHUNK = toml.getString("minecraft.discord_chunk", DISCORD_CHUNK);
        USERNAME_CHUNK = toml.getString("minecraft.username_chunk", USERNAME_CHUNK);
        MC_CHAT_MESSAGE = toml.getString("minecraft.message", MC_CHAT_MESSAGE);
        ATTACHMENTS = toml.getString("minecraft.attachments", ATTACHMENTS);
        DISCORD_COLOR = toml.getString("minecraft.discord_color", DISCORD_COLOR);
        ATTACHMENT_COLOR = toml.getString("minecraft.attachment_color", ATTACHMENT_COLOR);
    }

    // Assume it's the first run if the config hasn't been edited or has been created this run
    public boolean isFirstRun() {
        return DISCORD_TOKEN.equals(DefaultToken) || DEFAULT_CHANNEL_ID.equals(DefaultChannelId) || isFirstRun;
    }
}
