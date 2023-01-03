package pl.edu.pw.spdb.dal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pl.edu.pw.spdb.model.Point;

import static org.springframework.test.util.AssertionErrors.assertEquals;

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
        Point p = new Point(y,x);

        // when
        Integer nearestStartId = service.getNearestStartId(p);

        // then
        assertEquals("Found different start point",530709, nearestStartId);
    }
}
