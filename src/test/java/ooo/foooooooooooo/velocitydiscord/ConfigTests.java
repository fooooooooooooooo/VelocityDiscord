package ooo.foooooooooooo.velocitydiscord;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import ooo.foooooooooooo.velocitydiscord.config.BaseConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTests {
  @Test
  void getReturnsEmptyGivenFalse(@TempDir Path tempDir) {
    var toml = new BaseConfig(createConfig("test = false", tempDir));

    var test = BaseConfig.getOptional(toml, "test", Optional.of("default awa"));

    assertFalse(test.isPresent());
  }

  @Test
  void getReturnsEmptyGivenEmptyString(@TempDir Path tempDir) {
    var toml = new BaseConfig(createConfig("test = ''", tempDir));

    var test = BaseConfig.getOptional(toml, "test", Optional.of("default awa"));

    assertFalse(test.isPresent());
  }

  @Test
  void getReturnsValueGivenString(@TempDir Path tempDir) {
    var toml = new BaseConfig(createConfig("test = 'awa'", tempDir));

    var test = BaseConfig.getOptional(toml, "test", Optional.of("default awa"));

    assertTrue(test.isPresent());
    assertEquals(test.get(), "awa");
  }

  @Test
  void getReturnsDefaultValueGivenMissingKey(@TempDir Path tempDir) {
    var toml = new BaseConfig(createConfig("", tempDir));

    var test = BaseConfig.getOptional(toml, "test", Optional.of("default awa"));

    assertTrue(test.isPresent());
    assertEquals(test.get(), "default awa");
  }

  // this is insane I wish I could just read from a string instead of making a temp file
  Config createConfig(String s, @NotNull Path tempDir) {
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
