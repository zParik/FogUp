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
import org.fog.placement.ModulePlacementMapping;
import org.fog.utils.Config;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 * Smallest simulation that still has every moving part: one temperature sensor
 * on an edge node, one processing module on the gateway above it, one valve
 * actuator, and a cloud at the top that this application never touches.
 *
 * Run:  ./run.sh Ex01Hello        (or .\run.ps1 Ex01Hello on Windows)
 */
public class Ex01Hello {

	// Sensor fires every 5 ms of simulated time.
	private static final double SENSOR_INTERVAL = 5.0;

	public static void main(String[] args) throws Exception {
		Log.disable(); // iFogSim's own trace is noisy; drop this line to see it
		Config.MAX_SIMULATION_TIME = FogLab.intArg(args, "sim-time", 2000);

		// 1. CloudSim has to be initialised before any entity is constructed.
		CloudSim.init(1, Calendar.getInstance(), false);

		String appId = "hello-fog";
		FogBroker broker = new FogBroker("broker");

		// 2. The application: what runs, and how data flows between the parts.
		Application app = createApplication(appId, broker.getId());
		app.setUserId(broker.getId());

		// 3. The physical topology: cloud -> gateway -> edge node.
		List<FogDevice> devices = new ArrayList<FogDevice>();
		List<Sensor> sensors = new ArrayList<Sensor>();
		List<Actuator> actuators = new ArrayList<Actuator>();

		FogDevice cloud = FogLab.cloud();
		cloud.setParentId(-1);
		devices.add(cloud);

		FogDevice gateway = FogLab.gateway("gateway-1");
		gateway.setParentId(cloud.getId());
		gateway.setUplinkLatency(100); // 100 ms to reach the cloud over the WAN
		devices.add(gateway);

		FogDevice edge = FogLab.edgeNode("edge-1");
		edge.setParentId(gateway.getId());
		edge.setUplinkLatency(2); // 2 ms over the LAN to the gateway
		devices.add(edge);

		// 4. Sensor and actuator hang off the edge node, not off the gateway.
		Sensor thermometer = new Sensor("sensor-1", "TEMP", broker.getId(), appId,
				new DeterministicDistribution(SENSOR_INTERVAL));
		thermometer.setGatewayDeviceId(edge.getId());
		thermometer.setLatency(1.0);
		sensors.add(thermometer);

		Actuator valve = new Actuator("valve-1", broker.getId(), appId, "VALVE");
		valve.setGatewayDeviceId(edge.getId());
		valve.setLatency(1.0);
		actuators.add(valve);

		// 5. Placement: pin the one module to the gateway by name.
		ModuleMapping mapping = ModuleMapping.createModuleMapping();
		mapping.addModuleToDevice("watcher", "gateway-1");

		Controller controller = new Controller("controller", devices, sensors, actuators);
		controller.submitApplication(app, 0, new ModulePlacementMapping(devices, app, mapping));

		TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

		LinkedHashMap<String, String> labels = new LinkedHashMap<String, String>();
		labels.put("example", "Ex01Hello");
		labels.put("placement", "gateway");
		Results.collectOnExit(FogLab.arg(args, "csv", "results/ex01.csv"), app, devices, labels);

		// 6. Everything after this line is handled by the discrete event engine.
		CloudSim.startSimulation();
		CloudSim.stopSimulation();
	}

	private static Application createApplication(String appId, int userId) {
		Application app = Application.createApplication(appId, userId);

		// One module. The 10 is its RAM requirement in MB.
		app.addAppModule("watcher", 10);

		// Sensor -> module. 1000 = CPU length of the tuple (MI), 500 = size (bytes).
		app.addAppEdge("TEMP", "watcher", 1000, 500, "TEMP", Tuple.UP, AppEdge.SENSOR);
		// Module -> actuator.
		app.addAppEdge("watcher", "VALVE", 100, 50, "VALVE_CMD", Tuple.DOWN, AppEdge.ACTUATOR);

		// Every TEMP tuple that arrives produces exactly one VALVE_CMD tuple.
		app.addTupleMapping("watcher", "TEMP", "VALVE_CMD", new FractionalSelectivity(1.0));

		// The loop whose end-to-end latency gets measured.
		final AppLoop loop = new AppLoop(new ArrayList<String>() {
			private static final long serialVersionUID = 1L;
			{
				add("TEMP");
				add("watcher");
				add("VALVE");
			}
		});
		List<AppLoop> loops = new ArrayList<AppLoop>();
		loops.add(loop);
		app.setLoops(loops);

		return app;
	}
}
