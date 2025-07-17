use crate::java_type::JavaType;
use crate::properties::AsJava;

// only map string to string for now
pub struct MapProperty {
  pub value_kind: JavaType,
}

impl AsJava for MapProperty {
  fn as_property_type(&self) -> String {
    format!("Map<String, {}>", self.value_kind)
  }

  fn as_parse_function(&self, key: &str, _: &str) -> String {
    format!("config.get({key})")
  }
}
