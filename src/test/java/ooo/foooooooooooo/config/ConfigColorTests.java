package ooo.foooooooooooo.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigColorTests {
  public ConfigColorTests() {
    TestUtils.setLogLevel();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  static class TestColorConfig extends Config {
    @Key("normal_color")
    public Color NORMAL_COLOR = Color.BLACK;

    @Key("optional_color")
    public Optional<Color> OPTIONAL_COLOR = Optional.of(Color.BLACK);

    @Key("null_color")
    public Color NULL_COLOR = Color.BLACK;

    @Key("null_optional_color")
    public Optional<Color> NULL_OPTIONAL_COLOR = Optional.empty();

    public TestColorConfig(com.electronwill.nightconfig.core.Config config) {
      super(config);
      loadConfig();
    }
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  void shouldHandleValidColors(@TempDir Path tempDir) {
    var toml = """
      normal_color = "#ff0000"
      optional_color = "#00ff00"
      """;

    var config = TestUtils.createConfig(toml, tempDir, TestColorConfig::new);

    assertEquals(Color.decode("#ff0000"), config.NORMAL_COLOR);
    assertEquals(Color.decode("#00ff00"), config.OPTIONAL_COLOR.get());
    // Fields not in config should keep defaults
    assertEquals(Color.BLACK, config.NULL_COLOR);
    assertTrue(config.NULL_OPTIONAL_COLOR.isEmpty());
  }

  @Test
  void shouldHandleEmptyColorString(@TempDir Path tempDir) {
    var toml = """
      optional_color = ""
      """;

    var config = TestUtils.createConfig(toml, tempDir, TestColorConfig::new);

    // Optional colors with empty string should be Optional.empty()
    assertTrue(config.OPTIONAL_COLOR.isEmpty());
    // Default value should be kept for non-specified field
    assertEquals(Color.BLACK, config.NORMAL_COLOR);
  }

  @Test
  void shouldThrowForNonOptionalEmptyColor(@TempDir Path tempDir) {
    var toml = """
      normal_color = ""
      optional_color = "#00ff00"
      """;

    try {
      TestUtils.createConfig(toml, tempDir, TestColorConfig::new);
      fail("Expected RuntimeException");
    } catch (NumberFormatException e) {
      assertEquals("Zero length string", e.getMessage());
    }
  }

  @Test
  void shouldThrowForInvalidColorFormat(@TempDir Path tempDir) {
    var toml = """
      normal_color = "not-a-color"
      """;

    try {
      TestUtils.createConfig(toml, tempDir, TestColorConfig::new);
      fail("Expected NumberFormatException");
    } catch (NumberFormatException e) {
      assertTrue(e.getMessage().contains("not-a-color"));
    }
  }

  @Test
  void shouldHandleOptionalInvalidColorFormat(@TempDir Path tempDir) {
    var toml = """
      optional_color = "not-a-color"
      """;

    try {
      TestUtils.createConfig(toml, tempDir, TestColorConfig::new);
      fail("Expected NumberFormatException");
    } catch (NumberFormatException e) {
      assertTrue(e.getMessage().contains("not-a-color"));
    }
  }
}
