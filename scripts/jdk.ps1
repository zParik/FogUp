# Finds a JDK for the current PowerShell session, and downloads a portable one
# into jdk\ when the machine has none.
#
# Two problems are being avoided here. Installing a JDK needs administrator
# rights, which a locked-down laptop often will not give. And a fresh install
# does not appear on the PATH of an already-open window, which is the single
# most common reason "javac is not recognized" shows up two minutes after a
# successful install. A ZIP unpacked into this folder has neither problem.

$script:JdkRoot = Join-Path $PSScriptRoot "..\jdk"

function Find-JavacPath {
    # 1. The portable JDK this repository downloaded, if it is there.
    if (Test-Path $script:JdkRoot) {
        $local = Get-ChildItem -Path $script:JdkRoot -Filter "javac.exe" -Recurse -ErrorAction SilentlyContinue |
                 Select-Object -First 1
        if ($local) { return Split-Path $local.FullName }
    }

    # 2. Anything already on PATH.
    $cmd = Get-Command javac -ErrorAction SilentlyContinue
    if ($cmd) { return Split-Path $cmd.Source }

    # 3. JAVA_HOME, which an installer may have set without touching PATH.
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
        return "$env:JAVA_HOME\bin"
    }

    # 4. The usual install roots, for a JDK installed in a window opened later.
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

function Install-PortableJdk {
    # Temurin 21, the Eclipse Adoptium build, as a ZIP rather than an installer.
    # No administrator rights, no registry, no PATH changes outside this session.
    $arch = if ($env:PROCESSOR_ARCHITECTURE -eq "ARM64") { "aarch64" } else { "x64" }
    $url = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/$arch/jdk/hotspot/normal/eclipse"
    $zip = Join-Path $PSScriptRoot "..\.fogup-jdk.zip"

    Write-Host "      downloading a portable Temurin 21 JDK ($arch, about 200 MB)"
    $oldPref = $ProgressPreference
    $ProgressPreference = "SilentlyContinue"
    try {
        Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    } finally {
        $ProgressPreference = $oldPref
    }

    if (Test-Path $script:JdkRoot) { Remove-Item -Recurse -Force $script:JdkRoot }
    New-Item -ItemType Directory -Force -Path $script:JdkRoot | Out-Null
    Expand-Archive -Path $zip -DestinationPath $script:JdkRoot -Force
    Remove-Item $zip

    $bin = Find-JavacPath
    if (-not $bin) {
        Write-Error "The JDK downloaded but no javac.exe turned up under jdk\. Delete jdk\ and try again, or install a JDK from https://adoptium.net/"
    }
    Write-Host "      unpacked into jdk\ (delete that folder to undo this)"
    return $bin
}

function Use-Jdk {
    param([switch]$Install)

    $bin = Find-JavacPath
    if (-not $bin) {
        if (-not $Install) {
            Write-Error "No JDK found. Run .\setup.ps1, which downloads a portable one into jdk\."
        }
        $bin = Install-PortableJdk
    }
    if (($env:Path -split ';') -notcontains $bin) {
        $env:Path = "$bin;$env:Path"
    }
    $env:JAVA_HOME = Split-Path $bin
}
