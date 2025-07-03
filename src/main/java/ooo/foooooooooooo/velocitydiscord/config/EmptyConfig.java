package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlFormat;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EmptyConfig implements UnmodifiableConfig {
  @Override
  public <T> T getRaw(List<String> path) {
    return null;
  }

  @Override
  public boolean contains(List<String> path) {
    return false;
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  @SuppressWarnings({"deprecation", "RedundantSuppression"})
  public Map<String, Object> valueMap() {
    return Map.of();
  }

  @Override
  public Set<? extends Entry> entrySet() {
    return Set.of();
  }

  @Override
  public ConfigFormat<?> configFormat() {
    return TomlFormat.instance();
  }

  public static <T> T deserialize(T object) {
    Deserialization.deserializer().deserializeFields(new EmptyConfig(), object);
    return object;
  }
}
