package fogup;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;

/**
 * Shared helpers for the FogUp examples.
 *
 * iFogSim has no factory for a fog device: every published example pastes the
 * same 40 lines of CloudSim host construction. That block lives here once so the
 * examples stay readable.
 */
public class FogLab {

	/**
	 * Builds one fog device (cloud, gateway, router or edge node; iFogSim models
	 * them all with the same class and tells them apart by MIPS, level and
	 * uplink latency).
	 *
	 * @param name        unique name, also used in module mappings
	 * @param mips        processing capacity of the single core
	 * @param ram         RAM in MB
	 * @param upBw        uplink bandwidth in bytes/ms toward the parent
	 * @param downBw      downlink bandwidth in bytes/ms toward the children
	 * @param level       0 = cloud, larger numbers = closer to the sensors
	 * @param ratePerMips currency charged per MIPS-second (0 for anything you own)
	 * @param busyPower   watts at 100% CPU
	 * @param idlePower   watts at 0% CPU
	 */
	public static FogDevice device(String name, long mips, int ram, long upBw, long downBw,
			int level, double ratePerMips, double busyPower, double idlePower) {

		List<Pe> peList = new ArrayList<Pe>();
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000;
		int bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower));

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				"x86", "Linux", "Xen", host, 10.0, 3.0, 0.05, 0.001, 0.0);

		FogDevice fogDevice = null;
		try {
			fogDevice = new FogDevice(name, characteristics,
					new AppModuleAllocationPolicy(hostList),
					new LinkedList<Storage>(), 10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			throw new RuntimeException("could not create fog device " + name, e);
		}
		fogDevice.setLevel(level);
		return fogDevice;
	}

	/** Cloud data centre: huge MIPS, bills per MIPS, sits at level 0. */
	public static FogDevice cloud() {
		return device("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16 * 103, 16 * 83.25);
	}

	/** ISP-level proxy between the gateways and the cloud. */
	public static FogDevice proxy() {
		return device("proxy-server", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
	}

	/** Building or cell-tower gateway: a small server room box. */
	public static FogDevice gateway(String name) {
		return device(name, 2800, 4000, 10000, 10000, 2, 0.0, 107.339, 83.4333);
	}

	/** Edge device: phone, Raspberry Pi, camera with an SoC on board. */
	public static FogDevice edgeNode(String name) {
		return device(name, 1000, 1000, 10000, 270, 3, 0.0, 87.53, 82.44);
	}

	/** Reads --flag value pairs out of args; returns fallback when absent. */
	public static String arg(String[] args, String flag, String fallback) {
		for (int i = 0; i < args.length - 1; i++) {
			if (args[i].equals("--" + flag))
				return args[i + 1];
		}
		return fallback;
	}

	public static int intArg(String[] args, String flag, int fallback) {
		return Integer.parseInt(arg(args, flag, String.valueOf(fallback)));
	}

	public static double doubleArg(String[] args, String flag, double fallback) {
		return Double.parseDouble(arg(args, flag, String.valueOf(fallback)));
	}
}
