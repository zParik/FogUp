# When it breaks

## "running scripts is disabled on this system"

PowerShell's default execution policy. Start the script the way the install
guide says:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup.ps1
```

This affects that one process and changes nothing on the machine.

## "javac is not recognized" right after installing a JDK

The PowerShell window was open before the install, so its PATH is stale.
`build.ps1` and `run.ps1` call `Use-Jdk` from `scripts\jdk.ps1`, which searches
`jdk\`, PATH, `JAVA_HOME` and Program Files for `javac.exe` and fixes the
session PATH itself. If you are calling `javac` by hand, open a new window.

## "No JDK found. Run .\setup.ps1"

`build.ps1` and `run.ps1` never download anything; only setup does. Run
`powershell -ExecutionPolicy Bypass -File .\setup.ps1` and it will fetch a
portable JDK into `jdk\`.

## Java is installed but setup downloads its own JDK anyway

You have a JRE, not a JDK: `java -version` works and `javac -version` does not.
iFogSim ships as source, so a compiler is required. Let it download, or install
a JDK yourself with `winget install --id Microsoft.OpenJDK.21` and delete
`jdk\`.

## The JDK download fails or is too slow

It is one HTTPS GET of about 200 MB from api.adoptium.net. On a filtered
network, fetch the Temurin 21 ZIP by hand and unpack it under the FogUp folder
so that `jdk\<anything>\bin\javac.exe` exists, then re-run setup. The search is
recursive, so the folder name does not matter.

## Compile ends with "error: unreported exception Exception"

Something in your new code calls an iFogSim constructor that declares
`throws Exception`. `FogBroker` is the usual one. Add `throws Exception` to
`main`, which is what all three examples do.

## Warnings about deprecated APIs during the build

Expected. iFogSim carries a CloudSim 3 fork from 2015 and uses APIs that later
JDKs marked deprecated. The build is fine as long as no line starts with
`error:`.

## A loop shows a latency of about 1 ms when it should cross the WAN

The loop ends on a module it already visited, so iFogSim closed it on the first
hop. Rewrite the loop so the last name appears once. Details in
[03-ifogsim-concepts.md](03-ifogsim-concepts.md).

## A loop reports zero samples

Two loops share a sensor. The first one to close deletes the emit time, and the
second finds nothing to subtract. Measure one loop per sensor.

## Latency is identical no matter what I change

Check whether the path you are measuring crosses the link you edited. In
`Ex01Hello` the module sits on the gateway, so raising the gateway-to-cloud
latency from 100 ms to 400 ms leaves the loop at 6.422 ms. Nothing on that loop
goes to the cloud.

## The edge placement is slower than the cloud placement

Often correct rather than broken. `--mode edge --cameras 4 --frame-interval 5`
reports 394.7 ms for the edge against 218.6 ms for the cloud, because eight
cameras at one frame per 5 ms saturate a 1000 MIPS gateway and the queue grows
past the 100 ms WAN round trip. Give the fog layer more MIPS in
`FogLab.gateway` or slow the sensors down, and the ordering flips back.

## Nothing appears in results\

The CSV is written by a JVM shutdown hook, so a run killed with Ctrl+C or one
that threw before `CloudSim.startSimulation()` leaves no row. Check the console
for a stack trace first.

## Deleting the download and starting over

```powershell
Remove-Item -Recurse -Force ifogsim, build
powershell -ExecutionPolicy Bypass -File .\setup.ps1
```

Nothing under `ifogsim\` or `build\` is tracked in git, and `results\` is yours
to delete whenever it gets confusing.
