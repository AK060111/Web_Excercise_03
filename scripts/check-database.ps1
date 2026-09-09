$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
& java -cp 'target/classes;target/ServletCRUDMVC/WEB-INF/lib/*' scripts/CheckDatabase.java
if ($LASTEXITCODE -ne 0) { throw 'Database check failed' }
