package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.Config;

import java.util.HashMap;
import java.util.Map;

public class RolePrefixConfig extends BaseConfig {
  private final Map<String, String> rolePrefixes = new HashMap<>();

  public RolePrefixConfig(Config config) {
    super(config);
    loadConfig();
  }

  public RolePrefixConfig(Config config, RolePrefixConfig main) {
    super(config, main);
    loadConfig();
  }

  @Override
  protected void loadConfig() {
    this.rolePrefixes.clear();

    var prefixConfig = this.inner.get("minecraft.role_prefixes");
    if (prefixConfig instanceof Config roleConfig) {
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
