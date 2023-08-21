package ooo.foooooooooooo.velocitydiscord;

import com.electronwill.nightconfig.core.file.FileConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTests {
  @Test
  void getReturnsEmptyGivenFalse(@TempDir Path tempDir) {
    var toml = createConfig("test = false", tempDir);

    var test = Config.getOptional(toml, "test", "default awa");

    assertFalse(test.isPresent());
  }

  @Test
  void getReturnsEmptyGivenEmptyString(@TempDir Path tempDir) {
    var toml = createConfig("test = ''", tempDir);

    var test = Config.getOptional(toml, "test", "default awa");

    assertFalse(test.isPresent());
  }

  @Test
  void getReturnsValueGivenString(@TempDir Path tempDir) {
    var toml = createConfig("test = 'awa'", tempDir);

    var test = Config.getOptional(toml, "test", "default awa");

    assertTrue(test.isPresent());
    assertEquals(test.get(), "awa");
  }

  @Test
  void getReturnsDefaultValueGivenMissingKey(@TempDir Path tempDir) {
    var toml = createConfig("", tempDir);

    var test = Config.getOptional(toml, "test", "default awa");

    assertTrue(test.isPresent());
    assertEquals(test.get(), "default awa");
  }

  FileConfig createConfig(String s, @NotNull Path tempDir) {
    var test = tempDir.resolve("test.toml");

    try (var w = new FileWriter(test.toFile())) {
      w.write(s);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    var toml = FileConfig.of(test);
    toml.load();

    return toml;
  }
}
