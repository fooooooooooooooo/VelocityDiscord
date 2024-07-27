package ooo.foooooooooooo.velocitydiscord.util;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class StringTemplate {
  private final Map<String, String> variables = new HashMap<>();
  @Nonnull
  private String template;

  public StringTemplate(@Nonnull String template) {
    this.template = template;
  }

  public StringTemplate add(@Nonnull String key, @Nonnull String value) {
    variables.put(key, value);

    return this;
  }

  public StringTemplate add(@Nonnull String key, int value) {
    variables.put(key, String.valueOf(value));

    return this;
  }

  public StringTemplate add(@Nonnull String key, boolean value) {
    variables.put(key, String.valueOf(value));

    return this;
  }

  public StringTemplate add(@Nonnull String key, double value) {
    variables.put(key, String.valueOf(value));

    return this;
  }

  @Override
  @Nonnull
  public String toString() {
    var result = template;

    for (var entry : variables.entrySet()) {
      result = result.replace("{" + entry.getKey() + "}", entry.getValue());
    }

    return result;
  }

  public StringTemplate replace(@Nonnull String target, @Nonnull String replacement) {
    template = template.replace(target, replacement);

    return this;
  }
}
