package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;
import java.util.regex.Pattern;

public class WebhookConfig {
  private static final Pattern WEBHOOK_URL_REGEX = Pattern.compile("https?://.*\\.?discord.com/api/webhooks/(\\d*)/.*");
  private static final Logger LOGGER = LoggerFactory.getLogger(WebhookConfig.class);

  @SerdeKey("url")
  @SerdeDefault(provider = "defaultWebhookUrl")
  public String URL;

  @SuppressWarnings("unused")
  private final transient Supplier<String> defaultWebhookUrl = () -> "";

  @SerdeKey("avatar_url")
  @SerdeDefault(provider = "defaultAvatarUrl")
  public String AVATAR_URL;

  @SuppressWarnings("unused")
  private final transient Supplier<String> defaultAvatarUrl = () -> "https://visage.surgeplay.com/face/96/{uuid}";

  @SerdeKey("username")
  @SerdeDefault(provider = "defaultUsername")
  public String USERNAME;

  @SuppressWarnings("unused")
  private final transient Supplier<String> defaultUsername = () -> "{username}";

  public transient String webhookId = null;

  public boolean invalid() {
    var matcher = WEBHOOK_URL_REGEX.matcher(this.URL);
    if (matcher.matches()) {
      this.webhookId = matcher.group(1);
    } else {
      if (!this.URL.isEmpty()) {
        LOGGER.warn("Invalid webhook URL: {}", this.URL);
      }

      this.webhookId = null;
    }

    return this.URL.isEmpty() || this.webhookId == null;
  }
}
