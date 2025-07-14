package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.fail;

public class TestUtils {
  static Logger logger = LoggerFactory.getLogger(TestUtils.class);

  private static FileConfig loadConfigData(String content, Path tempDir) {
    var test = tempDir.resolve("test.toml");

    try (var w = new FileWriter(test.toFile())) {
      w.write(content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    var config = FileConfig.of(test);
    config.load();

    return config;
  }

  public static PluginConfig createPluginConfig(String content, Path tempDir) {
    var config = TestUtils.loadConfigData(content, tempDir);

    return new PluginConfig(config, logger);
  }

  public static <T> T createConfig(String content, Path tempDir, T instance) {
    var config = TestUtils.loadConfigData(content, tempDir);

    return Deserialization.deserialize(config, instance);
  }

  public static String readResource(String path) {
    try (var s = PluginConfigTests.class.getResourceAsStream(path)) {
      if (s == null) {
        fail("Resource not found: " + path);
      }

      return new String(s.readAllBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
