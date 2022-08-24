package ooo.foooooooooooo.velocitydiscord;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.plugin.annotation.DataDirectory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class Config {
    public static final String CONFIG_MAJOR_VERSION = "1";
    public static final String CONFIG_MINOR_VERSION = "2";
    public static final String CONFIG_VERSION = CONFIG_MAJOR_VERSION + "." + CONFIG_MINOR_VERSION;
    private static final String DefaultToken = "TOKEN";
    private static final String DefaultChannelId = "000000000000000000";
    private final Path dataDir;
    public String DISCORD_TOKEN = DefaultToken;
    public String CHANNEL_ID = DefaultChannelId;

    // toggles
    public Boolean SHOW_BOT_MESSAGES = false;
    public Boolean SHOW_ATTACHMENTS = true;

    // discord formats
    public String DISCORD_CHAT_MESSAGE = "{username}: {message}";
    public String JOIN_MESSAGE = "**{username} joined the game**";
    public String LEAVE_MESSAGE = "**{username} left the game**";
    public String SERVER_SWITCH_MESSAGE = "**{username} moved to {current} from {previous}**";
    public String DEATH_MESSAGE = "**{username} {death_message}**";
    public String ADVANCEMENT_MESSAGE = "**{username} has made the advancement __{advancement_title}__**\\n_{advancement_description}_";

    // discord commands
    public Boolean DISCORD_LIST_ENABLED = true;
    public String DISCORD_LIST_SERVER_FORMAT = "[{server_name} {online_players}/{max_players}]";
    public String DISCORD_LIST_PLAYER_FORMAT = "- {username}";
    public String DISCORD_LIST_NO_PLAYERS = "No players online";
    public String DISCORD_LIST_CODEBLOCK_LANG = "asciidoc";

    // minecraft formats
    public String DISCORD_CHUNK = "<dark_gray>[<{discord_color}>Discord<dark_gray>]";
    public String USERNAME_CHUNK = "<{role_color}><hover:show_text:{username}#{discriminator}>{nickname}</hover>";
    public String MC_CHAT_MESSAGE = "{discord_chunk} {username_chunk}<dark_gray>: <white>{message} {attachments}";
    public String ATTACHMENTS = "<click:open_url:{url}>[Attachment]</click>";

    // colors
    public String DISCORD_COLOR = "#7289da";
    public String ATTACHMENT_COLOR = "#4abdff";

    private Toml toml;

    private boolean isFirstRun = false;

    @Inject
    public Config(@DataDirectory Path dataDir) {
        this.dataDir = dataDir;

        loadFile();
        loadConfigs();
    }

    private void loadFile() {
        File dataDirectoryFile = dataDir.toFile();
        if (!dataDirectoryFile.exists()) {
            if (!dataDirectoryFile.mkdir()) {
                throw new RuntimeException("Could not create data directory at " + dataDirectoryFile.getAbsolutePath());
            }
        }

        File dataFile = new File(dataDirectoryFile, "config.toml");

        // create default config if it doesn't exist
        if (!dataFile.exists()) {
            isFirstRun = true;

            try {
                InputStream in = getClass().getResourceAsStream("/config.toml");
                Files.copy(Objects.requireNonNull(in), dataFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException("ERROR: Can't write default configuration file (permissions/filesystem error?)");
            }
        }

        toml = new Toml().read(dataFile);

        // make sure the config makes sense for the current plugin's version
        String version = toml.getString("config_version", CONFIG_VERSION);
        if (versionCompatible(version)) {
            throw new RuntimeException(String.format("ERROR: Can't use the existing configuration file: version mismatch (mod: %s, config: %s)", CONFIG_VERSION, version));
        }
    }

    private boolean versionCompatible(String newVersion) {
        return newVersion.split("\\.")[0].equals(CONFIG_MAJOR_VERSION);
    }

    public void loadConfigs() {
        DISCORD_TOKEN = toml.getString("discord.token", DISCORD_TOKEN);
        CHANNEL_ID = toml.getString("discord.channel", CHANNEL_ID);

        SHOW_BOT_MESSAGES = toml.getBoolean("discord.show_bot_messages", SHOW_BOT_MESSAGES);
        SHOW_ATTACHMENTS = toml.getBoolean("discord.show_attachments_ingame", SHOW_ATTACHMENTS);

        DISCORD_CHAT_MESSAGE = toml.getString("discord.chat.message", DISCORD_CHAT_MESSAGE);
        JOIN_MESSAGE = toml.getString("discord.chat.join_message", JOIN_MESSAGE);
        LEAVE_MESSAGE = toml.getString("discord.chat.leave_message", LEAVE_MESSAGE);
        SERVER_SWITCH_MESSAGE = toml.getString("discord.chat.server_switch_message", SERVER_SWITCH_MESSAGE);
        DEATH_MESSAGE = toml.getString("discord.chat.death_message", DEATH_MESSAGE);
        ADVANCEMENT_MESSAGE = toml.getString("discord.chat.advancement_message", ADVANCEMENT_MESSAGE);

        DISCORD_LIST_ENABLED = toml.getBoolean("discord.commands.list.enabled", DISCORD_LIST_ENABLED);
        DISCORD_LIST_SERVER_FORMAT = toml.getString("discord.commands.list.server_format", DISCORD_LIST_SERVER_FORMAT);
        DISCORD_LIST_PLAYER_FORMAT = toml.getString("discord.commands.list.player_format", DISCORD_LIST_PLAYER_FORMAT);
        DISCORD_LIST_NO_PLAYERS = toml.getString("discord.commands.list.no_players", DISCORD_LIST_NO_PLAYERS);
        DISCORD_LIST_CODEBLOCK_LANG = toml.getString("discord.commands.list.codeblock_lang", DISCORD_LIST_CODEBLOCK_LANG);

        DISCORD_CHUNK = toml.getString("minecraft.discord_chunk", DISCORD_CHUNK);
        USERNAME_CHUNK = toml.getString("minecraft.username_chunk", USERNAME_CHUNK);
        MC_CHAT_MESSAGE = toml.getString("minecraft.message", MC_CHAT_MESSAGE);
        ATTACHMENTS = toml.getString("minecraft.attachments", ATTACHMENTS);
        DISCORD_COLOR = toml.getString("minecraft.discord_color", DISCORD_COLOR);
        ATTACHMENT_COLOR = toml.getString("minecraft.attachment_color", ATTACHMENT_COLOR);
    }

    // Say it's the first run if the config hasn't been edited or has been created this run
    public boolean isFirstRun() {
        return DISCORD_TOKEN.equals(DefaultToken) || CHANNEL_ID.equals(DefaultChannelId) || isFirstRun;
    }
}
