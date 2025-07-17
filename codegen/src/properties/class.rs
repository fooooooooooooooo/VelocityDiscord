use crate::properties::AsJava;
use crate::utils::pascal;

pub struct ClassProperty {
  pub name: String,
}

impl AsJava for ClassProperty {
  fn as_property_type(&self) -> String {
    pascal(&self.name)
  }

  fn as_parse_function(&self, key: &str, _: &str) -> String {
    let name = pascal(&self.name);
    format!("new {name}().load(config.get({key}))")
  }
}
