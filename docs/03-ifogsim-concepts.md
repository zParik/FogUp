# What iFogSim models

iFogSim is a discrete event simulator built on CloudSim 3. Nothing runs in real
time and no packets exist. There is an event queue, a clock in milliseconds,
and entities that schedule events at each other.

## Five nouns

**FogDevice** is every piece of hardware. The cloud, an ISP proxy, a building
gateway and a phone are all `FogDevice` objects; what separates them is MIPS,
RAM, uplink bandwidth, uplink latency to the parent, a hierarchy level, a cost
rate, and busy and idle power in watts. `FogLab.java` has four named
constructors (`cloud`, `proxy`, `gateway`, `edgeNode`) so the examples read as
a topology rather than as CloudSim boilerplate.

Devices form a tree through `setParentId`. The latency you set with
`setUplinkLatency` applies to that one link, and a tuple pays every link it
crosses.

**Sensor** emits tuples of one type on a schedule.
`new DeterministicDistribution(5.0)` fires every 5 ms of simulated time;
`NormalDistribution` and `UniformDistribution` are also in
`org.fog.utils.distribution`. A sensor attaches to a device with
`setGatewayDeviceId` and has its own link latency.

**Actuator** receives tuples and does nothing with them except record when they
arrived. That arrival is what ends a measured loop.

**AppModule** is a unit of computation that can be placed on a device. Modules
have a RAM requirement and are the things a placement policy moves around.

**Tuple** is one message. It carries a CPU length in millions of instructions
and a size in bytes, and both of those come from the application edge it was
created by, not from the module that sent it.

## The application is a graph, not a program

`Application.createApplication` returns an empty directed graph. You add
modules as vertices and `AppEdge`s as edges:

```java
app.addAppEdge("CAMERA", "client", 1000, 20000, "CAMERA", Tuple.UP, AppEdge.SENSOR);
//              from      to        MI    bytes   tupleType  direction  kind
```

`AppEdge.SENSOR`, `AppEdge.MODULE` and `AppEdge.ACTUATOR` say what sits at the
ends. `Tuple.UP` means toward the cloud, `Tuple.DOWN` means toward the sensors.
An edge with an extra period argument is periodic: it fires on a timer instead
of in response to an incoming tuple.

Edges alone do not make a module emit anything. That is what tuple mappings are
for:

```java
app.addTupleMapping("client", "CAMERA", "FRAME", new FractionalSelectivity(0.9));
```

Nine FRAME tuples leave the client for every ten CAMERA tuples that arrive.
Selectivity is the whole data-reduction story of edge computing in one number,
and `Ex03Wearable` exposes it as the `--filter-ratio` flag.

## Loops are the metric, and they have a sharp edge

An `AppLoop` is a list of names that a tuple travels through, and iFogSim
reports the average end-to-end delay for each one:

```java
new AppLoop(Arrays.asList("CAMERA", "client", "detector", "client", "ALARM"))
```

The measurement works like this: when a tuple arrives at the loop's **last**
name, iFogSim subtracts the time the originating sensor tuple was emitted, then
deletes that emit time. Two consequences follow, and both bite:

1. A loop that ends on the same module it passes through early closes on the
   early visit. `[ECG, filter, diagnosis, filter]` ends at `filter`, and the
   sensor tuple reaches `filter` on its first hop, so the loop reports about
   1 ms instead of the 240 ms the cloud round trip costs.
2. Because the emit time is deleted, two loops fed by the same sensor compete.
   Whichever closes first consumes the record, and the other reports zero
   samples.

Keep one measured loop per sensor and end it somewhere it does not begin.
`Ex03Wearable` has a comment at the exact line where this was hit.

## Placement is the thing you are usually comparing

`ModuleMapping` is a set of pins: this module, that device, by name.

- `ModulePlacementMapping` places exactly what the mapping says and nothing else.
- `ModulePlacementEdgewards` starts at each sensor and walks up the tree,
  placing each module on the first device with enough spare MIPS and RAM, so
  work lands as low as capacity allows.

`Ex02SmartBuilding` swaps between the two on one line and pins only the modules
that genuinely belong somewhere fixed: the per-camera client on its camera, the
building-wide coordinator in the cloud.

## What you get at the end

The `Controller` prints application loop delays, average CPU time per tuple
type, energy per device, cloud cost and total network usage, then calls
`System.exit(0)`. That exit is why `Results.java` registers a JVM shutdown hook
rather than reading the numbers after `CloudSim.startSimulation()` returns, and
why sweeping a parameter means launching one process per point.

Two numbers the Controller prints deserve a caveat. Total network usage is
divided by `Config.MAX_SIMULATION_TIME`, so it is comparable only between runs
of the same simulated length. And `getTupleTypeToExecutedTupleCount()` is stuck
at 1 for every type, because `TimeKeeper.tupleEndedExecution` sets the count on
the first tuple and never increments it; use the per-loop sample count instead,
which is what the `loop1_samples` column in the CSVs is.
