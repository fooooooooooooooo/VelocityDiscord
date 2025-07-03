package ooo.foooooooooooo.velocitydiscord.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public class MinecraftGlobalConfig {
  @SerdeKey("plugin_command")
  @SerdeDefault(provider = "defaultPluginCommand")
  public String PLUGIN_COMMAND;
  private final transient Supplier<String> defaultPluginCommand = () -> "discord";

  public String debug() {
    return "plugin_command: " + ConfigConstants.debugString(this.PLUGIN_COMMAND) + "\n";
  }
}
