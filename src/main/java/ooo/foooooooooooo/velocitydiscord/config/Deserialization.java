package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.serde.DeserializerContext;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.core.serde.TypeConstraint;
import com.electronwill.nightconfig.core.serde.ValueDeserializer;

import java.awt.*;
import java.util.Optional;

public class Deserialization {
  public static ObjectDeserializer deserializer() {
    var deserializer = ObjectDeserializer.builder();

    deserializer.withDeserializerProvider((
      (valueClass, resultType) -> resultType.getSatisfyingRawType().map(resultClass -> Deserialization.getDeserializer(valueClass, resultClass)).orElse(null)));

    return deserializer.build();
  }

  public static <T> T deserialize(Config config, T object) {
    if (config == null) return EmptyConfig.deserialize(object);

    deserializer().deserializeFields(config, object);

    return object;
  }

  private static ValueDeserializer<?, ?> getDeserializer(Class<?> valueClass, Class<?> resultClass) {
    System.out.println("Getting deserializer for valueClass: " + valueClass.getName() + ", resultClass: " + resultClass.getName());

    if (valueClass != String.class) {
      return null;
    }

    // if resultClass is Optional<Color>
    if (resultClass == Optional.class && resultClass.getTypeParameters().length == 1 && resultClass.getTypeParameters()[0].getTypeName().equals(Color.class.getTypeName())) {
      return new OptionalColorDeserializer();
    }

    // if resultClass is Color
    if (resultClass == Color.class) {
      return new ColorDeserializer();
    }

    return null;
  }

  private static class OptionalColorDeserializer implements ValueDeserializer<String, Optional<Color>> {
    @Override
    public Optional<Color> deserialize(String value, Optional<TypeConstraint> resultType, DeserializerContext ctx) {
      System.out.println("Deserializing Optional<Color> from String: " + value);

      if (value == null || value.isEmpty()) {
        return Optional.empty();
      }

      return Optional.of(Color.decode(value));
    }
  }

  private static class ColorDeserializer implements ValueDeserializer<String, Color> {
    @Override
    public Color deserialize(String value, Optional<TypeConstraint> resultType, DeserializerContext ctx) {
      System.out.println("Deserializing Color from String: " + value);
      return Color.decode(value);
    }
  }
}
