package ooo.foooooooooooo.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Config {
  private static final String INVALID_VALUE_FORMAT_STRING =
    "ERROR: `%s` is not a valid value for `%s`, acceptable values: `false`, any string";

  final static Logger LOGGER = LoggerFactory.getLogger(Config.class);

  public @Nullable com.electronwill.nightconfig.core.Config inner;
  protected @Nullable Config fallback;

  public Config(@Nullable com.electronwill.nightconfig.core.Config config) {
    this.inner = config;
  }

  public Config(@Nullable com.electronwill.nightconfig.core.Config config, @Nullable Config fallback) {
    this.inner = config;
    // get every field as optional, otherwise fallback to the value of the same field
    this.fallback = fallback;
  }

  public Logger getLogger() {
    return LOGGER;
  }

  public static <T> T get(Config config, String path) {
    if (config.inner == null) return null;
    return config.inner.get(path);
  }

  public static Optional<String> getOptional(Config config, String path, Optional<String> defaultValue) {
    if (config.inner == null) return defaultValue;
    var value = config.inner.getRaw(path);

    if (value == null) {
      return defaultValue;
    }

    if (value instanceof Boolean bool) {
      if (!bool) {
        return Optional.empty();

      } else {
        throw new RuntimeException(String.format(INVALID_VALUE_FORMAT_STRING, "true", path));
      }
    }

    if (value instanceof String str) {
      if (str.isEmpty()) {
        return Optional.empty();
      }

      return Optional.of(str);
    }

    throw new RuntimeException(String.format(INVALID_VALUE_FORMAT_STRING, value, path));
  }

  public static <T extends Enum<T>> Enum<T> getEnum(String path, String value, Class<T> enumClass) {
    return getEnumValueByValue(path, value, enumClass);
  }

  private static Variants.Key getFieldVariantKey(Field field) {
    if (field.isAnnotationPresent(Variants.Key.class)) {
      return field.getAnnotation(Variants.Key.class);
    } else {
      throw new RuntimeException("Field " + field.getName() + " is missing @Variants.Key annotation");
    }
  }

  private static <T extends Enum<T>> Enum<T> getEnumValueByValue(String path, String value, Class<T> enumClass) {
    // if annotated with @Variants, use the value of @Variants.Key annotation on
    // fields
    // otherwise use the enum variant name toLowerCase
    if (enumClass.isAnnotationPresent(Variants.class)) {
      var field = Arrays
        .stream(enumClass.getFields())
        .map(f -> new Pair<>(f, getFieldVariantKey(f).value()))
        .filter(pair -> pair.second().equalsIgnoreCase(value))
        .map(Pair::first)
        .findFirst();

      if (field.isEmpty()) {
        var acceptableValues = Arrays
          .stream(enumClass.getFields())
          .map(f -> "`" + getFieldVariantKey(f).value() + "`")
          .collect(Collectors.joining(", "));
        throw new IllegalArgumentException("Invalid enum value `"
          + value
          + "` for `"
          + path
          + "`, acceptable values: "
          + acceptableValues);
      }

      return Enum.valueOf(enumClass, field.get().getName());
    }

    // find field where field name == value
    var field = Arrays.stream(enumClass.getFields()).filter(f -> f.getName().equalsIgnoreCase(value)).findFirst();

    if (field.isEmpty()) {
      var acceptableValues =
        Arrays.stream(enumClass.getFields()).map(f -> "`" + f.getName() + "`").collect(Collectors.joining(", "));
      throw new IllegalArgumentException("Invalid enum value `"
        + value
        + "` for `"
        + path
        + "`, acceptable values: "
        + acceptableValues);
    }

    return Enum.valueOf(enumClass, field.get().getName());
  }

  public void setInner(@Nullable com.electronwill.nightconfig.core.Config config) {
    this.inner = config;

    // get nested configs and set their inner to their path value
    Arrays
      .stream(this.getClass().getFields())
      .filter(f -> f.getType().getSuperclass() == Config.class && f.isAnnotationPresent(Key.class))
      .forEach(f -> {
        try {
          var nestedConfig = (Config) f.get(this);

          if (config == null) {
            nestedConfig.setInner(null);
          } else {
            nestedConfig.setInner(config.get(f.getAnnotation(Key.class).value()));
          }

        } catch (IllegalAccessException e) {
          LOGGER.error("Failed to set inner config for field {}", f.getName());
        }
      });
  }

  private void loadField(Field field) {
    var annotation = field.getAnnotation(Key.class);
    var path = annotation.value();

    // if fallback exists and field is not overridable use the already set fallback value
    if (this.fallback != null && !annotation.overridable()) {
      LOGGER.trace("Using fallback value for non overridable field {}", field.getName());
      return;
    }

    var type = field.getType();

    if (type == Optional.class) {
      var genericType = field.getGenericType();
      if (genericType instanceof ParameterizedType parameterizedType) {
        var actualType = parameterizedType.getActualTypeArguments()[0];

        loadFieldImpl(field, path, (Class<?>) actualType, true);
      } else {
        LOGGER.warn("Failed to get actual type of optional field {}", field.getName());
      }
    } else {
      loadFieldImpl(field, path, type, false);
    }
  }

  private static boolean extendsConfig(Class<?> type) {
    return Config.class.isAssignableFrom(type);
  }

  private static Config createNested(
    Class<?> type,
    com.electronwill.nightconfig.core.Config inner,
    @Nullable Object fallback
  ) {
    try {
      // use fallback constructor if fallback exists
      if (fallback != null) {
        var constructor = type.getConstructor(com.electronwill.nightconfig.core.Config.class, type);
        return (Config) constructor.newInstance(inner, fallback);
      } else {
        var constructor = type.getConstructor(com.electronwill.nightconfig.core.Config.class);
        return (Config) constructor.newInstance(inner);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void loadFieldImpl(Field field, String path, Class<?> type, boolean optional) {
    LOGGER.debug("loadFieldImpl field: {}, path: {}, type: {}, optional: {}", field.getName(), path, type, optional);
    if (optional && Config.isDisabledValue(this, path)) {
      LOGGER.debug("Setting optional field {} to Optional.empty() (disabled value)", field.getName());
      setField(field, path, Optional.empty());
      return;
    }

    if (type == Color.class) {
      LOGGER.trace("Loading color field {}", field.getName());
      String value = Config.get(this, path);

      // use default
      if (value == null) return;

      Color color;

      try {
        color = Color.decode(value);
      } catch (NumberFormatException e) {
        LOGGER.error("Failed to decode color value for field {}", field.getName(), e);
        throw e;
      }

      setFieldOptional(field, path, color, optional);
    } else if (extendsConfig(type)) {
      // nested configs should not have default values
      // don't even check if there is a default
      LOGGER.debug("Loading nested config field `{}`", field.getName());

      com.electronwill.nightconfig.core.Config nestedConfig = null;
      if (this.inner != null) nestedConfig = this.inner.get(path);
      LOGGER.trace("nestedConfig for field {}: {}", field.getName(), nestedConfig);

      // if the nested config is marked Optional<> and the path is not present in the
      // config
      // set the field to Optional.empty()
      // otherwise create a new instance and set inner to null which will load
      // defaults
      if (optional && nestedConfig == null) {
        LOGGER.debug("Setting field {} to Optional.empty() (nestedConfig null)", field.getName());
        setField(field, path, Optional.empty());
        return;
      }

      try {
        Object nestedFallback = null;
        if (this.fallback != null) {
          nestedFallback = getFieldWithSameKey(this.fallback.getClass().getFields(), path).get(this.fallback);
        }

        var config = createNested(type, nestedConfig, nestedFallback);

        setFieldOptional(field, path, config, optional);

        LOGGER.trace("loadConfig of nested config field {}", field.getName());
        config.loadConfig();
      } catch (Exception e) {
        LOGGER.error("Failed to set nested config field {}", field.getName(), e);
        throw new RuntimeException(e);
      }
    } else if (type.isEnum()) {
      LOGGER.trace("Loading enum field {}", field.getName());
      String value = Config.get(this, path);

      // use default
      if (value == null) return;

      @SuppressWarnings({"unchecked", "RedundantSuppression"})
      Enum<?> enumValue = Config.getEnum(path, value, type.asSubclass(Enum.class));

      setFieldOptional(field, path, enumValue, optional);
    } else {
      LOGGER.trace("Loading normal field {}", field.getName());
      var value = Config.get(this, path);

      // use default
      if (value == null) return;

      setFieldOptional(field, path, value, optional);
    }
  }

  private static boolean isDisabledValue(Config config, String path) {
    if (config.inner == null) return false;
    var value = config.inner.get(path);

    if (value == null) {
      return false;
    }

    if (value instanceof Boolean bool) {
      if (!bool) {
        return true;
      } else {
        throw new RuntimeException(String.format(INVALID_VALUE_FORMAT_STRING, "true", path));
      }
    }

    if (value instanceof String str) {
      return str.isEmpty();
    }

    return false;
  }

  private static Field getFieldWithSameKey(Field[] fields, String key) {
    return Arrays
      .stream(fields)
      .filter(f -> f.isAnnotationPresent(Key.class) && f.getAnnotation(Key.class).value().equals(key))
      .findFirst()
      .orElse(null);
  }

  private static void inheritFields(Config src, Config dst) {
    LOGGER.trace("Inheriting fields in {} from {}", dst.getClass().getName(), src.getClass().getName());

    Arrays.stream(dst.getClass().getFields()).filter(f -> f.isAnnotationPresent(Key.class)).forEach(f -> {
      try {
        if (extendsConfig(f.getType())) {
          LOGGER.trace("Inheriting nested config field {} from fallback", f.getName());
          // find field with same Key annotation in src and dst
          var destKey = f.getAnnotation(Key.class).value();
          var sourceField = getFieldWithSameKey(src.getClass().getFields(), destKey);
          if (sourceField == null) {
            LOGGER.error("Failed to find field with same Key annotation in src: {}", f.getName());
            return;
          }

          var nestedSrc = (Config) sourceField.get(src);
          var nestedDst = (Config) f.get(dst);

          // dst nested configs are most likely not initialized
          // create a new instance
          if (nestedDst == null) {
            LOGGER.trace("Creating new instance of nested config field {}", f.getName());
            nestedDst = createNested(f.getType(), dst.inner == null ? null : dst.inner.get(destKey), nestedSrc);
            f.set(dst, nestedDst);
            nestedDst.loadConfig();
          } else {
            inheritFields(nestedSrc, nestedDst);
          }
        } else {
          LOGGER.trace("Inheriting value of field {} from fallback", f.getName());
          f.set(dst, f.get(src));
        }
      } catch (IllegalAccessException e) {
        LOGGER.error("Failed to inherit value of field from fallback: {}", f.getName(), e);
      }
    });
  }

  public void loadConfig() {
    // if fallback exists, first copy all values from fallback
    if (this.fallback != null) {
      LOGGER.trace("Inheriting values from fallback");
      inheritFields(this.fallback, this);
    }

    Arrays.stream(this.getClass().getFields()).filter(f -> f.isAnnotationPresent(Key.class)).forEach(this::loadField);
  }

  private <T> void setField(Field field, String path, T value) {
    try {
      field.set(this, value);
      LOGGER.trace("Set value of {} (`{}`) to {}", field.getName(), path, value);
    } catch (IllegalAccessException e) {
      LOGGER.error("Failed to set value of field {}", field.getName());
    }
  }

  /// conditionally wrap value in `Optional` if `optional` is true
  private <T> void setFieldOptional(Field field, String path, T value, boolean optional) {
    if (optional) {
      LOGGER.trace("Setting field {} to Optional.of({}) [{}]", field.getName(), value, value.getClass());
      setField(field, path, Optional.of(value));
    } else {
      LOGGER.trace("Setting field {} to {} [{}]", field.getName(), value, value.getClass());
      setField(field, path, value);
    }
  }

  @SuppressWarnings("unused")
  public void logInner() {
    if (this.inner == null) {
      System.out.println("No inner config");
      return;
    }

    this.inner.entrySet().forEach(entry -> logInnerEntry(entry.getKey(), entry.getValue(), 0));
  }

  private void logInnerEntry(String path, Object value, int depth) {
    var indent = "  ".repeat(depth);
    if (value instanceof CommentedConfig config) {
      System.out.printf("%s%s:%n", indent, path);
      config.entrySet().forEach(entry -> logInnerEntry(entry.getKey(), entry.getValue(), depth + 1));
    } else {
      if (value instanceof String str) {
        System.out.printf("%s%s: '%s'%n", indent, path, str.replace("\n", "\\n"));
      } else {
        System.out.printf("%s%s: %s%n", indent, path, value);
      }
    }
  }

  private record Pair<T, U>(T first, U second) {}
}
