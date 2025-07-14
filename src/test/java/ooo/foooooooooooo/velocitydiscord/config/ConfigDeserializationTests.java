package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ConfigDeserializationTests {
  Logger logger = LoggerFactory.getLogger(PluginConfig.class);

  @SuppressWarnings("InnerClassMayBeStatic")
  public class OptionalStringConfig {
    @SerdeKey("first")
    @SerdeDefault(provider = "defaultOptional")
    public Optional<String> first;
    @SerdeKey("second")
    @SerdeDefault(provider = "defaultOptional")
    public Optional<String> second;

    private final transient Supplier<Optional<?>> defaultOptional = Optional::empty;

  }

  @Test
  public void testOptionalStringDeserialization(@TempDir Path tempDir) {
    var config = TestUtils.createConfig("first = 'test'", tempDir, new OptionalStringConfig());

    assertNotNull(config);
    assertTrue(config.first.isPresent(), "First should be present");
    assertEquals("test", config.first.get(), "First should equal 'test'");
    assertFalse(config.second.isPresent(), "Second should not be present");
  }
}
