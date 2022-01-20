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
    static final String CONFIG_VERSION = "1";
    public String DISCORD_TOKEN;
    public String CHANNEL_ID;

    // toggles
    public Boolean SHOW_BOT_MESSAGES;
    public Boolean SHOW_ATTACHMENTS;

    // discord formats
    public String DISCORD_CHAT_MESSAGE = "{username}: {message}";
    public String JOIN_MESSAGE = "**{username} joined the game**";
    public String LEAVE_MESSAGE = "**{username} left the game**";
    public String SERVER_SWITCH_MESSAGE = "**{username} moved to {current} from {previous}**";

    // minecraft formats
    public String DISCORD_CHUNK = "<dark_gray>[<{discord_color}>Discord<dark_gray>]";
    public String USERNAME_CHUNK = "<{role_color}><hover:show_text:{username}#{discriminator}>{nickname}</hover>";
    public String MC_CHAT_MESSAGE = "{discord_chunk} {username_chunk}<dark_gray>: <white>{message} {attachments}";
    public String ATTACHMENTS = "<click:open_url:{url}>[Attachment]</click>";

    Path dataDir;
    Toml toml;

    @Inject
    public Config(@DataDirectory Path dataDir) {
        this.dataDir = dataDir;

        loadFile();
        loadConfigs();
    }

    private void loadFile() {
        File dataDirectoryFile = dataDir.toFile();
        if (!dataDirectoryFile.exists()) {
            dataDirectoryFile.mkdir(); // TODO ensure it succeeds
        }

        File dataFile = new File(dataDirectoryFile, "config.toml");

        // create default config if it doesn't exist
        if (!dataFile.exists()) {
            try {
                InputStream in = getClass().getResourceAsStream("/config.toml");
                Files.copy(Objects.requireNonNull(in), dataFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException("ERROR: Can't write default configuration file (permissions/filesystem error?)");
            }
        }

        toml = new Toml().read(dataFile);

        // make sure the config makes sense for the current plugin's version
        String version = toml.getString("config_version", "1");
        if (!version.equals(CONFIG_VERSION)) {
            throw new RuntimeException("ERROR: Can't use the existing configuration file: version mismatch (intended for another, older version?)");
        }
    }

    public void loadConfigs() {
        DISCORD_TOKEN = toml.getString("discord.token", "");
        CHANNEL_ID = toml.getString("discord.channel", "");

        SHOW_BOT_MESSAGES = toml.getBoolean("discord.show_bot_messages", false);
        SHOW_ATTACHMENTS = toml.getBoolean("discord.show_attachments_ingame", true);

        DISCORD_CHAT_MESSAGE = toml.getString("discord.chat.message", DISCORD_CHAT_MESSAGE);
        JOIN_MESSAGE = toml.getString("discord.join_message", JOIN_MESSAGE);
        LEAVE_MESSAGE = toml.getString("discord.leave_message", LEAVE_MESSAGE);
        SERVER_SWITCH_MESSAGE = toml.getString("discord.server_switch_message", SERVER_SWITCH_MESSAGE);

        DISCORD_CHUNK = toml.getString("minecraft.discord_chunk", DISCORD_CHUNK);
        USERNAME_CHUNK = toml.getString("minecraft.username_chunk", USERNAME_CHUNK);
        MC_CHAT_MESSAGE = toml.getString("minecraft.message", MC_CHAT_MESSAGE);
        ATTACHMENTS = toml.getString("minecraft.attachments", ATTACHMENTS);
    }
}
