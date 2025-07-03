package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class RolePrefixConfig {
  @SerdeKey("role_prefixes")
  @SerdeDefault(provider = "defaultRolePrefixes")
  public final Map<String, String> rolePrefixes = new HashMap<>();
  private final transient Supplier<Map<String, String>> defaultRolePrefixes = HashMap::new;

  public String getPrefixForRole(String roleId) {
    return this.rolePrefixes.getOrDefault(roleId, "");
  }

  public String debug() {
    if (this.rolePrefixes.isEmpty()) {
      return "[]\n";
    }

    var sb = new StringBuilder();

    for (var entry : this.rolePrefixes.entrySet()) {
      sb.append("[")
        .append(entry.getKey())
        .append(" => ")
        .append(entry.getValue())
        .append("]\n");
    }

    return sb.toString();
  }
}
