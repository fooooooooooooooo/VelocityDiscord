package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.Config;
import java.util.HashMap;
import java.util.Map;

public class RolePrefixConfig extends BaseConfig {
  private final Map<String, String> rolePrefixes = new HashMap<>();

  public RolePrefixConfig(Config config) {
    loadConfig(config);
  }

  @Override
  protected void loadConfig(Config config) {
    rolePrefixes.clear();

    var prefixConfig = config.get("minecraft.role_prefixes");
    if (prefixConfig instanceof Config roleConfig) {
      for (var entry : roleConfig.entrySet()) {
        if (entry.getValue() instanceof String) {
          rolePrefixes.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  public String getPrefixForRole(String roleId) {
    return rolePrefixes.getOrDefault(roleId, "");
  }
}