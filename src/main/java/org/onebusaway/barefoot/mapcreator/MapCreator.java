/**
 * Copyright (C) 2017 Sean Óg Crudden <og.crudden@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.barefoot.mapcreator;

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

import com.bmwcarit.barefoot.road.BaseRoad;
import com.bmwcarit.barefoot.road.BfmapWriter;

import com.esri.core.geometry.Line;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;

/**
 * @author Sean Óg Crudden This is a command line application to create a
 *         barefoot map file from a GTFS file.
 *
 */
public class MapCreator {

	/**
	 * This is run like this.
	 * 
	 * java -jar onebusaway-barefoot-mapcreator.jar -gtfs filename -barefootmap
	 * mapfilename
	 */
	public static void main(String[] args) {
		CommandLine commandLine = processCommandLineOptions(args);

		if (commandLine != null) {
			String gtfsFilePath = commandLine.getOptionValue("gtfs");
			String mapFilePath = commandLine.getOptionValue("barefootmap");

			GtfsReader reader = new GtfsReader();

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

					Collections.sort(byId.get(key), new ShapePointComparator());
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
							(short) 1, 1F, 60F, 60F, 100F, polyLine);
					map.add(road);
				}
				BfmapWriter writer = new BfmapWriter(mapFilePath);
				writer.open();

				for (BaseRoad road : map) {
					writer.write(road);
				}
				writer.close();

			} catch (IOException e) {

				e.printStackTrace();
			}
		}
	}

	private static CommandLine processCommandLineOptions(String[] args) {

		Options options = new Options();

		options.addOption("h", false, "Display usage and help info.");

		options.addOption("gtfs", true, "Location of the gtfs file.");

		options.addOption("barefootmap", true, "Location to put barefoot map file.");

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			displayCommandLineOptions(options);
			return null;
		}

		if (cmd.hasOption("h") || !cmd.hasOption("gtfs") || !cmd.hasOption("barefootmap")) {
			displayCommandLineOptions(options);
			return null;
		}
		return cmd;
	}

	/**
	 * Displays the command line options
	 * 
	 * @param options
	 */
	private static void displayCommandLineOptions(Options options) {

		final String commandLineSyntax = "java -jar onebusaway-barefoot-gtfs-mapcreator.jar";
		final PrintWriter writer = new PrintWriter(System.out);
		final HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.printHelp(writer, 80, commandLineSyntax, "args:", options, 2, 2, null, true);
		writer.close();
	}
}
