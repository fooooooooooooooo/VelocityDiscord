# VelocityDiscord

Velocity global chat/discord bridge

Default config generated on startup:

```toml
# Don't change this
config_version="1"

[discord]
# Bot token from https://discordapp.com/developers/applications/
token="TOKEN"
# Channel ID to send minecraft chat messages to
channel="000000000000000000"

# Show messages from bots in minecraft chat
show_bot_messages=true
# Show clickable links for attachments in minecraft chat
show_attachments_ingame=true

# Minecraft > Discord message formats
# Uses the same formatting as the Discord client
[discord.chat]
message="{username}: {message}"
join_message="**{username} joined the game**"
leave_message="**{username} left the game**"
server_switch_message="**{username} moved to {current} from {previous}**"

# Discord > Minecraft message formats
# Uses XML-like formatting with https://docs.adventure.kyori.net/minimessage#format
[minecraft]
discord_chunk="<dark_gray>[<{discord_color}>Discord<dark_gray>]<reset>"
username_chunk="<{role_color}><hover:show_text:{username}#{discriminator}>{nickname}</hover><reset>"
message="{discord_chunk} {username_chunk}<dark_gray>: <reset>{message} {attachments}"
attachments="<dark_gray><click:open_url:{url}>[<{attachment_color}>Attachment<dark_gray>]</click><reset>"
```
