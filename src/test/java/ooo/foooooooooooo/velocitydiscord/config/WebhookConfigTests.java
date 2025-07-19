package ooo.foooooooooooo.velocitydiscord.config;

import ooo.foooooooooooo.velocitydiscord.config.definitions.WebhookConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class WebhookConfigTests {
  @Test
  public void webhookIdParsedCorrectly(@TempDir Path tempDir) {
    var content = """
      url = "https://discord.com/api/webhooks/1290368230789893527/tokentokentokentokentokentokentokentokentokentokentokentokentoken"
      username = "{username}"
      """;

    var config = TestUtils.createConfig(content, tempDir);
    var webhookConfig = new WebhookConfig();
    webhookConfig.load(config);

    assertNotNull(webhookConfig.id);
    assertEquals("1290368230789893527", webhookConfig.id);
    assertFalse(webhookConfig.isInvalid());
  }
}
