package pl.edu.pw.spdb.dal;

import pl.edu.pw.spdb.model.Point;
import pl.edu.pw.spdb.model.Route;

public interface DatabaseService {
    Integer getStartOrEnd(Point point, boolean isStartPoint);
    Route findRoute(Integer startId, Integer endId, Integer maxSpeed, float distanceWeight);
}
