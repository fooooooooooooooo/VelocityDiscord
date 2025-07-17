use crate::properties::AsJava;
use crate::utils::string_literal;

#[allow(clippy::enum_variant_names)]
pub enum StringProperty {
  DisableableWithDefault { default: String },
  DisableableWithoutDefault,
  NormalWithDefault { default: String },
  NormalWithoutDefault,
}

impl AsJava for StringProperty {
  fn as_property_type(&self) -> String {
    match self {
      StringProperty::DisableableWithDefault { .. } => "Optional<String>",
      StringProperty::DisableableWithoutDefault => "Optional<String>",
      StringProperty::NormalWithDefault { .. } => "String",
      StringProperty::NormalWithoutDefault => "String",
    }
    .into()
  }

  fn as_default(&self) -> Option<String> {
    match self {
      StringProperty::DisableableWithDefault { default } => Some(format!("Optional.of({})", string_literal(default))),
      StringProperty::NormalWithDefault { default } => Some(string_literal(default)),
      _ => None,
    }
  }

  fn as_parse_function(&self, key: &str, default_property: &str) -> String {
    match self {
      StringProperty::DisableableWithDefault { .. } => {
        format!("ConfigUtils.parseDisableableStringWithDefault(config, {key}, {default_property})")
      }
      StringProperty::DisableableWithoutDefault => format!("ConfigUtils.parseDisableableString(config, {key})"),
      StringProperty::NormalWithDefault { .. } => {
        format!("ConfigUtils.parseStringWithDefault(config, {key}, {default_property})")
      }
      StringProperty::NormalWithoutDefault => format!("ConfigUtils.parseString(config, {key})"),
    }
  }
}
