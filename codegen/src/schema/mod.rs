use std::collections::HashMap;

use serde::Deserialize;

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct Schema {
  pub title: Option<String>,

  #[serde(rename = "type")]
  pub kind: Option<Type>,

  pub definitions: Option<HashMap<String, Schema>>,

  #[serde(rename = "$ref")]
  pub reference: Option<String>,

  pub additional_items: Option<Box<Schema>>,
  pub additional_properties: Option<Box<Schema>>,

  pub all_of: Option<Vec<Schema>>,
  pub any_of: Option<Vec<Schema>>,
  pub one_of: Option<Vec<Schema>>,

  pub default: Option<serde_json::Value>,

  /// if enum value is not a string, just error
  #[serde(rename = "enum")]
  pub r#enum: Option<Vec<String>>,

  pub items: Option<ItemsUnion>,

  pub properties: Option<HashMap<String, Schema>>,

  #[serde(rename = "required")]
  pub required_properties: Option<Vec<String>>,

  pub parsed_as: Option<String>,
}

#[derive(Deserialize, Debug)]
#[serde(untagged)]
pub enum ItemsUnion {
  Schema(Box<Schema>),
  Array(Vec<Schema>),
}

#[derive(Deserialize, Debug)]
#[serde(untagged)]
pub enum DependencyValue {
  Schema(Box<Schema>),
  Array(Vec<String>),
}

#[derive(Deserialize, Debug)]
#[serde(untagged)]
pub enum Type {
  Single(SimpleType),
  Union(Vec<SimpleType>),
}

impl std::fmt::Display for Type {
  fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
    match self {
      Type::Single(t) => write!(f, "{}", t),
      Type::Union(types) => {
        let types_str: Vec<String> = types.iter().map(|t| t.to_string()).collect();
        write!(f, "{}", types_str.join(" | "))
      }
    }
  }
}

#[derive(Deserialize, Debug, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum SimpleType {
  Array,
  Boolean,
  Integer,
  Null,
  Number,
  Object,
  String,
}

impl std::fmt::Display for SimpleType {
  fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
    match self {
      SimpleType::Array => write!(f, "array"),
      SimpleType::Boolean => write!(f, "boolean"),
      SimpleType::Integer => write!(f, "integer"),
      SimpleType::Null => write!(f, "null"),
      SimpleType::Number => write!(f, "number"),
      SimpleType::Object => write!(f, "object"),
      SimpleType::String => write!(f, "string"),
    }
  }
}
