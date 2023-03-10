package pl.edu.pw.spdb.dal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pl.edu.pw.spdb.model.Point;
import pl.edu.pw.spdb.model.Route;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class DatabaseServiceImplTest {

    @Autowired
    private DatabaseService service;

    @Test
    public void findNearestStartId() {
        // given
        int x = 17;
        int y = 50;
        Point p = new Point(y, x);

        // when
        Long nearestStartId = service.getStartOrEnd(p, true);

        // then
        assertEquals("Found different start point", 813149L, nearestStartId);
    }

    @Test
    public void findNearestEndId() {
        // given
        int x = 20;
        int y = 48;
        Point p = new Point(y, x);

        // when
        Long nearestEndId = service.getStartOrEnd(p, false);

        // then
        assertEquals("Found different end point", 1834415L, nearestEndId);
    }

    @Test
    public void findRoute() {
        // given
        long startId = 1457211;
        long endId = 3263579;
        int velocity = 200;
        float w = 1;

        // whem
        Route r = service.findRoute(startId, endId, velocity, w);

        // then
        assertNotNull("Route should not be null", r);
        assertNotNull("Route should contain not empty segment list", r.getSegments());

        assertEquals("Distance should be around 551km", 316, (int) r.getDistance());
        assertEquals("Time should be around 7h", 4, (int) r.getEstimatedTime());
    }
}
