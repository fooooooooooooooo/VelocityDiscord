use crate::properties::AsJava;

pub enum BooleanProperty {
  NormalWithDefault { default: bool },
  NormalWithoutDefault,
}

impl AsJava for BooleanProperty {
  fn as_property_type(&self) -> String {
    match self {
      BooleanProperty::NormalWithDefault { .. } => "Boolean",
      BooleanProperty::NormalWithoutDefault => "Boolean",
    }
    .into()
  }

  fn as_default(&self) -> Option<String> {
    match self {
      BooleanProperty::NormalWithDefault { default } => Some(default.to_string()),
      BooleanProperty::NormalWithoutDefault => None,
    }
  }

  fn as_parse_function(&self, key: &str, default_property: &str) -> String {
    match self {
      BooleanProperty::NormalWithDefault { .. } => format!("config.getOrElse({key}, {default_property})"),
      BooleanProperty::NormalWithoutDefault => format!("config.get({key})"),
    }
  }
}
