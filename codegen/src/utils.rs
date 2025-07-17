use std::path::Path;

use convert_case::{Case, Casing};

pub fn string_literal(value: &str) -> String {
  format!(
    "\"{}\"",
    value
      .replace('"', "\\\"")
      .replace('\\', "\\\\")
      .replace('\n', "\\n")
      .replace('\r', "\\r")
      .replace('\t', "\\t")
  )
}

pub fn default_property_name(key: &str) -> String {
  format!("{}_DEFAULT", key.to_case(Case::UpperSnake))
}

pub fn display_path(path: &Path) -> String {
  path
    .to_str()
    .unwrap_or("<invalid path>")
    .replace(r"\\", "/")
    .replace('\\', "/")
}

pub fn pascal<S>(str: S) -> String
where
  S: AsRef<str>,
  S: ToString,
{
  str.to_case(Case::Pascal)
}

pub fn camel<S>(str: S) -> String
where
  S: AsRef<str>,
  S: ToString,
{
  str.to_case(Case::Camel)
}
