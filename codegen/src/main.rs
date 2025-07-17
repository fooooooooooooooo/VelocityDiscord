use std::collections::HashMap;
use std::fs::{File, OpenOptions};
use std::io::Write;
use std::path::Path;

use anyhow::{Context, bail};

use crate::generate::generate_structure;
use crate::process::{process_class, process_override_config};
use crate::schema::Schema;
use crate::structures::{Class, Structure};
use crate::utils::display_path;

pub mod generate;
pub mod java_type;
#[macro_use]
pub mod properties;
pub mod process;
pub mod schema;
pub mod structures;
pub mod utils;

const REMOVE_OLD_ARG: &str = "--remove-old";

fn main() -> anyhow::Result<()> {
  let args = std::env::args().collect::<Vec<_>>();
  if args.len() != 3 && args.len() != 4 {
    bail!("Usage: {} <schema_file> <output_dir> [{REMOVE_OLD_ARG}]", args[0]);
  }

  if args.len() == 4 && args[3] != REMOVE_OLD_ARG {
    bail!("Unknown argument: {}", args[3]);
  }

  let remove_old = args.len() == 4 && args[3] == REMOVE_OLD_ARG;

  let schema_file = &args[1];
  let output_dir = &args[2];

  // check if schema file exists
  let schema_file = Path::new(schema_file);
  if !schema_file.exists() {
    bail!("Schema file '{}' does not exist", display_path(schema_file));
  }

  // check if output directory exists
  let output_dir = Path::new(output_dir);
  if !output_dir.exists() {
    bail!("Output directory '{}' does not exist", display_path(output_dir));
  }

  if remove_old {
    println!("Removing existing generated files in {}", display_path(output_dir));

    for entry in output_dir
      .read_dir()
      .with_context(|| format!("Failed to read output directory: {}", display_path(output_dir)))?
    {
      let entry = entry.with_context(|| "Failed to read directory entry")?;
      let path = entry.path();
      if path.is_file() {
        let file_name = path.file_name().and_then(|s| s.to_str()).unwrap_or("<unknown>");
        if file_name.ends_with(".java") {
          std::fs::remove_file(&path)
            .with_context(|| format!("Failed to remove existing: {}", display_path(&path)))
            .map_err(|e| e.context(format!("Error removing existing: {}", display_path(&path))))?;
        }
      }
    }
  }

  let schema = File::open(schema_file).with_context(|| "Failed to open schema.json file")?;
  let schema: Schema =
    serde_json::from_reader(schema).with_context(|| "Failed to parse schema.json as valid JSON schema")?;

  let mut global_structures: HashMap<String, Structure> = HashMap::new();

  process_class("Global", &schema, &schema, "Root".to_owned(), &mut global_structures)?;

  let global_structures = global_structures
    .into_iter()
    .map(|(name, structure)| {
      generate_structure(&name, structure).with_context(|| format!("Failed to generate structure for {name}"))
    })
    .collect::<anyhow::Result<Vec<_>>>()?;

  let mut structures = HashMap::new();

  // if override config exists
  match process_override_config(&schema, &mut structures) {
    Ok(properties) => {
      structures.insert("OverrideConfig".into(), Structure::Class(Class { properties }));
    }
    Err(e) => eprintln!("failed to process override config: {e}"),
  }

  let structures = structures
    .into_iter()
    .map(|(name, structure)| {
      generate_structure(&name, structure).with_context(|| format!("Failed to generate structure for {name}"))
    })
    .collect::<anyhow::Result<Vec<_>>>()?;

  for (name, content) in structures.into_iter().chain(global_structures.into_iter()) {
    let file_name = format!("{}.java", name);
    let file_path = output_dir.join(file_name);
    let display_path = display_path(&file_path);

    let mut file = OpenOptions::new()
      .create(true)
      .write(true)
      .truncate(true)
      .open(&file_path)
      .with_context(|| format!("Failed to open file for writing: {display_path}"))?;

    file
      .write_all(content.as_bytes())
      .with_context(|| format!("Failed to write content to file: {display_path}"))?;

    println!("generated: {display_path}");
  }

  Ok(())
}
