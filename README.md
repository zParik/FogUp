# FogUp

iFogSim, installed and running from a cold Windows laptop in about fifteen
minutes, with three worked simulations of edge devices to read and change.

iFogSim ships as an Eclipse project with no build file, no release jar and no
install instructions beyond "import into your IDE". The scripts here skip the
IDE entirely: they check for a JDK, install one if it is missing, download
iFogSim, compile it together with the examples in `src/fogup`, and run a
simulation that prints its results.

## Install

Windows, in PowerShell, from this folder:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup.ps1
```

Linux or macOS:

```bash
./setup.sh
```

If `javac` is missing the script downloads a portable Temurin 21 JDK, about
200 MB, and unpacks it into `jdk\`. Nothing is installed system-wide, no
administrator rights are needed, and deleting the `jdk\` folder undoes it. An
existing JDK is used as it is. The script then downloads iFogSim into
`ifogsim\`, compiles 450 source files into `build\classes`, and runs
`Ex01Hello`. Re-running it is safe; it skips whatever is already in place.

Full walkthrough with screenshots of what each step prints:
[docs/01-install-windows.md](docs/01-install-windows.md).

## Run something

```powershell
.\run.ps1 Ex01Hello
.\run.ps1 Ex02SmartBuilding --mode cloud
.\run.ps1 Ex02SmartBuilding --mode edge
.\run.ps1 Ex03Wearable --filter-ratio 1.0
.\experiment.ps1
```

On Linux and macOS the same commands are `./run.sh Ex01Hello` and so on.

Every run appends a row to a CSV in `results\`, so two runs of the same example
with different flags sit next to each other in one file.

## The three examples

**`Ex01Hello`** is the smallest topology that still has every part iFogSim
needs: a cloud, a gateway, one edge node, one temperature sensor, one valve
actuator, and one module that turns readings into valve commands. 131 lines,
half of them comments. Read this one first.

**`Ex02SmartBuilding`** puts cameras on the edge nodes and runs motion detection
either in the cloud or on the fog layer, with the same topology both times:

| mode | alert loop | network usage | cloud cost |
|---|---|---|---|
| `cloud` | 216.5 ms | 38782.6 | 99488.0 |
| `edge` | 7.0 ms | 4028.0 | 4270.2 |

(2 gateways, 2 cameras each, frames every 20 ms, 100 ms WAN latency. Reproduce
with `.\experiment.ps1`.)

The edge does not always win, and the example can show that too. Push the frame
rate up and the number of cameras with it:

```powershell
.\run.ps1 Ex02SmartBuilding --mode cloud --cameras 4 --frame-interval 5
.\run.ps1 Ex02SmartBuilding --mode edge  --cameras 4 --frame-interval 5
```

Now the cloud answers in 218.6 ms and the edge takes 394.7 ms. Eight cameras at
200 frames per second outrun the 1000 MIPS of a gateway, and the queue on the
fog node costs more than the 100 ms round trip to a data centre that has
capacity to spare. Finding that crossover point is the kind of question
iFogSim exists to answer.

**`Ex03Wearable`** runs ECG monitors on patient phones. The on-phone module
drops most samples before uploading, and `--filter-ratio` controls how many
survive: at 0.2 the network usage is 58452, at 1.0 it is 290685, for identical
sensors and identical alert latency.

## What is in here

```
setup.ps1 / setup.sh        get a JDK, fetch iFogSim, compile, smoke test
scripts\jdk.ps1 / jdk.sh    find a JDK, or download a portable one into jdk\
build.ps1 / build.sh        recompile after editing anything in src\fogup
run.ps1   / run.sh          run one example class with flags
experiment.ps1 / .sh        sweep Ex02 over both placements and three sizes
src\fogup\FogLab.java       device factory and argument parsing
src\fogup\Results.java      collects metrics and appends them to a CSV
src\fogup\Ex0*.java         the three simulations
docs\                       install guide, session plan, concepts, cheat sheet
```

`ifogsim\`, `jdk\` and `build\` are downloaded or generated, and are not in git.

## Docs

- [01-install-windows.md](docs/01-install-windows.md) is the click-by-click install.
- [02-two-hour-session.md](docs/02-two-hour-session.md) is a teaching plan with timings.
- [03-ifogsim-concepts.md](docs/03-ifogsim-concepts.md) explains sensors, modules, tuples, loops and placement.
- [04-cheatsheet.md](docs/04-cheatsheet.md) is the "I want to change X, which line is it" table.
- [05-troubleshooting.md](docs/05-troubleshooting.md) covers the errors that actually come up.

## Versions

Tested against iFogSim `main` (the iFogSim 2 codebase) on OpenJDK 21. iFogSim
bundles its own CloudSim 3 fork and every jar it needs under `ifogsim\jars`, so
there is nothing to resolve from Maven Central.

MIT licensed. iFogSim itself is GPL-3.0 and belongs to
[Cloudslab](https://github.com/Cloudslab/iFogSim); this repository downloads it
rather than copying it.
