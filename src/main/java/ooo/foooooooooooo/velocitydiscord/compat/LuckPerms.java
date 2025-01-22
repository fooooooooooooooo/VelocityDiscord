package ooo.foooooooooooo.velocitydiscord.compat;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.UserManager;

public class LuckPerms {
  private final net.luckperms.api.LuckPerms luckPerms;

  public LuckPerms() {
    this.luckPerms = LuckPermsProvider.get();
  }

  public UserManager getUserManager() {
    return this.luckPerms.getUserManager();
  }
}
