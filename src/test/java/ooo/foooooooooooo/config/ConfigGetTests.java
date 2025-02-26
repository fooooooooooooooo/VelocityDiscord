package ooo.foooooooooooo.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigGetTests {
  @Variants
  enum TestEnum {
    @Variants.Key("first")
    ONE,
    @Variants.Key("second")
    TWO,
    @Variants.Key("third")
    THREE
  }

  enum TestEnumNoAttributes {
    ONE, TWO, THREE
  }

  @Test
  void getEnumReturnsCorrectVariant(@TempDir Path tempDir) {
    var toml = TestUtils.createConfig("test = 'second'", tempDir, Config::new);

    String value = Config.get(toml, "test");
    var test = Config.getEnum("test", value, TestEnum.class);

    assertEquals(TestEnum.TWO, test);
  }

  @Test
  void getEnumThrowsGivenOriginalVariantName(@TempDir Path tempDir) {
    var toml = TestUtils.createConfig("test = 'ONE'", tempDir, Config::new);

    try {
      String value = Config.get(toml, "test");
      Config.getEnum("test", value, TestEnum.class);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals(
        "Invalid enum value `ONE` for `test`, acceptable values: `first`, `second`, `third`",
        e.getMessage()
      );
    }
  }

  @Test
  void getEnumHandlesCaseInsensitiveKey(@TempDir Path tempDir) {
    var toml = TestUtils.createConfig("test = 'FiRsT'", tempDir, Config::new);

    String value = Config.get(toml, "test");
    var test = Config.getEnum("test", value, TestEnum.class);

    assertEquals(TestEnum.ONE, test);
  }

  @Test
  void getEnumReturnsCorrectVariantNoAnnotations(@TempDir Path tempDir) {
    var toml = TestUtils.createConfig("test = 'two'", tempDir, Config::new);

    String value = Config.get(toml, "test");
    var test = Config.getEnum("test", value, TestEnumNoAttributes.class);

    assertEquals(TestEnumNoAttributes.TWO, test);
  }


  @Test
  void getEnumHandlesCaseInsensitiveKeyNoAnnotations(@TempDir Path tempDir) {
    var toml = TestUtils.createConfig("test = 'oNe'", tempDir, Config::new);

    String value = Config.get(toml, "test");
    var test = Config.getEnum("test", value, TestEnumNoAttributes.class);

    assertEquals(TestEnumNoAttributes.ONE, test);
  }

  @Test
  void getOrDefaultReturnsEmptyGivenFalse(@TempDir Path tempDir) {
    var toml = TestUtils.createConfig("test = false", tempDir, Config::new);

    var test = Config.getOptional(toml, "test", Optional.of("default awa"));

    assertFalse(test.isPresent());
  }

  @Test
  void getOrDefaultReturnsEmptyGivenEmptyString(@TempDir Path tempDir) {
    var toml = TestUtils.createConfig("test = ''", tempDir, Config::new);

    var test = Config.getOptional(toml, "test", Optional.of("default awa"));

    assertFalse(test.isPresent());
  }

  @Test
  void getOrDefaultReturnsValueGivenString(@TempDir Path tempDir) {
    var toml = TestUtils.createConfig("test = 'awa'", tempDir, Config::new);

    var test = Config.getOptional(toml, "test", Optional.of("default awa"));

    assertTrue(test.isPresent());
    assertEquals("awa", test.get());
  }

  @Test
  void getOrDefaultReturnsDefaultValueGivenMissingKey(@TempDir Path tempDir) {
    var toml = TestUtils.createConfig("", tempDir, Config::new);

    var test = Config.getOptional(toml, "test", Optional.of("default awa"));

    assertTrue(test.isPresent());
    assertEquals("default awa", test.get());
  }
}
