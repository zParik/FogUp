# One-shot setup for Windows: install a JDK if needed, download iFogSim,
# compile everything, run the smallest example. Safe to re-run.
#
# Start it from PowerShell in this folder:
#   powershell -ExecutionPolicy Bypass -File .\setup.ps1
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

. "$PSScriptRoot\scripts\jdk.ps1"

$zipUrl = "https://codeload.github.com/Cloudslab/iFogSim/zip/refs/heads/main"

Write-Host "[1/4] checking for a JDK"
$bin = Find-JavacPath
if (-not $bin) {
    Write-Host "      no JDK found, installing Microsoft OpenJDK 21 with winget"
    if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
        Write-Host ""
        Write-Host "winget is not available on this machine."
        Write-Host "Install a JDK by hand, then re-run this script:"
        Write-Host "  https://adoptium.net/temurin/releases/?version=21&package=jdk&os=windows"
        exit 1
    }
    winget install --id Microsoft.OpenJDK.21 --silent --accept-package-agreements --accept-source-agreements
    if ($LASTEXITCODE -ne 0) {
        Write-Host "      Microsoft OpenJDK failed, trying Eclipse Temurin"
        winget install --id EclipseAdoptium.Temurin.21.JDK --silent --accept-package-agreements --accept-source-agreements
    }
    $bin = Find-JavacPath
    if (-not $bin) {
        Write-Error "The install finished but javac is still not on disk where expected. Open a new PowerShell window and re-run .\setup.ps1."
    }
}
Use-Jdk
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
