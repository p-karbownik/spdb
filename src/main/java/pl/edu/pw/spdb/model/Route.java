package pl.edu.pw.spdb.model;

import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.CoordinateLine;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Getter
@Setter
public class Route {

    private List<RouteSegment> segments;
    private double distance;
    private double estimatedTime;
    private CoordinateLine coordinateLine;

    public Route(List<RouteSegment> segments, double distance, double estimatedTime) {
        this.segments = segments;
        this.distance = distance;
        this.estimatedTime = estimatedTime;
    }

    public CoordinateLine getCoordinateLine() {
        if (coordinateLine == null) {
            List<Coordinate> coordinates = new ArrayList<>();

            Iterator<RouteSegment> listIterator = segments.iterator();

            boolean isFirst = true;

            while (listIterator.hasNext()) {
                RouteSegment routeSegment = listIterator.next();

                if (isFirst) {
                    coordinates.add(new Coordinate(routeSegment.y1(), routeSegment.x1()));
                    isFirst = false;
                }
                coordinates.add(new Coordinate(routeSegment.y2(), routeSegment.x2()));
            }
            coordinateLine = new CoordinateLine(coordinates);
        }
        return coordinateLine;
    }
}
