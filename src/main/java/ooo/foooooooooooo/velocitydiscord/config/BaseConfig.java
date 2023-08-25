package ooo.foooooooooooo.velocitydiscord.config;

import java.util.Optional;

public abstract class BaseConfig {
  protected abstract void loadConfig(com.electronwill.nightconfig.core.Config config);

  public static <T> T get(com.electronwill.nightconfig.core.Config config, String key, T defaultValue) {
    return config.getOrElse(key, defaultValue);
  }

  private static final String invalidValueFormatString = "ERROR: `%s` is not a valid value for `%s`, acceptable values: `false`, any string";

  public static Optional<String> getOptional(com.electronwill.nightconfig.core.Config config, String key, String defaultValue) {
    var value = config.getRaw(key);

    if (value == null) {
      return Optional.of(defaultValue);
    }

    if (value instanceof Boolean bool) {
      if (!bool) {
        return Optional.empty();

      } else {
        throw new RuntimeException(String.format(invalidValueFormatString, "true", key));
      }
    }

    if (value instanceof String str) {
      if (str.isEmpty()) {
        return Optional.empty();
      }

      return Optional.of(str);
    }

    throw new RuntimeException(String.format(invalidValueFormatString, value, key));
  }
}
