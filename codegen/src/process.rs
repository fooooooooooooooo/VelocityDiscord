use std::collections::HashMap;

use anyhow::bail;
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

struct References {
  map: HashMap<String, String>,
}

impl References {
  fn new() -> Self {
    Self { map: HashMap::new() }
  }

  fn get(&self, reference: &str) -> Option<&String> {
    self.map.get(reference.trim_start_matches(REFERENCE_PREFIX))
  }

  fn insert(&mut self, reference: &str, name: String) {
    self
      .map
      .insert(reference.trim_start_matches(REFERENCE_PREFIX).to_owned(), name);
  }
}

pub struct Context<'a> {
  pub root: &'a Schema,
  structures: HashMap<String, Structure>,
  definitions: References,
}

impl<'a> Context<'a> {
  pub fn new(root: &'a Schema) -> Self {
    Self {
      root,
      structures: HashMap::new(),
      definitions: References::new(),
    }
  }

  pub fn into_structures(self) -> HashMap<String, Structure> {
    self.structures
  }

  fn get_definition(&mut self, reference: &str) -> anyhow::Result<String> {
    let resolved = self.definitions.get(reference).map(ToOwned::to_owned);

    if let Some(name) = resolved {
      return Ok(name);
    }

    if let Some(definitions) = &self.root.definitions {
      let reference = reference.trim_start_matches(REFERENCE_PREFIX);
      let resolved = definitions.get(reference);
      if let Some(schema) = resolved {
        let name = process_class(self, reference.to_owned(), schema)?;
        self.insert_definition(reference, name.to_owned());
        return Ok(name);
      }
    }

    bail!("Failed to resolve reference '{reference}' in schema definitions");
  }

  fn insert_definition(&mut self, reference: &str, name: String) {
    self.definitions.insert(reference, name);
  }

  fn insert_structure(&mut self, name: String, structure: Structure) {
    self.structures.insert(name, structure);
  }
}

pub fn collect_definitions(context: &mut Context) -> anyhow::Result<()> {
  println!(
    "processing {} definitions",
    context.root.definitions.as_ref().map_or(0, |d| d.len())
  );

  if let Some(definitions) = &context.root.definitions {
    for (reference, schema) in definitions.iter() {
      let name = process_class(context, reference.to_owned(), schema)?;
      context.insert_definition(reference, name);
    }
  }

  println!("Collected {} definitions from schema", context.definitions.map.len());
  for (reference, name) in context.definitions.map.iter() {
    println!(" - {reference}: {name}");
  }

  Ok(())
}

fn process_property(
  context: &mut Context,
  parent_key: &str,
  key: &str,
  value: &Schema,
) -> anyhow::Result<Option<Property>> {
  if key == "$schema" {
    eprintln!("Skipping $schema property");
    return Ok(None);
  }

  if value.kind.is_none() {
    if let Some(reference) = &value.reference {
      let name = context.get_definition(reference)?;
      return Ok(Some(Property::Class(ClassProperty { name })));
    }

    bail!("Property '{key}' is missing required 'type' field");
  }

  match &value.kind {
    // string or color or enum
    Some(Type::Single(SimpleType::String)) => {
      // enum
      if let Some(r#enum) = &value.r#enum {
        let variants: Vec<String> = r#enum.iter().map(|v| v.to_owned()).collect();
        let name = format!("{parent_key}_{key}");

        context.insert_structure(
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
          let name = format!("{parent_key}_{key}");

          context.insert_structure(
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
      // let nested_name = process_class(context, format!("{parent_key}_{key}"),
      // value)?;
      let nested_name = process_class(context, key.to_owned(), value)?;
      Ok(Some(Property::Class(ClassProperty { name: nested_name })))
    }
    Some(other) => bail!("Property '{key}' has unsupported type: {other:?}"),
    None => bail!("Property '{key}' is missing required 'type' field"),
  }
}

pub fn process_class(context: &mut Context, name: String, schema: &Schema) -> anyhow::Result<String> {
  // if $ref at root level, just return the definition name
  if let Some(reference) = &schema.reference {
    return context.get_definition(reference);
  }

  let mut properties = HashMap::new();

  // if allOf, resolve references as sub classes, and objects as properties
  if let Some(all_of) = &schema.all_of {
    for item in all_of.iter() {
      if let Some(reference) = &item.reference {
        let name = context.get_definition(reference)?;
        properties.insert(name.to_owned(), Property::Class(ClassProperty { name }));
      }
    }
  }

  if let Some(props) = &schema.properties {
    for (key, value) in props.iter() {
      match process_property(context, &name, key, value) {
        Ok(Some(property)) => {
          properties.insert(key.to_owned(), property);
        }
        Ok(None) => {}
        Err(e) => eprintln!("Error processing property {key} in class {name}: {e:?}"),
      }
    }
  }

  let class = Class { properties };
  context.insert_structure(name.to_owned(), Structure::Class(class));
  Ok(name)
}
