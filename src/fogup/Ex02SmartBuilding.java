package fogup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.utils.Config;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 * Camera network in a building, run twice with the same topology and two
 * placements, so the pair of CSV rows shows what moving work to the edge buys.
 *
 *   ./run.sh Ex02SmartBuilding --mode cloud --gateways 2 --cameras 2
 *   ./run.sh Ex02SmartBuilding --mode edge  --gateways 2 --cameras 2
 *
 * mode=cloud   detection runs in the data centre, 100 ms of WAN each way
 * mode=edge    ModulePlacementEdgewards pushes detection as far down the tree
 *              as the devices have capacity for
 */
public class Ex02SmartBuilding {

	public static void main(String[] args) throws Exception {
		String mode = FogLab.arg(args, "mode", "edge");
		int gateways = FogLab.intArg(args, "gateways", 2);
		int cameras = FogLab.intArg(args, "cameras", 2);
		double frameInterval = FogLab.doubleArg(args, "frame-interval", 20.0);
		double wanLatency = FogLab.doubleArg(args, "wan-latency", 100.0);
		String csv = FogLab.arg(args, "csv", "results/ex02.csv");

		if (!mode.equals("cloud") && !mode.equals("edge"))
			throw new IllegalArgumentException("--mode must be cloud or edge, got: " + mode);

		Log.disable();
		Config.MAX_SIMULATION_TIME = FogLab.intArg(args, "sim-time", 2000);
		CloudSim.init(1, Calendar.getInstance(), false);

		String appId = "smart-building";
		FogBroker broker = new FogBroker("broker");
		Application app = createApplication(appId, broker.getId());
		app.setUserId(broker.getId());

		List<FogDevice> devices = new ArrayList<FogDevice>();
		List<Sensor> sensors = new ArrayList<Sensor>();
		List<Actuator> actuators = new ArrayList<Actuator>();
		List<String> cameraNodes = new ArrayList<String>();

		FogDevice cloud = FogLab.cloud();
		cloud.setParentId(-1);
		devices.add(cloud);

		FogDevice proxy = FogLab.proxy();
		proxy.setParentId(cloud.getId());
		proxy.setUplinkLatency(wanLatency);
		devices.add(proxy);

		for (int g = 0; g < gateways; g++) {
			FogDevice gw = FogLab.gateway("gw-" + g);
			gw.setParentId(proxy.getId());
			gw.setUplinkLatency(4); // 4 ms from the floor gateway to the proxy
			devices.add(gw);

			for (int c = 0; c < cameras; c++) {
				String name = "cam-" + g + "-" + c;
				FogDevice node = FogLab.edgeNode(name);
				node.setParentId(gw.getId());
				node.setUplinkLatency(2);
				devices.add(node);
				cameraNodes.add(name);

				Sensor camera = new Sensor("sensor-" + name, "CAMERA", broker.getId(), appId,
						new DeterministicDistribution(frameInterval));
				camera.setGatewayDeviceId(node.getId());
				camera.setLatency(1.0);
				sensors.add(camera);

				Actuator alarm = new Actuator("alarm-" + name, broker.getId(), appId, "ALARM");
				alarm.setGatewayDeviceId(node.getId());
				alarm.setLatency(1.0);
				actuators.add(alarm);
			}
		}

		ModuleMapping mapping = ModuleMapping.createModuleMapping();
		// The building-wide roll-up always lives in the cloud: it needs data from
		// every gateway, so no single edge node is the right home for it.
		mapping.addModuleToDevice("coordinator", "cloud");
		for (String name : cameraNodes)
			mapping.addModuleToDevice("client", name);

		if (mode.equals("cloud"))
			mapping.addModuleToDevice("detector", "cloud");

		Controller controller = new Controller("controller", devices, sensors, actuators);
		controller.submitApplication(app, 0,
				mode.equals("cloud")
						? new ModulePlacementMapping(devices, app, mapping)
						: new ModulePlacementEdgewards(devices, sensors, actuators, app, mapping));

		TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

		LinkedHashMap<String, String> labels = new LinkedHashMap<String, String>();
		labels.put("example", "Ex02SmartBuilding");
		labels.put("mode", mode);
		labels.put("gateways", String.valueOf(gateways));
		labels.put("cameras_per_gateway", String.valueOf(cameras));
		labels.put("edge_nodes", String.valueOf(cameraNodes.size()));
		labels.put("frame_interval_ms", String.valueOf(frameInterval));
		labels.put("wan_latency_ms", String.valueOf(wanLatency));
		Results.collectOnExit(csv, app, devices, labels);

		System.out.println("Ex02SmartBuilding: mode=" + mode + " edge nodes=" + cameraNodes.size());
		CloudSim.startSimulation();
		CloudSim.stopSimulation();
	}

	private static Application createApplication(String appId, int userId) {
		Application app = Application.createApplication(appId, userId);

		app.addAppModule("client", 10);      // frame grab and alarm driver, per camera
		app.addAppModule("detector", 10);    // the expensive part: motion detection
		app.addAppModule("coordinator", 10); // building-wide state, one instance

		app.addAppEdge("CAMERA", "client", 1000, 20000, "CAMERA", Tuple.UP, AppEdge.SENSOR);
		app.addAppEdge("client", "detector", 3500, 2000, "FRAME", Tuple.UP, AppEdge.MODULE);
		app.addAppEdge("detector", "client", 500, 100, "DETECTION", Tuple.DOWN, AppEdge.MODULE);
		// Periodic edge: one summary per 1000 ms regardless of frame rate.
		app.addAppEdge("detector", "coordinator", 100, 200, 1000, "BUILDING_STATE", Tuple.UP, AppEdge.MODULE);
		app.addAppEdge("client", "ALARM", 100, 50, "ALARM_CMD", Tuple.DOWN, AppEdge.ACTUATOR);

		// Selectivity 0.9: the client drops one frame in ten as a duplicate.
		app.addTupleMapping("client", "CAMERA", "FRAME", new FractionalSelectivity(0.9));
		app.addTupleMapping("detector", "FRAME", "DETECTION", new FractionalSelectivity(1.0));
		app.addTupleMapping("client", "DETECTION", "ALARM_CMD", new FractionalSelectivity(1.0));

		final AppLoop alarmLoop = new AppLoop(new ArrayList<String>() {
			private static final long serialVersionUID = 1L;
			{
				add("CAMERA");
				add("client");
				add("detector");
				add("client");
				add("ALARM");
			}
		});
		List<AppLoop> loops = new ArrayList<AppLoop>();
		loops.add(alarmLoop);
		app.setLoops(loops);

		return app;
	}
}
