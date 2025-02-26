package ooo.foooooooooooo.velocitydiscord.config;

import ooo.foooooooooooo.config.Config;

import java.util.HashMap;
import java.util.Map;

public class RolePrefixConfig extends Config {
  private final Map<String, String> rolePrefixes = new HashMap<>();

  public RolePrefixConfig(com.electronwill.nightconfig.core.Config config) {
    super(config);
  }

  public RolePrefixConfig(com.electronwill.nightconfig.core.Config config, RolePrefixConfig main) {
    super(config, main);
  }

  @Override
  public void loadConfig() {
    this.rolePrefixes.clear();

    if (this.inner == null) return;

    var prefixConfig = this.inner.get("minecraft.role_prefixes");
    if (prefixConfig instanceof com.electronwill.nightconfig.core.Config roleConfig) {
      for (var entry : roleConfig.entrySet()) {
        if (entry.getValue() instanceof String) {
          this.rolePrefixes.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  public String getPrefixForRole(String roleId) {
    return this.rolePrefixes.getOrDefault(roleId, "");
  }
}
