use crate::properties::AsJava;
use crate::utils::string_literal;

#[allow(clippy::enum_variant_names)]
pub enum EnumProperty {
  DisableableWithDefault { name: String, default: String },
  DisableableWithoutDefault { name: String },
  NormalWithDefault { name: String, default: String },
  NormalWithoutDefault { name: String },
}

impl AsJava for EnumProperty {
  fn as_property_type(&self) -> String {
    match self {
      EnumProperty::DisableableWithDefault { name, .. } => format!("Optional<{}>", name),
      EnumProperty::DisableableWithoutDefault { name } => format!("Optional<{}>", name),
      EnumProperty::NormalWithDefault { name, .. } => name.to_owned(),
      EnumProperty::NormalWithoutDefault { name } => name.to_owned(),
    }
  }

  fn as_default(&self) -> Option<String> {
    match self {
      EnumProperty::DisableableWithDefault { name, default } => {
        Some(format!("Optional.of({name}.from({})))", string_literal(default)))
      }
      EnumProperty::NormalWithDefault { default, name } => Some(format!("{name}.from({})", string_literal(default))),
      _ => None,
    }
  }

  fn as_parse_function(&self, key: &str, default_property: &str) -> String {
    match self {
      EnumProperty::DisableableWithDefault { name, .. } => {
        format!("{name}.from(ConfigUtils.parseDisableableStringWithDefault(config, {key}, {default_property}))")
      }
      EnumProperty::DisableableWithoutDefault { name } => {
        format!("{name}.from(ConfigUtils.parseDisableableString(config, {key}))")
      }
      EnumProperty::NormalWithDefault { name, .. } => {
        format!("{name}.from(ConfigUtils.parseStringWithDefault(config, {key}, {default_property}))")
      }
      EnumProperty::NormalWithoutDefault { name } => format!("{name}.from(ConfigUtils.parseString(config, {key}))"),
    }
  }
}
