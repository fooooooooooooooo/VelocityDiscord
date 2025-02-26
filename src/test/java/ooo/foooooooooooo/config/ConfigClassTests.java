package ooo.foooooooooooo.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigClassTests {
  public ConfigClassTests() {
    TestUtils.setLogLevel();
  }

  static class TestConfig extends Config {
    @Key("test")
    public String TEST = "default";
    @Key("test_enum")
    public TestEnum TEST_ENUM = TestEnum.THREE;
    @Key("inner")
    public TestConfigInnerClass INNER;

    public TestConfig(com.electronwill.nightconfig.core.Config config) {
      super(config);
      loadConfig();
    }

    @Variants
    enum TestEnum {
      @Variants.Key("first")
      ONE,
      @Variants.Key("second")
      TWO,
      @Variants.Key("third")
      THREE
    }

    static class TestConfigInnerClass extends Config {
      @Key("test")
      public String INNER_TEST = "default";
      @Key("test_enum")
      public TestEnum INNER_TEST_ENUM = TestEnum.THREE;

      public TestConfigInnerClass(com.electronwill.nightconfig.core.Config config) {
        super(config);
      }
    }
  }

  @Test
  void loadConfigSetsFields(@TempDir Path tempDir) {
    var toml = """
      test = 'new'
      test_enum = 'second'
      """;
    var config = TestUtils.createConfig(toml, tempDir, TestConfig::new);

    assertEquals("new", config.TEST);
    assertEquals(TestConfig.TestEnum.TWO, config.TEST_ENUM);
  }

  @Test
  void loadConfigSetsDefaultFields(@TempDir Path tempDir) {
    var config = TestUtils.createConfig("", tempDir, TestConfig::new);

    assertEquals("default", config.TEST);
    assertEquals(TestConfig.TestEnum.THREE, config.TEST_ENUM);
  }

  @Test
  void loadConfigSetsInnerFields(@TempDir Path tempDir) {
    var toml = """
      test = 'new'
      test_enum = 'second'
      
      [inner]
      test = 'new'
      test_enum = 'second'
      """;

    var config = TestUtils.createConfig(toml, tempDir, TestConfig::new);

    assertEquals("new", config.TEST);
    assertEquals(TestConfig.TestEnum.TWO, config.TEST_ENUM);
    assertEquals("new", config.INNER.INNER_TEST);
    assertEquals(TestConfig.TestEnum.TWO, config.INNER.INNER_TEST_ENUM);
  }
}
