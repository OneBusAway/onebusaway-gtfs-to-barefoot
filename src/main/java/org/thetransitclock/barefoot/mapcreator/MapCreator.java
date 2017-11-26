package org.thetransitclock.barefoot.mapcreator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;

import org.apache.commons.cli.Options;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmwcarit.barefoot.road.BaseRoad;
import com.bmwcarit.barefoot.road.BfmapWriter;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Line;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;

/**
 * @author Sean Óg Crudden This is an command line application to create a barefoot map file
 *         from a GTFS file.
 *
 */
public class MapCreator {

	// Logging important in this class
	private static final Logger logger = LoggerFactory.getLogger(MapCreator.class);

	private static String gtfsFilePath = null;
	private static String mapFilePath = null;

	/**
	 * This is run like this.
	 * 
	 * java -jar thetransitclock-barefoot-mapcreator.jar -gtfs filename -barefootmap
	 * mapfilename
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CommandLine commandLine = processCommandLineOptions(args);
		gtfsFilePath = commandLine.getOptionValue("gtfs");
		mapFilePath = commandLine.getOptionValue("barefootmap");

		GtfsReader reader = new GtfsReader();
		String agencyId = null;
		reader.setDefaultAgencyId(agencyId);
		boolean internStrings = false;
		reader.setInternStrings(internStrings);

		HashMap<Integer, List<ShapePoint>> byId = new HashMap<Integer, List<ShapePoint>>();

		try {
			reader.setInputLocation(new File(gtfsFilePath));
			GtfsRelationalDaoImpl entityStore = new GtfsRelationalDaoImpl();
			entityStore.setGenerateIds(true);
			reader.setEntityStore(entityStore);

			reader.run();
			Collection<ShapePoint> shapes = entityStore.getAllShapePoints();

			for (ShapePoint shape : shapes) {
				List<ShapePoint> byIdShapes = byId.get(shape.getId());
				if (byIdShapes == null) {
					byIdShapes = new ArrayList<ShapePoint>();
					byId.put(shape.getId(), byIdShapes);
				}
				byIdShapes.add(shape);
			}

			List<BaseRoad> map = new LinkedList<BaseRoad>();

			Set<Integer> keys = byId.keySet();
			long segmentCounter = 0;
			for (Integer key : keys) {
				Collections.sort(byId.get(key));
				Polyline polyLine = new Polyline();

				Point startPoint = null;
				Point endPoint = null;
				for (ShapePoint shape : byId.get(key)) {
					Line segment = new Line();

					if (endPoint != null)
						startPoint = endPoint;
					else {
						startPoint = new Point();
						startPoint.setXY(shape.getLon(), shape.getLat());
					}
					endPoint = new Point();
					endPoint.setXY(shape.getLon(), shape.getLat());

					segment.setStart(startPoint);
					segment.setEnd(endPoint);

					boolean bStartNewPath = false;
					polyLine.addSegment(segment, bStartNewPath);
				}
				BaseRoad road = new BaseRoad(new Long(key), segmentCounter, segmentCounter++, new Long(key), true,
						(short) 1, 1F, 60F, 60F, 100F, (Polyline) polyLine);
				map.add(road);
			}
			BfmapWriter writer = new BfmapWriter(mapFilePath);
			writer.open();

			for (BaseRoad road : map) {
				logger.debug("Road id=" + road.id() + " : " + GeometryEngine.geometryToGeoJson(road.geometry()));
				writer.write(road);
			}
			writer.close();

		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	private static CommandLine processCommandLineOptions(String[] args) {
		// Specify the options
		Options options = new Options();

		options.addOption("h", false, "Display usage and help info.");

		options.addOption("gtfs", true, "Location of the gtfs file.");

		options.addOption("barefootmap", true, "Location to put barefoot map file.");

		// Parse the options
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (Exception e) {
			// There was a parse problem so log the problem,
			// display the command line options so user knows
			// what is needed, and exit since can't continue.
			logger.error(e.getMessage());
			System.err.println(e.getMessage());
			displayCommandLineOptions(options);
			System.exit(0);
		}

		// Handle help option
		if (cmd.hasOption("h")||!cmd.hasOption("gtfs")||!cmd.hasOption("barefootmap")) {
			displayCommandLineOptions(options);
			System.exit(0);
		}

		// Return the CommandLine so that arguments can be accessed
		return cmd;
	}

	/**
	 * Displays the command line options on stdout
	 * 
	 * @param options
	 */
	private static void displayCommandLineOptions(Options options) {

		final String commandLineSyntax = "java -jar thetransitclock-barefoot-mapcreator.jar";
		final PrintWriter writer = new PrintWriter(System.out);
		final HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.printHelp(writer, 80, // printedRowWidth
				commandLineSyntax, "args:", // header
				options, 2, // spacesBeforeOption
				2, // spacesBeforeOptionDescription
				null, // footer
				true); // displayUsage
		writer.close();
	}
}
