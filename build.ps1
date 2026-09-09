param([switch]$Offline)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$arguments = @('-B', '-ntp', '-s', '.mvn/settings.xml', '-gs', '.mvn/settings.xml', "-Dmaven.repo.local=$PSScriptRoot/.m2/repository")
if ($Offline) { $arguments += '-o' }
& mvn @arguments clean package
if ($LASTEXITCODE -ne 0) { throw 'Maven build failed' }
