package pl.edu.pw.spdb.model;

import java.math.BigInteger;

import org.postgis.*;

public record RouteSegment(long id, Object geom, long source, long target, double length,
                           double maxSpeedForward, double x1, double y1, double x2, double y2) {
}
