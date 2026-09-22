# One-shot setup for Windows: get a JDK if needed, download iFogSim,
# compile everything, run the smallest example. Safe to re-run.
#
# Start it from PowerShell in this folder:
#   powershell -ExecutionPolicy Bypass -File .\setup.ps1
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

. "$PSScriptRoot\scripts\jdk.ps1"

$zipUrl = "https://codeload.github.com/Cloudslab/iFogSim/zip/refs/heads/main"

Write-Host "[1/4] checking for a JDK"
# -Install means: if no JDK is on this machine, fetch a portable one into jdk\.
# Nothing is installed system-wide and no administrator rights are needed.
Use-Jdk -Install
Write-Host "      using $env:JAVA_HOME"
javac -version

Write-Host "[2/4] fetching iFogSim"
if (Test-Path "ifogsim\src") {
    Write-Host "      ifogsim\ already present, skipping download"
} else {
    foreach ($stale in "ifogsim", "iFogSim-main", ".fogup-tmp.zip") {
        if (Test-Path $stale) { Remove-Item -Recurse -Force $stale }
    }
    # Invoke-WebRequest is slow with its progress bar on large files.
    $oldPref = $ProgressPreference
    $ProgressPreference = "SilentlyContinue"
    Invoke-WebRequest -Uri $zipUrl -OutFile ".fogup-tmp.zip"
    Expand-Archive -Path ".fogup-tmp.zip" -DestinationPath "." -Force
    $ProgressPreference = $oldPref
    Rename-Item "iFogSim-main" "ifogsim"
    Remove-Item ".fogup-tmp.zip"
    Write-Host "      iFogSim unpacked into ifogsim\"
}

Write-Host "[3/4] compiling"
& "$PSScriptRoot\build.ps1"

Write-Host "[4/4] smoke test: Ex01Hello"
& "$PSScriptRoot\run.ps1" Ex01Hello

Write-Host ""
Write-Host "Setup finished. Next:"
Write-Host "  .\run.ps1 Ex02SmartBuilding --mode cloud"
Write-Host "  .\run.ps1 Ex02SmartBuilding --mode edge"
Write-Host "  .\experiment.ps1"
Write-Host "Session plan: docs\02-two-hour-session.md"
