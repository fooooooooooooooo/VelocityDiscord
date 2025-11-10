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

### Configuration

The config file is generated at `plugins/discord/config.toml`. See [here](wiki/Configuration) for the default config
