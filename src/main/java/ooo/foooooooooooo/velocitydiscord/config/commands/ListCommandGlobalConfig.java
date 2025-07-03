package ooo.foooooooooooo.velocitydiscord.config.commands;

import com.electronwill.nightconfig.core.serde.annotations.SerdeKey;

public class ListCommandGlobalConfig {
  @SerdeKey("enabled")
  public Boolean ENABLED = true;

  @SerdeKey("ephemeral")
  public Boolean EPHEMERAL = true;
}
