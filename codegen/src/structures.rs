use std::collections::HashMap;

use crate::properties::Property;

pub struct Class {
  pub properties: HashMap<String, Property>,
}

pub struct Enum {
  pub name: String,
  pub variants: Vec<String>,
}

pub enum Structure {
  Class(Class),
  Enum(Enum),
}
