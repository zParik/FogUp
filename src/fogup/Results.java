package fogup;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.utils.Config;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;

/**
 * Collects the numbers iFogSim leaves scattered across static singletons and
 * appends one row per run to a CSV.
 *
 * iFogSim's Controller calls System.exit(0) as soon as the simulation stops, so
 * nothing written after CloudSim.startSimulation() in main() ever runs. A JVM
 * shutdown hook does run, which is why collection is registered up front and
 * fires on the way out.
 *
 * The per-loop sample count comes from TimeKeeper.getLoopIdToCurrentNum().
 * getTupleTypeToExecutedTupleCount() looks like the same thing but is stuck at
 * 1: iFogSim writes that map on the first tuple of a type and never increments
 * it (TimeKeeper.tupleEndedExecution, the else branch updates the average only).
 */
public class Results {

	/**
	 * Registers the CSV dump. Call once, before CloudSim.startSimulation().
	 *
	 * @param csvPath where to append (header written when the file is new)
	 * @param labels  run identifiers, e.g. mode=edge, devices=4
	 */
	public static void collectOnExit(final String csvPath, final Application app,
			final List<FogDevice> devices, final Map<String, String> labels) {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					write(csvPath, app, devices, labels);
				} catch (IOException e) {
					System.err.println("could not write " + csvPath + ": " + e.getMessage());
				}
			}
		});
	}

	private static void write(String csvPath, Application app, List<FogDevice> devices,
			Map<String, String> labels) throws IOException {

		Map<String, String> row = new LinkedHashMap<String, String>(labels);

		TimeKeeper tk = TimeKeeper.getInstance();
		List<AppLoop> loops = app.getLoops();
		double worstLoop = 0.0;
		for (int i = 0; i < loops.size(); i++) {
			AppLoop loop = loops.get(i);
			Double avg = tk.getLoopIdToCurrentAverage().get(loop.getLoopId());
			Integer samples = tk.getLoopIdToCurrentNum().get(loop.getLoopId());
			double ms = (avg == null) ? Double.NaN : avg;
			row.put("loop" + (i + 1) + "_latency_ms", fmt(ms));
			row.put("loop" + (i + 1) + "_samples", samples == null ? "0" : samples.toString());
			if (!Double.isNaN(ms) && ms > worstLoop)
				worstLoop = ms;
		}
		row.put("worst_loop_ms", fmt(worstLoop));

		double cloudEnergy = 0.0, edgeEnergy = 0.0, totalEnergy = 0.0;
		double cloudCost = 0.0;
		for (FogDevice d : devices) {
			double e = d.getEnergyConsumption();
			totalEnergy += e;
			if (d.getName().equals("cloud")) {
				cloudEnergy += e;
				cloudCost = d.getTotalCost();
			} else {
				edgeEnergy += e;
			}
		}
		row.put("energy_cloud_j", fmt(cloudEnergy));
		row.put("energy_fog_j", fmt(edgeEnergy));
		row.put("energy_total_j", fmt(totalEnergy));
		row.put("cloud_cost", fmt(cloudCost));
		row.put("network_usage", fmt(NetworkUsageMonitor.getNetworkUsage() / Config.MAX_SIMULATION_TIME));

		File file = new File(csvPath);
		File parent = file.getParentFile();
		if (parent != null)
			parent.mkdirs();
		boolean fresh = !file.exists() || file.length() == 0;

		PrintWriter out = new PrintWriter(new FileWriter(file, true));
		try {
			if (fresh)
				out.println(join(new ArrayList<String>(row.keySet())));
			out.println(join(new ArrayList<String>(row.values())));
		} finally {
			out.close();
		}

		System.out.println();
		System.out.println("FogUp results appended to " + file.getPath());
		for (Map.Entry<String, String> e : row.entrySet())
			System.out.println("  " + e.getKey() + " = " + e.getValue());
	}

	private static String fmt(double v) {
		if (Double.isNaN(v))
			return "";
		return String.format("%.3f", v);
	}

	private static String join(List<String> cells) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cells.size(); i++) {
			if (i > 0)
				sb.append(',');
			sb.append(cells.get(i));
		}
		return sb.toString();
	}
}
