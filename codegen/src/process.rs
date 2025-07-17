use std::collections::HashMap;

use anyhow::{Context, bail};
use convert_case::{Case, Casing};
use serde_json::Value;

use crate::java_type::JavaType;
use crate::properties::Property;
use crate::properties::boolean::BooleanProperty;
use crate::properties::class::ClassProperty;
use crate::properties::color::ColorProperty;
use crate::properties::r#enum::EnumProperty;
use crate::properties::integer::IntegerProperty;
use crate::properties::list::ListProperty;
use crate::properties::map::MapProperty;
use crate::properties::string::StringProperty;
use crate::schema::{ItemsUnion, Schema, SimpleType, Type};
use crate::structures::{Class, Enum, Structure};

const REFERENCE_PREFIX: &str = "#/definitions/";

fn resolve_reference<'a>(root: &'a Schema, reference: &'a str) -> anyhow::Result<&'a Schema> {
  match &root.definitions {
    Some(definitions) => {
      if let Some(schema) = definitions.get(reference.trim_start_matches(REFERENCE_PREFIX)) {
        return Ok(schema);
      }

      bail!("Reference '{reference}' not found in schema definitions");
    }
    None => bail!("Schema has no definitions section"),
  }
}

fn process_property(
  prefix: &str,
  root: &Schema,
  parent_key: &str,
  key: &str,
  value: &Schema,
  structures: &mut HashMap<String, Structure>,
) -> anyhow::Result<Option<Property>> {
  if key == "$schema" {
    eprintln!("Skipping $schema property");
    return Ok(None);
  }

  if value.kind.is_none() {
    if let Some(reference) = &value.reference {
      let reference = reference.trim_start_matches("#/definitions/");
      let referenced_schema = resolve_reference(root, reference)?;

      return process_property(prefix, root, parent_key, key, referenced_schema, structures);
    }

    bail!("Property '{key}' is missing required 'type' field");
  }

  match &value.kind {
    // string or color or enum
    Some(Type::Single(SimpleType::String)) => {
      // enum
      if let Some(r#enum) = &value.r#enum {
        let variants: Vec<String> = r#enum.iter().map(|v| v.to_owned()).collect();
        let name = format!("{parent_key}_{key}").to_case(Case::Pascal);

        structures.insert(
          name.clone(),
          Structure::Enum(Enum {
            name: name.clone(),
            variants,
          }),
        );

        let property = match &value.default {
          Some(Value::String(default)) => EnumProperty::NormalWithDefault {
            name,
            default: default.to_owned(),
          },
          None => EnumProperty::NormalWithoutDefault { name },
          Some(other) => bail!("Property '{key}' has invalid default value type, expected string, got {other:?}"),
        };

        return Ok(Some(Property::Enum(property)));
      }

      // color
      match &value.parsed_as.as_deref() {
        Some("java.awt.Color") => {
          let property = match &value.default {
            Some(Value::String(default)) => ColorProperty::NormalWithDefault {
              default: default.to_owned(),
            },
            None => ColorProperty::NormalWithoutDefault,
            Some(other) => bail!("Property '{key}' has invalid default value type, expected string, got {other:?}"),
          };

          return Ok(Some(Property::Color(property)));
        }
        Some(parsed_as) => bail!("Property '{key}' has unsupported parsed_as value: {parsed_as}"),
        _ => {}
      }

      // string
      let property = match &value.default {
        Some(Value::String(default)) => StringProperty::NormalWithDefault {
          default: default.to_owned(),
        },
        None => StringProperty::NormalWithoutDefault,
        Some(other) => bail!("Property '{key}' has invalid default value type, expected string, got {other:?}"),
      };

      Ok(Some(Property::String(property)))
    }
    // integer
    Some(Type::Single(SimpleType::Integer)) => {
      let property = match &value.default {
        Some(Value::Number(default)) if default.is_i64() => IntegerProperty::NormalWithDefault {
          default: default.as_i64().unwrap(),
        },
        None => IntegerProperty::NormalWithoutDefault,
        Some(other) => bail!("Property '{key}' has invalid default value type, expected integer, got {other:?}"),
      };

      Ok(Some(Property::Integer(property)))
    }
    // boolean
    Some(Type::Single(SimpleType::Boolean)) => {
      let property = match &value.default {
        Some(Value::Bool(default)) => BooleanProperty::NormalWithDefault { default: *default },
        None => BooleanProperty::NormalWithoutDefault,
        Some(other) => bail!("Property '{key}' has invalid default value type, expected boolean, got {other:?}"),
      };

      Ok(Some(Property::Boolean(property)))
    }
    // disableable types
    Some(Type::Union(types)) => {
      if types.len() != 2 || !types.contains(&SimpleType::Boolean) {
        bail!(
          "Property '{key}' has invalid union type - expected exactly two types with one being boolean, got {types:?}"
        );
      }

      if types.contains(&SimpleType::String) {
        // disableable enum
        if let Some(r#enum) = &value.r#enum {
          let variants: Vec<String> = r#enum.iter().map(|v| v.to_owned()).collect();
          let name = format!("{parent_key}_{key}").to_case(Case::Pascal);

          structures.insert(
            name.clone(),
            Structure::Enum(Enum {
              name: name.clone(),
              variants,
            }),
          );

          let property = match &value.default {
            Some(Value::String(default)) => EnumProperty::DisableableWithDefault {
              name,
              default: default.to_owned(),
            },
            None => EnumProperty::DisableableWithoutDefault { name },
            Some(other) => bail!("Property '{key}' has invalid default value type, expected string, got {other:?}"),
          };

          return Ok(Some(Property::Enum(property)));
        }

        // disableable color
        match &value.parsed_as.as_deref() {
          Some("java.awt.Color") => {
            let property = match &value.default {
              Some(Value::String(default)) => ColorProperty::DisableableWithDefault {
                default: default.to_owned(),
              },
              None => ColorProperty::DisableableWithoutDefault,
              Some(other) => bail!("Property '{key}' has invalid default value type, expected string, got {other:?}"),
            };

            return Ok(Some(Property::Color(property)));
          }
          Some(parsed_as) => bail!("Property '{key}' has unsupported parsed_as value: {parsed_as}"),
          _ => {}
        }

        // disableable string
        let property = match &value.default {
          Some(Value::String(default)) => StringProperty::DisableableWithDefault {
            default: default.to_owned(),
          },
          None => StringProperty::DisableableWithoutDefault,
          Some(other) => {
            bail!("Property '{key}' has invalid default value type, expected string or null, got {other:?}")
          }
        };

        return Ok(Some(Property::String(property)));
      }

      // disableable integer
      if types.contains(&SimpleType::Integer) {
        let property = match &value.default {
          Some(Value::Number(default)) if default.is_i64() => IntegerProperty::DisableableWithDefault {
            default: default.as_i64().unwrap(),
          },
          None => IntegerProperty::DisableableWithoutDefault,
          Some(other) => {
            bail!("Property '{key}' has invalid default value type, expected integer or null, got {other:?}")
          }
        };

        return Ok(Some(Property::Integer(property)));
      }

      bail!("Property '{key}' has unsupported union type combination: {types:?}");
    }
    // array
    Some(Type::Single(SimpleType::Array)) => match &value.items {
      Some(ItemsUnion::Schema(schema)) => match &schema.kind {
        Some(Type::Single(kind)) => {
          let kind = match kind {
            SimpleType::String => JavaType::String,
            SimpleType::Integer => JavaType::Integer,
            SimpleType::Boolean => JavaType::Boolean,
            _ => bail!("Property '{key}' has array with unsupported item type: {kind:?}"),
          };

          Ok(Some(Property::List(ListProperty { kind })))
        }
        None => bail!("Property '{key}' has array items without a type"),
        other => bail!("Property '{key}' has array with invalid item type: {other:?}"),
      },
      None => bail!("Property '{key}' is an array but missing 'items' field"),
      other => bail!("Property '{key}' has invalid 'items' field: {other:?}"),
    },
    // objects or maps
    Some(Type::Single(SimpleType::Object)) => {
      // treat as map<string, _> if value.additional_properties is defined
      if let Some(additional) = value.additional_properties.as_deref() {
        match &additional.kind {
          Some(Type::Single(kind)) => {
            let kind = match kind {
              SimpleType::String => JavaType::String,
              SimpleType::Integer => JavaType::Integer,
              SimpleType::Boolean => JavaType::Boolean,
              _ => bail!("Property '{key}' has map with unsupported value type: {kind:?}"),
            };

            return Ok(Some(Property::Map(MapProperty { value_kind: kind })));
          }
          None => bail!("Property '{key}' is an object but missing 'additionalProperties' type"),
          other => bail!("Property '{key}' has map with invalid value type: {other:?}"),
        }
      }

      // otherwise treat as class
      let nested_name = process_class(prefix, root, value, key.to_owned(), structures)?;
      Ok(Some(Property::Class(ClassProperty { name: nested_name })))
    }
    Some(other) => bail!("Property '{key}' has unsupported type: {other:?}"),
    None => bail!("Property '{key}' is missing required 'type' field"),
  }
}

fn collect_properties(
  prefix: &str,
  root: &Schema,
  schema: &Schema,
  name: &str,
  structures: &mut HashMap<String, Structure>,
) -> anyhow::Result<HashMap<String, Property>> {
  let mut properties = HashMap::new();

  if let Some(all_of) = &schema.all_of {
    for item in all_of.iter() {
      let nested_properties = collect_properties(prefix, root, item, name, structures)?;
      properties.extend(nested_properties);
    }
  }

  if let Some(reference) = &schema.reference {
    let reference = reference.trim_start_matches("#/definitions/");
    let referenced_schema = resolve_reference(root, reference)?;

    return collect_properties(prefix, root, referenced_schema, name, structures);
  }

  if let Some(props) = &schema.properties {
    for (key, value) in props.iter() {
      // skip server override config
      if key == "override" {
        continue;
      }

      match process_property(prefix, root, name, key, value, structures) {
        Ok(Some(property)) => {
          properties.insert(key.to_case(Case::Camel), property);
        }
        Ok(None) => {}
        Err(e) => eprintln!("Error processing property {key} in class {name}: {e:?}"),
      }
    }
  }

  Ok(properties)
}

pub fn process_class(
  prefix: &str,
  root: &Schema,
  schema: &Schema,
  name: String,
  structures: &mut HashMap<String, Structure>,
) -> anyhow::Result<String> {
  print!("mapping name: {name} -> ");

  let name = name.trim_end_matches("Config").trim_end_matches("config");

  let name = if name == "Root" {
    "RootConfig".to_owned()
  } else {
    format!("{prefix}_{name}Config")
  };
  let name = name.to_case(Case::Pascal);

  println!("{name}");

  if let Some(reference) = &schema.reference {
    let reference = reference.trim_start_matches("#/definitions/");
    let referenced_schema = resolve_reference(root, reference)?;

    return process_class(prefix, root, referenced_schema, reference.into(), structures);
  }

  let properties = match collect_properties(prefix, root, schema, &name, structures) {
    Ok(props) => props,
    Err(e) => {
      eprintln!("Failed to collect properties for class '{name}': {e:#}");
      HashMap::new()
    }
  };

  let class = Class { properties };
  structures.insert(name.to_owned(), Structure::Class(class));
  Ok(name)
}

pub fn process_override_config(
  root: &Schema,
  structures: &mut HashMap<String, Structure>,
) -> anyhow::Result<HashMap<String, Property>> {
  let properties = root.properties.as_ref().context("Root schema has no properties")?;
  let overrides = properties
    .get("override")
    .context("schema.properties missing 'override'")?;
  let overrides = overrides
    .additional_properties
    .as_ref()
    .context("overrides missing additionalProperties")?;

  collect_properties("", root, overrides, "OverrideConfig", structures)
}
