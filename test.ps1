./gradlew.bat build

$dest = "$HOME/minecraft_test_servers/velocity/plugins"

Remove-Item "$dest/velocity-discord-*.jar" -Force

$jars = Get-ChildItem ./build/libs
$sorted = $jars | Sort-Object -Property Name
$latest = $sorted | Select-Object -Last 1

Write-Host "build/libs/$latest -> $dest/$latest"
Copy-Item "./build/libs/$latest" "$dest/"
