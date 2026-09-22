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
 * ECG wearables feeding a hospital dashboard. Two loops are measured
 * separately: the local arrhythmia alert that must beat a human reaction time,
 * and the long-term upload that nobody is waiting on.
 *
 *   ./run.sh Ex03Wearable --patients 6 --filter-ratio 0.2
 *
 * Raising --filter-ratio toward 1.0 turns the phone into a dumb relay and shows
 * up in the network_usage column.
 */
public class Ex03Wearable {

	public static void main(String[] args) throws Exception {
		int patients = FogLab.intArg(args, "patients", 6);
		double ecgInterval = FogLab.doubleArg(args, "ecg-interval", 2.0);
		double filterRatio = FogLab.doubleArg(args, "filter-ratio", 0.2);
		String csv = FogLab.arg(args, "csv", "results/ex03.csv");

		Log.disable();
		Config.MAX_SIMULATION_TIME = FogLab.intArg(args, "sim-time", 2000);
		CloudSim.init(1, Calendar.getInstance(), false);

		String appId = "wearable-ecg";
		FogBroker broker = new FogBroker("broker");
		Application app = createApplication(appId, broker.getId(), filterRatio);
		app.setUserId(broker.getId());

		List<FogDevice> devices = new ArrayList<FogDevice>();
		List<Sensor> sensors = new ArrayList<Sensor>();
		List<Actuator> actuators = new ArrayList<Actuator>();

		FogDevice cloud = FogLab.cloud();
		cloud.setParentId(-1);
		devices.add(cloud);

		FogDevice ward = FogLab.gateway("ward-router");
		ward.setParentId(cloud.getId());
		ward.setUplinkLatency(120); // hospital uplink to the vendor's cloud
		devices.add(ward);

		ModuleMapping mapping = ModuleMapping.createModuleMapping();
		mapping.addModuleToDevice("diagnosis", "cloud");

		for (int p = 0; p < patients; p++) {
			String name = "phone-" + p;
			FogDevice phone = FogLab.edgeNode(name);
			phone.setParentId(ward.getId());
			phone.setUplinkLatency(3); // Wi-Fi hop to the ward router
			devices.add(phone);

			// Every phone gets its own instance of the filter module.
			mapping.addModuleToDevice("filter", name);

			Sensor ecg = new Sensor("ecg-" + p, "ECG", broker.getId(), appId,
					new DeterministicDistribution(ecgInterval));
			ecg.setGatewayDeviceId(phone.getId());
			ecg.setLatency(1.0);
			sensors.add(ecg);

			Actuator buzzer = new Actuator("buzzer-" + p, broker.getId(), appId, "BUZZER");
			buzzer.setGatewayDeviceId(phone.getId());
			buzzer.setLatency(1.0);
			actuators.add(buzzer);
		}

		Controller controller = new Controller("controller", devices, sensors, actuators);
		controller.submitApplication(app, 0, new ModulePlacementMapping(devices, app, mapping));

		TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

		LinkedHashMap<String, String> labels = new LinkedHashMap<String, String>();
		labels.put("example", "Ex03Wearable");
		labels.put("patients", String.valueOf(patients));
		labels.put("ecg_interval_ms", String.valueOf(ecgInterval));
		labels.put("filter_ratio", String.valueOf(filterRatio));
		Results.collectOnExit(csv, app, devices, labels);

		System.out.println("Ex03Wearable: patients=" + patients + " filter ratio=" + filterRatio);
		CloudSim.startSimulation();
		CloudSim.stopSimulation();
	}

	private static Application createApplication(String appId, int userId, double filterRatio) {
		Application app = Application.createApplication(appId, userId);

		app.addAppModule("filter", 10);
		app.addAppModule("diagnosis", 10);

		app.addAppEdge("ECG", "filter", 500, 1000, "ECG", Tuple.UP, AppEdge.SENSOR);
		app.addAppEdge("filter", "BUZZER", 100, 50, "ALERT", Tuple.DOWN, AppEdge.ACTUATOR);
		app.addAppEdge("filter", "diagnosis", 2000, 800, "ECG_WINDOW", Tuple.UP, AppEdge.MODULE);
		app.addAppEdge("diagnosis", "filter", 200, 100, "THRESHOLD_UPDATE", Tuple.DOWN, AppEdge.MODULE);

		// One beat in twenty looks abnormal enough to buzz.
		app.addTupleMapping("filter", "ECG", "ALERT", new FractionalSelectivity(0.05));
		// filterRatio of the raw samples are worth sending upstream.
		app.addTupleMapping("filter", "ECG", "ECG_WINDOW", new FractionalSelectivity(filterRatio));
		app.addTupleMapping("diagnosis", "ECG_WINDOW", "THRESHOLD_UPDATE", new FractionalSelectivity(0.1));

		final AppLoop alertLoop = new AppLoop(new ArrayList<String>() {
			private static final long serialVersionUID = 1L;
			{
				add("ECG");
				add("filter");
				add("BUZZER");
			}
		});
		// Only the alert path is registered as a loop. A second loop through
		// diagnosis and back would read 1 ms, not the 240 ms the WAN costs:
		// iFogSim credits a loop the moment a tuple reaches the loop's LAST
		// module, and it deletes the emit time when it does. A loop that starts
		// and ends on the same module therefore closes on its own first hop, and
		// a second loop over the same sensor tuple finds the emit time already
		// gone. Keep one loop per sensor, ending somewhere it does not begin.
		List<AppLoop> loops = new ArrayList<AppLoop>();
		loops.add(alertLoop); // loop1_latency_ms in the CSV
		app.setLoops(loops);

		return app;
	}
}
