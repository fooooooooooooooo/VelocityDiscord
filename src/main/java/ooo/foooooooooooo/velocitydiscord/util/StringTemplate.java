package ooo.foooooooooooo.velocitydiscord.util;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class StringTemplate {
  private final Map<String, String> variables = new HashMap<>();
  @NotNull
  private String template;

  public StringTemplate(@NotNull String template) {
    this.template = template;
  }

  public StringTemplate add(@NotNull String key, @NotNull String value) {
    this.variables.put(key, value);

    return this;
  }

  public StringTemplate add(@NotNull String key, int value) {
    this.variables.put(key, String.valueOf(value));

    return this;
  }

  public StringTemplate add(@NotNull String key, boolean value) {
    this.variables.put(key, String.valueOf(value));

    return this;
  }

  public StringTemplate add(@NotNull String key, double value) {
    this.variables.put(key, String.valueOf(value));

    return this;
  }

  @Override
  @NotNull
  public String toString() {
    var result = this.template;

    for (var entry : this.variables.entrySet()) {
      result = result.replace("{" + entry.getKey() + "}", entry.getValue());
    }

    return result;
  }

  public StringTemplate replace(@NotNull String target, @NotNull String replacement) {
    this.template = this.template.replace(target, replacement);

    return this;
  }
}
