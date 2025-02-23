open .env | from toml | load-env

if $env.TEST_SERVER_DIR == '' {
  print 'TEST_SERVER_DIR is not set'
  exit 1
}

# ./gradlew.bat build

let dest_dir = ($env.TEST_SERVER_DIR | path join 'plugins')

let old_jars_paths = (ls $dest_dir | get name | where $it =~ '(?i)velocitydiscord-.*\.jar')

$old_jars_paths | each { rm $in }

let new_jar_path = (ls 'build/libs' | sort-by modified | last | get name)
let new_jar_name = ($new_jar_path | path split | last)

let dest_jar_path = $'($dest_dir)/($new_jar_name)'

print $'($new_jar_path) -> ($dest_jar_path)'

cp $new_jar_path $dest_jar_path
