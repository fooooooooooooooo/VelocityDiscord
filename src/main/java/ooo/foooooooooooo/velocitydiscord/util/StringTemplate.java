package ooo.foooooooooooo.velocitydiscord.util;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StringTemplate {
  private final Map<String, String> variables = new HashMap<>();
  @Nonnull
  private String template;

  public StringTemplate(String template) {
    this.template = Objects.requireNonNull(template);
  }

  public StringTemplate add(String key, String value) {
    variables.put(key, value);
    return this;
  }

  public StringTemplate add(String key, int value) {
    variables.put(key, String.valueOf(value));
    return this;
  }

  public StringTemplate add(String key, boolean value) {
    variables.put(key, String.valueOf(value));
    return this;
  }

  public StringTemplate add(String key, double value) {
    variables.put(key, String.valueOf(value));
    return this;
  }

  @Override
  @Nonnull
  public String toString() {
    for (var entry : variables.entrySet()) {
      template = Objects.requireNonNull(template.replace("{" + entry.getKey() + "}", entry.getValue()));
    }

    return template;
  }

  public StringTemplate replace(String target, String replacement) {
    template = Objects.requireNonNull(template.replace(target, replacement));
    return this;
  }
}
