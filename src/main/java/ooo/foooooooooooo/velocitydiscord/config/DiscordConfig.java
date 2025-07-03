package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.core.serde.annotations.SerdeAssert;
import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;
import ooo.foooooooooooo.velocitydiscord.config.commands.ListCommandConfig;
import ooo.foooooooooooo.velocitydiscord.config.commands.ListCommandGlobalConfig;

import java.util.Optional;
import java.util.function.Supplier;

import static com.electronwill.nightconfig.core.serde.annotations.SerdeAssert.AssertThat.NOT_NULL;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DiscordConfig {
  private static final String DefaultChannelId = "000000000000000000";

  // bot
  @SerdeKey("channel")
  @SerdeDefault(provider = "defaultChannelId")
  public String MAIN_CHANNEL_ID;

  @SuppressWarnings("unused")
  private final transient Supplier<String> defaultChannelId = () -> DefaultChannelId;

  // pings
  @SerdeKey("enable_mentions")
  @SerdeDefault(provider = "defaultEnableMentions")
  public Boolean ENABLE_MENTIONS;

  @SuppressWarnings("unused")
  private final transient Supplier<Boolean> defaultEnableMentions = () -> true;

  @SerdeKey("enable_everyone_and_here")
  @SerdeDefault(provider = "defaultEnableEveryoneAndHere")
  public Boolean ENABLE_EVERYONE_AND_HERE;

  @SuppressWarnings("unused")
  private final transient Supplier<Boolean> defaultEnableEveryoneAndHere = () -> false;

  // base webhook
  @SerdeKey("webhook")
  @SerdeDefault(provider = "defaultWebhookConfig")
  public WebhookConfig WEBHOOK;

  @SuppressWarnings("unused")
  private final transient Supplier<WebhookConfig> defaultWebhookConfig = () -> EmptyConfig.deserialize(new WebhookConfig());

  // list command
  @SerdeKey("commands.list")
  @SerdeDefault(provider = "defaultCommandsListConfig")
  public ListCommandConfig COMMANDS_LIST;

  @SuppressWarnings("unused")
  private final transient Supplier<ListCommandConfig> defaultCommandsListConfig = () -> EmptyConfig.deserialize(new ListCommandConfig());

  @SerdeKey("commands.list.global")
  @SerdeDefault(provider = "defaultCommandsListGlobalConfig")
  public ListCommandGlobalConfig COMMANDS_LIST_GLOBAL;

  @SuppressWarnings("unused")
  private final transient Supplier<ListCommandGlobalConfig> defaultCommandsListGlobalConfig = () -> EmptyConfig.deserialize(new ListCommandGlobalConfig());

  // channel topic
  @SerdeKey("channel_topic.format")
  @SerdeDefault(provider = "defaultTopicFormat")
  public Optional<String> TOPIC_FORMAT;

  @SuppressWarnings("unused")
  private final transient Supplier<Optional<String>> defaultTopicFormat = () -> Optional.of("""
    {players}/{max_players}
    {player_list}
    {hostname}:{port}
    Uptime: {uptime}""");

  @SerdeKey("channel_topic.server")
  @SerdeDefault(provider = "defaultTopicServerFormat")
  public Optional<String> TOPIC_SERVER_FORMAT;

  @SuppressWarnings("unused")
  private final transient Supplier<Optional<String>> defaultTopicServerFormat = () -> Optional.of("{name}: {players}/{max_players}");

  @SerdeKey("channel_topic.server_offline")
  @SerdeDefault(provider = "defaultTopicServerOfflineFormat")
  public Optional<String> TOPIC_SERVER_OFFLINE_FORMAT;

  @SuppressWarnings("unused")
  private final transient Supplier<Optional<String>> defaultTopicServerOfflineFormat = () -> Optional.of("{name}: Offline");

  @SerdeKey("channel_topic.player_list_no_players_header")
  @SerdeDefault(provider = "defaultTopicPlayerListNoPlayersHeader")
  public Optional<String> TOPIC_PLAYER_LIST_NO_PLAYERS_HEADER;

  @SuppressWarnings("unused")
  private final transient Supplier<Optional<String>> defaultTopicPlayerListNoPlayersHeader = () -> Optional.of("No players online");

  @SerdeKey("channel_topic.player_list_header")
  @SerdeDefault(provider = "defaultTopicPlayerListHeader")
  public Optional<String> TOPIC_PLAYER_LIST_HEADER;

  @SuppressWarnings("unused")
  private final transient Supplier<Optional<String>> defaultTopicPlayerListHeader = () -> Optional.of("Players: ");

  @SerdeKey("channel_topic.player_list_player")
  @SerdeDefault(provider = "defaultTopicPlayerListFormat")
  public String TOPIC_PLAYER_LIST_FORMAT;

  @SuppressWarnings("unused")
  private final transient Supplier<String> defaultTopicPlayerListFormat = () -> "{username}";

  @SerdeKey("channel_topic.player_list_separator")
  @SerdeDefault(provider = "defaultTopicPlayerListSeparator")
  public String TOPIC_PLAYER_LIST_SEPARATOR;

  @SuppressWarnings("unused")
  private final transient Supplier<String> defaultTopicPlayerListSeparator = () -> ", ";

  @SerdeKey("channel_topic.player_list_max_count")
  @SerdeDefault(provider = "defaultTopicPlayerListMaxCount")
  public int TOPIC_PLAYER_LIST_MAX_COUNT;

  @SuppressWarnings("unused")
  private final transient Supplier<Integer> defaultTopicPlayerListMaxCount = () -> 10;

  public boolean isDefaultValues() {
    return this.MAIN_CHANNEL_ID.equals(DefaultChannelId);
  }
}
