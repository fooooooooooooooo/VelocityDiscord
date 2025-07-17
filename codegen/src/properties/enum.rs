use crate::properties::AsJava;
use crate::utils::{pascal, string_literal};

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
      EnumProperty::DisableableWithDefault { name, .. } => format!("Optional<{}>", pascal(name)),
      EnumProperty::DisableableWithoutDefault { name } => format!("Optional<{}>", pascal(name)),
      EnumProperty::NormalWithDefault { name, .. } => pascal(name),
      EnumProperty::NormalWithoutDefault { name } => pascal(name),
    }
  }

  fn as_default(&self) -> Option<String> {
    match self {
      EnumProperty::DisableableWithDefault { name, default } => {
        let name = pascal(name);
        let default = string_literal(default);
        Some(format!("Optional.of({name}.from({default})))",))
      }
      EnumProperty::NormalWithDefault { default, name } => {
        let name = pascal(name);
        let default = string_literal(default);
        Some(format!("{name}.from({default})"))
      }
      _ => None,
    }
  }

  fn as_parse_function(&self, key: &str, default_property: &str) -> String {
    match self {
      EnumProperty::DisableableWithDefault { name, .. } => {
        let name = pascal(name);
        format!("{name}.from(ConfigUtils.parseDisableableStringWithDefault(config, {key}, {default_property}))")
      }
      EnumProperty::DisableableWithoutDefault { name } => {
        let name = pascal(name);
        format!("{name}.from(ConfigUtils.parseDisableableString(config, {key}))")
      }
      EnumProperty::NormalWithDefault { name, .. } => {
        let name = pascal(name);
        format!("{name}.from(ConfigUtils.parseStringWithDefault(config, {key}, {default_property}))")
      }
      EnumProperty::NormalWithoutDefault { name } => {
        let name = pascal(name);
        format!("{name}.from(ConfigUtils.parseString(config, {key}))")
      }
    }
  }
}
