package pl.edu.pw.spdb.dal;

import pl.edu.pw.spdb.model.Point;
import pl.edu.pw.spdb.model.Route;

public interface DatabaseService {
    Long getStartOrEnd(Point point, boolean isStartPoint);
    Route findRoute(long startId, long endId, Integer maxSpeed, float distanceWeight);
}
