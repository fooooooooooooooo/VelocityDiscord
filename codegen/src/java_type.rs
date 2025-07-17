use anyhow::bail;

use crate::schema::SimpleType;

pub enum JavaType {
  String,
  Integer,
  Boolean,
}

impl TryFrom<SimpleType> for JavaType {
  type Error = anyhow::Error;

  fn try_from(value: SimpleType) -> Result<Self, Self::Error> {
    match value {
      SimpleType::String => Ok(JavaType::String),
      SimpleType::Integer => Ok(JavaType::Integer),
      SimpleType::Boolean => Ok(JavaType::Boolean),
      _ => bail!("unsupported type: {value}"),
    }
  }
}

impl std::fmt::Display for JavaType {
  fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
    match self {
      JavaType::String => write!(f, "String"),
      JavaType::Integer => write!(f, "Integer"),
      JavaType::Boolean => write!(f, "Boolean"),
    }
  }
}
