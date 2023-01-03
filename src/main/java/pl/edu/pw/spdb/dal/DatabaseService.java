package pl.edu.pw.spdb.dal;

import pl.edu.pw.spdb.model.Point;

public interface DatabaseService {
    Integer getNearestStartId(Point point);

}
