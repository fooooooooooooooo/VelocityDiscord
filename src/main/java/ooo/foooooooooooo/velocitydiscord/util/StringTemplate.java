package ooo.foooooooooooo.velocitydiscord.util;

import java.util.HashMap;
import java.util.Map;

public class StringTemplate {
    private final Map<String, String> variables = new HashMap<>();
    private String template;

    public StringTemplate(String template) {
        this.template = template;
    }

    public StringTemplate add(String key, String value) {
        variables.put(key, value);
        return this;
    }

    public StringTemplate add(String key, int value) {
        variables.put(key, value + "");
        return this;
    }

    public StringTemplate add(String key, boolean value) {
        variables.put(key, value + "");
        return this;
    }

    public StringTemplate add(String key, double value) {
        variables.put(key, value + "");
        return this;
    }

    @Override
    public String toString() {
        for (var entry : variables.entrySet()) {
            template = template.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return template;
    }

    public StringTemplate replace(String target, String replacement) {
        template = template.replace(target, replacement);
        return this;
    }
}
