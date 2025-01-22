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
    this.variables.put(key, value);

    return this;
  }

  public StringTemplate add(@Nonnull String key, int value) {
    this.variables.put(key, String.valueOf(value));

    return this;
  }

  public StringTemplate add(@Nonnull String key, boolean value) {
    this.variables.put(key, String.valueOf(value));

    return this;
  }

  public StringTemplate add(@Nonnull String key, double value) {
    this.variables.put(key, String.valueOf(value));

    return this;
  }

  @Override
  @Nonnull
  public String toString() {
    var result = this.template;

    for (var entry : this.variables.entrySet()) {
      result = result.replace("{" + entry.getKey() + "}", entry.getValue());
    }

    return result;
  }

  public StringTemplate replace(@Nonnull String target, @Nonnull String replacement) {
    this.template = this.template.replace(target, replacement);

    return this;
  }
}
