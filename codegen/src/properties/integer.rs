use crate::properties::AsJava;

#[allow(clippy::enum_variant_names)]
pub enum IntegerProperty {
  DisableableWithDefault { default: i64 },
  DisableableWithoutDefault,
  NormalWithDefault { default: i64 },
  NormalWithoutDefault,
}

impl AsJava for IntegerProperty {
  fn as_property_type(&self) -> String {
    match self {
      IntegerProperty::DisableableWithDefault { .. } => "Optional<Integer>",
      IntegerProperty::DisableableWithoutDefault => "Optional<Integer>",
      IntegerProperty::NormalWithDefault { .. } => "Integer",
      IntegerProperty::NormalWithoutDefault => "Integer",
    }
    .into()
  }

  fn as_default(&self) -> Option<String> {
    match self {
      IntegerProperty::DisableableWithDefault { default } => Some(format!("Optional.of({default})")),
      IntegerProperty::NormalWithDefault { default } => Some(default.to_string()),
      _ => None,
    }
  }

  fn as_parse_function(&self, key: &str, default_property: &str) -> String {
    match self {
      IntegerProperty::DisableableWithDefault { .. } => {
        format!("ConfigUtils.parseDisableableIntegerWithDefault(config, {key}, {default_property})")
      }
      IntegerProperty::DisableableWithoutDefault => format!("ConfigUtils.parseDisableableInteger(config, {key})"),
      IntegerProperty::NormalWithDefault { .. } => format!("config.getOrElse({key}, {default_property})"),
      IntegerProperty::NormalWithoutDefault => format!("config.get({key})"),
    }
  }
}
