package ooo.foooooooooooo.velocitydiscord.config;

import ooo.foooooooooooo.config.Config;

import java.util.HashMap;
import java.util.Map;

public class RolePrefixConfig extends Config {
  private final Map<String, String> rolePrefixes = new HashMap<>();

  @SuppressWarnings("unused")
  public RolePrefixConfig(com.electronwill.nightconfig.core.Config config, String parentPath) {
    super(config, parentPath);
  }

  @SuppressWarnings("unused")
  public RolePrefixConfig(com.electronwill.nightconfig.core.Config config, String parentPath, RolePrefixConfig main) {
    super(config, parentPath, main);
  }

  @Override
  public void loadConfig() {
    this.rolePrefixes.clear();

    if (this.inner == null) return;

    var prefixConfig = this.inner.get("role_prefixes");
    if (prefixConfig instanceof com.electronwill.nightconfig.core.Config roleConfig) {
      for (var entry : roleConfig.entrySet()) {
        if (entry.getValue() instanceof String) {
          this.rolePrefixes.put(entry.getKey(), entry.getValue());
        }
      }
    } else {

    }
  }

  public String getPrefixForRole(String roleId) {
    return this.rolePrefixes.getOrDefault(roleId, "");
  }
}
