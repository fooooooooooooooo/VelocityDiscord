package ooo.foooooooooooo.config;

import ch.qos.logback.classic.Level;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.fail;

public class TestUtils {
  public static String readResource(String path) {
    try (var s = TestUtils.class.getResourceAsStream(path)) {
      if (s == null) {
        fail("Resource not found: " + path);
      }

      return new String(s.readAllBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @FunctionalInterface
  public interface ConfigConstructor<T extends Config> {
    T get(com.electronwill.nightconfig.core.Config config);
  }

  // this is insane I wish I could just read from a string instead of making a temp file
  public static <T extends Config> T createConfig(String s, @NotNull Path tempDir, ConfigConstructor<T> ctor) {
    var test = tempDir.resolve("test.toml");

    try (var w = new FileWriter(test.toFile())) {
      w.write(s);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    var toml = FileConfig.of(test);
    toml.load();

    return ctor.get(toml);
  }

  public static void setLogLevel() {
    ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Config.class)).setLevel(Level.INFO);
  }
}
