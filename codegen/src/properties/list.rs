use crate::java_type::JavaType;
use crate::properties::AsJava;

pub struct ListProperty {
  pub kind: JavaType,
}

impl AsJava for ListProperty {
  fn as_property_type(&self) -> String {
    format!("List<{}>", self.kind)
  }

  fn as_parse_function(&self, key: &str, _: &str) -> String {
    format!("config.get({key})")
  }
}
