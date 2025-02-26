package ooo.foooooooooooo.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Variants {
  @Target({ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface Key {
    String value();
  }
}
