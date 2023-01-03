package pl.edu.pw.spdb.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.edu.pw.spdb.model.Point;

import java.sql.*;

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

    private Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        connection.setAutoCommit(true);
        return connection;
    }


    @Override
    public Integer getNearestStartId(Point point) {
        try(Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement(NEAREST_START_ID_SQL);
            statement.setDouble(1, point.latitude());
            statement.setDouble(2, point.longitude());
            log.info(statement.toString());

            ResultSet result = statement.executeQuery();
            if(result.next()) {
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

}
