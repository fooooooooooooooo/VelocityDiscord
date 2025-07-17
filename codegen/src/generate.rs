use std::collections::{HashMap, HashSet};
use std::fmt::Write as _;

use convert_case::{Case, Casing};

use crate::all_properties;
use crate::properties::boolean::BooleanProperty;
use crate::properties::color::ColorProperty;
use crate::properties::r#enum::EnumProperty;
use crate::properties::integer::IntegerProperty;
use crate::properties::string::StringProperty;
use crate::properties::{AsJava, Property};
use crate::structures::{Class, Enum, Structure};
use crate::utils::{default_property_name, pascal, string_literal};

const GENERATED_HEADER: &str = "// this file is generated\n// do not edit manually\n";
const PACKAGE_NAME: &str = "ooo.foooooooooooo.velocitydiscord.config.generated";

const MAP_IMPORT: &str = "java.util.Map";
const LIST_IMPORT: &str = "java.util.List";
const OPTIONAL_IMPORT: &str = "java.util.Optional";
const NIGHTCONFIG_IMPORT: &str = "com.electronwill.nightconfig.core.Config";
const CONFIG_UTILS_IMPORT: &str = "ooo.foooooooooooo.velocitydiscord.config.ConfigUtils";
const COLOR_IMPORT: &str = "java.awt.Color";

fn generate_class(name: &str, class: Class) -> anyhow::Result<String> {
  let mut content = String::new();
  let name = pascal(name);

  let mut imports = HashSet::new();

  imports.insert(NIGHTCONFIG_IMPORT);

  for property in class.properties.values() {
    let property_imports: &[&'static str] = match property {
      Property::Map(_) => &[MAP_IMPORT],
      Property::List(_) => &[LIST_IMPORT],
      Property::String(StringProperty::DisableableWithDefault { .. }) => &[OPTIONAL_IMPORT, CONFIG_UTILS_IMPORT],
      Property::String(StringProperty::DisableableWithoutDefault) => &[OPTIONAL_IMPORT, CONFIG_UTILS_IMPORT],
      Property::String(_) => &[CONFIG_UTILS_IMPORT],
      Property::Color(ColorProperty::DisableableWithDefault { .. }) => {
        &[OPTIONAL_IMPORT, CONFIG_UTILS_IMPORT, COLOR_IMPORT]
      }
      Property::Color(ColorProperty::DisableableWithoutDefault) => {
        &[OPTIONAL_IMPORT, CONFIG_UTILS_IMPORT, COLOR_IMPORT]
      }
      Property::Color(_) => &[CONFIG_UTILS_IMPORT, COLOR_IMPORT],
      Property::Integer(IntegerProperty::DisableableWithDefault { .. }) => &[OPTIONAL_IMPORT, CONFIG_UTILS_IMPORT],
      Property::Integer(IntegerProperty::DisableableWithoutDefault) => &[OPTIONAL_IMPORT, CONFIG_UTILS_IMPORT],
      Property::Integer(_) => &[CONFIG_UTILS_IMPORT],
      Property::Boolean(BooleanProperty::NormalWithDefault { .. }) => &[],
      Property::Boolean(BooleanProperty::NormalWithoutDefault) => &[],
      Property::Enum(EnumProperty::DisableableWithDefault { .. }) => &[OPTIONAL_IMPORT],
      Property::Enum(EnumProperty::DisableableWithoutDefault { .. }) => &[OPTIONAL_IMPORT],
      Property::Enum(_) => &[],
      Property::Class(_) => &[],
    };

    for import in property_imports {
      imports.insert(*import);
    }
  }

  writeln!(content, "{GENERATED_HEADER}")?;
  writeln!(content, "package {PACKAGE_NAME};")?;
  writeln!(content)?;

  let optional_used = imports.contains(OPTIONAL_IMPORT);

  let mut imports: Vec<_> = imports.into_iter().collect();
  imports.sort_unstable();

  for import in imports {
    writeln!(content, "import {import};")?;
  }

  writeln!(content)?;

  if optional_used {
    writeln!(content, r#"@SuppressWarnings("OptionalUsedAsFieldOrParameterType")"#)?;
  }

  writeln!(content, "public class {name} {{")?;

  let mut properties = class.properties.into_iter().collect::<Vec<_>>();
  properties.sort_unstable_by_key(|(prop_name, _)| prop_name.to_owned());

  for (prop_name, prop) in &properties {
    let property = all_properties!(prop, prop.as_property_type());

    writeln!(content, "  public {property} {prop_name};")?;
  }

  writeln!(content)?;

  // load
  // this.channel = ConfigUtils.parseDisableableString(config, "channel");
  // override
  // this.channel = ConfigUtils.maybeOverride(config, "channel", this.channel, ()
  // -> ConfigUtils.parseDisableableString(config, "channel"));

  // defaults

  let mut has_defaults = false;

  for (prop_name, prop) in &properties {
    let property = all_properties!(
      prop,
      prop.as_default().map(|default| (prop.as_property_type(), default))
    );

    if let Some((kind, default)) = property {
      has_defaults = true;
      let default_key = default_property_name(prop_name);
      writeln!(content, "  private static final {kind} {default_key} = {default};")?;
    }
  }

  if has_defaults {
    writeln!(content)?;
  }

  writeln!(content, "  public {name} load(Config config) {{")?;
  writeln!(content, "    if (config == null) return this;")?;
  for (name, prop) in &properties {
    let key = string_literal(name);
    let default_property = default_property_name(name);

    let parse_function = all_properties!(prop, prop.as_parse_function(&key, &default_property));

    writeln!(content, "    this.{name} = {parse_function};")?;
  }

  writeln!(content)?;
  writeln!(content, "    return this;")?;
  writeln!(content, "  }}")?;

  writeln!(content)?;

  writeln!(content, "  public void override(Config config) {{")?;
  for (name, prop) in properties {
    let key = string_literal(&name);
    let default_property = default_property_name(&name);

    if matches!(prop, Property::Class(_)) {
      // dont override classes, call override on them
      writeln!(content, "    this.{name}.override(config.get({key}));")?;
      continue;
    };

    let parse_function = all_properties!(prop, prop.as_parse_function(&key, &default_property));

    writeln!(
      content,
      "    this.{name} = ConfigUtils.maybeOverride(config, {key}, this.{name}, () -> {parse_function});"
    )?;
  }
  writeln!(content, "  }}")?;

  writeln!(content, "}}")?;

  Ok(content)
}

fn generate_enum(Enum { name, variants }: Enum) -> anyhow::Result<String> {
  let mut content = String::new();
  let name = pascal(name);

  writeln!(content, "{GENERATED_HEADER}")?;
  writeln!(content, "package {PACKAGE_NAME};")?;
  writeln!(content)?;

  writeln!(content, "public enum {name} {{")?;

  let key_to_variant: HashMap<String, String> = variants
    .iter()
    .map(|v| (v.to_owned(), v.to_case(Case::UpperSnake)))
    .collect();

  for (i, variant) in key_to_variant.values().enumerate() {
    if i > 0 {
      writeln!(content, ",")?;
    }

    write!(content, "  {variant}")?;
  }
  writeln!(content, ";")?;

  writeln!(content)?;

  writeln!(content, "  public static {name} from(String value) {{")?;
  writeln!(content, "    return switch (value) {{")?;
  for (key, variant) in key_to_variant {
    let key = string_literal(&key);
    writeln!(content, "      case {key} -> {variant};")?;
  }
  writeln!(
    content,
    "      default -> throw new IllegalArgumentException(\"Unknown {name} enum value: \" + value);"
  )?;
  writeln!(content, "    }};")?;
  writeln!(content, "  }}")?;

  writeln!(content, "}}")?;

  Ok(content)
}

pub fn generate_structure(name: &str, structure: Structure) -> anyhow::Result<(String, String)> {
  let content = match structure {
    Structure::Class(class) => generate_class(name, class)?,
    Structure::Enum(r#enum) => generate_enum(r#enum)?,
  };

  Ok((pascal(name), content))
}
