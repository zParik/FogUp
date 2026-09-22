# I want to change X

Line numbers are for `src\fogup\Ex02SmartBuilding.java` unless noted. After any
edit, run `.\build.ps1`, which recompiles all 450 files and not just the one
you touched, then `.\run.ps1`.

## Topology

| Change | Where |
|---|---|
| Add a layer between gateway and cloud | Create another `FogLab.device(...)`, set its `setParentId` to the cloud and point the gateways at it |
| Make edge nodes weaker or stronger | `FogLab.edgeNode` in `FogLab.java`, the `1000` is MIPS |
| Change WAN delay | `--wan-latency 250`, or `setUplinkLatency` on the device whose link you mean |
| More devices | `--gateways 4 --cameras 8` |
| Different power draw | The last two arguments of `FogLab.device`, busy watts and idle watts |
| Charge for cloud time | The `ratePerMips` argument, `0.01` in `FogLab.cloud()` |

## Workload

| Change | Where |
|---|---|
| Sensor rate | `--frame-interval 5`, which is the `DeterministicDistribution` argument |
| Non-uniform sensor rate | Swap in `NormalDistribution(mean, stdDev)` or `UniformDistribution(min, max)` from `org.fog.utils.distribution` |
| Heavier processing per tuple | The MI argument of the relevant `addAppEdge`, e.g. `3500` on the client to detector edge |
| Bigger payloads | The bytes argument of the same call, e.g. `20000` for a camera frame |
| Drop more data at the edge | The `FractionalSelectivity` in `addTupleMapping` |
| Simulated duration | `--sim-time 5000`, which sets `Config.MAX_SIMULATION_TIME` |

## Placement

| Change | Where |
|---|---|
| Pin a module to a named device | `mapping.addModuleToDevice("detector", "gw-0")` |
| Let the policy decide | Leave the module out of the mapping and use `ModulePlacementEdgewards` |
| Compare policies | `--mode cloud` against `--mode edge` |
| Write your own policy | Extend `ModulePlacement` in `org.fog.placement` and pass it to `controller.submitApplication` |

## Measurement

| Change | Where |
|---|---|
| Add a metric to the CSV | `Results.java`, one `row.put(...)` line |
| Measure a different path | Add an `AppLoop`; read the loop rules in [03-ifogsim-concepts.md](03-ifogsim-concepts.md) first |
| Keep runs in separate files | `--csv results\my-run.csv` |
| See iFogSim's own event trace | Delete the `Log.disable()` line near the top of `main` |

## Running iFogSim's bundled examples

`run.ps1` takes any fully qualified class name, so the published case studies
work without changes:

```powershell
.\run.ps1 org.fog.test.perfeval.VRGameFog
.\run.ps1 org.fog.test.perfeval.DCNSFog
.\run.ps1 org.fog.test.perfeval.TranslationServiceFog
```

Those print to the console and write nothing, since they predate `Results.java`
and do not call it.
