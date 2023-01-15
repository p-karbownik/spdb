package pl.edu.pw.spdb.service;

import com.sothawo.mapjfx.Coordinate;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import pl.edu.pw.spdb.dal.DatabaseService;
import pl.edu.pw.spdb.model.Point;
import pl.edu.pw.spdb.model.Route;

/***
 * Klasa z zaimplementowaną logiką wyszukiwania najlepszej drogi
 */
@Service
public class SearchPathService {

    private final DatabaseService databaseService;

    public SearchPathService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /***
     * Metoda wyszukująca drogę
     * @param startPointCoordinate - obiekt reprezentujący współrzędne punktu początkowego
     * @param endPointCoordinate - obiekt reprezentujący współrzędne punktu końcowego
     * @param maxSpeed - maksymalna prędkość
     * @param costParameter - parametr kosztu
     * @return Obiekt reprezentujący wyszukaną drogę
     */
    public Route findRoute(@NotNull Coordinate startPointCoordinate, Coordinate endPointCoordinate,
                           int maxSpeed, double costParameter) throws RuntimeException {
        Point startPoint = new Point(startPointCoordinate.getLatitude(), startPointCoordinate.getLongitude());
        Point endPoint = new Point(endPointCoordinate.getLatitude(), endPointCoordinate.getLongitude());

        long startPointId = databaseService.getStartOrEnd(startPoint, true);
        long endPointId = databaseService.getStartOrEnd(endPoint, false);

        return databaseService.findRoute(startPointId, endPointId, maxSpeed, (float) costParameter);
    }
}
