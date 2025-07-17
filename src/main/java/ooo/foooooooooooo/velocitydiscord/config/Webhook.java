package ooo.foooooooooooo.velocitydiscord.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class Webhook {
  private static final Logger logger = LoggerFactory.getLogger(Webhook.class);

  private static final Pattern WEBHOOK_URL_REGEX = Pattern.compile("https?://.*\\.?discord.com/api/webhooks/(\\d*)/.*");

  public static String getWebhookId(String url) {
    var matcher = WEBHOOK_URL_REGEX.matcher(url);

    if (matcher.matches()) {
      return matcher.group(1);
    } else {
      if (!url.isEmpty()) {
        logger.warn("Invalid webhook URL: {}", url);
      }

      return null;
    }
  }

  public static boolean isValidWebhookUrl(String url) {
    return !url.isEmpty() && WEBHOOK_URL_REGEX.matcher(url).matches();
  }
}
