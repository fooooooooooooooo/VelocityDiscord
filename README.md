# Velocity Discord

Chat from all servers gets bridged with a discord channel

## Features

- Configurable
- Webhooks or embeds or normal text for messages
- Player count in bot status
- List command
- Templating syntax for all messages
- Death and Advancement messages shown
- Server start/stop messages
- Server status in channel topic
- Reload command for config changes while the server is running

> **Note**
> This requires a [companion Velocity plugin](https://github.com/unilock/YepLib)
> and [companion backend mod/plugin](https://github.com/unilock/YepTwo) for advancement/death messages

## Installation

1. Create a bot application [here](https://discordapp.com/developers/applications/)
    - Go to the `Bot` tab and click `Add bot`
2. Enable the `SERVER MEMBERS INTENT` and `MESSAGE CONTENT INTENT` under `Privileged Gateway Intents`
3. Copy the bot's token, you might have to click `Reset Token` first
4. Install the plugin on your server, start the server once, then stop the server again
5. Open the plugin config file at `plugins/discord/config.toml`
6. Under `[discord]`, paste your token in place of `TOKEN`
7. Under `[discord]`, paste the channel id you want to use
    - To get a channel id, you have to enable developer mode in Discord
    - Open Discord settings, go to `Advanced`, then turn on `Developer Mode`
    - Now right-click the channel you want to use and click `Copy ID`
8. Set any additional config options you want
9. Start the server and check if it works

### For Webhooks

1. Create a webhook in the channel you want to use
    - Right-click the channel, click `Edit Channel`, go to `Integrations`, click `Create Webhook`
    - Copy the webhook URL
2. Paste the webhook URL under `[discord.webhook]` in the config file

### For advancements/deaths

1. Install the [YepLib](https://github.com/unilock/YepLib) velocity plugin alongside this plugin
2. Install the [YepTwo](https://github.com/unilock/YepTwo) backend mod/plugin on each of your backend servers that you want to
   receive advancements/deaths from

## Configuration

Default config generated on startup:

```toml
# Don't change this
config_version = "1.9"

# Comma separated list of server names to exclude from the bridge (defined under [servers] inside your velocity.toml)
# e.g., exclude_servers = ["lobby", "survival"]
exclude_servers = []
excluded_servers_receive_messages = false

# How often to ping all servers to check for online status (seconds)
# Set to 0 to disable
# Excluded servers will not be pinged
ping_interval = 30

[discord]
# Bot token from https://discordapp.com/developers/applications/
token = "TOKEN"
# Channel ID to send Minecraft chat messages to
channel = "000000000000000000"

# Show messages from bots in Minecraft chat
show_bot_messages = false
# Show clickable links for attachments in Minecraft chat
show_attachments_ingame = true

# Show a text as playing activity of the bot
show_activity = true
# Activity text of the bot to show in Discord
# Placeholders available: {amount}
activity_text = "with {amount} players online"

# Enable mentioning Discord users from Minecraft chat
enable_mentions = true
# Enable @everyone and @here pings from Minecraft chat
enable_everyone_and_here = false

# OPTIONAL - Configuration for updating the Discord channel topic
# Set the interval (in minutes) for updating the channel topic.
# Use a value less than 10 to disable this feature.
update_channel_topic_interval = -1

# Template for the channel topic.
# Placeholders available:
# {players} - Total number of players online
# {player_list} - List of players (format is defined below)
# {servers} - Number of servers
# {server_list} - List of server names
# {hostname} - Server hostname
# {port} - Server port
# {motd} - Message of the Day (MOTD)
# {query_port} - Query port
# {max_players} - Maximum number of players
# {plugins} - Number of plugins
# {plugin_list} - List of plugin names
# {version} - Server version
# {software} - Software name
# {average_ping} - Average ping of all players
# {uptime} - Server uptime in hours and minutes
# {server[SERVERNAME]} - Dynamic placeholder for each server's name and status (e.g., {server[MyServer]}, {server[AnotherServer]}, {server[Lobby]}, etc.)
topic = """{players}/{max_players}
{player_list}
{hostname}:{port}
Uptime: {uptime}"""

# Template for server[SERVERNAME] placeholder in the channel topic.
# Placeholders available: {name}, {players}, {max_players}, {motd}, {version}, {protocol}
topic_server = "{name}: {players}/{max_players}"

# Template for server[SERVERNAME] placeholder in the channel topic when the server is offline.
# Placeholders available: {name}
topic_server_offline = "{name}: Offline"

# Can be disabled to hide the list completely when no players are online
topic_player_list_no_players_header = "No players online"

# Can be disabled to hide the header and only show the player list
topic_player_list_header = "Players: "

# Placeholders available: {username}, {ping}
topic_player_list_player = "{username}"

# Separator between players in the list, \n can be used for new line
topic_player_list_separator = ", "

# Maximum number of players to show in the topic
# Set to < 1 to show all players
topic_player_list_max_count = 10

[discord.webhook]
# Full webhook URL to send more fancy Minecraft chat messages to
webhook_url = ""
# Full URL of an avatar service to get the player's avatar from
# Placeholders available: {uuid}, {username}
avatar_url = "https://visage.surgeplay.com/face/96/{uuid}"

# The format of the webhook's username
# Placeholders available: {username}, {server}
webhook_username = "{username}"

# Minecraft > Discord message formats
# Uses the same formatting as the Discord client (a subset of markdown)
# Messages can be disabled with empty string ("") or false
#
# x_message_type can be one of the following:
# "text"  - Normal text only message with the associated x_message format
# "embed" - Discord embed with the associated x_message format as the description field
# Default for all is "text"
#
# x_message_embed_color is the color of the embed, in #RRGGBB format
[discord.chat]

# Placeholders available: {username}, {prefix}, {server}, {message}
# Can be disabled
message = "{username}: {message}"

# for user messages, the following types can be used
# "text"    - Normal text only message with the above
#
# "webhook" - Use a Discord webhook to have the bot use the player's username and avatar when sending messages
#             Requires a webhook URL to be set below
#             Ignores the above message format, and just sends the message as the content of the webhook
#
# "embed"   - Discord embed with the above format as the description field
message_type = "text"
# Can be disabled
message_embed_color = ""

# Placeholders available: {username}, {prefix}, {server}
# Can be disabled
join_message = "**{username} joined the game**"
join_message_type = "text"
# Can be disabled
join_message_embed_color = "#40bf4f"

leave_message = "**{username} left the game**"
leave_message_type = "text"
# Can be disabled
leave_message_embed_color = "#bf4040"

# Possible different format for timeouts or other terminating connections
# Placeholders available: {username}, {prefix}
# Can be disabled
disconnect_message = "**{username} disconnected**"
disconnect_message_type = "text"
# Can be disabled
disconnect_message_embed_color = "#bf4040"

# Placeholders available: {username}, {prefix}, {current}, {previous}
# Can be disabled
server_switch_message = "**{username} moved to {current} from {previous}**"
server_switch_message_type = "text"
# Can be disabled
server_switch_message_embed_color = "#40bf4f"

# Placeholders available: {username}, {death_message}
# death_message includes the username just as it is shown ingame
# Can be disabled
death_message = "**{death_message}**"
death_message_type = "text"
# Can be disabled
death_message_embed_color = "#bf4040"

# Placeholders available: {username}, {advancement_title}, {advancement_description}
# Can be disabled
advancement_message = "**{username} has made the advancement __{advancement_title}__**\n_{advancement_description}_"
advancement_message_type = "text"
# Can be disabled
advancement_message_embed_color = "#40bf4f"

# Can be disabled
proxy_start_message = "**Proxy started**"
proxy_start_message_type = "text"
# Can be disabled
proxy_start_message_embed_color = "#40bf4f"

# Can be disabled
proxy_stop_message = "**Proxy stopped**"
proxy_stop_message_type = "text"
# Can be disabled
proxy_stop_message_embed_color = "#bf4040"

# Placeholders available: {server}
# Can be disabled
server_start_message = "**{server} has started**"
server_start_message_type = "text"
# Can be disabled
server_start_message_embed_color = "#40bf4f"

# Placeholders available: {server}
# Can be disabled
server_stop_message = "**{server} has stopped**"
server_stop_message_type = "text"
# Can be disabled
server_stop_message_embed_color = "#bf4040"

[discord.commands.list]
enabled = true

# Ephemeral messages are only visible to the user who sent the command
ephemeral = true

# Placeholders available: {server_name}, {online_players}, {max_players}
server_format = "[{server_name} {online_players}/{max_players}]"

# Placeholders available: {username}
player_format = "- {username}"

# Can be disabled
no_players = "No players online"

# Can be disabled
server_offline = "Server offline"
codeblock_lang = "asciidoc"

# Discord > Minecraft message formats
# Uses XML-like formatting with https://docs.advntr.dev/minimessage/format.html
[minecraft]
# Placeholders available: {discord}
discord_chunk = "<dark_gray>[<{discord_color}>Discord<dark_gray>]<reset>"

# Placeholders available: {role_color}, {display_name}, {username}, {nickname}
# <insert> tag allows you to shift right-click the username to insert @{username} in the chat
username_chunk = "<{role_color}><insert:@{username}><hover:show_text:{display_name}>{nickname}</hover></insert><reset>"

# Placeholders available: {discord_chunk}, {username_chunk}, {attachments}, {message}
message = "{discord_chunk} {username_chunk}<dark_gray>: <reset>{message} {attachments}"

# Placeholders available: {url}, {attachment_color}
attachments = "<dark_gray><click:open_url:{url}>[<{attachment_color}>Attachment<dark_gray>]</click><reset>"

# Colors for the <{discord_color}> and <{attachment_color}> tags
discord_color = "#7289da"
attachment_color = "#4abdff"
```
