package pl.edu.pw.spdb.dal;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.edu.pw.spdb.model.Point;
import pl.edu.pw.spdb.model.Route;
import pl.edu.pw.spdb.model.RouteSegment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class DatabaseServiceImpl implements DatabaseService {

    private static final String NEAREST_START_ID_SQL = "SELECT source FROM ways " +
            "order by st_distance(st_makepoint(?,?), st_makepoint(y1,x1)) limit 1;";
    private static final String NEAREST_END_ID_SQL = "SELECT source FROM ways " +
            "order by st_distance(st_makepoint(?,?), st_makepoint(y2,x2)) limit 1;";
    private static final String FIND_ROUTE_SQL =
            "SELECT w.gid, w.the_geom, w.source, w.target, w.length_m, w.maxspeed_forward, " +
                    "w.maxspeed_backward, w.x1, w.y1, w.x2, w.y2 " +
                    "FROM astar(?, ?, ?, ?, 0) res join ways w on res.edge=w.gid;";
    @Value("${db.url}")
    private String DB_URL;
    @Value("${db.username}")
    private String DB_USERNAME;
    @Value("${db.password}")
    private String DB_PASSWORD;

    /***
     * Funkcja która przygotowuje zapytanie pod wyszukanie nakrótszej trasy - zamienia znaki zapytania z FIND_ROUTE_SQL
     * na odpowiednie wartości podane przy jej wywołaniu
     * @param startId - id punktu startu
     * @param endId - id punktu końcowego
     * @param maxSpeed - maksymalna prędkość pojazdu
     * @param distanceWeight - waga wpływająca na liczenie kosztu wyszukania
     * @param connection - połączenie z baż
     * @return PreparedStatement
     */
    private static PreparedStatement getStatement(long startId, long endId, Integer maxSpeed, float distanceWeight, Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(FIND_ROUTE_SQL);
        statement.setLong(1, startId);
        statement.setLong(2, endId);
        statement.setInt(3, maxSpeed);
        statement.setFloat(4, distanceWeight);
        return statement;
    }

    /***
     * Funkcja parsuje jeden rekord z ResultSet otrzymanego po wykonaniu zapytania FIND_ROTE_SQL na RouteSegment -
     * wyciąga porzebne informacje oraz oblicza długość segmentu w km
     * @param result - rekord otrzymany z zaytania
     * @return RouteSegment
     */
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

        return new RouteSegment(id, geom, source, target, length / 1000, maxSpeedForward, x1, y1, x2, y2);
    }

    /***
     * Funkcja zwraca połączenie z bazą danych
     * @return java.sql.Connection
     */
    private Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        connection.setAutoCommit(true);
        return connection;
    }

    /***
     * Funkcja wyszukująca początek lub koniec trasy na podstawie punktu, parametr isStartPoin pozwala nam określić
     * którego zapytania powinniśmy użyć - NEAREST_START_ID_SQL czy NEAREST_END_ID_SQL
     * @param point - punkt wbrany w GUI po którym  wyszukujemy najbliższego początku trasy
     * @param isStartPoint - parametr pomagający stwierdzić którego zapytania należy użyć
     * @return id punktu w bazie
     */
    @Override
    public Long getStartOrEnd(Point point, boolean isStartPoint) throws RuntimeException {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection
                    .prepareStatement(isStartPoint ? NEAREST_START_ID_SQL : NEAREST_END_ID_SQL);
            statement.setDouble(1, point.latitude());
            statement.setDouble(2, point.longitude());
            log.info(statement.toString());

            ResultSet result = executeStatement(statement);

            if (result.next()) {
                long id = result.getInt("source");
                log.info("Received id: " + id);
                return id;
            }

            return (long) -1;

        } catch (SQLException e) {
            log.error("SQLException has occurred with message: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /***
     * Funkcja wyszukująca najlepszą trasę dla zadanych parametrów przy użyciu zdefiniowanego wsześniej zapytania FIND_ROUTE_SQL
     * @param startId - id punktu początkowego
     * @param endId - id punktu końcowego
     * @param maxSpeed - maksymalna prędkość pojazdu
     * @param distanceWeight - waga wpływająca na liczenie kosztu wyszukania
     * @return Route - znaleziona ścieżka
     */
    @Override
    public Route findRoute(long startId, long endId, Integer maxSpeed, float distanceWeight) throws RuntimeException {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = getStatement(startId, endId, maxSpeed, distanceWeight, connection);

            log.info(statement.toString());

            ResultSet result = executeStatement(statement);

            return parseQueryResult(result, maxSpeed);

        } catch (SQLException e) {
            log.error("SQLException has occurred with message: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /***
     * Funkcja wykonująca przygotowane zapytanie - jej zadaniem jest także zmierzenie czasu potrzebnego do wykonania
     * okreslonego zapytania
     * @param statement - przygotowane zapytanie
     * @return ResultSet - wynik wykonania zapytania SQL
     */
    private ResultSet executeStatement(@NotNull PreparedStatement statement) throws SQLException {
        long start = System.currentTimeMillis();
        ResultSet result = statement.executeQuery();
        long end = System.currentTimeMillis();
        long time = end - start;
        log.info("Query executed in " + time / 1000 + " seconds");

        return result;
    }

    /***
     * Funkcja której zadaniem jest przeparsować otrzymane wyniki z wykonania zapytania FIND_ROUTE_SQL
     * @param result - wynik otrzymany z bazy
     * @param maxSpeed - maksymalna prędkość pojazdu, potrzebna do wyliczenia czasu potrzebnego na pokonanie odcinka drogi
     * @return Route - klasa zawierająca informacje o znalezionej trasie
     */
    private Route parseQueryResult(ResultSet result, int maxSpeed) throws SQLException {
        List<RouteSegment> segments = new ArrayList<>();

        float distanceSum = 0;
        float timeSum = 0;
        while (result.next()) {
            RouteSegment seg = parseRecord(result);
            distanceSum += seg.length();
            timeSum += (seg.length() / (Math.min(seg.maxSpeedForward(), maxSpeed)));
            segments.add(seg);
        }
        log.info("Segments number: " + segments.size());
        return new Route(segments, distanceSum, timeSum);
    }
}
