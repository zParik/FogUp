# Installing on Windows

Fifteen minutes on a laptop with nothing installed, most of it download time.
You need an account that can install software (winget writes to Program Files)
and a network that allows GitHub.

## 1. Get the files

With Git:

```powershell
git clone https://github.com/zParik/FogUp.git
cd FogUp
```

Without Git, download the ZIP from the green Code button on GitHub, extract it,
and open PowerShell in the extracted folder (Shift + right click in the folder,
"Open PowerShell window here").

## 2. Run setup

```powershell
powershell -ExecutionPolicy Bypass -File .\setup.ps1
```

The `-ExecutionPolicy Bypass` is there because Windows blocks unsigned scripts
by default and the error it gives ("running scripts is disabled on this
system") sends people down a long and unnecessary detour.

## What the four steps do

**[1/4] checking for a JDK.** If `javac` is already on PATH, the script prints
its version and moves on. Otherwise it runs
`winget install --id Microsoft.OpenJDK.21`. A JRE is not enough: iFogSim ships
as source, so the compiler has to be there.

**[2/4] fetching iFogSim.** Downloads
`codeload.github.com/Cloudslab/iFogSim/zip/refs/heads/main`, roughly 17 MB, and
unpacks it to `ifogsim\`. Git is never required. The folder contains the source
tree, the bundled jars including a CloudSim 3 fork, and the mobility datasets.

**[3/4] compiling.** `javac` builds 450 files into `build\classes`. Expect
deprecation warnings from the CloudSim fork; they are normal and predate this
repository. Only a line starting with `error:` matters.

**[4/4] smoke test.** Runs `Ex01Hello`, which prints the loop delay, the energy
per device and a results block, then exits.

A successful finish ends with:

```
FogUp results appended to results/ex01.csv
  loop1_latency_ms = 6.422
  ...
Setup finished. Next:
```

If `loop1_latency_ms` reads 6.422 on your machine too, the install is correct.
The simulator is deterministic, so that number is reproducible.

## If winget is missing

Windows 10 builds before 1809 and some managed machines have no winget.
Install Temurin 21 by hand from
<https://adoptium.net/temurin/releases/?version=21&package=jdk&os=windows>,
tick "Set JAVA_HOME" in the installer options, then re-run `setup.ps1`. It will
find the JDK and skip straight to the download.

## If the JDK installs but javac is still "not recognized"

An install does not update the PATH of a PowerShell window that was already
open. `scripts\jdk.ps1` works around this by searching the usual install roots
under Program Files and putting the JDK on PATH for the current session, which
is why `build.ps1` and `run.ps1` both dot-source it. Opening a fresh PowerShell
window also fixes it.

## Behind a proxy or a filtered network

The download is one HTTPS GET to codeload.github.com. If it fails, fetch the
ZIP by any other means, extract it, and rename the resulting `iFogSim-main`
folder to `ifogsim` inside the FogUp folder. Then run `.\build.ps1` directly;
setup is only doing those two things for you.
