package ooo.foooooooooooo.velocitydiscord.config;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.HashMap;
import java.util.Optional;

@SuppressWarnings({
  "OptionalUsedAsFieldOrParameterType",
  "unused"
})
public class Config {
  private static final String INVALID_VALUE_FORMAT_STRING =
    "ERROR: `%s` is not a valid value for `%s`, acceptable values: `false`, any string";

  public static final Color GREEN = new Color(0x40bf4f);
  public static final Color RED = new Color(0xbf4040);

  private final com.electronwill.nightconfig.core.Config config;

  public Config(com.electronwill.nightconfig.core.Config config) {
    this.config = config;
  }

  public boolean isEmpty() {
    return this.config.isEmpty();
  }

  public <T> T getOrDefault(String key, T defaultValue) {
    return this.config.getOrElse(key, defaultValue);
  }

  public <T> T get(String key) {
    return this.config.get(key);
  }

  public <T> HashMap<String, T> getMapOrDefault(String key, HashMap<String, T> defaultValue) {
    var value = this.config.getRaw(key);
    if (value instanceof com.electronwill.nightconfig.core.Config subConfig) {
      var map = new HashMap<String, T>();
      for (var entry : subConfig.entrySet()) {
        map.put(entry.getKey(), entry.getValue());
      }
      return map;
    } else if (value == null) {
      return defaultValue;
    } else {
      throw new RuntimeException(String.format("ERROR: `%s` is not a valid map for `%s`", value, key));
    }
  }

  public @Nullable Config getConfig(String key) {
    var value = this.config.getRaw(key);
    if (value instanceof com.electronwill.nightconfig.core.Config subConfig) {
      return new Config(subConfig);
    } else if (value == null) {
      return null;
    } else {
      throw new RuntimeException(String.format("ERROR: `%s` is not a valid config for `%s`", value, key));
    }
  }

  public Optional<String> getOptional(String key, Optional<String> defaultValue) {
    var value = this.config.getRaw(key);

    switch (value) {
      case null -> {
        return defaultValue;
      }
      case Boolean bool -> {
        if (!bool) {
          return Optional.empty();
        } else {
          throw new RuntimeException(String.format(INVALID_VALUE_FORMAT_STRING, "true", key));
        }
      }
      case String str -> {
        if (str.isEmpty()) {
          return Optional.empty();
        }

        return Optional.of(str);
      }
      default -> throw new RuntimeException(String.format(INVALID_VALUE_FORMAT_STRING, value, key));
    }
  }

  public Optional<String> getDisableableString(String path) {
    return getOptional(path, Optional.empty());
  }

  public Optional<String> getDisableableStringOrDefault(String path, Optional<String> defaultValue) {
    return getOptional(path, defaultValue);
  }

  public Color getColor(String path) {
    return Color.decode(this.config.get(path));
  }

  public Color getColorOrDefault(String path, Color defaultValue) {
    return Color.decode(getOrDefault(path, encodeColor(defaultValue)));
  }

  public Optional<Color> getDisableableColor(String path) {
    return getDisableableString(path).map(Color::decode);
  }

  public Optional<Color> getDisableableColorOrDefault(String path, Optional<Color> defaultValue) {
    return getDisableableStringOrDefault(path, defaultValue.map(Config::encodeColor)).map(Color::decode);
  }

  public static String encodeColor(Color color) {
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
  }
}
