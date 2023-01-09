package pl.edu.pw.spdb.dal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pl.edu.pw.spdb.model.Point;
import pl.edu.pw.spdb.model.Route;

import static org.springframework.test.util.AssertionErrors.*;

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
        Integer nearestStartId = service.getStartOrEnd(p, true);

        // then
        assertEquals("Found different start point", 530709, nearestStartId);
    }

    @Test
    public void findNearestEndId() {
        // given
        int x = 20;
        int y = 48;
        Point p = new Point(y, x);

        // when
        Integer nearestEndId = service.getStartOrEnd(p, false);

        // then
        assertEquals("Found different end point", 3130606, nearestEndId);
    }

    @Test
    public void findRoute() {
        // given
        Integer startId = 1457211;
        Integer endId = 3263579;
        int velocity = 200;
        float w = 1;

        // whem
        Route r = service.findRoute(startId, endId, velocity, w);

        // then
        assertNotNull("Route should not be null", r);
        assertNotNull("Route should contain not empty segment list", r.segments());

        assertEquals("First segment should start with start point Id", r.segments().get(0).source(), (long) startId);
        assertEquals("Last segment should end with end point Id", r.segments().get(r.segments().size() - 1).target(), (long) endId);
    }
}
