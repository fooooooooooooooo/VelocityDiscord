use crate::properties::AsJava;
use crate::utils::string_literal;

#[allow(clippy::enum_variant_names)]
pub enum ColorProperty {
  DisableableWithDefault { default: String },
  DisableableWithoutDefault,
  NormalWithDefault { default: String },
  NormalWithoutDefault,
}

impl AsJava for ColorProperty {
  fn as_property_type(&self) -> String {
    match self {
      ColorProperty::DisableableWithDefault { .. } => "Optional<Color>",
      ColorProperty::DisableableWithoutDefault => "Optional<Color>",
      ColorProperty::NormalWithDefault { .. } => "Color",
      ColorProperty::NormalWithoutDefault => "Color",
    }
    .into()
  }

  fn as_default(&self) -> Option<String> {
    match self {
      ColorProperty::DisableableWithDefault { default } => {
        Some(format!("Optional.of(Color.decode({}))", string_literal(default)))
      }
      ColorProperty::NormalWithDefault { default } => Some(format!("Color.decode({})", string_literal(default))),
      _ => None,
    }
  }

  fn as_parse_function(&self, key: &str, default_property: &str) -> String {
    match self {
      ColorProperty::DisableableWithDefault { .. } => {
        format!("ConfigUtils.parseDisableableColorWithDefault(config, {key}, {default_property})")
      }
      ColorProperty::DisableableWithoutDefault => format!("ConfigUtils.parseDisableableColor(config, {key})"),
      ColorProperty::NormalWithDefault { .. } => {
        format!("ConfigUtils.parseColorWithDefault(config, {key}, {default_property})")
      }
      ColorProperty::NormalWithoutDefault => format!("ConfigUtils.parseColor(config, {key})"),
    }
  }
}
