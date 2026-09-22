# Finds a JDK and puts it on PATH for the current PowerShell session.
#
# A fresh winget install does not appear on the PATH of an already-open window,
# which is the single most common reason "javac is not recognized" shows up two
# minutes after a successful install. Searching the usual install roots avoids
# telling anyone to reboot.

function Find-JavacPath {
    $cmd = Get-Command javac -ErrorAction SilentlyContinue
    if ($cmd) { return Split-Path $cmd.Source }

    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
        return "$env:JAVA_HOME\bin"
    }

    $roots = @(
        "$env:ProgramFiles\Microsoft",
        "$env:ProgramFiles\Eclipse Adoptium",
        "$env:ProgramFiles\Java",
        "$env:ProgramFiles\Amazon Corretto",
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium"
    )
    foreach ($root in $roots) {
        if (-not (Test-Path $root)) { continue }
        $hit = Get-ChildItem -Path $root -Filter "javac.exe" -Recurse -ErrorAction SilentlyContinue |
               Sort-Object FullName -Descending | Select-Object -First 1
        if ($hit) { return Split-Path $hit.FullName }
    }
    return $null
}

function Use-Jdk {
    $bin = Find-JavacPath
    if (-not $bin) {
        Write-Error "No JDK found. Run .\setup.ps1, which installs one."
    }
    if (($env:Path -split ';') -notcontains $bin) {
        $env:Path = "$bin;$env:Path"
    }
    $env:JAVA_HOME = Split-Path $bin
}
