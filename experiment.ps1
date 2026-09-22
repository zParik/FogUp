# Sweeps Ex02SmartBuilding over both placements and three topology sizes.
# iFogSim calls System.exit() when a simulation ends, so every point needs its
# own JVM; that is why this is a loop of processes and not a loop in Java.
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$out = "results\experiment.csv"
New-Item -ItemType Directory -Force -Path "results" | Out-Null
if (Test-Path $out) { Remove-Item $out }

foreach ($cameras in 2, 4, 8) {
    foreach ($mode in "cloud", "edge") {
        Write-Host "--- mode=$mode cameras=$cameras"
        & "$PSScriptRoot\run.ps1" Ex02SmartBuilding --mode $mode --gateways 2 --cameras $cameras --csv $out | Out-Null
    }
}

Write-Host ""
Write-Host "$out :"
Import-Csv $out | Format-Table -AutoSize
