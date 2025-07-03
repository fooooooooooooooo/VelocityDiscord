package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;

import java.util.HashMap;
import java.util.Map;

public class RolePrefixConfig {
  @SerdeKey("role_prefixes")
  public final Map<String, String> rolePrefixes = new HashMap<>();

  public String getPrefixForRole(String roleId) {
    return this.rolePrefixes.getOrDefault(roleId, "");
  }
}
