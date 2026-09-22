# Two-hour session plan

Written for teaching one colleague on their own laptop. The order matters:
everything runs before anything is explained, so nobody spends the first hour
on an IDE import that fails.

## 0:00 to 0:15, get it running

They run `setup.ps1` on their machine while you talk over the download. By
minute fifteen they have `Ex01Hello` printing a loop delay of 6.422 ms.

Do not open Eclipse. The published iFogSim instructions tell you to import the
project, add jars to the build path, and run a class from the IDE; that path
produces most of the questions people ask about iFogSim, and none of them are
about fog computing.

## 0:15 to 0:35, what the simulator actually models

Walk through `src\fogup\Ex01Hello.java` top to bottom. It is one screen of
setup and one method that builds the application graph. Cover the five nouns in
[03-ifogsim-concepts.md](03-ifogsim-concepts.md): fog device, sensor, actuator,
module, tuple. Point out that iFogSim has no separate class for "edge device":
cloud, gateway and Raspberry Pi are all `FogDevice`, told apart by MIPS, level
and uplink latency.

Then change one number in front of them. Set the gateway uplink latency from
100 to 400, rebuild, re-run, and watch the loop delay not move, because the
module sits on the gateway and nothing crosses the WAN. That non-result is
worth more than a slide.

## 0:35 to 1:05, the experiment that has a point

Run both halves of `Ex02SmartBuilding`:

```powershell
.\run.ps1 Ex02SmartBuilding --mode cloud
.\run.ps1 Ex02SmartBuilding --mode edge
```

216.5 ms against 7.0 ms, with network usage falling from 38782.6 to 4028.0 and
cloud cost from 99488.0 to 4270.2. Same topology, same sensors, one line
different: `ModulePlacementMapping` pins the detector to the cloud,
`ModulePlacementEdgewards` walks the tree upward from each sensor and drops
each module on the first device with room for it.

Then break the result on purpose:

```powershell
.\run.ps1 Ex02SmartBuilding --mode cloud --cameras 4 --frame-interval 5
.\run.ps1 Ex02SmartBuilding --mode edge  --cameras 4 --frame-interval 5
```

The edge now loses, 394.7 ms against 218.6 ms. Ask them why before you explain
it. The answer is queueing: eight cameras at one frame per 5 ms exceed what a
1000 MIPS gateway can clear, and a saturated fog node beats a 100 ms round trip
only until the queue grows past it.

Finish the block with `.\experiment.ps1` and open `results\experiment.csv` in
Excel. Six rows, two placements, three sizes.

## 1:05 to 1:30, they change the model

Give them `Ex03Wearable` and one task: make the phones upload everything
instead of a fifth of it, and report what moves. The knob is `--filter-ratio`,
network usage goes from 58452 to 290685, and the alert latency does not change
at all, because the alert never leaves the phone.

Second task, this time editing Java rather than passing a flag: add a second
actuator type, or move `diagnosis` off the cloud and onto `ward-router` by
changing one line in the module mapping. Rebuild with `.\build.ps1`, which
recompiles iFogSim along with the examples.

## 1:30 to 1:50, mapping it to their own research

Open the cheat sheet ([04-cheatsheet.md](04-cheatsheet.md)) and turn whatever
they want to model into rows of it: how many devices, at what capacity, which
module goes where, which loop is the deadline that matters. Most fog papers are
one topology, one application graph and two placement policies compared on
latency, energy and network usage, which is exactly what `Ex02SmartBuilding`
already is.

If they want mobility, random waypoint, or clustering, those live in
`ifogsim\src\org\fog\test\perfeval` and run with the same `run.ps1`, for
example `.\run.ps1 org.fog.test.perfeval.TranslationServiceFog_RandomMobility`.
Say that those examples need the datasets under `ifogsim\dataset` and are a
second session, not a footnote to this one.

## 1:50 to 2:00, what to do next week

Leave them with:

- `docs\05-troubleshooting.md` for the errors they will hit alone.
- The knowledge that `Results.java` is where any new metric gets added, and
  that a CSV row per run beats reading console output.
- One concrete next step, not a reading list. "Add a third placement mode to
  Ex02 that pins the detector to the proxy" is a next step. "Read the iFogSim
  paper" is not.
