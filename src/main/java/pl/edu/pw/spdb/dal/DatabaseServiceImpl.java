package pl.edu.pw.spdb.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.edu.pw.spdb.model.Point;
import pl.edu.pw.spdb.model.Route;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.postgis.*;

import pl.edu.pw.spdb.model.RouteSegment;

@Service
@Slf4j
public class DatabaseServiceImpl implements DatabaseService {

    @Value("${db.url}")
    private String DB_URL;

    @Value("${db.username}")
    private String DB_USERNAME;

    @Value("${db.password}")
    private String DB_PASSWORD;

    private static final String NEAREST_START_ID_SQL = "SELECT source FROM ways " +
            "order by st_distance(st_makepoint(?,?), st_makepoint(y1,x1)) limit 1;";

    private static final String NEAREST_END_ID_SQL = "SELECT source FROM ways " +
            "order by st_distance(st_makepoint(?,?), st_makepoint(y2,x2)) limit 1;";

    private static final String FIND_ROUTE_SQL =
            "SELECT w.gid, w.the_geom, w.source, w.target, w.length, w.maxspeed_forward, " +
                    "w.maxspeed_backward, w.x1, w.y1, w.x2, w.y2 " +
                    "FROM astar(?, ?, ?, ?, 0) res join ways w on res.edge=w.gid;";

    private Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        connection.setAutoCommit(true);
        return connection;
    }


    @Override
    public Integer getStartOrEnd(Point point, boolean isStartPoint) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection
                    .prepareStatement(isStartPoint ? NEAREST_START_ID_SQL : NEAREST_END_ID_SQL);
            statement.setDouble(1, point.latitude());
            statement.setDouble(2, point.longitude());
            log.info(statement.toString());

            ResultSet result = statement.executeQuery();
            if (result.next()) {
                int id = result.getInt("source");
                log.info("Received id: " + id);
                return id;
            }

            return -1;

        } catch (SQLException e) {
            log.error("SQLException has occurred with message: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Route findRoute(Integer startId, Integer endId, Integer maxSpeed, float distanceWeight) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = getStatement(startId, endId, maxSpeed, distanceWeight, connection);

            log.info(statement.toString());

            ResultSet result = statement.executeQuery();

            return parseQueryResult(result);

        } catch (SQLException e) {
            log.error("SQLException has occurred with message: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static PreparedStatement getStatement(Integer startId, Integer endId, Integer maxSpeed, float distanceWeight, Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(FIND_ROUTE_SQL);
        statement.setInt(1, startId);
        statement.setInt(2, endId);
        statement.setInt(3, maxSpeed);
        statement.setFloat(4, distanceWeight);
        return statement;
    }

    private Route parseQueryResult(ResultSet result) throws SQLException {
        List<RouteSegment> segments = new ArrayList<>();
        while (result.next()) {
            segments.add(parseRecord(result));
        }
        return new Route(segments, 0, 0);
    }

    private static RouteSegment parseRecord(ResultSet result) throws SQLException {
        long id = result.getLong(1);
        Object geom = result.getObject(2);
        long source = result.getLong(3);
        long target = result.getLong(4);
        double length = result.getDouble(5);
        double maxSpeedForward = result.getDouble(6);
        double x1 = result.getDouble(8);
        double y1 = result.getDouble(9);
        double x2 = result.getDouble(10);
        double y2 = result.getDouble(11);

        return new RouteSegment(id, geom, source, target, length, maxSpeedForward, x1, y1, x2, y2);
    }

}
