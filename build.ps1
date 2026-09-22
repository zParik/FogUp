# Compiles iFogSim plus the FogUp examples into build\classes.
# Usage:  .\build.ps1
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

. "$PSScriptRoot\scripts\jdk.ps1"
Use-Jdk

if (-not (Test-Path "ifogsim\src")) {
    Write-Error "ifogsim\ is missing. Run .\setup.ps1 first."
}

$cp = "ifogsim\jars\*;ifogsim\jars\commons-math3-3.5\*"

if (Test-Path "build\classes") { Remove-Item -Recurse -Force "build\classes" }
New-Item -ItemType Directory -Force -Path "build\classes" | Out-Null

# javac argument files treat a backslash as an escape character, so paths go in
# with forward slashes and quotes. Without this, any user folder containing a
# space (C:\Users\Firstname Lastname\...) fails to compile.
$sources = Get-ChildItem -Recurse -Filter *.java -Path "ifogsim\src","src" | ForEach-Object {
    '"' + ($_.FullName -replace '\\', '/') + '"'
}
$sources | Set-Content -Encoding ASCII "build\sources.txt"
Write-Host "compiling $($sources.Count) files..."

javac -nowarn -encoding UTF-8 -cp $cp -d "build\classes" "@build\sources.txt"
if ($LASTEXITCODE -ne 0) { Write-Error "javac failed with exit code $LASTEXITCODE" }
Write-Host "build\classes is up to date."
