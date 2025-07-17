use crate::properties::AsJava;

pub struct ClassProperty {
  pub name: String,
}

impl AsJava for ClassProperty {
  fn as_property_type(&self) -> String {
    self.name.to_owned()
  }

  fn as_parse_function(&self, _: &str, _: &str) -> String {
    format!("new {}().load(config)", self.name)
  }
}
