package org.thetransitclock.barefoot.mapcreator;

import java.util.Comparator;

import org.onebusaway.gtfs.model.ShapePoint;

class ShapePointComparator implements Comparator<ShapePoint> {

	public int compare(ShapePoint arg0, ShapePoint arg1) {
		return arg0.getId().compareTo(arg1.getId());
	}

}
