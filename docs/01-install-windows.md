# Installing on Windows

Fifteen minutes on a laptop with nothing installed, most of it download time.
Administrator rights are not needed: a missing JDK is downloaded as a ZIP into
the FogUp folder rather than installed. You need a network that allows GitHub
and api.adoptium.net.

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

**[1/4] checking for a JDK.** The script looks in four places, in this order:
`jdk\` inside this folder, PATH, `JAVA_HOME`, then the usual install roots under
Program Files. Finding none, it downloads Temurin 21 from api.adoptium.net,
about 200 MB, and unpacks it into `jdk\`. That copy is used for this repository
only, through a PATH change that lasts for the current PowerShell session, and
deleting `jdk\` reverses the whole thing.

A JRE is not enough: iFogSim ships as source, so the compiler has to be there.
If `java -version` works on the machine but `javac -version` does not, the
script will download its own JDK and carry on.

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

## Using a system-wide JDK instead

If you would rather have Java available outside this folder, install it first
and the script will use it:

```powershell
winget install --id Microsoft.OpenJDK.21
```

Or download the installer from
<https://adoptium.net/temurin/releases/?version=21&package=jdk&os=windows> and
tick "Set JAVA_HOME". Either way, delete `jdk\` afterwards if the portable copy
is already there, since it takes priority.

## If javac is still "not recognized" after installing one

An install does not update the PATH of a PowerShell window that was already
open. `scripts\jdk.ps1` works around this by searching `jdk\`, PATH,
`JAVA_HOME` and the install roots under Program Files, then putting what it
finds on PATH for the current session, which is why `build.ps1` and `run.ps1`
both dot-source it. Opening a fresh PowerShell window also fixes it.

## Behind a proxy or a filtered network

Setup makes at most two HTTPS GETs: one to api.adoptium.net for the JDK, one to
codeload.github.com for iFogSim. Both can be done by hand on another machine.

For the JDK, unpack the Temurin ZIP so that `jdk\<something>\bin\javac.exe`
exists under the FogUp folder; the search is recursive, so the exact folder name
does not matter. For iFogSim, extract the repository ZIP and rename the
resulting `iFogSim-main` folder to `ifogsim`. Then run `.\build.ps1` directly.
