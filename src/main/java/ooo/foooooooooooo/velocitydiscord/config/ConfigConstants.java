package ooo.foooooooooooo.velocitydiscord.config;

import java.awt.*;

public class ConfigConstants {
  public static final Color RED = new Color(0xbf4040);
  public static final Color GREEN = new Color(0x40bf4f);

  // wrap string in "" and escape quotes and newlines
  public static String debugString(String str) {
    return "\"" + str.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
  }
}
