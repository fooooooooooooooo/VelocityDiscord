use crate::properties::boolean::BooleanProperty;
use crate::properties::class::ClassProperty;
use crate::properties::color::ColorProperty;
use crate::properties::r#enum::EnumProperty;
use crate::properties::integer::IntegerProperty;
use crate::properties::list::ListProperty;
use crate::properties::map::MapProperty;
use crate::properties::string::StringProperty;

pub mod boolean;
pub mod class;
pub mod color;
pub mod r#enum;
pub mod integer;
pub mod list;
pub mod map;
pub mod string;

pub trait AsJava {
  fn as_property_type(&self) -> String;

  fn as_default(&self) -> Option<String> {
    None
  }

  fn as_parse_function(&self, key: &str, default_property: &str) -> String;
}

pub enum Property {
  Map(MapProperty),
  List(ListProperty),
  Enum(EnumProperty),
  String(StringProperty),
  Color(ColorProperty),
  Integer(IntegerProperty),
  Boolean(BooleanProperty),
  Class(ClassProperty),
}

/// ```no_run
/// let property = all_properties!(prop, property.as_property_type());
/// ```
///
/// generates
///
/// ```no_run
/// let property = match prop {
///   Property::Map(property) => property.as_property_type(),
///   Property::List(property) => property.as_property_type(),
///   Property::Enum(property) => property.as_property_type(),
///   Property::String(property) => property.as_property_type(),
///   Property::Integer(property) => property.as_property_type(),
///   Property::Boolean(property) => property.as_property_type(),
///   Property::Class(property) => property.as_property_type(),
/// };
/// ```
#[macro_export]
macro_rules! all_properties {
  ($prop:ident, $property:expr) => {
    match $prop {
      Property::Map($prop) => $property,
      Property::List($prop) => $property,
      Property::Enum($prop) => $property,
      Property::String($prop) => $property,
      Property::Color($prop) => $property,
      Property::Integer($prop) => $property,
      Property::Boolean($prop) => $property,
      Property::Class($prop) => $property,
    }
  };
}
