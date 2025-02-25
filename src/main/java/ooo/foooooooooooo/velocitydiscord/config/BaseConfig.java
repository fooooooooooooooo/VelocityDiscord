package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import ooo.foooooooooooo.velocitydiscord.VelocityDiscord;

import javax.annotation.Nullable;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class BaseConfig {
  private static final String INVALID_VALUE_FORMAT_STRING =
    "ERROR: `%s` is not a valid value for `%s`, acceptable values: `false`, any string";

  public Config inner;
  private @Nullable BaseConfig main;

  public BaseConfig(com.electronwill.nightconfig.core.Config config) {
    this.inner = config;
  }

  public BaseConfig(com.electronwill.nightconfig.core.Config config, @Nullable BaseConfig main) {
    this.inner = config;
    // get every field as optional, fallback to main
    this.main = main;
  }

  public static <T> T get(BaseConfig config, String key, T defaultValue) {
    return config.inner.getOrElse(key, defaultValue);
  }

  public static Optional<String> getOptional(BaseConfig config, String key, Optional<String> defaultValue) {
    var value = config.inner.getRaw(key);

    if (value == null) {
      return defaultValue;
    }

    if (value instanceof Boolean bool) {
      if (!bool) {
        return Optional.empty();

      } else {
        throw new RuntimeException(String.format(INVALID_VALUE_FORMAT_STRING, "true", key));
      }
    }

    if (value instanceof String str) {
      if (str.isEmpty()) {
        return Optional.empty();
      }

      return Optional.of(str);
    }

    throw new RuntimeException(String.format(INVALID_VALUE_FORMAT_STRING, value, key));
  }

  public static Optional<Color> getColor(BaseConfig config, String key, Optional<Color> defaultValue) {
    var defaultHex = defaultValue.map((c) -> String.format("#%06X", (0xFFFFFF & c.getRGB())));
    return BaseConfig.getOptional(config, key, defaultHex).map(Color::decode);
  }

  public static MessageType getMessageType(BaseConfig config, String key, MessageType defaultValue) {
    var type = BaseConfig.get(config, key, defaultValue.toString().toLowerCase());
    return switch (type) {
      case "text" -> MessageType.TEXT;
      case "embed" -> MessageType.EMBED;
      case "" -> defaultValue;
      default -> throw new RuntimeException("Invalid message type: " + type);
    };
  }

  public static UserMessageType getUserMessageType(BaseConfig config, String key, UserMessageType defaultValue) {
    var type = BaseConfig.get(config, key, defaultValue.toString().toLowerCase());
    return switch (type) {
      case "text" -> UserMessageType.TEXT;
      case "webhook" -> UserMessageType.WEBHOOK;
      case "embed" -> UserMessageType.EMBED;
      case "" -> defaultValue;
      default -> throw new RuntimeException("Invalid message type: " + type);
    };
  }

  public static Set<Field> getConfigFields(Object instance) {
    var clazz = instance.getClass();
    return Arrays.stream(clazz.getFields()).filter(f -> f.isAnnotationPresent(Key.class)).collect(Collectors.toSet());
  }

  public void setInner(com.electronwill.nightconfig.core.Config config) {
    this.inner = config;
  }

  protected void loadConfig() {
    for (var field : getConfigFields(this)) {
      var annotation = field.getAnnotation(Key.class);
      var key = annotation.value();

      if (this.main != null) {
        // check if config key does not exist in this.inner or if the field is not overridable
        // and set field value to the value of the same field in main
        if (!this.inner.contains(key) || !annotation.overridable()) {
          VelocityDiscord.LOGGER.trace("Inheriting value of field {} from main config", field.getName());
          try {
            field.set(this, field.get(this.main));
            continue;
          } catch (IllegalAccessException e) {
            VelocityDiscord.LOGGER.error("Failed to get inherit value of field {}", field.getName());
          }
        } else {
          VelocityDiscord.LOGGER.trace("Field {} has a value in override config", field.getName());
        }
      }

      // choose which getter to use based on type of field
      // if Optional<Color> then getColor
      // if Optional<anything else> then getOptional
      // if MessageType then getMessageType
      // if UserMessageType then getUserMessageType

      var type = field.getType();

      if (type == Optional.class) {
        var genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
          var actualType = parameterizedType.getActualTypeArguments()[0];

          if (actualType == Color.class) {
            VelocityDiscord.LOGGER.trace("Loading color field {}", field.getName());
            loadField(field, key, BaseConfig::getColor);
          } else {
            VelocityDiscord.LOGGER.trace("Loading optional field {}", field.getName());
            loadField(field, key, BaseConfig::getOptional);
          }
        }
      } else if (type == MessageType.class) {
        VelocityDiscord.LOGGER.trace("Loading message type field {}", field.getName());
        loadField(field, key, BaseConfig::getMessageType);
      } else if (type == UserMessageType.class) {
        VelocityDiscord.LOGGER.trace("Loading user message type field {}", field.getName());
        loadField(field, key, BaseConfig::getUserMessageType);
      } else {
        VelocityDiscord.LOGGER.trace("Loading field {}", field.getName());
        loadField(field, key, BaseConfig::get);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T> void loadField(Field field, String key, Getter<T> getter) {
    T defaultValue = null;

    try {
      //noinspection unchecked
      defaultValue = (T) field.get(this);
      VelocityDiscord.LOGGER.trace("Default value of {} (`{}`) is {}", field.getName(), key, defaultValue);
    } catch (IllegalAccessException e) {
      VelocityDiscord.LOGGER.error("Failed to get default value of field {}", field.getName());
    }

    var value = getter.get(this, key, defaultValue);

    try {
      field.set(this, value);
      VelocityDiscord.LOGGER.trace("Set value of {} (`{}`) to {}", field.getName(), key, value);
    } catch (IllegalAccessException e) {
      VelocityDiscord.LOGGER.error("Failed to set value of field {}", field.getName());
    }
  }

  @SuppressWarnings("unused")
  public void logInner() {
    this.inner.entrySet().forEach(entry -> logInnerEntry(entry.getKey(), entry.getValue(), 0));
  }

  private void logInnerEntry(String key, Object value, int depth) {
    var indent = "  ".repeat(depth);
    if (value instanceof CommentedConfig config) {
      System.out.printf("%s%s:%n", indent, key);
      config.entrySet().forEach(entry -> logInnerEntry(entry.getKey(), entry.getValue(), depth + 1));
    } else {
      if (value instanceof String str) {
        System.out.printf("%s%s: '%s'%n", indent, key, str.replace("\n", "\\n"));
      } else {
        System.out.printf("%s%s: %s%n", indent, key, value);
      }
    }
  }

  public enum UserMessageType {
    TEXT, WEBHOOK, EMBED
  }

  public enum MessageType {
    TEXT, EMBED
  }

  @FunctionalInterface
  private interface Getter<T> {
    T get(BaseConfig config, String key, T defaultValue);
  }
}
