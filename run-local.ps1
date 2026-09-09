param(
    [string]$TomcatHome = (Join-Path $PSScriptRoot '.runtime/apache-tomcat-11.0.25'),
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not (Test-Path "$TomcatHome/bin/bootstrap.jar")) {
    throw 'Tomcat not found. Supply -TomcatHome with your Tomcat 11 directory. See README.md.'
}
if (-not $SkipBuild) { & "$PSScriptRoot/build.ps1" }
$env:CATALINA_HOME = (Resolve-Path $TomcatHome).Path
# Keep deployments, logs, work and configuration inside this workspace.
$env:CATALINA_BASE = Join-Path $PSScriptRoot '.runtime/tomcat-base'
$env:APP_UPLOAD_DIR = Join-Path $PSScriptRoot 'upload'
foreach ($directory in 'conf','logs','temp','webapps','work') {
    New-Item -ItemType Directory -Force (Join-Path $env:CATALINA_BASE $directory) | Out-Null
}
if (-not (Test-Path "$env:CATALINA_BASE/conf/server.xml")) {
    Copy-Item "$env:CATALINA_HOME/conf/*" "$env:CATALINA_BASE/conf" -Recurse
    $serverFile = "$env:CATALINA_BASE/conf/server.xml"
    [xml]$server = Get-Content $serverFile
    foreach ($connector in $server.Server.Service.Connector) {
        $connector.SetAttribute('address','127.0.0.1')
    }
    $server.Save($serverFile)
}
Copy-Item -LiteralPath "$PSScriptRoot/target/ServletCRUDMVC.war" -Destination "$env:CATALINA_BASE/webapps/ServletCRUDMVC.war"
& "$env:CATALINA_HOME/bin/catalina.bat" run
