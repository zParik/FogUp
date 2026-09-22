# Runs one example:  .\run.ps1 Ex02SmartBuilding --mode edge --cameras 4
# Fully qualified names also work:  .\run.ps1 org.fog.test.perfeval.VRGameFog
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

. "$PSScriptRoot\scripts\jdk.ps1"
Use-Jdk

if ($args.Count -lt 1) {
    Write-Host "usage: .\run.ps1 <ExampleName> [options]"
    Write-Host "examples: Ex01Hello, Ex02SmartBuilding, Ex03Wearable"
    exit 1
}
if (-not (Test-Path "build\classes")) {
    Write-Error "Nothing compiled yet. Run .\build.ps1 first."
}

$class = $args[0]
if ($class -notmatch '\.') { $class = "fogup.$class" }
$rest = @()
if ($args.Count -gt 1) { $rest = $args[1..($args.Count - 1)] }

$cp = "build\classes;ifogsim\jars\*;ifogsim\jars\commons-math3-3.5\*"
New-Item -ItemType Directory -Force -Path "results" | Out-Null
& java -cp $cp $class @rest
