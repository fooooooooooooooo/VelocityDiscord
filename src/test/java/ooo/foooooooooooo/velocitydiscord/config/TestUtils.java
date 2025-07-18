package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import org.jetbrains.annotations.NotNull;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class TestUtils {
  public static Config createConfig(String s, @NotNull Path tempDir) {
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

  public static String getResource(String name) {
    var resource = PluginConfigTests.class.getClassLoader().getResourceAsStream(name);

    try (resource) {
      if (resource == null) throw new RuntimeException("Resource not found: " + name);
      return new String(resource.readAllBytes());
    } catch (IOException e) {
      throw new RuntimeException("Failed to read resource: " + name, e);
    }
  }
}
