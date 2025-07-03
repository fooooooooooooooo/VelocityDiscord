package ooo.foooooooooooo.velocitydiscord.config.commands;

import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public class ListCommandGlobalConfig {
  @SerdeKey("enabled")
  @SerdeDefault(provider = "defaultEnabled")
  public Boolean ENABLED;
  private final transient Supplier<Boolean> defaultEnabled = () -> true;

  @SerdeKey("ephemeral")
  @SerdeDefault(provider = "defaultEphemeral")
  public Boolean EPHEMERAL;
  private final transient Supplier<Boolean> defaultEphemeral = () -> true;

  public String debug() {
    return "enabled: " + this.ENABLED.toString() + "\n"
      + "ephemeral: " + this.EPHEMERAL.toString() + "\n";
  }
}
