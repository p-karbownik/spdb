package pl.edu.pw.spdb.model;

import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.CoordinateLine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public record Route(List<RouteSegment> segments, double distance, double estimatedTime) {
    public CoordinateLine getCoordinateLine() {
        List<Coordinate> coordinates = new ArrayList<>();

        Iterator<RouteSegment> listIterator = segments.iterator();

        boolean isFirst = true;

        while (listIterator.hasNext()) {
            RouteSegment routeSegment = listIterator.next();

            if(isFirst) {
                coordinates.add(new Coordinate(routeSegment.x1(), routeSegment.y1()));
                isFirst = false;
            }
            coordinates.add(new Coordinate(routeSegment.x2(), routeSegment.y2()));
        }

        return new CoordinateLine(coordinates);
    }
}
