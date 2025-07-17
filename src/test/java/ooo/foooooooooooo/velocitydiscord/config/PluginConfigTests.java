package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PluginConfigTests {
  Logger logger = LoggerFactory.getLogger(PluginConfigTests.class);

  Config createConfig(String s, @NotNull Path tempDir) {
    var test = tempDir.resolve("test.toml");

    try (var w = new FileWriter(test.toFile())) {
      w.write(s);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    var toml = FileConfig.of(test);
    toml.load();

    return new Config(toml);
  }

  String getResource(String name) {
    var resource = PluginConfigTests.class.getClassLoader().getResourceAsStream(name);

    try (resource) {
      if (resource == null) throw new RuntimeException("Resource not found: " + name);
      return new String(resource.readAllBytes());
    } catch (IOException e) {
      throw new RuntimeException("Failed to read resource: " + name, e);
    }
  }

  @Test
  public void testPluginConfig(@TempDir Path tempDir) {
    var config = createConfig(getResource("config.toml"), tempDir);
    var pluginConfig = new PluginConfig(config, this.logger);

    assertEquals("test_token", pluginConfig.global.discord.token);
  }
}
