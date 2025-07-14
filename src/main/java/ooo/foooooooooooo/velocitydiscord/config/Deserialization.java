package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.concurrent.StampedConfig;
import com.electronwill.nightconfig.core.serde.*;
import ooo.foooooooooooo.velocitydiscord.config.commands.ListCommandConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("CommentedOutCode")
public class Deserialization {
  private static final Logger logger = LoggerFactory.getLogger(Deserialization.class);

  public static ObjectDeserializer deserializer() {
    var deserializer = ObjectDeserializer.blankBuilder();

    var trivialDe = new StandardDeserializers.TrivialDeserializer();
    var mapDe = new StandardDeserializers.MapDeserializer();
    var collDe = new StandardDeserializers.CollectionDeserializer();
    var arrDe = new StandardDeserializers.CollectionToArrayDeserializer();
    var enumDe = new StandardDeserializers.EnumDeserializer();
    var uuidDe = new StandardDeserializers.UuidDeserializer();
    var numberDe = new StandardDeserializers.RiskyNumberDeserializer();

    deserializer.withDeserializerProvider((valueClass, resultType) -> resultType.getSatisfyingRawType().map(resultClass -> {
      var customDeserializer = Deserialization.getDeserializer(valueClass, resultClass, resultType);

      if (customDeserializer != null) return customDeserializer;

      var fullType = resultType.getFullType();

      if (Util.canAssign(resultClass, valueClass) && (valueClass == null || fullType instanceof Class)) {
        return trivialDe; // value to value (same type or compatible type)

        // Note that we rule out TypeConstraint where getFullType() is not a simple Class,
        // which means that there are type parameters and that we cannot just blindly assign.
      }
      if (Collection.class.isAssignableFrom(valueClass)) {
        if (Collection.class.isAssignableFrom(resultClass)) {
          return collDe; // collection<value> to collection<T>
        } else if (resultClass.isArray()) {
          return arrDe; // collection<value> to array<T>
        }
      }
      if ((
        UnmodifiableConfig.class.isAssignableFrom(valueClass) || Map.class.isAssignableFrom(valueClass)) && Map.class.isAssignableFrom(resultClass)) {
        return mapDe; // config to map<K, V>
      }
      if (resultClass == UUID.class && valueClass == String.class) {
        return uuidDe;
      }
      if (valueClass == String.class && Enum.class.isAssignableFrom(resultClass)) {
        return enumDe; // value to Enum
      }
      if (StandardDeserializers.RiskyNumberDeserializer.isNumberTypeSupported(valueClass) && Util.isPrimitiveOrWrapperNumber(resultClass)) {
        return numberDe;
      }
      return null; // no standard deserializer matches this case

    }).orElse(null));

    return deserializer.build();
  }

  public static <T> T deserialize(Config config, T object) {
    if (config == null) return EmptyConfig.deserialize(object);

    deserializer().deserializeFields(config, object);

    return object;
  }

  private static boolean checkGenericType(TypeConstraint constraint, Class<?> clazz) {
    var fullType = constraint.getFullType();

    if (fullType instanceof ParameterizedType type) {
      return type.getActualTypeArguments()[0].getTypeName().equals(clazz.getName());
    }

    return fullType.getTypeName().equals(clazz.getName());
  }

  private static ValueDeserializer<?, ?> getDeserializer(Class<?> valueClass, Class<?> resultClass, TypeConstraint resultType) {
    logger.info("Getting deserializer for valueClass: {}, resultClass: {}", valueClass.getName(), resultClass.getName());

    if (valueClass == StampedConfig.class) {
      if (resultClass == WebhookConfig.class) return new NestedConfigDeserializer<>(WebhookConfig::new);
      if (resultClass == ListCommandConfig.class) return new NestedConfigDeserializer<>(ListCommandConfig::new);
      if (resultClass == RolePrefixConfig.class) return new NestedConfigDeserializer<>(RolePrefixConfig::new);
      // only necessary if optional nested configs
      // if (resultClass == Optional.class) {
      //   if (checkGenericType(resultType, RolePrefixConfig.class)) return new OptionalNestedConfigDeserializer<>(RolePrefixConfig::new);
      // }
    }

    if (valueClass != String.class) {

      return null;
    }

    if (resultClass.isAssignableFrom(DiscordChatConfig.ServerMessageType.class)) {
      logger.info("Found DiscordChatConfig.ServerMessageType");
      return new DiscordChatConfig.ServerMessageTypeDeserializer();
    }

    if (resultClass.isAssignableFrom(DiscordChatConfig.UserMessageType.class)) {
      logger.info("Found DiscordChatConfig.UserMessageType");
      return new DiscordChatConfig.UserMessageTypeDeserializer();
    }

    if (resultClass == Optional.class) {
      // if resultClass is Optional<Color>
      if (checkGenericType(resultType, Color.class)) {
        return new OptionalColorDeserializer();
      }

      // if resultClass is Optional<String>
      if (checkGenericType(resultType, String.class)) {
        return new OptionalStringDeserializer();
      }


      return new OptionalDeserializer();
    }

    // if resultClass is Color
    if (resultClass == Color.class) {
      return new ColorDeserializer();
    }

    return null;
  }


  private static class OptionalDeserializer implements ValueDeserializer<String, Optional<?>> {
    @Override
    public Optional<?> deserialize(String value, Optional<TypeConstraint> resultType, DeserializerContext ctx) {
      logger.info("Deserializing Optional<?> from String: {}", value);

      if (value == null || value.isEmpty()) {
        return Optional.empty();
      }

      logger.info("  Generic type for Optional: {}", resultType.map(TypeConstraint::getFullType).orElse(null));

      if (resultType.isPresent() && checkGenericType(resultType.get(), DiscordChatConfig.ServerMessageType.class)) {
        logger.info("  Deserializing Optional<DiscordChatConfig.ServerMessageType> from String: {}", value);
        return Optional.ofNullable(new DiscordChatConfig.ServerMessageTypeDeserializer().deserialize(value, resultType, ctx));
      }

      if (resultType.isPresent() && checkGenericType(resultType.get(), DiscordChatConfig.UserMessageType.class)) {
        logger.info("  Deserializing Optional<DiscordChatConfig.UserMessageType> from String: {}", value);
        return Optional.ofNullable(new DiscordChatConfig.UserMessageTypeDeserializer().deserialize(value, resultType, ctx));
      }

      logger.warn("Unsupported type for Optional deserialization: {}", resultType.orElse(null));

      return Optional.of(value);
    }
  }

  private static class OptionalColorDeserializer implements ValueDeserializer<String, Optional<Color>> {
    @Override
    public Optional<Color> deserialize(String value, Optional<TypeConstraint> resultType, DeserializerContext ctx) {
      logger.info("Deserializing Optional<Color> from String: {}", value);
      if (value == null || value.isEmpty()) return Optional.empty();
      return Optional.of(Color.decode(value));
    }
  }

  private static class ColorDeserializer implements ValueDeserializer<String, Color> {
    @Override
    public Color deserialize(String value, Optional<TypeConstraint> resultType, DeserializerContext ctx) {
      logger.info("Deserializing Color from String: {}", value);
      return Color.decode(value);
    }
  }

  private static class OptionalStringDeserializer implements ValueDeserializer<String, Optional<String>> {
    @Override
    public Optional<String> deserialize(String value, Optional<TypeConstraint> resultType, DeserializerContext ctx) {
      logger.info("Deserializing Optional<String> from String: {}", value);
      if (value == null || value.isEmpty()) return Optional.empty();
      return Optional.of(value);
    }
  }

  private static class NestedConfigDeserializer<T> implements ValueDeserializer<StampedConfig, T> {
    private final Supplier<T> supplier;

    public NestedConfigDeserializer(Supplier<T> supplier) {
      this.supplier = supplier;
    }

    @Override
    public T deserialize(StampedConfig value, Optional<TypeConstraint> resultType, DeserializerContext ctx) {
      logger.info("Deserializing WebhookConfig from StampedConfig: {}", value);
      var config = this.supplier.get();
      return Deserialization.deserialize(value, config);
    }
  }

  // private static class OptionalNestedConfigDeserializer<T> implements ValueDeserializer<StampedConfig, Optional<T>> {
  //   private final Supplier<T> supplier;
  //
  //   public OptionalNestedConfigDeserializer(Supplier<T> supplier) {
  //     this.supplier = supplier;
  //   }
  //
  //   @Override
  //   public Optional<T> deserialize(StampedConfig value, Optional<TypeConstraint> resultType, DeserializerContext ctx) {
  //     logger.info("Deserializing Optional<RolePrefixConfig> from StampedConfig: {}", value);
  //     if (value == null || value.isEmpty()) return Optional.empty();
  //     var config = this.supplier.get();
  //     return Optional.of(Deserialization.deserialize(value, config));
  //   }
  // }
}
